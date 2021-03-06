package ua.ucu.fp.keyscollector.stage

import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Flow
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection}
import org.bson.Document
import ua.ucu.fp.keyscollector.dto.{Statistics, StatisticsItem}

import scala.concurrent.duration.{FiniteDuration, SECONDS}

object StatisticsSource {

  val dbName = "keys_collector"
  val collectionName = "statistics"

  val collection: MongoCollection[Document] = MongoClients.create().getDatabase(dbName).getCollection(collectionName)

  val duration: FiniteDuration = new FiniteDuration(10, SECONDS)

  def apply(): Flow[Any, Statistics, Any] = {
    Flow[Any]
      .flatMapConcat(_ =>
        MongoSource(collection.find())
          .map(d => StatisticsItem(d.get("_id", classOf[String]), d.get("count", classOf[Integer])))
          .fold(List[StatisticsItem]())((l, d) => d :: l)
          .map(m => Statistics(m))
      )
  }
}
