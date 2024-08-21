name := """githubTutorial"""
organization := "com.example"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "githubTutorial"
  )
  .enablePlugins(PlayScala)

resolvers += "HMRC-open-artefacts-maven2" at "https://open.artefacts.tax.service.gov.uk/maven2"

libraryDependencies ++= Seq(
  "uk.gov.hmrc.mongo"      %% "hmrc-mongo-play-28"   % "0.63.0",
  guice,
  "org.scalatest"          %% "scalatest"               % "3.2.15"             % Test,
  "org.scalamock"          %% "scalamock"               % "5.2.0"             % Test,
  "org.scalatestplus.play" %% "scalatestplus-play"   % "5.1.0"              % Test,
  "org.mockito"             % "mockito-core"         % "4.2.0"              % Test,
  "org.mockito"             %% "mockito-scala"       % "1.16.46"           % Test
)


libraryDependencies += ws

libraryDependencies += ("org.typelevel"                %% "cats-core"                 % "2.3.0")

//Wire Mocking Dependencies:
libraryDependencies += "com.github.tomakehurst" % "wiremock-jre8" % "2.33.2" % Test
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws" % "2.8.15",  // Replace with the correct version of Play WS
  "com.typesafe.play" %% "play-ws-standalone" % "2.1.6" % Test  // Add this if it's not already included
)

dependencyOverrides +="com.fasterxml.jackson.core" % "jackson-databind" % "2.11.0"
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
