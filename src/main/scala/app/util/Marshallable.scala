package app.util
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.HttpResponse
import cats.effect.IO

import scala.concurrent.Future
import scala.util.Try

trait Marshallable[F[_]] {
  def marshaller[A: ToResponseMarshaller]: ToResponseMarshaller[F[A]]
}

object Marshallable {

  implicit def marshaller[F[_], A: ToResponseMarshaller](
      implicit M: Marshallable[F]): ToResponseMarshaller[F[A]] =
    M.marshaller

  implicit val futureMarshallable: Marshallable[Future] =
    new Marshallable[Future] {
      def marshaller[A: ToResponseMarshaller]
        : Marshaller[Future[A], HttpResponse] = implicitly
    }

  implicit def ioMarshaller[A, B](
      implicit m: Marshaller[A, B]): Marshaller[IO[A], B] =
    Marshaller(implicit ec => _.unsafeToFuture().flatMap(m(_)))

  implicit val ioMarshallable: Marshallable[IO] = new Marshallable[IO] {
    def marshaller[A: ToResponseMarshaller]: Marshaller[IO[A], HttpResponse] =
      implicitly
  }

  implicit val tryMarshallable: Marshallable[Try] = new Marshallable[Try] {
    def marshaller[A: ToResponseMarshaller]: Marshaller[Try[A], HttpResponse] =
      implicitly
  }
}
