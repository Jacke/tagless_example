package app

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import app.model.NewsOps
import cats.Id
import cats.effect.IO
import doobie.util.fragment.Fragment
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import cats.effect._
import scala.concurrent.ExecutionContext


class ShapelessUtilTest extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {
  implicit val clock = IO.timer(system.dispatcher).clock
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  private val initializer = TestInitializer.init[IO].unsafeRunSync()

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  "SetEncoder" must {
    "Fold any product in the List(stdLib) of Fragments(doobie)" in {
      import app.util.doobie.SetEncoder.instance._
      import app.util.doobie.SetEncoder.ops._
      val noIdList = List(
        Fragment.const("title = 'SomeTitle'"),
        Fragment.const("greeting = 'SomeGreeting'"),
        Fragment.const("content_text = 'SomeContentText'"),
        Fragment.const("signature = 'SomeSignature'"),
        Fragment.const("start_view = '21-11-1990T00:05:00'"),
        Fragment.const("end_view = '21-11-1990T00:06:00'"),
        Fragment.const("read_required = true"),
        Fragment.const("segments = '{1,2,3,4,5}'"),
        Fragment.const("channels = '{1,2,3,4,5}'"),
        Fragment.const("inns = '{1,2,3,4,5}'")
      )
      val noId = NewsOps[Id](
        "SomeTitle",
        "SomeGreeting",
        "SomeContentText",
        "SomeSignature",
        "21-11-1990T00:05:00",
        "21-11-1990T00:06:00",
        true,
        List(1, 2, 3, 4, 5),
        List("1", "2", "3", "4", "5"),
        List("1", "2", "3", "4", "5")
      )

      val noOpt =
        NewsOps[Option](Some("SomeTitle"), Some("SomeGreeting"), None, None, None, None, Some(false), None, None, None)
      val noOptList = List(
        Fragment.const("title = 'SomeTitle'"),
        Fragment.const("greeting = 'SomeGreeting'"),
        Fragment.const("read_required = false")
      )

      maybeSet(noId).forall { x =>
        noIdList.exists(y => y.toString === x.toString)
      } shouldBe true

      maybeSet(noOpt).forall { x =>
        noOptList.exists(y => y.toString === x.toString)
      } shouldBe true
    }
  }
  "AdtEmptyChecker" must {
    "return true when adt do not contain at least one 'Some' value" in {
      import app.util.AdtEmptyChecker.ops._
      import app.util.AdtEmptyChecker.instances._
      val trueAdt = NewsOps[Option](Some("SomeTitle"), None, None, None, None, None, None, None, None, None)
      val falseAdt = NewsOps[Option](None, None, None, None, None, None, None, None, None, None)
      true shouldBe isEmpty(falseAdt)
      false shouldBe isEmpty(trueAdt)
    }
  }
}
