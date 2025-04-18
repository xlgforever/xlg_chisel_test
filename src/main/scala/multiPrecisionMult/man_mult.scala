package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class man_mult(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module {
	val io = FlatIO( new Bundle {
		val mode = Input(Bool())
		val a_sp1_m = Input(UInt((manWidthFp32+1).W))
		val b_sp1_m = Input(UInt((manWidthFp32+1).W))
		val a_sp2_m = Input(UInt((manWidthFp32+1).W))
		val b_sp2_m = Input(UInt((manWidthFp32+1).W))
		val a_m  = Input(UInt((manWidthFp64+1).W))
		val b_m  = Input(UInt((manWidthFp64+1).W))

		val dp_man_mult_stage1 = Output(UInt(((manWidthFp64+1)*2).W))
		val sp1_man_mult_stage1 = Output(UInt(((manWidthFp32+1)*2).W))
		val sp2_man_mult_stage1 = Output(UInt(((manWidthFp32+1)*2).W))
	})
/*以下为booth乘法器的相关代码，待实现booth乘法器后再取消注释
// booth乘法器的第一个操作数， 53b
val m1_tmp1 = Mux(io.mode, io.a_m，Cat( Fill(manWidthFp64-manWidthFp32, 0.U(1.W)), io.a_sp1_m) )
val m1_tmp2 = Mux(io.mode, io.a_m, Cat( io.a_sp2_m, Fill(manWidthFp64-manWidthFp32, 0.U(1.W))) )

// booth乘法器的第二个操作数， 53b
val m2_tmp = Mux(io.mode, io.b_m, Cat(io.b_sp2_m, Fill( manWidthFp64+1-(manWidthFp32+1)*2) ,io.b_sp1_m  ) )
 */
// 先用普通的乘法器代替
	io.dp_man_mult_stage1 := RegNext(Mux(io.mode, io.a_m * io.b_m, 0.U))
	io.sp1_man_mult_stage1 := RegNext(Mux(!io.mode, io.a_sp1_m * io.b_sp1_m, 0.U))
	io.sp2_man_mult_stage1 := RegNext(Mux(!io.mode, io.a_sp2_m * io.b_sp2_m, 0.U))
}

object man_mult extends App {
  ChiselStage.emitSystemVerilogFile(
    new man_mult(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}