package ua.ucu.fp.keyscollector.dto

case class Message[+T <: MessagePayload](service: String, `type`: String, payload: T)

object Message {
  def create[T <: MessagePayload](payload: T): Message[T] = {
    Message(payload.service, payload.`type`, payload)
  }
}

sealed trait MessagePayload {
  val service: String
  val `type`: String = getClass.getSimpleName
}

case class KeyFinding(service: String, language: String, projectUrl: String, url: String, fragment: String) extends MessagePayload
case class NewProjectLeaked(service: String, language: String, projectUrl: String) extends MessagePayload
case class Statistics(statistics: List[StatisticsItem]) extends MessagePayload {
  override val service: String = null
}

case class StatisticsItem(language: String, count: Int)
