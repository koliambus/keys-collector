package ua.ucu.fp.keyscollector.integration

import ua.ucu.fp.keyscollector.dto.KeyFinding

case object LanguageDecoder {
  def apply(fileURL: String): String = {
    fileURL match {
      case file if file.endsWith(".rb") => "ruby"
      case file if file.endsWith(".php") => "php"
      case file if file.endsWith(".py") => "python"
      case file if file.endsWith(".java") => "java"
      case file if file.endsWith(".scala") => "scala"
      case file if file.endsWith(".ts") => "typescript"
      case _ => "unknown"
    }
  }
}

case object RepositoryDecoder {
  implicit class RegexOps(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  def apply(service: String, message: Object): KeyFinding = {
    val messageAsMap = message.asInstanceOf[Map[String, Any]]
    val repository = messageAsMap("repository").asInstanceOf[Map[String, Any]]
    val textMatches = messageAsMap("text_matches").asInstanceOf[List[Map[String, Any]]]
    val fileURL = messageAsMap("html_url").toString
    val language = LanguageDecoder(fileURL)
    val projectUrl: String = repository("html_url").toString
    val textMatch: String = textMatches.head("fragment").toString
    val keyFinding = KeyFinding(service, language, projectUrl, fileURL, textMatch)
    //println(keyFinding)
    keyFinding
  }
}

