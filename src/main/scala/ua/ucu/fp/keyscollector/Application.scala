package edu.fp.examples.app

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits.fanOut2flow
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Flow, GraphDSL, Keep, Merge, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import edu.fp.examples.app.dto.Message
import edu.fp.examples.app.integration.CryptoCompareSource
import edu.fp.examples.app.stages.{MongoDBSink, PriceAvgFlow, PriceFlow, TradeFlow}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

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
              }
              }
            }
          )
        }
      )
    }
}
