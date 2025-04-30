package multiPrecisionMult


import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import os.group

class exp_update(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module{
	val io = FlatIO( new Bundle{
		val mode = Input(Bool()) // 1表示FP64模式，0表示FP32模式
		val dp_sp2_e_stage1 = Input(UInt((expWidthFp64+2).W))
		val sp1_e_stage1 = Input(UInt((expWidthFp32+2).W))

		val dp_man_mult_stage2  = Input(UInt(((manWidthFp64+1)*2).W))
		val sp1_man_mult_stage2 = Input(UInt(((manWidthFp32+1)*2).W))
		val sp2_man_mult_stage2 = Input(UInt(((manWidthFp32+1)*2).W))

		val left_shift_sp1_update_stage2 = Input(UInt(log2Ceil(manWidthFp32).W))
		val left_shift_sp2_update_stage2 = Input(UInt(log2Ceil(manWidthFp32).W))
		val left_shift_dp_update_stage2  = Input(UInt(log2Ceil(manWidthFp64).W))

		val sp1_exp_stage3 = Output(UInt((expWidthFp32+2).W))
		val sp2_exp_stage3 = Output(UInt((expWidthFp32+2).W))
		val dp_exp_stage3 = Output(UInt((expWidthFp64+2).W))
	})

	// dp和sp2的指数更新
	val dp_sp2_e_stage2 = RegNext(io.dp_sp2_e_stage1)
	val sp1_e_stage2 = RegNext(io.sp1_e_stage1)

	val dp_sp2_left_shift = Mux(io.mode, io.left_shift_dp_update_stage2, io.left_shift_sp2_update_stage2)
	val dp_sp2_exp_stage3 = RegNext(dp_sp2_e_stage2 + io.dp_man_mult_stage2(io.dp_man_mult_stage2.getWidth-1) - dp_sp2_left_shift)

	io.sp1_exp_stage3 := RegNext(sp1_e_stage2 + io.sp1_man_mult_stage2(io.sp1_man_mult_stage2.getWidth-1) - io.left_shift_sp1_update_stage2)
	io.sp2_exp_stage3 := dp_sp2_exp_stage3(expWidthFp32+1, 0) // 拓展了两位， 10bit
	io.dp_exp_stage3  := dp_sp2_exp_stage3(expWidthFp64+1, 0) // 拓展了两位， 13bit
}

object exp_update extends App {
  ChiselStage.emitSystemVerilogFile(
    new exp_update(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}