package ua.ucu.fp.keyscollector.stage

import akka.stream.FlowShape
import akka.stream.alpakka.mongodb.DocumentReplace
import akka.stream.alpakka.mongodb.scaladsl.MongoFlow
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Zip}
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection}
import org.bson.{BsonDocument, Document}
import ua.ucu.fp.keyscollector.dto.{KeyFinding, Message, NewProjectLeaked}

object NewProjectFlow {
  val dbName = "keys_collector"
  val collectionName = "projects"

  val collection: MongoCollection[Document] = MongoClients.create().getDatabase(dbName).getCollection(collectionName)

  def apply(): Flow[Message[KeyFinding], Message[NewProjectLeaked], Any] =
    Flow[Message[KeyFinding]]
      .log("NewProjectFlowGraph")
      .via(
        GraphDSL.create() { implicit graphBuilder =>
          val IN = graphBuilder.add(Broadcast[Message[KeyFinding]](2))
          val MONGO_REPLACE = graphBuilder.add(Broadcast[DocumentReplace[Document]](1))
          val ZIPPED = graphBuilder.add(Zip[Boolean, Message[KeyFinding]]())
          val ONLY_NEW_KEY = graphBuilder.add(Broadcast[Message[KeyFinding]](1))
          val OUT = graphBuilder.add(Merge[Message[NewProjectLeaked]](1))

          IN ~> mapToReplaceFlow() ~> MONGO_REPLACE ~> checkIsNewFlow() ~> ZIPPED.in0
          IN                                                            ~> ZIPPED.in1

          ZIPPED.out ~> filterOnlyNewFlow() ~> ONLY_NEW_KEY ~> mapKeyToNewProject() ~> OUT


          FlowShape(IN.in, OUT.out)
        }
      )

  def filterOnlyNewFlow(): Flow[(Boolean, Message[KeyFinding]), Message[KeyFinding], _] = {
    Flow[(Boolean, Message[KeyFinding])]
      .log("filterOnlyNewFlow")
      .filter(_._1)
      .map(_._2)
  }

  def mapKeyToNewProject(): Flow[Message[KeyFinding], Message[NewProjectLeaked], _] = {
    Flow[Message[KeyFinding]]
      .log("mapKeyToNewProject")
      .map(key => Message(key.service, NewProjectLeaked(key.payload.service, key.payload.language, key.payload.projectUrl)))
  }

  def mapToReplaceFlow(): Flow[Message[KeyFinding], DocumentReplace[Document], _] = {
    Flow[Message[KeyFinding]]
      .log("mapToReplaceFlow")
      .map(_.payload.projectUrl)
      .map(project => DocumentReplace( // TODO put upsert: true somewhere
        BsonDocument.parse("{\"_id\": \"" + project + "\"}"),
        new Document().append("_id", project))
      )
  }

  def checkIsNewFlow(): Flow[DocumentReplace[Document], Boolean, _] = {
      MongoFlow.replaceOne[Document](collection, new ReplaceOptions().upsert(true))
        .log("checkIsNewFlow")
        .map(_._1)
        .map(_.getMatchedCount)
        .map(_ == 0)
  }
}
