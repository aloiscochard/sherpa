//  __..              
// (__ |_  _ ._.._  _.
// .__)[ )(/,[  [_)(_]
//              |     
//
//  (c) 2012, Alois Cochard
package sherpa
package serializer

import sherpa.effect._

import com.typesafe.config._

package object typesafe {

  implicit object Config extends ConfigSerializer

  trait ConfigSerializer extends Serializer[Config, DefaultGenerator, DefaultExtractor] {

    override def generate[T](value: T)(implicit writer: Writer[T, DefaultGenerator]) = {
      val generator = new ConfigGenerator
      writer(value)(generator).unsafePerformIO
      generator.output()
    }

    override def parse[T](config: Config)(implicit reader: Reader[T, DefaultExtractor]) = {
      val extractor = new ConfigExtractor(config)
      Right(reader(extractor).unsafePerformIO)
    }
  }

  class ConfigGenerator extends EntityGenerator[String] {
    def writeByteString(value: ByteString): IO[Unit] = // TODO IO { generator.writeBinary(value.toArray) }

    def writeField[T](name: String)(f: EntityGenerator[String] => IO[T]): IO[Unit] = IO {
      withKey(name) { f(this).unsafePerformIO }
    }

    def writeObject[T](f: EntityGenerator[String] => IO[T]): IO[Unit] = f(this).unsafePerformIO

    def writeBoolean(value: Boolean): IO[Unit] = IO { generator.writeBoolean(value) }
    def writeChar(value: Char): IO[Unit] = writeString(value.toString)
    def writeDouble(value: Double): IO[Unit] = IO { generator.writeNumber(value) }
    def writeFloat(value: Float): IO[Unit] = IO { generator.writeNumber(value) }
    def writeInt(value: Int): IO[Unit] = IO { generator.writeNumber(value) }
    def writeLong(value: Long): IO[Unit] = IO { generator.writeNumber(value) }
    def writeShort(value: Short): IO[Unit] = IO { generator.writeNumber(value) }
    def writeString(value: String): IO[Unit] = IO { generator.writeString(value) }

    def writeSeq[T](f: EntityGenerator[String] => Seq[IO[Unit]]): IO[Unit] = /* TODO IO {
      generator.writeStartArray
      f(this).foreach(_.unsafePerformIO)
      generator.writeEndArray
    }
    */

    def output(): Config = config

    private def withKey[T](key: String)(f: => T): T = {
      val before = current
      current = if (current.isEmpty) key else current + "." + key
      val value = f
      current = before
      value
    }

    private var current = ""

    private lazy val config = new Config
  }

  class ConfigExtractor(config: Config) extends EntityExtractor[String] {
    def readByteString(): IO[ByteString] = // TODO IO { ByteString(current.binaryValue) }

    def readField[T](name: String)(f: EntityExtractor[String] => IO[T]): IO[T] = IO {
      withKey(name) { f(this).unsafePerformIO }
    }

    def readObject[T](f: EntityExtractor[String] => IO[T]): IO[T] = f(this)

    def readBoolean(): IO[Boolean] = IO { config.getBoolean(current) }
    def readChar(): IO[Char] = IO { config.getString(current).head }
    def readDouble(): IO[Double] = IO { config.getDouble(current) }
    def readFloat(): IO[Float] = IO { config.getFloat(current) }
    def readInt(): IO[Int] = IO { config.getInt(current) }
    def readLong(): IO[Long] = IO { config.getLong(current) }
    def readShort(): IO[Short] = IO { config.getShort(current) }
    def readString(): IO[String] = IO { config.getString(current) }

    def readSeq[T](f: EntityExtractor[String] => IO[T]): IO[Seq[T]] = /* TODO IO {
      current.elements.asScala.map(node => {
        withNode(node) { f(this).unsafePerformIO }
      }).toSeq
    }
    */

    private def withKey[T](key: String)(f: => T): T = {
      val before = current
      current = if (current.isEmpty) key else current + "." + key
      val value = f
      current = before
      value
    }

    private var current = ""
  }
}

