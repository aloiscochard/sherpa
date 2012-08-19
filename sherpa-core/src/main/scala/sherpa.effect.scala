//  __..              
// (__ |_  _ ._.._  _.
// .__)[ )(/,[  [_)(_]
//              |     
//
//  (c) 2012, Alois Cochard
package sherpa
package effect

object IO {
  def apply[T](value: => T) = new IO[T] { override def unsafePerformIO() = value }
}

trait IO[A] {
  def map[B](f: A => B): IO[B] = IO(f(this.unsafePerformIO()))
  def flatMap[B](f: A => IO[B]) = IO(f(this.unsafePerformIO()).unsafePerformIO())
  def unsafePerformIO(): A
}
