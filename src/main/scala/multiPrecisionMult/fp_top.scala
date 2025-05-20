import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage


class fp_top(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module {
  val io = FlatIO(new Bundle {
	val mode = Input(Bool()) // 1表示FP64模式，0表示FP32模式
	val valid_in = Input(Bool())

	val a = Input(new fp_number(expWidthFp64, manWidthFp64))
	val b = Input(new fp_number(expWidthFp64, manWidthFp64))

	val sp1_result = Output(UInt((expWidthFp32+manWidthFp32+1).W))
	val sp2_result = Output(UInt((expWidthFp32+manWidthFp32+1).W))
	val dp_result =  Output(UInt((expWidthFp64+manWidthFp64+1).W))
	val valid_out = Output(Bool())
  })

  val dinExtract_m = Module(new dinExtract(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))
  val sign_exp_proc_m = Module(new sign_exp_proc(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))
  val man_mult_m = Module(new man_mult(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))

  val right_shift_cal_m = Module(new right_shift_cal(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))
  val lod_m = Module(new lod(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))
  val left_shift_update_m = Module(new left_shift_update(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))

  val right_shift_m = Module(new right_shift(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))
  val left_shift_m = Module(new left_shift(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))

  val man_shifted_norm_m = Module(new man_shifted_norm(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))
  val man_round_m = Module(new man_round(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))
  val exp_update_m = Module(new exp_update(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))

  val final_result_m = Module(new final_result(expWidthFp64, manWidthFp64, expWidthFp32, manWidthFp32))


  // connections
  dinExtract_m.io.a := io.a
  dinExtract_m.io.b := io.b

  sign_exp_proc_m.io.mode := io.mode
  sign_exp_proc_m.io.a_sp1_s := dinExtract_m.io.a_sp1_s
  sign_exp_proc_m.io.b_sp1_s := dinExtract_m.io.b_sp1_s
  sign_exp_proc_m.io.a_sp1_e := dinExtract_m.io.a_sp1_e
  sign_exp_proc_m.io.b_sp1_e := dinExtract_m.io.b_sp1_e
  sign_exp_proc_m.io.a_sp2_s := dinExtract_m.io.a_sp2_s
  sign_exp_proc_m.io.b_sp2_s := dinExtract_m.io.b_sp2_s
  sign_exp_proc_m.io.a_sp2_e := dinExtract_m.io.a_sp2_e
  sign_exp_proc_m.io.b_sp2_e := dinExtract_m.io.b_sp2_e
  sign_exp_proc_m.io.a_s := dinExtract_m.io.a_s
  sign_exp_proc_m.io.b_s := dinExtract_m.io.b_s
  sign_exp_proc_m.io.a_e := dinExtract_m.io.a_e
  sign_exp_proc_m.io.b_e := dinExtract_m.io.b_e

  man_mult_m.io.mode :=	io.mode
  man_mult_m.io.a_sp1_m := dinExtract_m.io.a_sp1_m
  man_mult_m.io.b_sp1_m := dinExtract_m.io.b_sp1_m
  man_mult_m.io.a_sp2_m := dinExtract_m.io.a_sp2_m
  man_mult_m.io.b_sp2_m := dinExtract_m.io.b_sp2_m
  man_mult_m.io.a_m := dinExtract_m.io.a_m
  man_mult_m.io.b_m := dinExtract_m.io.b_m

  right_shift_cal_m.io.mode := io.mode
  right_shift_cal_m.io.dp_sp2_e_stage1 := sign_exp_proc_m.io.dp_sp2_e_stage1
  right_shift_cal_m.io.sp1_e_stage1 := sign_exp_proc_m.io.sp1_e_stage1
  right_shift_cal_m.io.sp2_e_stage1 := sign_exp_proc_m.io.sp2_e_stage1
  right_shift_cal_m.io.dp_e_sgate1 := sign_exp_proc_m.io.dp_e_stage1
  right_shift_cal_m.io.a_dp_sp2_e := sign_exp_proc_m.io.a_dp_sp2_e
  right_shift_cal_m.io.b_dp_sp2_e := sign_exp_proc_m.io.b_dp_sp2_e

  lod_m.io.mode := io.mode
  lod_m.io.dp_man_mult_stage1 := man_mult_m.io.dp_man_mult_stage1
  lod_m.io.sp1_man_mult_stage1 := man_mult_m.io.sp1_man_mult_stage1
  lod_m.io.sp2_man_mult_stage1 := man_mult_m.io.sp2_man_mult_stage1

  left_shift_update_m.io.mode := io.mode
  left_shift_update_m.io.left_shift_dp_stage1 := lod_m.io.left_shift_dp_stage1
  left_shift_update_m.io.left_shift_sp1_stage1 := lod_m.io.left_shift_sp1_stage1
  left_shift_update_m.io.left_shift_sp2_stage1 := lod_m.io.left_shift_sp2_stage1
  left_shift_update_m.io.sp1_e_stage1 := sign_exp_proc_m.io.sp1_e_stage1
  left_shift_update_m.io.sp2_e_stage1 := sign_exp_proc_m.io.sp2_e_stage1
  left_shift_update_m.io.dp_e_stage1 := sign_exp_proc_m.io.dp_e_stage1
  left_shift_update_m.io.dp_sp2_e_stage1 := sign_exp_proc_m.io.dp_sp2_e_stage1

  right_shift_m.io.mode := io.mode
  right_shift_m.io.r_shift_dp_stage2 := right_shift_cal_m.io.r_shift_dp_stage2
  right_shift_m.io.r_shift_sp1_stage2 := right_shift_cal_m.io.r_shift_sp1_stage2
  right_shift_m.io.r_shift_sp2_stage2 := right_shift_cal_m.io.r_shift_sp2_stage2
  right_shift_m.io.dp_man_mult_stage1 := man_mult_m.io.dp_man_mult_stage1
  right_shift_m.io.sp1_man_mult_stage1 := man_mult_m.io.sp1_man_mult_stage1
  right_shift_m.io.sp2_man_mult_stage1 := man_mult_m.io.sp2_man_mult_stage1
  
  left_shift_m.io.mode := io.mode
  left_shift_m.io.dp_man_mult_stage2 := right_shift_m.io.dp_man_mult_stage2
  left_shift_m.io.sp1_man_mult_stage2 := right_shift_m.io.sp1_man_mult_stage2
  left_shift_m.io.sp2_man_mult_stage2 := right_shift_m.io.sp2_man_mult_stage2
  left_shift_m.io.left_shift_sp1_update_stage2 := left_shift_update_m.io.left_shift_sp1_update_stage2
  left_shift_m.io.left_shift_sp2_update_stage2 := left_shift_update_m.io.left_shift_sp2_update_stage2
  left_shift_m.io.left_shift_dp_update_stage2 := left_shift_update_m.io.left_shift_dp_update_stage2

  man_shifted_norm_m.io.mode := io.mode
  man_shifted_norm_m.io.l_shift_out_stage2 := left_shift_m.io.l_shift_out_stage2
  man_shifted_norm_m.io.r_shift_out_stage2 := right_shift_m.io.r_shift_out_stage2
  man_shifted_norm_m.io.r_shift_dp_sp2_chk_stage1 := right_shift_cal_m.io.r_shift_dp_sp2_chk_stage1
  man_shifted_norm_m.io.r_shift_sp1_chk_stage1 := right_shift_cal_m.io.r_shift_sp1_chk_stage1

  man_round_m.io.mode := io.mode
  man_round_m.io.man_shifted_norm_stage2 := man_shifted_norm_m.io.man_shifted_norm_stage2
  
  exp_update_m.io.mode := io.mode
  exp_update_m.io.dp_sp2_e_stage1 := sign_exp_proc_m.io.dp_sp2_e_stage1
  exp_update_m.io.sp1_e_stage1 := sign_exp_proc_m.io.sp1_e_stage1
  exp_update_m.io.dp_man_mult_stage2 := right_shift_m.io.dp_man_mult_stage2
  exp_update_m.io.sp1_man_mult_stage2 := right_shift_m.io.sp1_man_mult_stage2
  exp_update_m.io.sp2_man_mult_stage2 := right_shift_m.io.sp2_man_mult_stage2
  exp_update_m.io.left_shift_sp1_update_stage2 := left_shift_update_m.io.left_shift_sp1_update_stage2
  exp_update_m.io.left_shift_sp2_update_stage2 := left_shift_update_m.io.left_shift_sp2_update_stage2
  exp_update_m.io.left_shift_dp_update_stage2  := left_shift_update_m.io.left_shift_dp_update_stage2

  final_result_m.io.mode := io.mode
  final_result_m.io.dp_sp2_e_stage3 := exp_update.io.dp_exp_stage3
  final_result_m.io.sp1_e_stage3 := exp_update.io.sp1_exp_stage3
  final_result_m.io.sp2_e_stage3 := exp_update.io.sp2_exp_stage3
  final_result_m.io.sp1_s_stage1 := sign_exp_proc_m.io.sp1_s_stage1
  final_result_m.io.sp2_s_stage1 := sign_exp_proc_m.io.sp2_s_stage1
  final_result_m.io.dp_s_stage1  := sign_exp_proc_m.io.dp_s_stage1
  final_result_m.io.man_hi_stage3 := man_round_m.io.man_hi_stage3
  final_result_m.io.man_lo_stage3 := man_round_m.io.man_lo_stage3

  io.sp1_result := final_result_m.io.sp1_result
  io.sp2_result := final_result_m.io.sp2_result
  io.dp_result := final_result_m.io.dp_result
  io.valid_out := ShiftRegister(io.valid_in, 4)
}