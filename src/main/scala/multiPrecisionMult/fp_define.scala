package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class fp_number (expWidth: Int, manWidth: Int) extends Bundle {
  val sign = Bool()
  val exp  = UInt(expWidth.W)
  val man  = UInt(manWidth.W)
}

// 隐式转换在两种情况下会发生
// 1. 如果表达式e是类型S，并且S不符合表达式的期望类型T
// 2. 在具有类型S的e的e.m表达中，如果m不表示S的成员
// 此处，重点是第二种情况用的比较多
// 首先会在当前代码作用域下查找隐式实体（隐式方法、隐式类、隐式对象）。（一般是这种情况）
// 如果第一条规则查找隐式实体失败，会继续在隐式参数的类型的作用域里查找。类型的作用域是指与该类型相关联的全部伴生对象以及该类型所在包的包对象。


// 隐式类的主构造函数参数有且仅有一个！之所以只能有一个参数，是因为隐式转换是将一种类型转换为另外一种类型，源类型与目标类型是一一对应的；
// implicit修饰符修饰类时只能存在于“类”或“伴生对象”或“包对象”或“特质”之内，即隐式类不能是顶级的；
// 隐式类不能是case class； 
// 在同一作用域内，不能有任何方法、成员或对象与隐式类同名；

/* 
Scala 的隐式解析顺序为：

1. 当前作用域的隐式值（如局部 implicit val）。
2. 相关类型的伴生对象（如 A 的伴生对象中的 AInt）。
3. 显式导入的隐式（如 import A._）。
 */
////////////////////////////////////////////////举例说明/////////////////////////////////////////////////////
 /* 
 abstract class A[T] {
  implicit def toOps(t: T): AOps[T]  // 抽象隐式方法
}

class AOps[T](t: T) {
  def show: String = s"AOps($t)"
}

object A {
  // 为 Int 提供隐式实例
  implicit object AInt extends A[Int] {
    override implicit def toOps(i: Int): AOps[Int] = new AOps(i) {
      override def show: String = s"CustomAOps($i)"  // 自定义行为
    }
  }
}

// 测试
val ops = 42.toOps  // 隐式转换触发 AInt.toOps
println(ops.show)   // 输出: CustomAOps(42)
  */
/* 
首先要明确的概念是，A[Int]是一个类型，AInt是A[Int]的隐式实例；
因此实际上是，A[Int] 的隐式实例（AInt）提供了 toOps 方法，将 Int 转换为 AOps[Int]。

为什么编译器会使用该隐式实例：
首先需要找到一个能将Int转成有toOps方法的类型，发现A[T]定义了implicit def toOps，符合要求，（为什么能发现，因为此处是位于同一个package中，如果是不同package，则需要导入）
于是需要A[Int]的实例，自动检查A的伴生对象​​，找到implicit object AInt

42的类型是Int，没有toOps方法；
编译器发现A[Int]的隐式实例是AInt，因此调用AInt.toOps，返回new AOps（42）
AOps实例调用show方法，输出CustomAOps(42)。

 */

 abstract class Arithmetic[T <: Data] {
  implicit def cast(t: T): ArithmeticOps[T]
}

abstract class ArithmeticOps[T <: Data](self: T) {
  def *(t: T): T
  def mac(m1: T, m2: T): T // Returns (m1 * m2 + self)
  def +(t: T): T
  def -(t: T): T
  // def >>(u: UInt): T // This is a rounding shift! Rounds away from 0
  // def >(t: T): Bool
  // def identity: T
  // def withWidthOf(t: T): T
  // def clippedToWidthOf(t: T): T // Like "withWidthOf", except that it saturates
  // def relu: T
  // def zero: T
  // def minimum: T

  // // Optional parameters, which only need to be defined if you want to enable various optimizations for transformers
  // def divider(denom_t: UInt, options: Int = 0): Option[(DecoupledIO[UInt], DecoupledIO[T])] = None
  // def sqrt: Option[(DecoupledIO[UInt], DecoupledIO[T])] = None
  // def reciprocal[U <: Data](u: U, options: Int = 0): Option[(DecoupledIO[UInt], DecoupledIO[U])] = None
  // def mult_with_reciprocal[U <: Data](reciprocal: U) = self
}


object Arithmetic {
  implicit object UIntArithmetic extends Arithmetic[UInt] {
    override def cast(self: UInt): ArithmeticOps[UInt] = new ArithmeticOps(self) {
      override def *(t: UInt): UInt = self * t
      override def mac(m1: UInt, m2: UInt): UInt = (m1 * m2) + self
      override def +(t: UInt): UInt = self + t
      override def -(t: UInt): UInt = self - t
    }
  }
}