package ua.ucu.fp.keyscollector.ws

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import ua.ucu.fp.keyscollector.dto.{Message, MessagePayload}
import ua.ucu.fp.keyscollector.stage.StatisticsFlow
import akka.http.scaladsl.server.Directives._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object WsController {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()


  def apply(findingSource: Source[MessagePayload, Cancellable]): Future[Done] = {
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
                      .tick(0.second, FiniteDuration(tick.toInt, unit), "tick")
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

}
