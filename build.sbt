import java.time.ZonedDateTime
import Key._
import ByteConversions._

scalaVersion := "2.12.8"
organization := "com.spread0x"
name := "example-news-admin"
projectVersion := "1.0.0"

enablePlugins(BuildInfoPlugin, JavaAppPackaging)


buildNumber := sys.props.getOrElse("BUILD_NUMBER", "1")
version := s"${projectVersion.value}.${buildNumber.value}"
buildBranch := sys.props.getOrElse("BRANCH", "unknown")
buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  buildBranch,
  buildTime,
  "title" ->"Example admin service.",
  "description" -> "Example admin service."
)
buildInfoOptions += BuildInfoOption.ToJson
buildInfoPackage := "buildInfo"
buildTime := ZonedDateTime.now()

version := "1.0.0"
logLevel := Level.Debug

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-laws" % "1.1.0", //or `cats-testkit` if you are using ScalaTest
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6"
)

libraryDependencies ++= {
  import Dependency._

  val compile = List(
    playLegacyWSSub,
    playLegacyWS,
    libPhoneNumber,
    akkaHttp,
    catsCore,
    circeCore,
    catsEffect,
    circeGeneric,
    circeParser,
    akkaHttpCirce,
    akkaHttpCors,
    akkaHttpTest,
    doobieCore,
    doobiePostgres,
    doobieHikari,
    logback,
    scalaLogging,
    pureConfig,
    scalaConductrBundleLib,
    circeJava8,
    flyway,
    nimbusOidcSdk,
    nimbusJoseJwt,
    shapeless,
    kindProjector,
    pureConfigCats,
    log4catCore,
    log4catsSlf4j,
    catsPar,
    catsMtl
  )

  val test = List(scalaTest)

  compile ++ test.map(_ % Test)
}

resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "emailaddress" % "3.2.0"


resolvers ++= Seq(
  Resolver.typesafeIvyRepo("releases"),
  Resolver.sonatypeRepo("releases"),
  "hs" at "https://dl.bintray.com/hmrc/release-candidates/"
)


scalacOptions ++= List(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
//  "-Xlint",
//  "-Xlint:nullary-unit",
//  "-Ywarn-nullary-unit",
//    "-Xlog-implicits",
  "-Ypartial-unification",
  "-language:higherKinds"
)
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")

trapExit := false
scalafmtOnCompile in ThisBuild := true
ensimeIgnoreScalaMismatch in ThisBuild := true
BundleKeys.nrOfCpus := 1.0
BundleKeys.memory := 1024.MiB
BundleKeys.diskSpace := 100.MiB
BundleKeys.roles := Set("rbp-news-admin")
BundleKeys.endpoints := Map("example-admin" -> Endpoint("http",0,"example-admin",RequestAcl(Http("^/example-admin".r -> "/"))))
BundleKeys.compatibilityVersion := s"${projectVersion.value}.${buildNumber.value}-${buildBranch.value}"


