package multiPrecisionMult


import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class left_shift(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module{
	val io = FlatIO (new Bundle {
		val mode = Input(Bool())

		val dp_man_mult_stage2  = Input(UInt(((manWidthFp64+1)*2).W))
		val sp1_man_mult_stage2 = Input(UInt(((manWidthFp32+1)*2).W))
		val sp2_man_mult_stage2 = Input(UInt(((manWidthFp32+1)*2).W))

		val left_shift_sp1_update_stage2 = Input(UInt(log2Ceil(manWidthFp32).W))
		val left_shift_sp2_update_stage2 = Input(UInt(log2Ceil(manWidthFp32).W))
		val left_shift_dp_update_stage2  = Input(UInt(log2Ceil(manWidthFp64).W))

		val l_shift_out_stage2 = Output(UInt((manWidthFp64*2+2).W))
	})

	// 左移的输入：
	// 与右移只取部分高位不同的是，左移取了尾数相乘的所有bit位
	val l_shift_in = Mux(io.mode, 
						io.dp_man_mult_stage2, 
						Cat(io.sp1_man_mult_stage2, 0.U((manWidthFp64+1-2*manWidthFp32-2).W), io.sp2_man_mult_stage2, 0.U((manWidthFp64+1-2*manWidthFp32-2).W))
						)
	// 先处理dp的最高位
	val l_shift_in_tmp = Mux(io.left_shift_dp_update_stage2(log2Ceil(manWidthFp64)-1),
							l_shift_in << (1 << (log2Ceil(manWidthFp64)-1)),
							l_shift_in
							)
	// 创建 log2Ceil(manWidthFp32) 个 lr_shift_sub模块
	val startShiftSize =  1 << (log2Ceil(manWidthFp32)-1);
	println("startShiftSize = " + startShiftSize)
	val totalWidth = manWidthFp64*2+2
	val shift_mode = 1
	val paramSets = Seq.tabulate(log2Ceil(manWidthFp32)) { idx =>
		(
			startShiftSize / (1 << idx),
			totalWidth,
			shift_mode
		)
	}
	val lr_shift_subs = paramSets.zipWithIndex.map { 
		case ((shiftSize, totalWidth, shiftMode), i) =>
		val mod = Module(new lr_shift_sub(shiftSize, totalWidth, shiftMode))
		mod
  	}
	val l_shift_out_tmp = Seq.fill(log2Ceil(manWidthFp32)-1)(Wire(UInt((manWidthFp64*2+2).W)))
	for (	i <- ( (log2Ceil(manWidthFp32)-1) to 0 by -1 )   ) {// 4，3，2，1，0
		if(i==(log2Ceil(manWidthFp32)-1)){
			lr_shift_subs(i).io.mode := io.mode
			lr_shift_subs(i).io.shift_in := l_shift_in_tmp
			lr_shift_subs(i).io.dp_shift_en := io.left_shift_dp_update_stage2(i)
			lr_shift_subs(i).io.sp2_shift_en := io.left_shift_sp2_update_stage2(i)
			lr_shift_subs(i).io.sp1_shift_en := io.left_shift_sp1_update_stage2(i)
			l_shift_out_tmp(i-1) := lr_shift_subs(i).io.shift_out
		} else if(i==0){
			lr_shift_subs(i).io.mode := io.mode
			lr_shift_subs(i).io.shift_in := l_shift_out_tmp(i)
			lr_shift_subs(i).io.dp_shift_en := io.left_shift_dp_update_stage2(i)
			lr_shift_subs(i).io.sp2_shift_en := io.left_shift_sp2_update_stage2(i)
			lr_shift_subs(i).io.sp1_shift_en := io.left_shift_sp1_update_stage2(i)
			io.l_shift_out_stage2 := lr_shift_subs(i).io.shift_out
		} else {
			lr_shift_subs(i).io.mode := io.mode
			lr_shift_subs(i).io.shift_in := l_shift_out_tmp(i)
			lr_shift_subs(i).io.dp_shift_en := io.left_shift_dp_update_stage2(i)
			lr_shift_subs(i).io.sp2_shift_en := io.left_shift_sp2_update_stage2(i)
			lr_shift_subs(i).io.sp1_shift_en := io.left_shift_sp1_update_stage2(i)
			l_shift_out_tmp(i-1) := lr_shift_subs(i).io.shift_out
		}
	}
}

object left_shift extends App {
  ChiselStage.emitSystemVerilogFile(
    new left_shift(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}