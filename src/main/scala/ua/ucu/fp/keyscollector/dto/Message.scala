package ua.ucu.fp.keyscollector.dto

case class Message[+T <: MessagePayload](service: String, payload: T) {
  type Type = payload.type
}

sealed trait MessagePayload

final case class KeyFinding(service: String, language: String, projectUrl: String, url: String, fragment: String) extends MessagePayload
final case class NewProjectLeaked(service: String, language: String, projectUrl: String) extends MessagePayload
final case class Statistics(statistics: List[StatisticsItem]) extends MessagePayload

case class StatisticsItem(language: String, count: Int)
