// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `teleproto` =
  project
    .in(file("."))
    .enablePlugins(GitVersioning, GitBranchPrompt)
    .settings(commonSettings: _*)
    .settings(sonatypeSettings: _*)
    .settings(Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings): _*)
    .settings(
      name := "teleproto",
      version := "1.0.3",
      libraryDependencies ++= Seq(
        library.scalaPB     % "protobuf",
        library.scalaPBJson % Compile,
        library.scalaTest   % Test,
        library.scalaCheck  % Test
      )
    )

// *****************************************************************************
// Dependencies
// *****************************************************************************

lazy val library =
  new {

    object Version {
      val scalaPB      = scalapb.compiler.Version.scalapbVersion
      val scalaPBJson  = "0.7.2"
      val scalaLogging = "3.9.0"
      val scalaCheck   = "1.14.0"
      val scalaTest    = "3.0.5"
      val scapeGoat    = "1.3.8"
    }

    val scalaPB      = "com.thesamet.scalapb"       %% "scalapb-runtime" % Version.scalaPB
    val scalaPBJson  = "com.thesamet.scalapb"       %% "scalapb-json4s"  % Version.scalaPBJson
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"   % Version.scalaLogging
    val scalaCheck   = "org.scalacheck"             %% "scalacheck"      % Version.scalaCheck
    val scalaTest    = "org.scalatest"              %% "scalatest"       % Version.scalaTest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val commonSettings =
  compilerSettings ++
    gitSettings ++
    organizationSettings ++
    sbtSettings ++
    scalaFmtSettings ++
    scapegoatSettings

lazy val compilerSettings =
  Seq(
    scalaVersion := "2.12.9",
    mappings.in(Compile, packageBin) +=
      baseDirectory.in(ThisBuild).value / "LICENSE" -> "LICENSE",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit"
    ),
    javacOptions ++= Seq(
      "-source",
      "1.8",
      "-target",
      "1.8"
    ),
    unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value)
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := false
  )

lazy val organizationSettings =
  Seq(
    organization := "io.moia",
    organizationName := "MOIA GmbH",
    startYear := Some(2019),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
  )

lazy val sonatypeSettings = {
  import xerial.sbt.Sonatype._
  Seq(
    publishTo := sonatypePublishTo.value,
    sonatypeProfileName := organization.value,
    publishMavenStyle := true,
    sonatypeProjectHosting := Some(GitHubHosting("moia-dev", "teleproto", "oss-support@moia.io"))
  )
}

lazy val sbtSettings =
  Seq(
    cancelable in Global := true,
    logLevel in sourceGenerators := Level.Error
  )

lazy val scalaFmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

lazy val scapegoatSettings =
  Seq(
    scapegoatVersion in ThisBuild := library.Version.scapeGoat,
    scapegoatDisabledInspections := Seq("FinalModifierOnCaseClass"),
    // do not check generated files
    scapegoatIgnoredFiles := Seq(".*/src_managed/.*")
  )

PB.targets in Test := Seq(scalapb.gen(flatPackage = false) -> (sourceManaged in Test).value)
PB.protoSources in Test := Seq(file("src/test/protobuf"))
