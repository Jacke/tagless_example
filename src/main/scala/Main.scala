import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import app.config.AppConfig
import app.config.AppConfig.DbConfig
import app.http.router.{NewsRoute, RestApi, VersionRoute}
import app.repository.NewsRepository
import app.service.NewsService
import app.util.{DefaultFlyway, Marshallable}

import scala.concurrent.duration._
import scala.concurrent.Await
import com.typesafe.conductr.bundlelib.scala.{Env, StatusService}
import com.typesafe.conductr.lib.scala.ConnectionContext
import doobie.hikari.HikariTransactor
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
object Main extends IOApp {

  private def actorSystemResource[F[_]](
      log: SelfAwareStructuredLogger[F]
  )(implicit F: Effect[F]): Resource[F, ActorSystem] =
    Resource.make {
      for {
        system <- F.delay(ActorSystem("system"))
        _ <- log.info(
          s"Starting application default Akka system: ${system.name}")
        _ <- F.delay {
          CoordinatedShutdown(system).addJvmShutdownHook {
            val hook = for {
              _ <- log.info(s"Terminating akka system: ${system.name}")
              _ <- F.delay(Await.result(system.whenTerminated, 30.seconds))
              _ <- log.info(s"Akka system terminated: ${system.name}")
            } yield ()
            F.toIO(hook).unsafeRunSync()
          }
        }
      } yield system
    }(s => F.delay(s.terminate()))

  private def transactor[F[_]](config: DbConfig)(
      implicit F: Async[F]): F[HikariTransactor[F]] = {
    import doobie.hikari._
    HikariTransactor.newHikariTransactor[F](config.driverClassName,
                                            config.url,
                                            config.user,
                                            config.pass)
  }

  private def migrate[F[_]](
      appConfig: AppConfig,
      log: SelfAwareStructuredLogger[F])(implicit F: Sync[F]): F[Unit] =
    if (appConfig.flyway.enableMigrations) for {
      flyway <- DefaultFlyway.create[F](appConfig)
      migrate <- flyway.migrate
    } yield migrate
    else log.info("Skip migration")

  private def akkaApp[E, F[_]](
      config: AppConfig,
      restApi: RestApi[F],
      log: SelfAwareStructuredLogger[F],
      s: ActorSystem
  )(implicit F: Sync[F]): F[Unit] =
    F.delay {
      implicit val system: ActorSystem = s
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      val bFuture =
        Http().bindAndHandle(restApi.route, config.http.host, config.http.port)
      bFuture.failed.foreach { ex =>
        log.error(ex)(
          s"Failed to bind to ${config.http.host} : ${config.http.port}")
      }(system.dispatcher)

      StatusService
        .signalStartedOrExit()(ConnectionContext(system.dispatcher))
        .foreach { _ =>
          if (Env.isRunByConductR) log.debug("Signalled start to ConductR")
          else log.debug("Cannot signalled start to ConductR")
        }(system.dispatcher)
    }

  private def restApi[F[_]: Marshallable: Clock](
      tr: HikariTransactor[F],
      cfg: AppConfig)(implicit F: Effect[F]) =
    for {
      ns <- NewsService(NewsRepository(), tr)
      vr = VersionRoute()
      nr <- NewsRoute.create(ns, cfg)
    } yield RestApi.create(nr, vr)

  private def program[F[_]: Marshallable: Clock](
      implicit F: Effect[F]): F[ExitCode] =
    for {
      log <- Slf4jLogger.create[F]
      code <- actorSystemResource(log).use { system =>
        for {
          cfg <- AppConfig.load
          _ <- migrate(cfg, log)
          tr <- transactor(cfg.db)
          api <- restApi(tr, cfg)
          _ <- akkaApp(cfg, api, log, system)
          _ <- log.info(
            s"News service online at: ${cfg.http.host}:${cfg.http.port}")
          _ <- F.never[ExitCode]
        } yield ExitCode.Success
      }
    } yield code

  override def run(args: List[String]): IO[ExitCode] = program[IO]
}
