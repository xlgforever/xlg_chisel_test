package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class right_shift(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module {
	
	val io = FlatIO(new Bundle {
		val mode = Input(Bool())

		// 三个右移量
		val r_shift_dp_stage2 = Input(UInt((log2Ceil(manWidthFp64)).W))
		val r_shift_sp1_stage2 = Input(UInt((log2Ceil(manWidthFp32)).W))
		val r_shift_sp2_stage2 = Input(UInt((log2Ceil(manWidthFp32)).W))

		// 尾数相乘的结果
		val dp_man_mult_stage1 = Input(UInt(((manWidthFp64+1)*2).W))
		val sp1_man_mult_stage1 = Input(UInt(((manWidthFp32+1)*2).W))
		val sp2_man_mult_stage1 = Input(UInt(((manWidthFp32+1)*2).W))

		//右移的结果
		val r_shift_out = Output(UInt((2^log2Ceil(manWidthFp64)).W))

		// 尾数相乘的延迟后的输出
		val dp_man_mult_stage2 = Output(UInt(((manWidthFp64+1)*2).W))
		val sp1_man_mult_stage2 = Output(UInt(((manWidthFp32+1)*2).W))
		val sp2_man_mult_stage2 = Output(UInt(((manWidthFp32+1)*2).W))
	})

	val dp_man_mult_stage2_tmp = RegNext(io.dp_man_mult_stage1)
	val sp1_man_mult_stage2_tmp = RegNext(io.sp1_man_mult_stage1)
	val sp2_man_mult_stage2_tmp = RegNext(io.sp2_man_mult_stage1)

	io.dp_man_mult_stage2 := dp_man_mult_stage2_tmp
	io.sp1_man_mult_stage2 := sp1_man_mult_stage2_tmp
	io.sp2_man_mult_stage2 := sp2_man_mult_stage2_tmp

	// 生成输入
	// 为什么FP64只取尾数乘法结果106位的高64位？因为r_shift_dp_stage2为6bit
	// 为什么FP32只取尾数乘法结果48位的高32位？ 因为r_shift_sp1_stage2为5bit
	// 以FP32为例，假设两个sp1的指数位exp1和exp2, 假设127-(exp1+exp2) = 32（6'b10_0000），
	// 这意味着要将尾数右移125位，而尾数相乘的结果只有48位，所以计算FP32的右移量时，最多只需要log2Ceil((manWidthFp32+1)*2)位即可，大于48位的直接赋值为48
	// 本例中，由于只取了最后5bit，所以r_shift_sp1_stage2为0；这难道不是背离了初衷？应该加一个判断，大于31，就直接右移31位，，目前没有加
	
	val r_shift_in = Mux(io.mode,
						dp_man_mult_stage2_tmp(((manWidthFp64+1)*2)-1, ((manWidthFp64+1)*2)-2^log2Ceil(manWidthFp64)),
						Cat(sp2_man_mult_stage2_tmp( ((manWidthFp32+1)*2)-1, (manWidthFp32+1)*2-2^log2Ceil(manWidthFp32)), sp1_man_mult_stage2_tmp(((manWidthFp32+1)*2)-1, (manWidthFp32+1)*2-2^log2Ceil(manWidthFp32)))
						)
	
    // 先单独处理r_shift_dp_stage2的最高位
	val r_shift_in_tmp = Mux(io.r_shift_dp_stage2(log2Ceil(manWidthFp64)-1),
						r_shift_in >> ( 2^(log2Ceil(manWidthFp64)-1) ),
						r_shift_in
						)

	// 右移

}