package ua.ucu.fp.keyscollector.ws

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import akka.{Done, NotUsed}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import ua.ucu.fp.keyscollector.dto.{Message, MessagePayload, Statistics}
import ua.ucu.fp.keyscollector.zip.ZipMainLatest

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContextExecutor, Future}

object WsController {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()

  def statisticsFilter(findingSource: Source[MessagePayload, Any]): Source[Statistics, Any] =
    findingSource.filter(p => p.isInstanceOf[Statistics]).map(_.asInstanceOf[Statistics])


  def apply(findingSource: Source[MessagePayload, Any]): Future[Done] = {
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
                      .via(GraphDSL.create() {
                        implicit graphBuilder =>
                          val zip = graphBuilder.add(ZipMainLatest[Any, Statistics]())
                          statisticsFilter(findingSource) ~> zip.in1
                          FlowShape(zip.in0, zip.out)
                      })
                      .map(s => s._2)
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
