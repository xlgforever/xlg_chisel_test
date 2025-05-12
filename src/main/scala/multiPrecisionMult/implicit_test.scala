package multiPrecisionMult


import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class implicit_test(implicit ev : Arithmetic[UInt]) extends Module{
	import ev._ // 引入隐式转换
  val io = FlatIO(new Bundle{
	val a = Input(UInt(8.W))
	val b = Input(UInt(8.W))
	val c = Output(UInt(8.W))
  })

  // 使用隐式转换
  io.c := 1.U(8.W).mac(io.a,io.b)
}

object implicit_test extends App {
  ChiselStage.emitSystemVerilogFile(
    new implicit_test(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}