import java.time.ZonedDateTime

import sbt._

object Key {

  /*** settings ***/
  val buildNumber = settingKey[String]("Project current build version")
  val buildBranch = settingKey[String]("Git branch.")
  val projectVersion = settingKey[String]("Project current version")

  /*** tasks ***/
  val buildTime = taskKey[ZonedDateTime]("Time of this build")
  val refreshDb = taskKey[Unit]("refresh dev database")
}
