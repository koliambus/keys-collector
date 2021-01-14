package ua.ucu.fp.keyscollector

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Flow, GraphDSL, Keep, Merge, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import ua.ucu.fp.keyscollector.dto.{KeyFinding, Message, MessagePayload, NewProjectLeaked}
import ua.ucu.fp.keyscollector.integration.GitHubSource
import ua.ucu.fp.keyscollector.stage.{NewProjectFlow, StatisticsFlow, StatisticsSink}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContextExecutor, Future}


object Application extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val sources: List[Map[String, String]] = List(
    Map("service" -> "Foursquare", "q" -> "foursquare_key"),
    Map("service" -> "Facebook", "q" -> "facebook_key")
  )

  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()

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

  val bindingFuture =
    serverSource.runForeach { connection => // foreach materializes the source
      println("Accepted new connection from " + connection.remoteAddress)
      // ... and then actually handle the connection
      connection.handleWith(
        get {
          concat(
            path("stats") {
              parameters("tick", "unit") { (tick: String, unit: String) => {
                handleWebSocketMessages(
                  Flow.fromSinkAndSource(
                    Sink.ignore,
                    Source
                      .tick(1.second, FiniteDuration(tick.toInt, unit), "tick")
                      .via(StatisticsFlow())
                      .map(mapper.writeValueAsString(_))
                      .map(TextMessage(_))
                  ))
              }}
            },
            path("stream") {
              parameter("services".repeated) { services => {
                println(s"Start websocket with services $services")
                val source = (if (services.isEmpty) {
                  findingSource
                } else {
                  findingSource
                    .filter(model => services.exists(_.equalsIgnoreCase(model.service)))
                })
                  .map(key => Message.create(key))
                  .map(mapper.writeValueAsString(_))
                  .map(TextMessage(_))

                val flow: Flow[Any, TextMessage.Strict, NotUsed] = Flow.fromSinkAndSource(
                  Sink.ignore,
                  source
                )
                handleWebSocketMessages(flow)
              }}
            }
          )
        }
      )
    }
}
