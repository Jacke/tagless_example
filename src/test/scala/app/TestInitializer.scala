package app
import app.config.AppConfig
import app.config.AppConfig.DbConfig
import app.http.router.{NewsRoute, RestApi, VersionRoute}
import app.repository.NewsRepository
import app.service.NewsService
import app.util.Marshallable
import cats.effect.{Async, Clock, Effect, Sync}
import doobie.util.transactor.Transactor
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect._
import doobie.hikari.HikariTransactor
case class TestInitializer[F[_] : Marshallable : Effect, G[_]] private (
  cfg: AppConfig,
  tr: Transactor[F],
  rApi: RestApi[F],
)

object TestInitializer {

  private def transactor[F[_]](config: DbConfig)(implicit F: Async[F], shift: ContextShift[F]): Resource[F, HikariTransactor[F]] = {
    import doobie.hikari._
    import doobie.util.{ExecutionContexts}

    for {
      be <- Blocker[F]    // our blocking EC
      xa <- HikariTransactor.newHikariTransactor[F](
        config.driverClassName,
        config.url,
        config.user,
        config.pass,
        ExecutionContexts.synchronous,
        be
      )
    } yield xa
  }

  private def restApi[F[_] : Marshallable : Clock](tr: HikariTransactor[F], cfg: AppConfig)(
    implicit F: Effect[F]
  ): F[RestApi[F]] =
    for {
      ns <- NewsService(NewsRepository(), tr)
      vr = VersionRoute()
      nr <- NewsRoute.create(ns, cfg)
    } yield RestApi.create(nr, vr)

  def init[F[_] : Marshallable : Effect : Clock : ContextShift] =
    for {
      cfg <- AppConfig.load
      app <- transactor(cfg.db).use { case (tr) =>
        for {
          rApi <- restApi(tr, cfg)
        } yield TestInitializer(cfg, tr, rApi)
      }
    } yield app
}
