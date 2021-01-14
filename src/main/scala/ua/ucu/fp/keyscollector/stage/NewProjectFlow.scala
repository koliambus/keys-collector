package ua.ucu.fp.keyscollector.stage

import akka.stream.FlowShape
import akka.stream.alpakka.mongodb.DocumentReplace
import akka.stream.alpakka.mongodb.scaladsl.MongoFlow
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Zip}
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection}
import org.bson.{BsonDocument, Document}
import ua.ucu.fp.keyscollector.dto.{KeyFinding, NewProjectLeaked}

object NewProjectFlow {
  val dbName = "keys_collector"
  val collectionName = "projects"

  val collection: MongoCollection[Document] = MongoClients.create().getDatabase(dbName).getCollection(collectionName)

  def apply(): Flow[KeyFinding, NewProjectLeaked, Any] =
    Flow[KeyFinding]
      .log("NewProjectFlowGraph")
      .via(
        GraphDSL.create() { implicit graphBuilder =>
          val IN = graphBuilder.add(Broadcast[KeyFinding](2))
          val ZIPPED = graphBuilder.add(Zip[Boolean, KeyFinding]())
          val OUT = graphBuilder.add(Merge[NewProjectLeaked](1))

          IN ~> mapToReplaceFlow() ~> checkIsNewFlow() ~> ZIPPED.in0
          IN                                           ~> ZIPPED.in1

          ZIPPED.out ~> filterOnlyNewFlow() ~> mapKeyToNewProject() ~> OUT


          FlowShape(IN.in, OUT.out)
        }
      )

  def filterOnlyNewFlow(): Flow[(Boolean, KeyFinding), KeyFinding, _] = {
    Flow[(Boolean, KeyFinding)]
      .log("filterOnlyNewFlow")
      .filter(_._1)
      .map(_._2)
  }

  def mapKeyToNewProject(): Flow[KeyFinding, NewProjectLeaked, _] = {
    Flow[KeyFinding]
      .log("mapKeyToNewProject")
      .map(key => NewProjectLeaked(key.service, key.language, key.projectUrl))
  }

  def mapToReplaceFlow(): Flow[KeyFinding, DocumentReplace[Document], _] = {
    Flow[KeyFinding]
      .log("mapToReplaceFlow")
      .map(_.projectUrl)
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
