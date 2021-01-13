name := "keys-collector"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies += "org.squbs" %% "squbs-ext" % "0.13.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

//libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0"



libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.1"
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.10"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "2.0.2"




libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-typed" % "2.6.10"

