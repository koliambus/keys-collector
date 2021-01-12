package ua.ucu.fp.keyscollector

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Flow, GraphDSL, Keep, Merge, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import ua.ucu.fp.keyscollector.dto.{KeyFinding, Message, MessagePayload}
import ua.ucu.fp.keyscollector.stage.NewProjectFlow

import scala.concurrent.{ExecutionContextExecutor, Future}


object Application extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()

  val findingSource: Source[Message[MessagePayload], NotUsed] =
    // TODO change to real Source
    Source.single(Message("Facebook", KeyFinding("Facebook", "scala", "https://github.com/koliambus/keys-collector", "https://github.com/koliambus/keys-collector/blob/43529519c6a6eed8f4e73bfaf975607da46045d6/src/main/scala/ua/ucu/fp/keyscollector/Application.scala#L11")))
      .via(GraphDSL.create() { implicit graphBuilder =>
        val IN = graphBuilder.add(Broadcast[Message[KeyFinding]](2))
        val OUT = graphBuilder.add(Merge[Message[MessagePayload]](2))

        IN ~> OUT
        IN ~> NewProjectFlow() ~> OUT

        FlowShape(IN.in, OUT.out)
      })
      .toMat(BroadcastHub.sink)(Keep.right)
      .run

  val bindingFuture =
    serverSource.runForeach { connection => // foreach materializes the source
      println("Accepted new connection from " + connection.remoteAddress)
      // ... and then actually handle the connection
      connection.handleWith(
        get {
          concat(
            path("stream") {
              parameter("services".repeated) { services => {
                println(s"Start websocket with services $services")
                val source = (if (services.isEmpty) {
                  findingSource
                } else {
                  findingSource
                    .filter(model => services.exists(_.equalsIgnoreCase(model.service)))
                })
                  .map(mapper.writeValueAsString(_))
                  .map(TextMessage(_))

                val flow: Flow[Any, TextMessage.Strict, NotUsed] = Flow.fromSinkAndSource(
                  Sink.ignore,
                  source
                )
                handleWebSocketMessages(flow)
              }
              }
            }
          )
        }
      )
    }
}
