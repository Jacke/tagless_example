package app.model
import cats.Id
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

@JsonCodec
case class News(
    id: String,
    title: String,
    greeting: String,
    contentText: String,
    signature: String,
    startView: String,
    endView: String,
    readRequired: Boolean,
    segments: List[Int],
    channels: List[String],
    inns: List[String],
    archive: Boolean,
    creationDate: String, // audit
    createdNews: String // audit
)
case class NewsOps[G[_]](
    title: G[String],
    greeting: G[String],
    contentText: G[String],
    signature: G[String],
    startView: G[String],
    endView: G[String],
    readRequired: G[Boolean],
    segments: G[List[Int]],
    channels: G[List[String]],
    inns: G[List[String]]
)
object NewsOps {
  implicit val newsOpsIdEncoder: Encoder[NewsOps[Id]] = deriveEncoder
  implicit val newsOpsIdDecoder: Decoder[NewsOps[Id]] = deriveDecoder

  implicit val newsOpsOptEncoder: Encoder[NewsOps[Option]] = deriveEncoder
  implicit val newsOpsOptDecoder: Decoder[NewsOps[Option]] = deriveDecoder
}
