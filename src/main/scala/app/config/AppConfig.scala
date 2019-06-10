package app.config

import AppConfig._
import cats.effect.Sync
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}
import pureconfig.module.catseffect._

case class AppConfig(
    db: DbConfig,
    http: HttpConfig,
    flyway: FlywayConfig,
    validation: ValidationConfig
)

object AppConfig {

  private implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def load[F[_]: Sync]: F[AppConfig] = loadConfigF[F, AppConfig]("app")

  case class DbConfig(driverClassName: String,
                      url: String,
                      user: String,
                      pass: String)

  case class FlywayConfig(enableMigrations: Boolean, locations: Vector[String])

  case class HttpConfig(host: String, port: Int)
  case class ValidationConfig(segments: List[Int], channels: List[String])

  case class JwtConfig(issuer: String, clientId: String)
  case class JwkConfig(maxSize: Int,
                       connectionTimeout: Int,
                       readTimeout: Int,
                       url: String)

}
