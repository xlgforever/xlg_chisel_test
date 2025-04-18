package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class sign_exp_proc (expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module{
	val io = FlatIO(new Bundle {
		val mode = Input(Bool()) // 1表示FP64模式，0表示FP32模式
		val a_sp1_s = Input(Bool())
		val b_sp1_s = Input(Bool())
		val a_sp1_e = Input(UInt(expWidthFp32.W))
		val b_sp1_e = Input(UInt(expWidthFp32.W))

		val a_sp2_s = Input(Bool())
		val b_sp2_s = Input(Bool())
		val a_sp2_e = Input(UInt(expWidthFp32.W))
		val b_sp2_e = Input(UInt(expWidthFp32.W))

		val a_s  = Input(Bool())
		val b_s  = Input(Bool())
		val a_e  = Input(UInt(expWidthFp64.W))
		val b_e  = Input(UInt(expWidthFp64.W))

		val sp1_s_stage1 = Output(Bool())
		val sp2_s_stage1 = Output(Bool())
		val dp_s_stage1  = Output(Bool())

		val sp1_e_stage1 = Output(UInt((expWidthFp32+2).W))
		val sp2_e_stage1 = Output(UInt((expWidthFp32+2).W))
		val dp_e_stage1  = Output(UInt((expWidthFp64+2).W))
		val dp_sp2_e_stage1 = Output(UInt((expWidthFp64+2).W))

		val a_dp_sp2_e   = Output(UInt((expWidthFp64).W))
		val b_dp_sp2_e   = Output(UInt((expWidthFp64).W))
	})

	// 将SP2的指数部分扩展到和FP64指数一样的宽度，并进行选择
	val a_dp_sp2_e_tmp = Mux(io.mode, io.a_e, Cat(0.U((expWidthFp64-expWidthFp32).W),io.a_sp2_e))
	val b_dp_sp2_e_tmp = Mux(io.mode, io.b_e, Cat(0.U((expWidthFp64-expWidthFp32).W),io.b_sp2_e))
	io.a_dp_sp2_e := a_dp_sp2_e_tmp// 其他模块也要用
	io.b_dp_sp2_e := b_dp_sp2_e_tmp

	// sp1_s 输出
	io.sp1_s_stage1 := RegNext(Mux(!io.mode, io.a_sp1_s ^ io.b_sp1_s, 0.U(1.W)))
	// sp2_s 输出
	io.sp2_s_stage1 := RegNext(Mux(!io.mode, io.a_sp2_s ^ io.b_sp2_s, 0.U(1.W)))
	// dp_s 输出
	io.dp_s_stage1  := RegNext(Mux(io.mode, io.a_s ^ io.b_s, 0.U(1.W)))

	// dp_sp2_e_stage1，因为SP2和DP的指数部分是重叠的，所以SP2和DP的指数相加的逻辑进行复用
	val dp_sp2_e_stage1_tmp = RegNext(a_dp_sp2_e_tmp +& b_dp_sp2_e_tmp -& Cat( Fill(expWidthFp64-expWidthFp32 ,io.mode.asUInt), ((1 << (expWidthFp32-1))-1).U((expWidthFp32-1).W) ) )

	// sp1的指数相加没有重叠，单独计算
	io.sp1_e_stage1 := RegNext(
		Mux(!io.mode,
			( io.a_sp1_e +& io.b_sp1_e -& ((1 << (expWidthFp32-1))-1).U((expWidthFp32-1).W)),
			0.U((expWidthFp32+2).W)
		)
	)

	io.dp_e_stage1 := Mux(io.mode, dp_sp2_e_stage1_tmp, 0.U((expWidthFp64+2).W))
	io.sp2_e_stage1 := 
		Mux(!io.mode,
			dp_sp2_e_stage1_tmp(expWidthFp32+1, 0),
			0.U
		)
	io.dp_sp2_e_stage1 := dp_sp2_e_stage1_tmp
}

object sign_exp_proc extends App {
  ChiselStage.emitSystemVerilogFile(
    new sign_exp_proc(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}