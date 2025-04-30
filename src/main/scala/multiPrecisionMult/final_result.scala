package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class final_result(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends  Module{
	val sp_norm_width = math.pow(2,log2Ceil(manWidthFp32)).toInt
	val dp_norm_width = math.pow(2,log2Ceil(manWidthFp64)).toInt
	val io = FlatIO(new Bundle {
		val mode = Input(Bool()) // 1表示FP64模式，0表示FP32模式
		// 指数部分
		val dp_sp2_e_stage3 = Input(UInt((expWidthFp64+2).W))
		val sp1_e_stage3 = Input(UInt((expWidthFp32+2).W))
		val sp2_e_stage3 = Input(UInt((expWidthFp32+2).W))
		// 正负号部分
		val sp1_s_stage1 = Input(Bool())
		val sp2_s_stage1 = Input(Bool())
		val dp_s_stage1  = Input(Bool())
		// 尾数部分，如果是dp模式，两个合并是最终的尾数，如果是sp模式，hi是sp2的尾数，lo是sp1的尾数
		val man_hi_stage3 = Input(UInt((sp_norm_width+1).W))
		val man_lo_stage3 = Input(UInt((sp_norm_width+1).W))

		val sp1_result = Output(UInt((expWidthFp32+manWidthFp32+1).W))
		val sp2_result = Output(UInt((expWidthFp32+manWidthFp32+1).W))
		val dp_result =  Output(UInt((expWidthFp64+manWidthFp64+1).W))
	})

	def fp_is_overflow(exp:UInt, expWidth:Int):Bool = {
		val is_pos = ~exp(expWidth+1).asBool // 符号位为0，表示指数是正数
		val is_overflow = exp(expWidth-1, 0).andR || exp(expWidth).asBool // 指数部分全为0，或者发生了进位

		val is_pos_and_overflow = is_pos && is_overflow
		is_pos_and_overflow
	}

	def fp_is_underflow(exp:UInt) : Bool = {
		val is_neg = exp(exp.getWidth-1).asBool // 如果exp是负数说明太小了无法表示
		is_neg
	}
	// 将符号位打2拍对齐
	val sp1_s_stage3 = ShiftRegister(io.sp1_s_stage1, 2)
	val sp2_s_stage3 = ShiftRegister(io.sp2_s_stage1, 2)
	val dp_s_stage3 = ShiftRegister(io.dp_s_stage1, 2)
	

	

	// final output
	class fp64_r(expWidthFp32:Int=expWidthFp32, manWidthFp32:Int=manWidthFp32)extends Bundle {
  		val high = UInt((1+expWidthFp32+manWidthFp32).W)
  		val low  = UInt((1+expWidthFp32+manWidthFp32).W)
	}
	val fp64_result  = Reg(new fp64_r(expWidthFp32, manWidthFp32))
	// val fp32_result1 = Reg(UInt((expWidthFp32+manWidthFp32+1).W))
	// val fp32_result2 = Reg(UInt((expWidthFp32+manWidthFp32+1).W))

	val man_dp = Mux(io.man_hi_stage3(sp_norm_width).asBool,
					Cat(io.man_hi_stage3, io.man_lo_stage3(sp_norm_width-1,0)), // 33+32 bits
					Cat(io.man_hi_stage3(sp_norm_width-1,0), io.man_lo_stage3(sp_norm_width-1,0), 0.U(1.W)) // 32+32+1 bits
					)
	val man_sp1 = Mux(io.man_lo_stage3(sp_norm_width).asBool,
					Cat(io.man_lo_stage3),
					Cat(io.man_lo_stage3(sp_norm_width-1,0), 0.U(1.W))
					)
	val man_sp2 = Mux(io.man_hi_stage3(sp_norm_width).asBool,
					  Cat(io.man_hi_stage3),
					  Cat(io.man_hi_stage3(sp_norm_width-1,0), 0.U(1.W))
					 )
				
	val exp_dp_update = Mux(io.man_hi_stage3(sp_norm_width).asBool,
							io.dp_sp2_e_stage3 + 1.U(1.W),
							io.dp_sp2_e_stage3
						)
	val exp_sp1_update = Mux(io.man_lo_stage3(sp_norm_width).asBool,
							io.sp1_e_stage3 + 1.U(1.W),
							io.sp1_e_stage3
						)
	val exp_sp2_update = Mux(io.man_hi_stage3(sp_norm_width).asBool,
							io.sp2_e_stage3 + 1.U(1.W),
							io.sp2_e_stage3
						)
	val sp1_is_overflow = fp_is_overflow(exp_sp1_update, expWidthFp32)
	val sp2_is_overflow = fp_is_overflow(exp_sp2_update, expWidthFp32)
	val dp_is_overflow = fp_is_overflow(exp_dp_update, expWidthFp64)

	val sp1_is_underflow = fp_is_underflow(exp_sp1_update)
	val sp2_is_underflow = fp_is_underflow(exp_sp2_update)
	val dp_is_underflow = fp_is_underflow(exp_dp_update)


	when(io.mode){
		when(dp_is_overflow){//上溢时，指数全为1，尾数全为0
			fp64_result := Cat(dp_s_stage3, Fill(expWidthFp64, 1.U(1.W)), 0.U(manWidthFp64.W)).asTypeOf(new fp64_r(expWidthFp32, manWidthFp32))
		} .elsewhen(dp_is_underflow){//下溢时，指数全为0，尾数保持不变
			fp64_result := Cat(dp_s_stage3, 0.U(expWidthFp64.W), man_dp(man_dp.getWidth-1, man_dp.getWidth-manWidthFp64)).asTypeOf(new fp64_r(expWidthFp32, manWidthFp32))
		} .elsewhen(dp_is_underflow){//下溢时，指数全为0，尾数保持不变
		}.otherwise{
			fp64_result := Cat(dp_s_stage3, io.dp_sp2_e_stage3(expWidthFp64-1,0), man_dp(man_dp.getWidth-1, man_dp.getWidth-manWidthFp64) ).asTypeOf(new fp64_r(expWidthFp32, manWidthFp32))
		} 
	} .otherwise{
		when(sp1_is_overflow) {
			fp64_result.low := Cat(sp1_s_stage3, Fill(expWidthFp32, 1.U(1.W)), 0.U(manWidthFp32.W))
		} .elsewhen(sp1_is_underflow){
			fp64_result.low := Cat(sp1_s_stage3, 0.U(expWidthFp32.W), man_sp1(sp_norm_width+1-1, sp_norm_width+1-manWidthFp32))
		} .otherwise{
			fp64_result.low := Cat(sp1_s_stage3, exp_sp1_update(expWidthFp32-1,0), man_sp1(sp_norm_width+1-1, sp_norm_width+1-manWidthFp32))
		}

		when(sp2_is_overflow) {
			fp64_result.high := Cat(sp2_s_stage3, Fill(expWidthFp32, 1.U(1.W)), 0.U(manWidthFp32.W))
		} .elsewhen(sp2_is_underflow){
			fp64_result.high := Cat(sp2_s_stage3, 0.U(expWidthFp32.W), man_sp2(sp_norm_width+1-1, sp_norm_width+1-manWidthFp32))
		} .otherwise{
			fp64_result.high := Cat(sp2_s_stage3, exp_sp2_update(expWidthFp32-1,0), man_sp2(sp_norm_width+1-1, sp_norm_width+1-manWidthFp32))
		}
	}

	io.sp1_result := fp64_result.low
	io.sp2_result := fp64_result.high
	io.dp_result  := fp64_result.asUInt
}

object final_result extends App {
  ChiselStage.emitSystemVerilogFile(
    new final_result(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}