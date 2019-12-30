package app.repository

import java.util.UUID

import app.model._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragment.Fragment
import doobie.Fragments._
import cats.{Id, Show}

class NewsRepository {
  def createNews(cn: NewsOps[Id], generatedNewsId: UUID) =
    sql"""
        insert into news(id, title, greeting, content_text, signature,
                         start_view, end_view, read_required, segments,
                         channels, inns)
             values ($generatedNewsId, ${cn.title}, ${cn.greeting}, ${cn.contentText}, ${cn.signature},
                     (${cn.startView})::timestamp, (${cn.endView})::timestamp, ${cn.readRequired}, ${cn.segments},
                     ${cn.channels}, ${cn.inns});
       """

  def archiveNews(newsId: UUID) =
    sql"""update news set archive = true where id = $newsId"""

  def audit[A <: Action: Show](userId: String, newsId: UUID, x: A): Fragment =
    sql"""
        insert into audit(news_id, user_id, action) values ($newsId, $userId, (${Show[
      A].show(x)}::audit_action))
       """

  def updateNews(un: NewsOps[Option], newsId: UUID): Fragment = {
    import app.util.doobie.SetEncoder.instance._
    import app.util.doobie.SetEncoder.ops._
    fr"update news " ++ set(maybeSet(un): _*) ++ fr" where id = $newsId"
  }

  def getNews: ConnectionIO[List[News]] =
    sql"""select n.id,
                 n.title,
                 n.greeting,
                 n.content_text,
                 n.signature,
                 to_char(n.start_view, 'yyyy-mm-dd"T"HH24:MI:SS'),
                 to_char(n.end_view, 'yyyy-mm-dd"T"HH24:MI:SS'),
                 n.read_required,
                 n.segments,
                 n.channels,
                 n.inns,
                 n.archive,
                 to_char(a.date, 'yyyy-mm-dd"T"HH24:MI:SS'), a.user_id
          from news n left join audit a on n.id = a.news_id where a.action = 'create' """
      .query[News]
      .to[List]

  def newsExists(newsId: UUID): doobie.ConnectionIO[Boolean] =
    sql"""
         select exists(select 1 from news where id = $newsId);
       """.query[Boolean].unique

}
object NewsRepository {
  def apply(): NewsRepository = new NewsRepository()
}
