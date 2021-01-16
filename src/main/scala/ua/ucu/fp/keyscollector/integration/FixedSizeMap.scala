package ua.ucu.fp.keyscollector.integration

import java.util
import java.util.function.BiFunction

import scala.collection.JavaConverters.{mapAsScalaMapConverter, mutableMapAsJavaMapConverter}


class FixedSizeMap[K, V](maxSize: Int) extends util.TreeMap[K, V]  {
  private var map: util.Map[K, V] = new util.TreeMap[K, V]()

  override def merge(key: K, value: V, remappingFunction: BiFunction[_ >: V, _ >: V, _ <: V]): V = {
    map = map.asScala.takeRight(maxSize).asJava
    val result = map.merge(key, value, remappingFunction)
    result
  }
}