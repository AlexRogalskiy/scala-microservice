name := "scala-microservice"

version := "0.1"

lazy val akkaVersion       = "2.6.9"
lazy val postgresVersion   = "42.2.2"
lazy val scalikejdbc       = "3.5.0"
lazy val akkaHttpVersion   = "10.2.4"

scalaVersion := "2.13.5"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"                    % akkaVersion,
  "com.typesafe.akka" %% "akka-http"                           % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json"                % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit"                   % akkaHttpVersion,
  "org.postgresql"     % "postgresql"                          % postgresVersion,
  "org.scalikejdbc"   %% "scalikejdbc"                         % scalikejdbc,
  "com.typesafe.akka" %% "akka-stream"                         % akkaVersion
)