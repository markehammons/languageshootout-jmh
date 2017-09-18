package mhammons.cnrs

class AdderImpl(val i: Int) extends Adder {
  override def add(o: Int): Int = i + o
}
