import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Sonatype.settings ++ Seq(
    organization        := "com.github.aloiscochard.sherpa",
    version             := "0.1-SNAPSHOT",
    scalaVersion        := "2.10.0-M6",
    scalacOptions       := Seq("-unchecked", "-deprecation"),
    resolvers           ++= Seq(
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    )
  )
}

object SherpaBuild extends Build {
  import BuildSettings._

  lazy val sherpa = Project (
    "sherpa",
    file ("."),
    settings = buildSettings
  ) aggregate (sherpa_core, sherpa_scalaz, sherpa_jackson)

  lazy val sherpa_core = Project(
    "sherpa-core",
    file("sherpa-core"),
    settings = buildSettings ++ testDependencies ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
    )
  )

  lazy val sherpa_scalaz = Project(
    "sherpa-scalaz",
    file("sherpa-scalaz"),
    settings = buildSettings ++ testDependencies ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalaz" %% "scalaz-core" % "7.0.0-M1",
        "org.scalaz" %% "scalaz-effect" % "7.0.0-M1"
      )
    )
  ) dependsOn (sherpa_core)

  lazy val sherpa_jackson = Project(
    "sherpa-jackson",
    file("sherpa-jackson"),
    settings = buildSettings ++ testDependencies ++ Seq(
      libraryDependencies ++= Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % "2.0.0",
        "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.0"
      )
    )
  ) dependsOn (sherpa_core)

  lazy val sherpa_typesafe_config = Project(
    "sherpa-typesafe-config",
    file("sherpa-typesafe-config"),
    settings = buildSettings ++ testDependencies ++ Seq(
      libraryDependencies += "com.typesafe" % "config" % "0.5.0"
    )
  ) dependsOn (sherpa_core)


  val testDependencies = Seq(libraryDependencies += "org.specs2" %% "specs2" % "1.11" % "test")
}

object Sonatype extends PublishToSonatype(SherpaBuild) {
  def projectUrl    = "https://github.com/aloiscochard/sherpa"
  def developerId   = "alois.cochard"
  def developerName = "Alois Cochard"
  def licenseName   = "Apache 2 License"
  def licenseUrl    = "http://www.apache.org/licenses/LICENSE-2.0.html"
}
