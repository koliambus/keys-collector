package ua.ucu.fp.keyscollector.stage

import akka.stream.alpakka.mongodb.DocumentUpdate
import akka.stream.alpakka.mongodb.scaladsl.MongoFlow
import akka.stream.scaladsl.Flow
import com.mongodb.client.model.UpdateOptions
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection}
import org.bson.{BsonDocument, Document}
import ua.ucu.fp.keyscollector.dto.KeyFinding

import scala.concurrent.duration.{FiniteDuration, SECONDS}

object StatisticsUpdate {

  val dbName = "keys_collector"
  val collectionName = "statistics"

  val collection: MongoCollection[Document] = MongoClients.create().getDatabase(dbName).getCollection(collectionName)

  val duration: FiniteDuration = new FiniteDuration(10, SECONDS)

  def apply(): Flow[KeyFinding, Any, Any] = {
    Flow[KeyFinding]
      .map(m => m.language)
      .map(l => DocumentUpdate(
        BsonDocument.parse("{_id: \"" + l + "\"}"),
        BsonDocument.parse("{ $inc: { count: 1 }}")
      ))
      .via(MongoFlow.updateOne(collection, new UpdateOptions().upsert(true)))
  }
}
