package ua.ucu.fp.keyscollector

import akka.actor.Cancellable
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge, Source}
import ua.ucu.fp.keyscollector.dto.{KeyFinding, MessagePayload}
import ua.ucu.fp.keyscollector.integration.GitHubSource
import ua.ucu.fp.keyscollector.stage.{NewProjectFlow, StatisticsSink}
import ua.ucu.fp.keyscollector.ws.WsController


object Application extends App {
  val sources: List[Map[String, String]] = List(
    Map("service" -> "Foursquare", "q" -> "foursquare_key"),
    Map("service" -> "Facebook", "q" -> "facebook_key")
  )

  val findingSource: Source[MessagePayload, Cancellable] =
    GitHubSource(sources)
      .via(GraphDSL.create() { implicit graphBuilder =>
        val IN = graphBuilder.add(Broadcast[KeyFinding](3))
        val OUT = graphBuilder.add(Merge[MessagePayload](2))

        IN ~> OUT
        IN ~> NewProjectFlow() ~> OUT
        IN ~> StatisticsSink()

        FlowShape(IN.in, OUT.out)
      })

  val bindingFuture = WsController(findingSource)
}
