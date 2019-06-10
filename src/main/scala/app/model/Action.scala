package app.model
import cats.Show

sealed trait Action
object Action {
  object Create extends Action {
    implicit val createdShowInstance: Show[Create.type] =
      _ => "create"
  }

  object Updated extends Action {
    implicit val updatedShowInstance: Show[Updated.type] =
      _ => "update"
  }

  object Archived extends Action {
    implicit val archivedShowInstance: Show[Archived.type] =
      _ => "archive"
  }
}
