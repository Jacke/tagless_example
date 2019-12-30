package app.http.router

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import app.service.NewsService
import akka.http.scaladsl.server.Directives._
import app.model._
import app.util.Marshallable
import com.google.common.io.BaseEncoding
import io.circe.syntax._
import io.circe.Json
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import NewsOps._
import app.config.AppConfig
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import app.util.NewsValidator._
import app.model.error.Errors

import cats.syntax.functor._
import cats.{Id, ~>}
import cats.data.{EitherT, ReaderT}
import cats.effect.{Clock, Effect}
import cats.data.Kleisli._
import cats.syntax.flatMap._
import cats.instances.parallel._
import cats.instances.option._
import cats.temp.par.Par._
import cats.mtl.instances.readert._
import cats.mtl.instances.local._
import cats.mtl.instances.handle._
import cats.syntax.monadError._
class NewsRoute[F[_]: Marshallable: Clock] private (
    ns: NewsService[F],
    log: SelfAwareStructuredLogger[F],
    cfg: AppConfig
)(implicit F: Effect[F])
    extends NewsHandler(log) {

  import app.util.Marshallable.marshaller

  private val g2f: ReaderT[EitherT[F, Errors, ?], AppConfig, ?] ~> F = {
    λ[ReaderT[EitherT[F, Errors, ?], AppConfig, ?] ~> F](
      _.run(cfg).valueOrF(err =>
        F.raiseError(ValidatorErrorsException(err.toNonEmptyList)))
    )
  }

  val route: Route = pathPrefix("news") {
    handler {
      updateNews ~ createNews ~ getNews ~ archiveNews
    }
  }
  private val updNewsEntity = {
    import app.util.AdtEmptyChecker.instances._
    import app.util.AdtEmptyChecker.ops._
    entity(as[NewsOps[Option]]).map { x =>
      F.ensure(F.pure(x))(UpdateNewsQueryIsEmpty("all-fields-is-null"))(x =>
        !isEmpty(x))
    }
  }

  private val login =
    headerValueByName("X-Forwarded-User").map { x =>
      F.catchNonFatal {
          BaseEncoding
            .base64()
            .decode(x.replace("Basic ", ""))
            .map(_.toChar)
            .mkString
            .takeWhile(_ != ':')
        }
        .adaptError {
          case _: IllegalArgumentException =>
            BadLoginException("Unexpected x-forwarded-* header.")
        }

    }

  private val uuidParameters =
    parameter('newsId).map { x =>
      F.catchNonFatal(UUID.fromString(x)).adaptError {
        case _: IllegalArgumentException =>
          BadLoginException("Unexpected x-forwarded-* header.")
      }
    }

  private def getNews: Route =
    get {
      complete(ns.getNews.map(_.asJson))
    }

  private def createNews: Route =
    post {
      entity(as[NewsOps[Id]]) { cn: NewsOps[Id] =>
        //login { loginF =>
        val query = for {
          //uname <- loginF
          vcn <- cn.validateTransform(g2f)
          _ <- F.ensure(ns.createNews(vcn, "uname"))(
            CannotModifyTableException(s"Create -> uname, $vcn"))(_ == 1)
        } yield HandlerModel[Json](vcn.asJson, 200, "news-created")
        complete(query.map(_.asJson))
        //}
      }
    }

  private def updateNews: Route =
    put {
      updNewsEntity { unF: F[NewsOps[Option]] =>
        (uuidParameters & login) { (newsIdF, loginF) =>
          val query = for {
            newsId <- newsIdF
            uname <- loginF
            un <- unF >>= { _.validateTransform(g2f) }
            _ <- F.ensure(ns.newsExists(newsId))(
              NewsDoesNotExistsException(s"$newsId"))(identity)
            _ <- F.ensure(ns.updateNews(newsId, un, uname))(
              CannotModifyTableException(s"Update -> $newsId, $un, $uname")
            )(_ == 1)
          } yield StatusCodes.OK
          complete(query)
        }
      }
    }

  private def archiveNews: Route =
    path("archive") {
      put {
        (uuidParameters & login) { (newsIdF, loginF) =>
          val query = for {
            newsId <- newsIdF
            login <- loginF
            _ <- F.ensure(ns.newsExists(newsId))(
              NewsDoesNotExistsException(s"$newsId"))(identity)
            _ <- F.ensure(ns.archiveNews(newsId, login))(
              CannotModifyTableException(s"Archive -> $newsId, $login"))(
              _ == 1
            )
          } yield StatusCodes.OK
          complete(query)
        }
      }
    }
}
object NewsRoute {

  def create[F[_]: Marshallable: Clock](ns: NewsService[F], cfg: AppConfig)(
      implicit F: Effect[F]): F[NewsRoute[F]] =
    Slf4jLogger.create[F].map(new NewsRoute[F](ns, _, cfg))
}
