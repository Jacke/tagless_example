package app.http.router

import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import akka.http.scaladsl.server.RouteConcatenation._

class RestApi[F[_]] private (nr: NewsRoute[F], vr: VersionRoute) {
  def route = cors() { nr.route ~ vr.route }
}
object RestApi {
  def create[F[_]](nr: NewsRoute[F], vr: VersionRoute): RestApi[F] =
    new RestApi(nr, vr)
}
