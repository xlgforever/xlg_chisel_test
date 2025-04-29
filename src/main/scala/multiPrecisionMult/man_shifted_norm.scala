package multiPrecisionMult


import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class man_shifted_norm(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends  Module{
	val sp_norm_width = math.pow(2,log2Ceil(manWidthFp32)).toInt
	val dp_norm_width = math.pow(2,log2Ceil(manWidthFp64)).toInt
	val io = FlatIO (new Bundle {
		val mode = Input(Bool())

		val l_shift_out_stage2 = Input(UInt((manWidthFp64*2+2).W))
		val r_shift_out_stage2 = Input(UInt((1 << log2Ceil(manWidthFp64)).W))

		val r_shift_dp_sp2_chk_stage1 = Input(Bool())
		val r_shift_sp1_chk_stage1 = Input(Bool())

		val sp1_shift_norm_stage2 = Output(UInt(sp_norm_width.W))
		val sp2_shift_norm_stage2 = Output(UInt(sp_norm_width.W))
		val dp_shift_norm_stage2  = Output(UInt(dp_norm_width.W))
	})

	val r_shift_dp_sp2_chk_dly = RegNext(io.r_shift_dp_sp2_chk_stage1)
	val r_shift_sp1_chk_dly = RegNext(io.r_shift_sp1_chk_stage1)
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// sp1左移或者右移的结果
	val sp1_r_shift_out = io.r_shift_out_stage2((1 << log2Ceil(manWidthFp64))/2-1,0)
	val sp1_l_shift_out = io.l_shift_out_stage2((manWidthFp64*2+2)/2-1,0)

	// sp1右移结果的处理
	// 单精度浮点数尾数相乘的结果是48bit，包含了2bit整数位，右移时，仅对48bit中的高32bit进行右移处理
	// 所以，如果 sp1_r_shift_out 的MSB如果是1，则表明sp1没有进行任何右移，否则该bit一定为0；所以此时sp1的尾数右移结果归一化后就是 sp1_r_shift_out 本身
	// 如果 sp1_r_shift_out 的MSB为0，则表示去掉最高位，在最低位后面补1个0
	val sp1_r_shift_norm = Mux(sp1_r_shift_out((1 << log2Ceil(manWidthFp64))-1),
											 sp1_r_shift_out,
											 Cat(sp1_r_shift_out((1 << log2Ceil(manWidthFp64))-2, 0), 0.U(1.W))
											 )
	// sp1左移结果的处理
	// 左移时，使用了尾数相乘结果的48bit的所有bit位
	// 左移同理，只不过由于左移时使用了所有的48bit位，所以当sp1_l_shift_out的MSB为0时不是补0，而是直接取下一位
	val sp1_l_shift_norm = Mux(sp1_l_shift_out((manWidthFp64*2+2)/2-1),
											 sp1_l_shift_out((manWidthFp64*2+2)/2-1, (manWidthFp64*2+2)/2-sp_norm_width),
											 sp1_l_shift_out((manWidthFp64*2+2)/2-2, (manWidthFp64*2+2)/2-sp_norm_width-1),
											 )
	// sp1的尾数的归一化输出
	val sp1_shift_norm = Mux(r_shift_sp1_chk_dly, sp1_r_shift_norm, sp1_l_shift_norm)
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// sp2左移或者右移的结果
	val sp2_r_shift_out = io.r_shift_out_stage2((1 << log2Ceil(manWidthFp64))-1, (1 << log2Ceil(manWidthFp64))/2)
	val sp2_l_shift_out = io.l_shift_out_stage2((manWidthFp64*2+2)-1, (manWidthFp64*2+2)/2)
	// sp2 右移结果的处理
	val sp2_r_shift_norm = Mux(sp2_r_shift_out((1 << log2Ceil(manWidthFp64))-1),
							   sp2_r_shift_out,
							   Cat(sp2_r_shift_out((1 << log2Ceil(manWidthFp64))-2, 0), 0.U(1.W))
							  )

	// sp2左移结果的处理
	val sp2_l_shift_norm = Mux(sp2_l_shift_out((manWidthFp64*2+2)/2-1),
											 sp2_l_shift_out((manWidthFp64*2+2)/2-1, (manWidthFp64*2+2)/2-sp_norm_width),
											 sp2_l_shift_out((manWidthFp64*2+2)/2-2, (manWidthFp64*2+2)/2-sp_norm_width-1),
											 )

	// sp2的尾数的归一化输出
	val sp2_shift_norm = Mux(r_shift_dp_sp2_chk_dly, sp2_r_shift_norm, sp2_l_shift_norm)
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// dp左移或者右移的结果
val dp_r_shift_out = io.r_shift_out_stage2((1 << log2Ceil(manWidthFp64))-1, 0)
val dp_l_shift_out = io.l_shift_out_stage2((manWidthFp64*2+2)-1, 0)

// dp右移结果的处理
val dp_r_shift_norm = Mux(dp_r_shift_out((1 << log2Ceil(manWidthFp64))-1),
						  dp_r_shift_out,
						  Cat(dp_r_shift_out((1 << log2Ceil(manWidthFp64))-2, 0), 0.U(1.W))
						 )

// dp左移结果的处理
val dp_l_shift_norm = Mux(dp_l_shift_out((manWidthFp64*2+2)-1),
						  dp_l_shift_out((manWidthFp64*2+2)-1, (manWidthFp64*2+2)-dp_norm_width),
						  dp_l_shift_out((manWidthFp64*2+2)-2, (manWidthFp64*2+2)-dp_norm_width-1),
						 )

// dp的尾数的归一化输出
val dp_shift_norm = Mux(r_shift_dp_sp2_chk_dly, dp_r_shift_norm, dp_l_shift_norm)
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

io.dp_shift_norm_stage2 := Mux(io.mode, dp_shift_norm, 0.U)
io.sp1_shift_norm_stage2 := Mux(io.mode, 0.U, sp1_shift_norm)
io.sp2_shift_norm_stage2 := Mux(io.mode, 0.U, sp2_shift_norm)
}