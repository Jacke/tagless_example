package app.http.router

import akka.http.scaladsl.server.Directives.{complete, get, pathPrefix}
import akka.http.scaladsl.server.Route
import io.circe.parser.decode
import buildInfo.BuildInfo
import io.circe.parser._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
class VersionRoute {

  private def version: Route = get {
    complete(decode[Map[String, String]](BuildInfo.toJson))
  }

  val route: Route = pathPrefix("version") {
    version
  }

}
object VersionRoute {
  def apply(): VersionRoute = new VersionRoute()
}
