import sbt._

object Dependency {
  object Version {
    val akkaHttp = "10.1.1"
    val cats = "1.2.0"
    val circe = "0.9.3"
    val doobie = "0.5.3"
  }

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaHttpCors = "ch.megard" %% "akka-http-cors" % "0.3.0"
  val akkaHttpTest = "com.typesafe.akka" %% "akka-http-testkit" % "10.1.4"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  val catsCore = "org.typelevel" %% "cats-core" % Version.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % "1.0.0"
  val catsPar = "io.chrisdavenport" %% "cats-par" % "0.2.0"
  val catsMtl = "org.typelevel" %% "cats-mtl-core" % "0.4.0"

  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeJava8 = "io.circe" %% "circe-java8" % Version.circe
  val akkaHttpCirce =   "de.heikoseeberger" %% "akka-http-circe" % "1.22.0"

  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe

  val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % "5.1"
  val nimbusOidcSdk  = "com.nimbusds" % "oauth2-oidc-sdk" % "5.36"

  val libPhoneNumber = "com.googlecode.libphonenumber" % "libphonenumber" % "8.5.0"

  val doobieCore = "org.tpolecat" %% "doobie-core" % Version.doobie
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Version.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Version.doobie

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

  val scalaConductrBundleLib = "com.typesafe.conductr" %% "scala-conductr-bundle-lib" % "1.9.0"

  val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.9.1"
  val pureConfigCats = "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.10.1"
  val flyway = "org.flywaydb" % "flyway-core" % "5.1.4"

  val playLegacyWS = "com.typesafe.play" %% "play-ws" % "2.7.0-M3" //todo по возможности выпилить и переписать
  val playLegacyWSSub = "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M3"

  val kindProjector = "org.spire-math" %% "kind-projector" % "0.9.8"
  val shapeless =  "com.chuusai" %% "shapeless" % "2.3.3"
  val log4catCore = "io.chrisdavenport" %% "log4cats-core"    % "0.2.0"
  val log4catsSlf4j = "io.chrisdavenport" %% "log4cats-slf4j"   % "0.2.0"
}
