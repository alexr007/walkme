Global / onChangedBuildSource := ReloadOnSourceChanges
scalaVersion := "2.13.3"

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"    % "2.6.10",
  "com.typesafe.akka" %% "akka-stream"         % "2.6.10",
  "com.typesafe.akka" %%"akka-http"            % "10.2.1",
  "com.typesafe.akka" %%"akka-http-spray-json" % "10.2.1",
  "ch.qos.logback"    % "logback-classic"      % "1.2.3",
)
