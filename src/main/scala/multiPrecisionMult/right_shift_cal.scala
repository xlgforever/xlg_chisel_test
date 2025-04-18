package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class right_shift_cal(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module {
	val io = FlatIO (new Bundle {
		val mode = Input(Bool())

		val dp_sp2_e_stage1 = Input(UInt((expWidthFp64+2).W))
		val sp1_e_stage1 = Input(UInt((expWidthFp32+2).W))
		val sp2_e_stage1 = Input(UInt((expWidthFp32+2).W))
		val dp_e_sgate1 = Input(UInt((expWidthFp64+2).W))

		val a_dp_sp2_e = Input(UInt((expWidthFp64).W))
		val b_dp_sp2_e = Input(UInt((expWidthFp64).W))

		val dp_man_mult_stage1 = Input(UInt(((manWidthFp64+1)*2).W))
		val sp1_man_mult_stage1 = Input(UInt(((manWidthFp32+1)*2).W))
		val sp2_man_mult_stage1 = Input(UInt(((manWidthFp32+1)*2).W))

		val a_sp1_e = Input(UInt(expWidthFp32.W))
		val b_sp1_e = Input(UInt(expWidthFp32.W))

		val r_shift_dp_stage2 = Output(UInt((log2Ceil(manWidthFp64)).W))
		val r_shift_sp1_stage2 = Output(UInt((log2Ceil(manWidthFp32)).W))
		val r_shift_sp2_stage2 = Output(UInt((log2Ceil(manWidthFp32)).W))
		val r_shift_dp_sp2_chk_stage1 = Output(Bool())
		val r_shift_sp1_chk_stage1 = Output(Bool())
	})

	//将输入信号都对齐到stage1
	val a_dp_sp2_e_stage1 = RegNext(io.a_dp_sp2_e)
	val b_dp_sp2_e_stage1 = RegNext(io.b_dp_sp2_e)
	val a_sp1_e_stage1 = RegNext(io.a_sp1_e)
	val b_sp1_e_stage1 = RegNext(io.b_sp1_e)
	
	// dp sp2 sp1是否需要右移；如果符号位为1，代表指数相加-BIAS的结果为负数，所以需要右移；或者结果为全0，也需要右移；
	io.r_shift_dp_sp2_chk_stage1 := io.dp_sp2_e_stage1(expWidthFp64+2-1).asBool || (!io.dp_sp2_e_stage1.orR)
	io.r_shift_sp1_chk_stage1 := io.sp1_e_stage1(expWidthFp32+2-1).asBool || (!io.sp1_e_stage1.orR)
	
	// 如果尾数相乘的MSB为1，也需要右移
	val dp_sp2_man_msb_is_one = Mux(io.mode, io.dp_man_mult_stage1((manWidthFp64+1)*2-1), io.sp2_man_mult_stage1((manWidthFp32+1)*2-1) )
	// sp2和dp的右移量
	val r_shift_dp_sp2_tmp = RegNext( Cat( Fill(expWidthFp64-expWidthFp32, io.mode.asUInt), ((1 << (expWidthFp32-1))-1).U((expWidthFp32-1).W) ) - (a_dp_sp2_e_stage1 + b_dp_sp2_e_stage1) + dp_sp2_man_msb_is_one.asUInt)
	// sp1的右移量
	io.r_shift_sp1_stage2 := RegNext( Mux( io.mode, 
										0.U, 
										((1 << (expWidthFp32-1))-1).U((expWidthFp32-1).W) - (a_sp1_e_stage1 & b_sp1_e_stage1) + io.sp1_man_mult_stage1((manWidthFp32+1)*2-1)
										)
								)
	
	io.r_shift_dp_stage2 := Mux(io.mode, r_shift_dp_sp2_tmp(log2Ceil(manWidthFp64)-1, 0), 0.U)
	io.r_shift_sp2_stage2 := Mux(io.mode, 0.U, r_shift_dp_sp2_tmp(log2Ceil(manWidthFp32)-1, 0))

}

object right_shift_cal extends App {
  ChiselStage.emitSystemVerilogFile(
    new right_shift_cal(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}