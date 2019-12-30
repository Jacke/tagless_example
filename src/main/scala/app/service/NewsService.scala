package app.service

import java.util.UUID
import app.model.Action.Archived
import app.model.Action.Create
import app.model.Action.Updated
import app.model._
import app.repository.NewsRepository
import cats.effect.Sync
import doobie.implicits._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.Id
import cats.syntax.apply._
import doobie.hikari.HikariTransactor
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

class NewsService[F[_]] private (
    repository: NewsRepository,
    transactor: HikariTransactor[F],
    log: SelfAwareStructuredLogger[F]
)(implicit F: Sync[F]) {

  def newsExists(newsId: UUID): F[Boolean] =
    for {
      _ <- log.info(s"Сhecking news $newsId")
      res <- repository.newsExists(newsId).transact(transactor)
    } yield res

  def archiveNews(newsId: UUID, userId: String): F[Int] =
    for {
      _ <- log.info(s"Archive news $newsId")
      res <- (repository.archiveNews(newsId).update.run <*
        repository.audit(userId, newsId, Archived).update.run)
        .transact(transactor)
    } yield res

  def createNews(cn: NewsOps[Id], userId: String): F[Int] =
    for {
      _ <- log.info("Creating news")
      uuid <- F.delay(UUID.randomUUID())
      res <- (repository.createNews(cn, uuid).update.run <*
        repository.audit(userId, uuid, Create).update.run).transact(transactor)
    } yield res

  def updateNews(newsId: UUID, un: NewsOps[Option], userId: String): F[Int] =
    for {
      _ <- log.info(s"Updating news id: $un.id")
      res <- (repository.updateNews(un, newsId).update.run <*
        repository.audit(userId, newsId, Updated).update.run)
        .transact(transactor)

    } yield res

  def getNews: F[List[News]] =
    for {
      _ <- log.info(s"Get news")
      res <- repository.getNews.transact(transactor)
    } yield res
}
object NewsService {
  def apply[F[_]: Sync](repository: NewsRepository,
                        transactor: HikariTransactor[F]): F[NewsService[F]] =
    Slf4jLogger.create[F].map(new NewsService[F](repository, transactor, _))
}
