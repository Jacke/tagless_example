package app
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import app.model.ErrorHandlerModel
import cats.effect.IO
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import akka.http.scaladsl.model.headers.RawHeader
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

class RestApiTest extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  implicit val clock = IO.timer(system.dispatcher).clock
  private val initializer = TestInitializer.init[IO].unsafeRunSync()

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  "RestApi" must {

    "Return status 500 into ErrorHandlerModel when we try archive news without \"x-forwarded-user\" header (nginx)" in {
      Put("/news/archive?newsId=c6fa2d12-b2a9-4e6a-911e-2c839f3a8bac") ~> Route.seal(initializer.rApi.route) ~> check {
        status shouldBe StatusCode.int2StatusCode(400)
        responseAs[ErrorHandlerModel[None.type]] shouldBe ErrorHandlerModel(None, 400, "missing-login-header")
      }
    }

    "Return status 500 into ErrorHandlerModel when we try archive or update news with bad newsId format" in {
      Put("/news/archive?newsId=ururu").addHeader(RawHeader("X-Forwarded-User", "Basic bG9naW46cGFzc3dvcmQ=")) ~> Route
        .seal(initializer.rApi.route) ~> check {
        status shouldBe StatusCode.int2StatusCode(400)
        responseAs[ErrorHandlerModel[None.type]] shouldBe ErrorHandlerModel(None, 400, "invalid-news-id")
      }
    }

    "Return status 400 into ErrorHandlerModel when we try create or update news without body parameters" in {
      Post("/news?newsId=c6fa2d12-b2a9-4e6a-911e-2c839f3a8bac") ~> Route.seal(initializer.rApi.route) ~> check {
        status shouldBe StatusCode.int2StatusCode(400)
        responseAs[ErrorHandlerModel[None.type]] shouldBe ErrorHandlerModel(None, 400, "missing-query-param")
      }

      Put("/news?newsId=c6fa2d12-b2a9-4e6a-911e-2c839f3a8bac") ~> Route.seal(initializer.rApi.route) ~> check {
        status shouldBe StatusCode.int2StatusCode(400)
        responseAs[ErrorHandlerModel[None.type]] shouldBe ErrorHandlerModel(None, 400, "missing-query-param")
      }
    }

  }
}
