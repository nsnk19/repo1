package StartsPerSecond

import com.fasterxml.jackson.databind.{ DeserializationFeature, JsonNode, ObjectMapper, SerializationFeature }
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JSONUtil {
  private val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true)
  mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)

  def encodeAsTree(value: Object): JsonNode = synchronized { mapper.valueToTree(value) }

  def decode[T](value: Array[Byte], valueType: Class[T]): T = synchronized { mapper.readValue(value, valueType) }

  def decode[T](value: String, valueType: Class[T]): T = mapper.readValue(value, valueType)

  def encodeAsString(value: Object): String = synchronized { mapper.writeValueAsString(value) }
}
