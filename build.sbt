name := "wilson"

version := "1.0"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"               %% "akka-actor"             % "2.3.10"         % "provided",
  "com.typesafe.akka"               %% "akka-remote"            % "2.3.10",
  "com.typesafe.akka"               %% "akka-slf4j"             % "2.3.10",
  "com.typesafe.akka"               %% "akka-testkit"           % "2.3.10"         % "test",
  "ch.qos.logback"                  % "logback-classic"         % "1.0.13",
  "com.google.code.gson"            % "gson"                    % "2.3.1",
  "org.antlr"                       % "antlr4"                  % "4.5",
  "org.json"                        % "json"                    % "20140107"
)