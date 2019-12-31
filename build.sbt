import java.time.ZonedDateTime
import Key._
// import ByteConversions._

scalaVersion := "2.13.1"
organization := "com.spread0x"
name := "example-entries"
projectVersion := "1.0.0"
//, JavaAppPackaging)

enablePlugins(BuildInfoPlugin)
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

version := "2.0.0"
// logLevel := Level.Debug

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-laws" % "2.0.0", //or `cats-testkit` if you are using ScalaTest
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3"
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
    pureConfig,
    // scalaConductrBundleLib,
    flyway,
    nimbusOidcSdk,
    nimbusJoseJwt,
    shapeless,
    // kindProjector,
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

// libraryDependencies += "uk.gov.hmrc" %% "emailaddress" % "3.4.0"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.6.1" % Test

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
  "-Ymacro-annotations",
//  "-Ypartial-unification",
  "-language:higherKinds"
)

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)

trapExit := false
// scalafmtOnCompile in ThisBuild := true
// ensimeIgnoreScalaMismatch in ThisBuild := true
// BundleKeys.nrOfCpus := 1.0
// BundleKeys.memory := 1024.MiB
// BundleKeys.diskSpace := 100.MiB
// BundleKeys.roles := Set("rbp-news-admin")
// BundleKeys.endpoints := Map("example-admin" -> Endpoint("http",0,"example-admin",RequestAcl(Http("^/example-admin".r -> "/"))))
// BundleKeys.compatibilityVersion := s"${projectVersion.value}.${buildNumber.value}-${buildBranch.value}"



resolvers += Resolver.jcenterRepo
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Sonatype OSS Snapshots1" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
resolvers += "iheartradio-maven" at "https://dl.bintray.com/iheartradio/maven"
resolvers += "atlassian-maven" at "https://maven.atlassian.com/content/repositories/atlassian-public"
//resolvers += "spread0x-slick" at "https://dl.bintray.com/spread/slick-repositories/"
