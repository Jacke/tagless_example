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
import doobie.hikari.HikariTransactor
case class TestInitializer[F[_] : Marshallable : Effect, G[_]] private (
  cfg: AppConfig,
  tr: Transactor[F],
  rApi: RestApi[F],
)

object TestInitializer {

  private def transactor[F[_]](config: DbConfig)(implicit F: Async[F]): F[HikariTransactor[F]] = {
    import doobie.hikari._
    HikariTransactor.newHikariTransactor[F](config.driverClassName, config.url, config.user, config.pass)
  }

  private def restApi[F[_] : Marshallable : Clock](tr: HikariTransactor[F], cfg: AppConfig)(
    implicit F: Effect[F]
  ): F[RestApi[F]] =
    for {
      ns <- NewsService(NewsRepository(), tr)
      vr = VersionRoute()
      nr <- NewsRoute.create(ns, cfg)
    } yield RestApi.create(nr, vr)

  def init[F[_] : Marshallable : Effect : Clock] =
    for {
      cfg <- AppConfig.load
      tr <- transactor(cfg.db)
      rApi <- restApi(tr, cfg)
    } yield TestInitializer(cfg, tr, rApi)
}
