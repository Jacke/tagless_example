package app.util

import app.config.AppConfig
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._

import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.flywaydb.core.Flyway

class DefaultFlyway[F[_]] private (
    flyway: Flyway,
    log: SelfAwareStructuredLogger[F])(implicit F: Sync[F]) {

  def clean: F[Unit] =
    for {
      _ <- log.info("Clean database: start")
      _ <- F.delay(flyway.clean())
      _ <- log.info("Clean database: success")
    } yield ()

  def migrate: F[Unit] =
    for {
      _ <- log.info("Migrate database: start")
      _ <- F.delay(flyway.migrate())
      _ <- log.info("Migrate database: success")
    } yield ()

  def refresh: F[Unit] =
    for {
      _ <- clean
      _ <- migrate
    } yield ()
}

object DefaultFlyway {

  def create[F[_]](appConfig: AppConfig)(
      implicit F: Sync[F]): F[DefaultFlyway[F]] =
    for {
      log <- Slf4jLogger.create[F]
      flyway <- F.delay {
        val flyway = new Flyway()
        val db = appConfig.db
        flyway.setDataSource(db.url, db.user, db.pass)
        flyway.setLocations(appConfig.flyway.locations: _*)
        new DefaultFlyway[F](flyway, log)
      }
    } yield flyway

}
