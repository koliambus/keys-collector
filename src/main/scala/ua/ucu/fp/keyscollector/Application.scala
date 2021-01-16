package ua.ucu.fp.keyscollector

import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge}
import ua.ucu.fp.keyscollector.dto.{KeyFinding, MessagePayload}
import ua.ucu.fp.keyscollector.integration.GitHubSource
import ua.ucu.fp.keyscollector.stage.{NewProjectFlow, StatisticsSource, StatisticsUpdate}
import ua.ucu.fp.keyscollector.ws.WsController


object Application extends App {
  val sources: List[Map[String, String]] = List(
    Map("service" -> "Foursquare", "q" -> "foursquare_key"),
    Map("service" -> "Facebook", "q" -> "facebook_key")
  )

  val graphSource =
    GitHubSource(sources).via(
      GraphDSL.create() { implicit graphBuilder =>
        val IN = graphBuilder.add(Broadcast[KeyFinding](3))
        val OUT = graphBuilder.add(Merge[MessagePayload](3))

        IN ~> OUT
        IN ~> NewProjectFlow() ~> OUT
        IN ~> StatisticsUpdate() ~> StatisticsSource() ~> OUT

        FlowShape(IN.in, OUT.out)
      }
    )

  val bindingFuture = WsController(graphSource)
}
