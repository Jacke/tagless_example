package app
import java.time.LocalDateTime

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import app.config.AppConfig
import app.model.ValidatorErrorsException
import app.model.error.Errors
import app.util.NewsValidator
import cats.data.EitherT
import cats.effect.IO
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import cats.syntax.functor._
import cats.{Id, ~>}
import cats.data.{EitherT, ReaderT}
import cats.effect.{Clock, Effect}
import cats.data.Kleisli._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import cats.instances.parallel._
import cats.instances.option._
import cats.temp.par.Par._
import cats.mtl.instances.readert._
import cats.mtl.instances.local._
import cats.mtl.instances.handle._

class NewsValidatorTest extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {
  implicit val clock = IO.timer(system.dispatcher).clock
  private val init = TestInitializer.init[IO].unsafeRunSync()

  private val validatorInstance = NewsValidator.apply[Errors, ReaderT[EitherT[IO, Errors, ?], AppConfig, ?]]

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  "NewsValidator " must {
    "check the title for a maximum of 255 characters" in {
      validatorInstance
        .title((0 until 255).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isRight shouldBe true
      validatorInstance
        .title((0 until 256).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isLeft shouldBe true
    }

    "check the greeting for a maximum of 255 characters" in {
      validatorInstance
        .greeting((0 until 255).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isRight shouldBe true
      validatorInstance
        .greeting((0 until 256).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isLeft shouldBe true
    }

    "check the signature for a maximum of 255 characters" in {
      validatorInstance
        .signature((0 until 255).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isRight shouldBe true
      validatorInstance
        .signature((0 until 256).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isLeft shouldBe true
    }

    "check the contentText for a maximum of 4000 characters" in {
      validatorInstance
        .contentText((0 until 4000).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isRight shouldBe true
      validatorInstance
        .contentText((0 until 4001).map(_ => "A").mkString)
        .run(init.cfg)
        .value
        .unsafeRunSync()
        .isLeft shouldBe true
    }

    "check that channels contains only allowed values" in {
      val cls0 = "IOS" :: "liTe" :: Nil
      val cls1 = "ANY" :: Nil
      val cls2 = "androiD" :: " pro " :: Nil
      val cls3 = "androiD" :: "PRO" :: "ANy" :: Nil
      val cls4 = Nil
      validatorInstance.channels(cls0).run(init.cfg).value.unsafeRunSync().isRight shouldBe true
      validatorInstance.channels(cls1).run(init.cfg).value.unsafeRunSync().isRight shouldBe true
      validatorInstance.channels(cls2).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.channels(cls3).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.channels(cls4).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
    }

    "check that inns contains only allowed values" in {
      val inns0 = "ANY" :: Nil
      val inns1 = "1234567890" :: "123456789011" :: Nil
      val inns2 = "1234567890" :: "123456789011" :: "ANY" :: Nil
      val inns3 = "1234567890" :: " 123456789011 " :: Nil
      val inns4 = "1234567890" :: "as3456789011" :: Nil
      val inns5 = "asdfghjkl;" :: Nil
      val inns6 = "12345678901" :: Nil
      validatorInstance.inns(inns0).run(init.cfg).value.unsafeRunSync().isRight shouldBe true
      validatorInstance.inns(inns1).run(init.cfg).value.unsafeRunSync().isRight shouldBe true
      validatorInstance.inns(inns2).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.inns(inns3).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.inns(inns4).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.inns(inns5).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.inns(inns6).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
    }
    "check that segments contains only allowed values" in {
      val smts0 = 1 :: 2 :: 3 :: 4 :: 0 :: Nil
      val smts1 = 0 :: Nil
      val smts2 = Nil
      val smts3 = 5 :: Nil
      validatorInstance.segments(smts0).run(init.cfg).value.unsafeRunSync().isRight shouldBe true
      validatorInstance.segments(smts1).run(init.cfg).value.unsafeRunSync().isRight shouldBe true
      validatorInstance.segments(smts2).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.segments(smts3).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
    }
    "verify that the date format is correct" in {
      val (frm0, to0) = "21-11-1990T00:05:00" -> "28-11-2018T16:26:00"
      val frm1, to1 = LocalDateTime.now().toString
      val (frm2, to2) = LocalDateTime.now().toString -> LocalDateTime.now().plusDays(1).toString
      val (frm3, to3) = "21-11-1990МЯЧ00:05:00" -> "28-11-2018T16:26:00"
      val (frm4, to4) = "21-11-1990T00:05:00" -> "28-11-2018МЯЧ16:26:00"
      validatorInstance.date(frm0, to0).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.date(frm1, to1).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.date(frm2, to2).run(init.cfg).value.unsafeRunSync().isRight shouldBe true
      validatorInstance.date(frm3, to3).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
      validatorInstance.date(frm4, to4).run(init.cfg).value.unsafeRunSync().isLeft shouldBe true
    }
  }

}
