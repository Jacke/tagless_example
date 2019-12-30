package app.http.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import app.model._
import app.util.Marshallable
import cats.effect.Effect
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.syntax._
abstract class NewsHandler[F[_]: Marshallable](
    log: SelfAwareStructuredLogger[F])(implicit F: Effect[F]) {
  import app.util.Marshallable.marshaller

  private val baseHandler = ExceptionHandler {
    case x: BadUUIDException => {
      extractUri { uri =>
        val error = for {
          _ <- log.error(x)(s"ERROR => Endpoint: $uri")
          ehm <- F.pure(HandlerModel[None.type](None, 400, "invalid-news-id"))
        } yield ehm
        complete(error.map(StatusCodes.BadRequest -> _.asJson))
      }
    }

    case x: BadLoginException => {
      extractUri { uri =>
        val error = for {
          _ <- log.error(x)(s"ERROR => Endpoint: $uri")
          ehm <- F.pure(
            HandlerModel[None.type](None, 400, "invalid-login-header"))
        } yield ehm
        complete(error.map(StatusCodes.BadRequest -> _.asJson))
      }
    }
    case x @ NewsDoesNotExistsException(a) => {
      extractUri { uri =>
        val error = for {
          _ <- log.error(x)(s"ERROR => Endpoint: $uri")
          ehm <- F.pure(HandlerModel[String](a, 400, "news-does-not-exists"))
        } yield ehm
        complete(error.map(StatusCodes.BadRequest -> _.asJson))
      }
    }
    case x @ CannotModifyTableException(a) => {
      extractUri { uri =>
        val error = for {
          _ <- log.error(x)(s"ERROR => Endpoint: $uri")
          ehm <- F.pure(HandlerModel[None.type](None, 500, "something-wrong"))
        } yield ehm
        complete(error.map(StatusCodes.InternalServerError -> _.asJson))
      }
    }
    case x @ ValidatorErrorsException(a) => {
      extractUri { uri =>
        val error = for {
          _ <- log.error(x)(
            s"ERROR => Endpoint: $uri \n Caused by: \n${a.mkString("\n")}")
          ehm <- F.pure(HandlerModel[List[String]](a, 400, "validation-error"))
        } yield ehm
        complete(error.map(StatusCodes.BadRequest -> _.asJson))
      }
    }
    case UpdateNewsQueryIsEmpty(_) =>
      reject(akka.http.scaladsl.server.RequestEntityExpectedRejection)

    case x =>
      extractUri { uri =>
        val error = for {
          _ <- log.error(x)(s"ERROR => Endpoint: $uri \n")
          ehm <- F.pure(HandlerModel[None.type](None, 500, "something-wrong"))
        } yield ehm
        complete(error.map(StatusCodes.InternalServerError -> _.asJson))
      }
  }

  val rejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case MissingHeaderRejection(_) =>
        extractUri { uri =>
          val error = for {
            _ <- log.error(s"ERROR => Endpoint: $uri \nMissing login header\n")
            ehm <- F.pure(
              HandlerModel[None.type](None, 400, "missing-login-header"))
          } yield ehm
          complete(error.map(StatusCodes.BadRequest -> _.asJson))
        }
    }
    .handle {
      case akka.http.scaladsl.server.RequestEntityExpectedRejection =>
        complete(
          StatusCodes.BadRequest -> HandlerModel[None.type](
            None,
            400,
            "missing-query-param").asJson)
    }
    .handle {
      case a: Rejection =>
        extractUri { uri =>
          val msg = a match {
            case MalformedRequestContentRejection(msg, e) => msg
            case _                                        => ""
          }
          val error = for {
            _ <- log.error(
              s"ERROR => Endpoint: $uri \n ${msg} ${a.getClass.getCanonicalName}\n")
            ehm <- F.pure(HandlerModel[None.type](None, 500, msg))
          } yield ehm
          complete(error.map(StatusCodes.InternalServerError -> _.asJson))
        }
    }
    .result()

  protected def handler =
    handleRejections(rejectionHandler) & handleExceptions(baseHandler)
}
