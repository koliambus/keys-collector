name := "keys-collector"

version := "0.1"

scalaVersion := "2.13.4"


libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0"



libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.1"
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.10"

libraryDependencies += "io.socket" % "socket.io-client" % "1.0.1"





libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-typed" % "2.6.10"
