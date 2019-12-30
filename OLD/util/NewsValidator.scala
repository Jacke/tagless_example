package app.util

import java.time.{LocalDate, LocalDateTime}

import app.config.AppConfig
import app.model._
import app.model.error._
import cats.data.Kleisli
import cats.effect.Sync
import cats.mtl.{ApplicativeAsk, FunctorRaise}
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.temp.par.Par
import cats.{Monad, Semigroupal, Traverse, ~>}
import cats.effect.Clock

import scala.Function.tupled
import scala.concurrent.duration.DAYS
abstract class NewsValidator[F[_]: Monad](implicit P: Par[F]) {

  def title(x: String): F[String]
  def greeting(x: String): F[String]
  def contentText(x: String): F[String]
  def signature(x: String): F[String]
  def date(start: String, end: String): F[Unit]
  def segments(x: List[Int]): F[List[Int]]
  def channels(x: List[String]): F[List[String]]
  def inns(x: List[String]): F[List[String]]

  def validate[G[_]: Traverse: Semigroupal](ops: NewsOps[G]): F[NewsOps[G]] = {
    import cats.temp.par._
    val ttl = ops.title.parTraverse(title)
    val gtg = ops.greeting.parTraverse(greeting)
    val ctx = ops.contentText.parTraverse(contentText)
    val sig = ops.signature.parTraverse(signature)
    val sts = ops.segments.parTraverse(segments)
    val cls = ops.channels.parTraverse(channels)
    val is = ops.inns.parTraverse(inns)
    val dte = (ops.startView, ops.endView).tupled.parTraverse(tupled(date))

    (ttl, gtg, ctx, sig, sts, cls, is).parMapN {
      (ttl, gtg, ctx, sig, sts, cls, is) =>
        NewsOps[G](ttl,
                   gtg,
                   ctx,
                   sig,
                   ops.startView,
                   ops.endView,
                   ops.readRequired,
                   sts,
                   cls,
                   is)
    } <& dte

  }
}
object NewsValidator {
  def apply[E,
            F[_]: Par: FunctorRaise[?[_], E]: Sync: Clock: ApplicativeAsk[
              ?[_],
              AppConfig]](
      implicit V: NewsValidator[F]
  ): NewsValidator[F] = V

  implicit class ValidatorOps[H[_]](private val ops: NewsOps[H])
      extends AnyVal {
    def validateTransform[
        F[_],
        G[_]: Par: FunctorRaise[?[_], Errors]: Sync: Clock: ApplicativeAsk[
          ?[_],
          AppConfig]](f2g: G ~> F)(implicit G: NewsValidator[G],
                                   T: Traverse[H],
                                   S: Semigroupal[H]): F[NewsOps[H]] =
      Kleisli(G.validate[H]).mapK(f2g).run(ops)
  }

  implicit def newsOpsInstance[F[_]: Par](
      implicit F: FunctorRaise[F, Errors],
      S: Sync[F],
      clock: Clock[F],
      A: ApplicativeAsk[F, AppConfig]
  ): NewsValidator[F] =
    new NewsValidator[F]() {
      import cats.temp.par._

      override def title(x: String): F[String] =
        lengthCheck(x, 255, "title-max-length-is-exceeded")

      override def greeting(x: String): F[String] =
        lengthCheck(x, 255, "greeting-max-length-is-exceeded")

      override def contentText(x: String): F[String] =
        lengthCheck(x, 4000, "content-max-length-is-exceeded")

      override def signature(x: String): F[String] =
        lengthCheck(x, 255, "signature-max-length-is-exceeded")

      override def segments(x: List[Int]): F[List[Int]] =
        A.ask >>= { cfg =>
          Either
            .cond(
              x.nonEmpty && x.forall(cfg.validation.segments.contains),
              x,
              SegmentFormatValidatorException(s"segments-incorrect-format")
            )
            .leftMap(Nec.one)
            .fold(F.raise, S.pure)
        }

      override def inns(x: List[String]): F[List[String]] = {
        val rCond = x.nonEmpty && x.forall(
          _.matches("""^(\d{12}|\d{10}|\d{5})$"""))
        val anyCond = isAny(x)
        Either
          .cond(rCond || anyCond,
                x,
                InnsFormatValidatorException(s"inns-incorrect-format"))
          .leftMap(Nec.one)
          .fold(F.raise, S.pure)
      }

      override def channels(x: List[String]): F[List[String]] = {
        val anyCond = isAny(x)
        def allowCond(allow: List[String]) =
          x.nonEmpty && x.forall(v => allow.exists(y => y.equalsIgnoreCase(v)))
        A.ask >>= { cfg =>
          Either
            .cond(
              anyCond || allowCond(cfg.validation.channels),
              x,
              ChannelsFormatValidatorException(s"channels-incorrect-format")
            )
            .leftMap(Nec.one)
            .fold(F.raise, S.pure)
        }
      }

      override def date(start: String, end: String): F[Unit] =
        for {
          t <- (dateFormat(start, "incorrect-start-date-format"),
                dateFormat(end, "incorrect-end-date-format")).parTupled
          n <- clock.realTime(DAYS)
          _ <- dateRange(t._1, t._2, n)
        } yield ()

      private def dateRange(start: LocalDateTime,
                            end: LocalDateTime,
                            now: Long): F[Unit] =
        if (start.isAfter(end) || start.isEqual(end) || start.toLocalDate
              .isBefore(LocalDate.ofEpochDay(now)))
          F.raise(
            Nec.one(BetweenStartAndEndDateException(s"incorrect-date-range")))
        else S.unit

      private def dateFormat(raw: String, msg: String): F[LocalDateTime] =
        Either
          .catchNonFatal(LocalDateTime.parse(raw))
          .leftMap(_ => Nec.one(DateFormatValidatorException(msg)))
          .fold(F.raise, S.pure)

      private def lengthCheck(raw: String,
                              limit: Int,
                              elseMsg: String): F[String] =
        Either
          .cond(raw.length <= limit, raw, NumberOfCharactersException(elseMsg))
          .leftMap(Nec.one)
          .fold(F.raise, S.pure)

      private def isAny(x: List[String]): Boolean =
        x match { case List(e) => e.equalsIgnoreCase("any"); case _ => false }

    }

}
