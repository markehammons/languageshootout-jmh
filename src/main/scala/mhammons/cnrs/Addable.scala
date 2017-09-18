package mhammons.cnrs

trait Addable[T] {
  def add(t: T, i: Int): Int
}

object Addable {
  implicit case object AAbleClassIsAAble extends Addable[AdderImpl] {
    def add(addable: AdderImpl, o: Int) = addable.add(o)
  }

}