//  __..              
// (__ |_  _ ._.._  _.
// .__)[ )(/,[  [_)(_]
//              |     
//
//  (c) 2012, Alois Cochard

import sherpa.effect._

// TODO Option
// TODO Array (fixed size)
// TODO Stream!
// TODO BigInteger

package object sherpa {

  // TODO Add typeclass instance for primitive type
  trait Serializable[T] {
    def put(value: T): ByteString
    def get(data: ByteString):  T
  }

  trait Persistable[T, G <: Generator, E <: Extractor] {
    object Reader { def apply(r: Reader[T, E]) = r }
    object Writer { def apply(w: Writer[T, G]) = w }

    implicit def writer: Writer[T, G]
    implicit def reader: Reader[T, E]

    def generate[F](value: T)(implicit serializer: Serializer[F, G, E]) =
      serializer.generate(value)
    def parse[F](input: F)(implicit serializer: Serializer[F, G, E]) =
      serializer.parse[T](input)
  }


  type Writer[T, G <: Generator] = T => G => IO[Unit]
  object Writer { def apply[T, G <: Generator](f: T => G => IO[Unit]) = f }

  type Reader[T, E <: Extractor] = E => IO[T]
  object Reader { def apply[T, E <: Extractor](f: E => IO[T]) = f }

  implicit def s2w[T](implicit
    serializable: Serializable[T],
    writer: Writer[ByteString, BinaryGenerator]
  ): Writer[T, BinaryGenerator] =
    value => writer(serializable.put(value))

  implicit def s2r[T](implicit
    serializable: Serializable[T],
    reader: Reader[ByteString, BinaryExtractor]
  ): Reader[T, BinaryExtractor] =
    extractor => reader(extractor).map(serializable.get(_))

  trait Serializer[F, G <: Generator, E <: Extractor] {
    def generate[T](value: T)(implicit writer: Writer[T, G]): F
    def parse[T](input: F)(implicit reader: Reader[T, E]): Either[SerializerError, T]
  }

  // TODO Add other errors
  sealed trait SerializerError
  case object InvalidFormat extends SerializerError

  trait Generator { def write[T](value: T)(implicit writer: Writer[T, this.type]): IO[Unit] = writer(value)(this) }
  trait Extractor { def read[T](implicit reader: Reader[T, this.type]): IO[T] = reader(this) }

  trait BinaryGenerator extends Generator { def writeByteString(value: ByteString): IO[Unit] }
  trait BinaryExtractor extends Extractor { def readByteString(): IO[ByteString] }

  trait PrimitiveGenerator extends Generator {
    def writeBoolean(value: Boolean): IO[Unit]
    def writeChar(value: Char): IO[Unit]
    def writeDouble(value: Double): IO[Unit]
    def writeFloat(value: Float): IO[Unit]
    def writeInt(value: Int): IO[Unit]
    def writeLong(value: Long): IO[Unit]
    def writeShort(value: Short): IO[Unit]
    def writeString(value: String): IO[Unit]
  }

  trait PrimitiveExtractor extends Extractor {
    def readBoolean(): IO[Boolean]
    def readChar(): IO[Char]
    def readDouble(): IO[Double]
    def readFloat(): IO[Float]
    def readInt(): IO[Int]
    def readLong(): IO[Long]
    def readShort(): IO[Short]
    def readString(): IO[String]
  }

  trait FieldGenerator[N, FG <: Generator] extends Generator { def writeField[T](name: N)(f: FG => IO[T]): IO[Unit] }
  trait FieldExtractor[N, FE <: Extractor] extends Extractor { def readField[T](name: N)(f: FE => IO[T]): IO[T] }

  trait SequenceGenerator[SG <: Generator] extends Generator { def writeSeq[T](f: SG => Seq[IO[Unit]]): IO[Unit] }
  trait SequenceExtractor[SE <: Extractor] extends Extractor { def readSeq[T](f: SE => IO[T]): IO[Seq[T]] }

  trait ObjectGenerator[OG <: Generator] extends Generator { def writeObject[T](f: OG => IO[T]): IO[Unit] }
  trait ObjectExtractor[OE <: Extractor] extends Extractor { def readObject[T](f: OE => IO[T]): IO[T] }

  type SimpleGenerator = BinaryGenerator with PrimitiveGenerator
  type SimpleExtractor = BinaryExtractor with PrimitiveExtractor

  type PropertyGenerator[T] = FieldGenerator[T, BinaryGenerator with PrimitiveGenerator]
  type PropertyExtractor[T] = FieldExtractor[T, BinaryExtractor with PrimitiveExtractor]
  
  trait EntityGenerator[T] extends
    BinaryGenerator with
    PrimitiveGenerator with
    FieldGenerator[T, EntityGenerator[T]] with
    SequenceGenerator[EntityGenerator[T]] with
    ObjectGenerator[EntityGenerator[T]]

  trait EntityExtractor[T] extends
    BinaryExtractor with
    PrimitiveExtractor with
    FieldExtractor[T, EntityExtractor[T]] with
    SequenceExtractor[EntityExtractor[T]] with
    ObjectExtractor[EntityExtractor[T]]

  type DefaultGenerator = EntityGenerator[String]
  type DefaultExtractor = EntityExtractor[String]

  implicit val booleanW: Writer[Boolean, PrimitiveGenerator] = value => generator => generator.writeBoolean(value)
  implicit val booleanR: Reader[Boolean, PrimitiveExtractor] = extractor => extractor.readBoolean

  implicit val charW: Writer[Char, PrimitiveGenerator] = value => generator => generator.writeChar(value)
  implicit val charR: Reader[Char, PrimitiveExtractor] = extractor => extractor.readChar

  implicit val doubleW: Writer[Double, PrimitiveGenerator] = value => generator => generator.writeDouble(value)
  implicit val doubleR: Reader[Double, PrimitiveExtractor] = extractor => extractor.readDouble

  implicit val floatW: Writer[Float, PrimitiveGenerator] = value => generator => generator.writeFloat(value)
  implicit val floatR: Reader[Float, PrimitiveExtractor] = extractor => extractor.readFloat

  implicit val intW: Writer[Int, PrimitiveGenerator] = value => generator => generator.writeInt(value)
  implicit val intR: Reader[Int, PrimitiveExtractor] = extractor => extractor.readInt

  implicit val longW: Writer[Long, PrimitiveGenerator] = value => generator => generator.writeLong(value)
  implicit val longR: Reader[Long, PrimitiveExtractor] = extractor => extractor.readLong

  implicit val shortW: Writer[Short, PrimitiveGenerator] = value => generator => generator.writeShort(value)
  implicit val shortR: Reader[Short, PrimitiveExtractor] = extractor => extractor.readShort

  implicit val stringW: Writer[String, PrimitiveGenerator] = value => generator => generator.writeString(value)
  implicit val stringR: Reader[String, PrimitiveExtractor] = extractor => extractor.readString

  implicit def seqW[T, G <: Generator](implicit writer: Writer[T, G]): Writer[Seq[T], SequenceGenerator[G]] =
    xs => generator => generator.writeSeq(s => xs.map(s.write(_)))
  implicit def seqR[T, E <: Extractor](implicit reader: Reader[T, E]): Reader[Seq[T], SequenceExtractor[E]] =
    extractor => extractor.readSeq(_.read[T])
}
