name := "wilson"

version := "1.0"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
)

val akkaVer = "2.5.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka"               %% "akka-actor"             % akkaVer          % "provided",
  "com.typesafe.akka"               %% "akka-remote"            % akkaVer,
  "com.typesafe.akka"               %% "akka-slf4j"             % akkaVer,
  "com.typesafe.akka"               %% "akka-testkit"           % akkaVer          % "test",
  "ch.qos.logback"                  % "logback-classic"         % "1.0.13",
  "com.google.code.gson"            % "gson"                    % "2.3.1",
  "org.antlr"                       % "antlr4"                  % "4.5"            % "provided",
  "org.antlr"                       % "ST4"                     % "4.0.8"          % "compile",
  "org.json"                        % "json"                    % "20140107",
  "javax.inject"                    % "javax.inject"            % "1",
  "org.springframework"             % "spring-context"          % "4.3.9.RELEASE",
  "org.springframework.amqp"        % "spring-rabbit"           % "1.7.3.RELEASE"
)
