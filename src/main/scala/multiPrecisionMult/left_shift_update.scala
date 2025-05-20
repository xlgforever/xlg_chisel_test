package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class left_shift_update(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends  Module{
	val io = FlatIO (new Bundle {
		val mode_sel = Input(Bool())

		val left_shift_dp_stage1 = Input(UInt(log2Ceil(manWidthFp64).W))
		val left_shift_sp1_stage1 = Input(UInt(log2Ceil(manWidthFp32).W))
		val left_shift_sp2_stage1 = Input(UInt(log2Ceil(manWidthFp32).W))

		val sp1_e_stage1 = Input(UInt((expWidthFp32+2).W))
		val sp2_e_stage1 = Input(UInt((expWidthFp32+2).W))
		val dp_e_stage1  = Input(UInt((expWidthFp64+2).W))

		val left_shift_sp1_update_stage2 = Output(UInt(log2Ceil(manWidthFp32).W))
		val left_shift_sp2_update_stage2 = Output(UInt(log2Ceil(manWidthFp32).W))
		val left_shift_dp_update_stage2 = Output(UInt(log2Ceil(manWidthFp64).W))
	})

	val sp1_exp_neg = io.sp1_e_stage1(expWidthFp32+2-1)
	val sp1_exp_smaller = io.sp1_e_stage1(expWidthFp32,0) < io.left_shift_sp1_stage1

	val sp2_exp_neg = io.sp2_e_stage1(expWidthFp32+2-1)
	val sp2_exp_smaller = io.sp2_e_stage1(expWidthFp32,0) < io.left_shift_sp2_stage1

	val dp_exp_neg = io.dp_e_stage1(expWidthFp64+2-1)
	val dp_exp_smaller = io.dp_e_stage1(expWidthFp64,0) < io.left_shift_dp_stage1

	val left_shift_sp1_updaate = Reg(UInt(log2Ceil(manWidthFp32).W))
	val left_shift_sp2_updaate = Reg(UInt(log2Ceil(manWidthFp32).W))
	val left_shift_dp_updaate = Reg(UInt(log2Ceil(manWidthFp64).W))

	when(io.mode_sel){
		left_shift_sp1_updaate := 0.U
		left_shift_sp2_updaate := 0.U
		left_shift_dp_updaate := Mux(dp_exp_neg, 0.U, Mux(dp_exp_smaller, io.dp_e_stage1 -1.U, io.left_shift_dp_stage1) )
	} .otherwise {
		left_shift_sp1_updaate := Mux(sp1_exp_neg, 0.U, Mux(sp1_exp_smaller, io.sp1_e_stage1 - 1.U, io.left_shift_sp1_stage1))
		left_shift_sp2_updaate := Mux(sp2_exp_neg, 0.U, Mux(sp2_exp_smaller, io.sp2_e_stage1 - 1.U, io.left_shift_sp2_stage1))
		left_shift_dp_updaate := 0.U
	}

	io.left_shift_sp1_update_stage2 := left_shift_sp1_updaate
	io.left_shift_sp2_update_stage2 := left_shift_sp2_updaate
	io.left_shift_dp_update_stage2 := left_shift_dp_updaate
}

object left_shift_update extends App {
  ChiselStage.emitSystemVerilogFile(
    new left_shift_update(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}