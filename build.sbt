name := "akka-persist-open-lesson"

version := "0.1"

scalaVersion := "2.13.3"

lazy val akkaVersion       = "2.6.9"
lazy val leveldbVersion    = "0.7"
lazy val leveldbjniVersion = "1.8"

libraryDependencies ++= Seq(
  //  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka"        %% "akka-testkit"           % akkaVersion % Test,
  "com.typesafe.akka"        %% "akka-slf4j"             % akkaVersion,
  "com.typesafe.akka"        %% "akka-actor-typed"       % akkaVersion,
  "com.typesafe.akka"        %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka"        %% "akka-persistence-query" % akkaVersion,
  "org.iq80.leveldb"          % "leveldb"                % leveldbVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all"         % leveldbjniVersion,
  "com.typesafe"              % "config"                 % "1.4.0",
  "ch.qos.logback"            % "logback-classic"        % "1.2.3"
)
