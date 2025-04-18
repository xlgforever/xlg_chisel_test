package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class lod_2() extends Module {
  val io = FlatIO(new Bundle {
	val din = Input(UInt(2.W))

	val out_vld = Output(Bool())
	val out_data = Output(UInt(1.W))
  })

  io.out_vld := io.din.orR
  io.out_data := (!io.din(1) && io.din(0))
}

class lod_sub(lodInputWidth:Int=4, lodTopWidth:Int=64, manWidthFp64: Int=52,manWidthFp32:Int=23) extends Module {
	val lodHalfWidth = lodInputWidth / 2
	val lodOutputWidth = log2Ceil(lodInputWidth)
}

object lod_sub extends App {
  ChiselStage.emitSystemVerilogFile(
    new lod_sub(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}