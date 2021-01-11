package ua.ucu.fp.keyscollector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.concurrent.{ExecutionContextExecutor, Future}


object Application extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  
  
  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()


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
                val flow = Flow.fromSinkAndSource(Sink.ignore, Source.single(TextMessage("Hello!")))
                handleWebSocketMessages(flow)
              }
              }
            }
          )
        }
      )
    }
}
