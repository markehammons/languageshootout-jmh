package mhammons.cnrs

trait Tree {
  def itemCheck: Int
  def left: Tree
  def right: Tree

}

final class TreeNode(val left: Tree, val right: Tree) extends Tree {
  def itemCheck: Int = { // if necessary deallocate here
    if (left == null) 1
    else
      1 + left.itemCheck + right.itemCheck
  }

  def this() = this(null, null)
}

class EmptyTree() extends Tree {
  def left: Tree = null
  def right: Tree = null
  def itemCheck = 1
}
