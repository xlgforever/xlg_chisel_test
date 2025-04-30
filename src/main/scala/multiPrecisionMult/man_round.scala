package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import os.group

class man_round(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module{
	val sp_norm_width = math.pow(2,log2Ceil(manWidthFp32)).toInt
	val dp_norm_width = math.pow(2,log2Ceil(manWidthFp64)).toInt

	val io = FlatIO (new Bundle {
		val mode = Input(Bool())
		
		val man_shifted_norm_stage2 = Input(UInt((dp_norm_width.W)))

		val man_hi_stage3 = Output(UInt((sp_norm_width+1).W)) // 高2位为整数位
		val man_lo_stage3 = Output(UInt((sp_norm_width+1).W)) // 高2位为整数位
	})


	val sp1_man_shifted_norm = io.man_shifted_norm_stage2(dp_norm_width/2-1,0)
	val sp2_man_shifted_norm = io.man_shifted_norm_stage2(dp_norm_width-1,dp_norm_width-sp_norm_width)
	val dp_man_shifted_norm = io.man_shifted_norm_stage2

	def round_to_nearest(a:UInt, manWidth:Int):UInt = {
		val totalWidth = a.getWidth
		val last_valid_bit = a(totalWidth-manWidth).asBool
		val guard_bit = a(totalWidth-manWidth-1).asBool
		val round_bit = a(totalWidth-manWidth-2).asBool
		val sticky_bit = a(totalWidth-manWidth-3).asBool

		val rounding = (guard_bit && (round_bit || sticky_bit)) || (last_valid_bit && guard_bit && ~(round_bit || sticky_bit))
		rounding
	}

	val sp1_rounding = round_to_nearest(sp1_man_shifted_norm, manWidthFp32)
	val sp2_rounding = round_to_nearest(sp2_man_shifted_norm, manWidthFp32)
	val dp_rounding = round_to_nearest(dp_man_shifted_norm, manWidthFp64)

	val man_hi_tmp = sp1_man_shifted_norm +& Mux(io.mode, dp_rounding ,sp1_rounding)
	io.man_hi_stage3 := RegNext(man_hi_tmp)
	io.man_lo_stage3 := RegNext(sp2_man_shifted_norm +& Mux(io.mode, man_hi_tmp(man_hi_tmp.getWidth-1), sp2_rounding))
}

object man_round extends App {
  ChiselStage.emitSystemVerilogFile(
    new man_round(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}