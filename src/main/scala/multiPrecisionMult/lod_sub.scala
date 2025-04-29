package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class lod_2() extends Module {
  val io = FlatIO(new Bundle {
	val din = Input(UInt(2.W))

	val out_vld = Output(Bool())
	val out_data = Output(UInt(1.W))
  })

  io.out_vld := io.din.orR
  io.out_data := (!io.din(1) && io.din(0))
}

class lod_sub(lodInputWidth:Int=64, lodTopWidth:Int=64, manWidthFp64: Int=52,manWidthFp32:Int=23) extends Module {
	val lodHalfWidth = lodInputWidth / 2
	val lodOutputWidth = log2Ceil(lodInputWidth)

  val io = FlatIO(new Bundle {
    val din = Input(UInt(lodInputWidth.W))
    val out_vld = Output(Bool())
    val out_data = Output(UInt(lodOutputWidth.W))
    val left_shift_dp = Output(UInt(log2Ceil(manWidthFp64).W))
    val left_shift_sp1 = Output(UInt(log2Ceil(manWidthFp32).W))
    val left_shift_sp2 = Output(UInt(log2Ceil(manWidthFp32).W))
  })

  if (lodInputWidth == 2){
    val lod_2 = Module(new lod_2())
    lod_2.io.din := io.din
    io.out_vld := lod_2.io.out_vld
    io.out_data := lod_2.io.out_data
    io.left_shift_dp := DontCare
    io.left_shift_sp1 := DontCare
    io.left_shift_sp2 := DontCare
  } else {
    val din_hi = io.din(lodInputWidth-1, lodHalfWidth)
    val din_lo = io.din(lodHalfWidth-1, 0)

    val out_vld_hi = Wire(Bool())
    val out_vld_lo = Wire(Bool())

    val out_data_hi = Wire(UInt(log2Ceil(lodHalfWidth).W))
    val out_data_lo = Wire(UInt(log2Ceil(lodHalfWidth).W))

    val lod_sub_hi = Module(new lod_sub(lodInputWidth = lodHalfWidth, lodTopWidth = lodTopWidth, manWidthFp64 = manWidthFp64, manWidthFp32 = manWidthFp32))
    val lod_sub_lo = Module(new lod_sub(lodInputWidth = lodHalfWidth, lodTopWidth = lodTopWidth, manWidthFp64 = manWidthFp64, manWidthFp32 = manWidthFp32))
    lod_sub_hi.io.din := din_hi
    lod_sub_lo.io.din := din_lo
    out_vld_hi := lod_sub_hi.io.out_vld
    out_vld_lo := lod_sub_lo.io.out_vld
    out_data_hi := lod_sub_hi.io.out_data
    out_data_lo := lod_sub_lo.io.out_data

    if(lodInputWidth == lodTopWidth){
      io.out_vld := out_vld_hi || out_vld_lo
      io.out_data := Mux(out_vld_hi, Cat(0.U(1.W), out_data_hi), Cat(1.U(1.W), out_data_lo))
      io.left_shift_dp := io.out_data
      io.left_shift_sp1 := out_data_hi
      io.left_shift_sp2 := out_data_lo
    } else {
      io.out_vld := out_vld_hi && out_vld_lo
      io.out_data := Mux(out_vld_hi, Cat(0.U(1.W), out_data_hi), Cat(1.U(1.W), out_data_lo))
      io.left_shift_dp := DontCare
      io.left_shift_sp1 := DontCare
      io.left_shift_sp2 := DontCare
    }
  }
}

class lod (expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends Module{
  val lodInputWidth = ( 1 << log2Ceil(manWidthFp64) )
  val lodHalf = lodInputWidth / 2

  val io = FlatIO(new Bundle{
    val mode_sel = Input(Bool())

    val dp_man_mult_stage1 = Input(UInt(((manWidthFp64+1)*2).W))
    val sp1_man_mult_stage1 = Input(UInt(((manWidthFp32+1)*2).W))
    val sp2_man_mult_stage1 = Input(UInt(((manWidthFp32+1)*2).W))

    val left_shift_dp_stage1 = Output(UInt(log2Ceil(manWidthFp64).W))
    val left_shift_sp1_stage1 = Output(UInt(log2Ceil(manWidthFp32).W))
    val left_shift_sp2_stage1 = Output(UInt(log2Ceil(manWidthFp32).W))
  })


  val lod_in = Mux(io.mode_sel,
                  Cat(io.dp_man_mult_stage1(((manWidthFp64+1)*2)-1, ((manWidthFp64+1)*2)-2).orR, io.dp_man_mult_stage1(((manWidthFp64+1)*2)-3, ((manWidthFp64+1)*2)-3-manWidthFp64+1), 0.U((lodInputWidth-manWidthFp64-1).W)),
                  Cat(io.sp2_man_mult_stage1(((manWidthFp32+1)*2)-1, ((manWidthFp32+1)*2)-2).orR, io.sp2_man_mult_stage1(((manWidthFp32+1)*2)-3, ((manWidthFp32+1)*2)-3-manWidthFp32+1), 0.U((lodHalf-manWidthFp32-1).W), io.sp1_man_mult_stage1(((manWidthFp32+1)*2)-1, ((manWidthFp32+1)*2)-2).orR, io.sp1_man_mult_stage1(((manWidthFp32+1)*2)-3, ((manWidthFp32+1)*2)-3-manWidthFp32+1),0.U((lodHalf-manWidthFp32-1).W) )
                  )

  val lod_sub = Module(new lod_sub(lodInputWidth = lodInputWidth, lodTopWidth = lodInputWidth, manWidthFp64 = manWidthFp64, manWidthFp32 = manWidthFp32))

  lod_sub.io.din := lod_in
  io.left_shift_dp_stage1 := lod_sub.io.left_shift_dp
  io.left_shift_sp1_stage1 := lod_sub.io.left_shift_sp1
  io.left_shift_sp2_stage1 := lod_sub.io.left_shift_sp2
}


object lod extends App {
  ChiselStage.emitSystemVerilogFile(
    new lod(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}