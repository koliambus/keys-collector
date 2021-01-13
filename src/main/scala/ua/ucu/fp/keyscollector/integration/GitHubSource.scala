package ua.ucu.fp.keyscollector.integration

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import org.squbs.streams.Deduplicate
import ua.ucu.fp.keyscollector.Config
import ua.ucu.fp.keyscollector.dto.KeyFinding

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

object GitHubSource {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val SEARCH_URI: Uri = "https://api.github.com/search/code"

  def request(source: Map[String, String]): Future[List[KeyFinding]] = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    Http().singleRequest(
        HttpRequest(uri = SEARCH_URI.withQuery(Query("q" -> source("q"), "access_token" -> Config.GITHUB_TOKEN)))
          .addHeader(RawHeader("Accept", "application/vnd.github.v3.text-match+json")))
        .map(x => x.entity)
        .flatMap(x => Unmarshal(x).to[String])
        .map(x => mapper.readValue[Map[String, Any]](x))
        .map(x => x("items").asInstanceOf[List[Object]].map(RepositoryDecoder(source("service"), _)))

  }
  def apply(params: List[Map[String, String]]): Source[KeyFinding, Cancellable] = Source
    .tick(1.second, 10.second, "tick")
    .mapAsync(1) { _ =>
      Future.sequence(params.map(request))
    }
    .mapConcat(x => x)
    .mapConcat(x => x)
    .via(Deduplicate())

  def main(args: Array[String]): Unit = {
    //    Source.tick(1.second, 1.second, "tick")
    //      .mapAsync(1) {_ => Future(List(1, 2, 3))}
    //      .mapConcat(x => x)
    //      .via(Deduplicate())
    //      .runForeach(println)
    //apply("foursquare_key")
  }
}
