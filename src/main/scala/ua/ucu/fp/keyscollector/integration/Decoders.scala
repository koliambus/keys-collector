package ua.ucu.fp.keyscollector.integration

import ua.ucu.fp.keyscollector.dto.KeyFinding

case object RepositoryDecoder {
  implicit class RegexOps(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  def apply(message: Object): KeyFinding = {
    val messageAsMap = message.asInstanceOf[Map[String, Any]]
    val repository = messageAsMap("repository").asInstanceOf[Map[String, Any]]
    val textMatches = messageAsMap("text_matches").asInstanceOf[List[Map[String, Any]]]
    val fileURL = messageAsMap("html_url").toString

    val language = fileURL match {
      case file if file.endsWith(".rb") => "ruby"
      case file if file.endsWith(".php") => "php"
      case file if file.endsWith(".py") => "python"
      case file if file.endsWith(".java") => "java"
      case file if file.endsWith(".scala") => "scala"
      case _ => "unknown"
    }
    val keyFinding = KeyFinding("Foursquare", language, repository("html_url").toString, fileURL, textMatches(0)("fragment").toString)
    //println(keyFinding)
    keyFinding
  }
}
