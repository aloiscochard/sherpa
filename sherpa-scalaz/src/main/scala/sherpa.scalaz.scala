//  __..              
// (__ |_  _ ._.._  _.
// .__)[ )(/,[  [_)(_]
//              |     
//
//  (c) 2012, Alois Cochard
package sherpa

import _root_.scalaz._
import _root_.scalaz.effect._
import Scalaz._

package object scalaz {

  def serializer[F, G <: Generator, E <: Extractor](serializer: Serializer[F, G, E]) = new Serialazy(serializer)

  class Serialazy[F, G <: Generator, E <: Extractor](serializer: Serializer[F, G, E]) {
    def generate[T](value: T)(implicit writer: Writer[T, G]): IO[F] = IO(serializer.generate(value))
    def parse[T](input: F)(implicit reader: Reader[T, E]): IO[Validation[SerializerError, T]] = IO {
      serializer.parse[T](input: F) match {
        case Right(value) => value.success
        case Left(error) => error.fail
      }
    }
  }
}


