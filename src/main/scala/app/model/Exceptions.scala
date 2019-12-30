package app.model
import cats.data.{NonEmptyChain, NonEmptyList}
import io.circe.generic.JsonCodec

import scala.util.control.NoStackTrace

case class BadLoginException(x: String) extends Exception(x) with NoStackTrace
case class BadUUIDException(x: String) extends Exception(x) with NoStackTrace
case class NewsDoesNotExistsException(x: String)
    extends Exception(x)
    with NoStackTrace
case class SomethingWrongException(x: String)
    extends Exception(x)
    with NoStackTrace
case class CannotModifyTableException(x: String)
    extends Exception(x)
    with NoStackTrace
case class UpdateNewsQueryIsEmpty(x: String)
    extends Exception(x)
    with NoStackTrace
case class MoreThat4000ValidatorException(x: String)
    extends Exception(x)
    with NoStackTrace
case class MoreThat255ValidatorException(x: String)
    extends Exception(x)
    with NoStackTrace
case class DateFormatValidatorException(x: String)
    extends Exception(x)
    with NoStackTrace
case class SegmentFormatValidatorException(x: String)
    extends Exception(x)
    with NoStackTrace
case class ChannelsFormatValidatorException(x: String)
    extends Exception(x)
    with NoStackTrace
case class InnsFormatValidatorException(x: String)
    extends Exception(x)
    with NoStackTrace
case class BetweenStartAndEndDateException(x: String)
    extends Exception(x)
    with NoStackTrace
case class NumberOfCharactersException(x: String)
    extends Exception(x)
    with NoStackTrace

case class ValidatorErrorsException private (x: List[String])
    extends Exception
    with NoStackTrace
object ValidatorErrorsException {
  def apply(x: NonEmptyList[Exception]): ValidatorErrorsException =
    new ValidatorErrorsException(x.toList.map(e => e.getMessage))
}

@JsonCodec
case class HandlerModel[A](body: A, status: Int, reason: String)

@JsonCodec
case class ErrorHandlerModel[A](body: A, status: Int, reason: String)


package object error {
  type Nec[+A] = NonEmptyChain[A]
  val Nec = NonEmptyChain
  type Errors = Nec[Exception]
}
