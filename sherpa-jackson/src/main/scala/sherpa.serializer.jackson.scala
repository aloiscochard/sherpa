//  __..              
// (__ |_  _ ._.._  _.
// .__)[ )(/,[  [_)(_]
//              |     
//
//  (c) 2012, Alois Cochard
package sherpa
package serializer

import scala.collection.JavaConverters._

import sherpa.effect._

import java.io.ByteArrayOutputStream
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
    
// TODO POC to be improved, using streaming, improving performance correct error handling

package object jackson {

  object Jackson extends JacksonSerializer

  trait JacksonSerializer extends Serializer[ByteString, DefaultGenerator, DefaultExtractor] {
    override def generate[T](value: T)(implicit writer: Writer[T, DefaultGenerator]) = {
      val generator = new JacksonGenerator
      writer(value)(generator).unsafePerformIO
      generator.output()
    }

    override def parse[T](input: ByteString)(implicit reader: Reader[T, DefaultExtractor]) = {
      val extractor = new JacksonExtractor(input)
      Right(reader(extractor).unsafePerformIO)
    }

    def parse[T](input: String)(implicit reader: Reader[T, DefaultExtractor]): Either[SerializerError, T] =
      parse[T](ByteString(input))
  }

  class JacksonGenerator extends EntityGenerator[String] {
    def writeByteString(value: ByteString): IO[Unit] = IO { generator.writeBinary(value.toArray) }

    def writeField[T](name: String)(f: EntityGenerator[String] => IO[T]): IO[Unit] = IO {
      generator.writeFieldName(name)
      f(this).unsafePerformIO
    }

    def writeObject[T](f: EntityGenerator[String] => IO[T]): IO[Unit] = IO {
      generator.writeStartObject
      f(this).unsafePerformIO
      generator.writeEndObject
    }

    def writeBoolean(value: Boolean): IO[Unit] = IO { generator.writeBoolean(value) }
    def writeChar(value: Char): IO[Unit] = writeString(value.toString)
    def writeDouble(value: Double): IO[Unit] = IO { generator.writeNumber(value) }
    def writeFloat(value: Float): IO[Unit] = IO { generator.writeNumber(value) }
    def writeInt(value: Int): IO[Unit] = IO { generator.writeNumber(value) }
    def writeLong(value: Long): IO[Unit] = IO { generator.writeNumber(value) }
    def writeShort(value: Short): IO[Unit] = IO { generator.writeNumber(value) }
    def writeString(value: String): IO[Unit] = IO { generator.writeString(value) }

    def writeSeq[T](f: EntityGenerator[String] => Seq[IO[Unit]]): IO[Unit] = IO {
      generator.writeStartArray
      f(this).foreach(_.unsafePerformIO)
      generator.writeEndArray
    }

    def output(): ByteString = {
      generator.close
      _output.close
      ByteString(_output.toByteArray)
    }

    private lazy val generator = jsonFactory.createJsonGenerator(_output, JsonEncoding.UTF8) // TODO Encoding configurable

    private lazy val _output = new ByteArrayOutputStream()
  }


  class JacksonExtractor(byteString: ByteString) extends EntityExtractor[String] {
    def readByteString(): IO[ByteString] = IO { ByteString(current.binaryValue) }

    def readField[T](name: String)(f: EntityExtractor[String] => IO[T]): IO[T] = IO {
      withNode(current.get(name)) { f(this).unsafePerformIO }
    }

    def readObject[T](f: EntityExtractor[String] => IO[T]): IO[T] = f(this)

    def readBoolean(): IO[Boolean] = IO { current.booleanValue }
    def readChar(): IO[Char] = IO { current.textValue().head }
    def readDouble(): IO[Double] = IO { current.doubleValue }
    def readFloat(): IO[Float] = IO { current.textValue().toFloat }
    def readInt(): IO[Int] = IO { current.intValue }
    def readLong(): IO[Long] = IO { current.longValue }
    def readShort(): IO[Short] = IO { current.textValue().toShort }
    def readString(): IO[String] = IO { current.textValue }

    def readSeq[T](f: EntityExtractor[String] => IO[T]): IO[Seq[T]] = IO {
      current.elements.asScala.map(node => {
        withNode(node) { f(this).unsafePerformIO }
      }).toSeq
    }

    private def withNode[T](node: JsonNode)(f: => T): T = {
      val before = current
      current = node
      val value = f
      current = before
      value
    }

    private val root = new ObjectMapper().readTree(byteString.toArray)
    private var current = root
  }

  private[jackson] val jsonFactory = new JsonFactory
}

