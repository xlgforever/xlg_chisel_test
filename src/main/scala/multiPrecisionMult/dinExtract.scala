package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class dinExtract(expWidthFp64: Int=11, manWidthFp64: Int=52, expWidthFp32: Int=8, manWidthFp32:Int=23) extends RawModule {
  val io = FlatIO(new Bundle {
	val a = Input(new fp_number(expWidthFp64, manWidthFp64))
	val b = Input(new fp_number(expWidthFp64, manWidthFp64))
	val a_sp1_s = Output(Bool())
	val b_sp1_s = Output(Bool())
	val a_sp1_e = Output(UInt(expWidthFp32.W))
	val b_sp1_e = Output(UInt(expWidthFp32.W))
	val a_sp1_m = Output(UInt((manWidthFp32+1).W))
	val b_sp1_m = Output(UInt((manWidthFp32+1).W))
	val a_sp2_s = Output(Bool())
	val b_sp2_s = Output(Bool())
	val a_sp2_e = Output(UInt(expWidthFp32.W))
	val b_sp2_e = Output(UInt(expWidthFp32.W))
	val a_sp2_m = Output(UInt((manWidthFp32+1).W))
	val b_sp2_m = Output(UInt((manWidthFp32+1).W))
	val a_s  = Output(Bool())
	val b_s  = Output(Bool())
	val a_e  = Output(UInt(expWidthFp64.W))
	val b_e  = Output(UInt(expWidthFp64.W))
	val a_m  = Output(UInt((manWidthFp64+1).W))
	val b_m  = Output(UInt((manWidthFp64+1).W))
	// val a_sp1 = Output(new fp_number(expWidthFp32, manWidthFp32))
	// val b_sp1 = Output(new fp_number(expWidthFp32, manWidthFp32))
	// val a_sp2 = Output(new fp_number(expWidthFp32, manWidthFp32))
	// val b_sp2 = Output(new fp_number(expWidthFp32, manWidthFp32))
	
	// sub normal number
	val a_sp1_is_sn = Output(Bool())
	val b_sp1_is_sn = Output(Bool())
	val a_sp2_is_sn = Output(Bool())
	val b_sp2_is_sn = Output(Bool())
	val a_is_sn  = Output(Bool())
	val b_is_sn  = Output(Bool())

	// NaN number
	val a_sp1_is_nan = Output(Bool())
	val b_sp1_is_nan = Output(Bool())
	val a_sp2_is_nan = Output(Bool())
	val b_sp2_is_nan = Output(Bool())
	val a_is_nan  = Output(Bool())
	val b_is_nan  = Output(Bool())
	
	// Infinity number
	val a_sp1_is_inf = Output(Bool())
	val b_sp1_is_inf = Output(Bool())
	val a_sp2_is_inf = Output(Bool())
	val b_sp2_is_inf = Output(Bool())
	val a_is_inf  = Output(Bool())
	val b_is_inf  = Output(Bool())
  })
	val halfWidth : Int = (expWidthFp64 + manWidthFp64+1) / 2;
	// 将FP64 拆分为两个FP32，高为为sp2，低位为sp1
	val a_sp1_tmp = Wire(new fp_number(expWidthFp32, manWidthFp32))
	val a_sp2_tmp = Wire(new fp_number(expWidthFp32, manWidthFp32))
	val b_sp1_tmp = Wire(new fp_number(expWidthFp32, manWidthFp32))
	val b_sp2_tmp = Wire(new fp_number(expWidthFp32, manWidthFp32))
	a_sp1_tmp := io.a.asUInt(halfWidth-1, 0).asTypeOf(new fp_number(expWidthFp32, manWidthFp32))
	a_sp2_tmp := io.a.asUInt(halfWidth*2-1, halfWidth).asTypeOf(new fp_number(expWidthFp32, manWidthFp32))
	b_sp1_tmp := io.b.asUInt(halfWidth-1, 0).asTypeOf(new fp_number(expWidthFp32, manWidthFp32))
	b_sp2_tmp := io.b.asUInt(halfWidth*2-1, halfWidth).asTypeOf(new fp_number(expWidthFp32, manWidthFp32))

	// 指数全为0
	val a_sp1_exp_is_zero = Wire(Bool())
	val b_sp1_exp_is_zero = Wire(Bool())
	val a_sp2_exp_is_zero = Wire(Bool())
	val b_sp2_exp_is_zero = Wire(Bool())
	val a_exp_is_zero = Wire(Bool())
	val b_exp_is_zero = Wire(Bool())
	a_sp1_exp_is_zero := !a_sp1_tmp.exp.orR
	b_sp1_exp_is_zero := !b_sp1_tmp.exp.orR
	a_sp2_exp_is_zero := !a_sp2_tmp.exp.orR
	b_sp2_exp_is_zero := !b_sp2_tmp.exp.orR
	a_exp_is_zero := a_sp2_exp_is_zero && !(io.a.exp(expWidthFp64-expWidthFp32-1,0).orR)
	b_exp_is_zero := b_sp2_exp_is_zero && !(io.b.exp(expWidthFp64-expWidthFp32-1,0).orR)
	// 指数全为1
	val a_sp1_exp_is_one = Wire(Bool())
	val b_sp1_exp_is_one = Wire(Bool())
	val a_sp2_exp_is_one = Wire(Bool())
	val b_sp2_exp_is_one = Wire(Bool())
	val a_exp_is_one = Wire(Bool())
	val b_exp_is_one = Wire(Bool())
	a_sp1_exp_is_one := a_sp1_tmp.exp.andR
	b_sp1_exp_is_one := b_sp1_tmp.exp.andR
	a_sp2_exp_is_one := a_sp2_tmp.exp.andR
	b_sp2_exp_is_one := b_sp2_tmp.exp.andR
	a_exp_is_one := a_sp2_exp_is_one && io.a.exp(expWidthFp64-expWidthFp32-1,0).andR
	b_exp_is_one := b_sp2_exp_is_one && io.b.exp(expWidthFp64-expWidthFp32-1,0).andR
	// 尾数全为0
	val a_sp1_man_is_zero = Wire(Bool())
	val b_sp1_man_is_zero = Wire(Bool())
	val a_sp2_man_is_zero = Wire(Bool())
	val b_sp2_man_is_zero = Wire(Bool())
	val a_man_is_zero = Wire(Bool())
	val b_man_is_zero = Wire(Bool())
	a_sp1_man_is_zero := !a_sp1_tmp.man.orR
	b_sp1_man_is_zero := !b_sp1_tmp.man.orR
	a_sp2_man_is_zero := !a_sp2_tmp.man.orR
	b_sp2_man_is_zero := !b_sp2_tmp.man.orR
	a_man_is_zero := !a_sp1_tmp.sign && a_sp1_exp_is_zero && a_sp1_man_is_zero && !io.a.man(manWidthFp64-1,expWidthFp32+manWidthFp32+1).orR
	b_man_is_zero := !b_sp1_tmp.sign && b_sp1_exp_is_zero && b_sp1_man_is_zero && !io.b.man(manWidthFp64-1,expWidthFp32+manWidthFp32+1).orR
	// 尾数全为1
	val a_sp1_man_is_one = Wire(Bool())
	val b_sp1_man_is_one = Wire(Bool())
	val a_sp2_man_is_one = Wire(Bool())
	val b_sp2_man_is_one = Wire(Bool())
	val a_man_is_one = Wire(Bool())
	val b_man_is_one = Wire(Bool())
	a_sp1_man_is_one := a_sp1_tmp.man.andR
	b_sp1_man_is_one := b_sp1_tmp.man.andR
	a_sp2_man_is_one := a_sp2_tmp.man.andR
	b_sp2_man_is_one := b_sp2_tmp.man.andR
	a_man_is_one := a_sp1_tmp.sign && a_sp1_exp_is_one && a_sp1_man_is_one && io.a.man(manWidthFp64-1, expWidthFp32+manWidthFp32+1).andR
	b_man_is_one := b_sp1_tmp.sign && b_sp1_exp_is_one && b_sp1_man_is_one && io.b.man(manWidthFp64-1, expWidthFp32+manWidthFp32+1).andR
	
	// sub normal number
	val a_sp1_is_sn_tmp = a_sp1_exp_is_zero && (!a_sp1_man_is_zero) // 指数全为0，尾数不全为0
	val b_sp1_is_sn_tmp = b_sp1_exp_is_zero && (!b_sp1_man_is_zero)
	val a_sp2_is_sn_tmp = a_sp2_exp_is_zero && (!a_sp2_man_is_zero)
	val b_sp2_is_sn_tmp = b_sp2_exp_is_zero && (!b_sp2_man_is_zero)
	val a_is_sn_tmp = a_exp_is_zero && (!a_man_is_zero)
	val b_is_sn_tmp = b_exp_is_zero && (!b_man_is_zero)

	// NaN number
	val a_sp1_is_nan_tmp = a_sp1_exp_is_one && (!a_sp1_man_is_zero)
	val b_sp1_is_nan_tmp = b_sp1_exp_is_one && (!b_sp1_man_is_zero)
	val a_sp2_is_nan_tmp = a_sp2_exp_is_one && (!a_sp2_man_is_zero)
	val b_sp2_is_nan_tmp = b_sp2_exp_is_one && (!b_sp2_man_is_zero)
	val a_is_nan_tmp = a_exp_is_one && (!a_man_is_zero)
	val b_is_nan_tmp = b_exp_is_one && (!b_man_is_zero)

	// Infinity number
	val a_sp1_is_inf_tmp = a_sp1_exp_is_one && a_sp1_man_is_zero
	val b_sp1_is_inf_tmp = b_sp1_exp_is_one && b_sp1_man_is_zero
	val a_sp2_is_inf_tmp = a_sp2_exp_is_one && a_sp2_man_is_zero
	val b_sp2_is_inf_tmp = b_sp2_exp_is_one && b_sp2_man_is_zero
	val a_is_inf_tmp = a_exp_is_one && a_man_is_zero
	val b_is_inf_tmp = b_exp_is_one && b_man_is_zero

	// Output various exception judgments
	io.a_sp1_is_sn := a_sp1_is_sn_tmp
	io.b_sp1_is_sn := b_sp1_is_sn_tmp
	io.a_sp2_is_sn := a_sp2_is_sn_tmp
	io.b_sp2_is_sn := b_sp2_is_sn_tmp
	io.a_is_sn := a_is_sn_tmp
	io.b_is_sn := b_is_sn_tmp

	io.a_sp1_is_nan := a_sp1_is_nan_tmp
	io.b_sp1_is_nan := b_sp1_is_nan_tmp
	io.a_sp2_is_nan := a_sp2_is_nan_tmp
	io.b_sp2_is_nan := b_sp2_is_nan_tmp
	io.a_is_nan := a_is_nan_tmp
	io.b_is_nan := b_is_nan_tmp

	io.a_sp1_is_inf := a_sp1_is_inf_tmp
	io.b_sp1_is_inf := b_sp1_is_inf_tmp
	io.a_sp2_is_inf := a_sp2_is_inf_tmp
	io.b_sp2_is_inf := b_sp2_is_inf_tmp
	io.a_is_inf := a_is_inf_tmp
	io.b_is_inf := b_is_inf_tmp
    // 规范化之后的符号位、exp和man输出
	io.a_sp1_s := a_sp1_tmp.sign
	io.b_sp1_s := b_sp1_tmp.sign
	io.a_sp1_e := Cat(a_sp1_tmp.exp(expWidthFp32-1,1), (a_sp1_tmp.exp(0) || a_sp1_is_sn_tmp) )// 如果是非规格化数，保持exp为1
	io.b_sp1_e := Cat(b_sp1_tmp.exp(expWidthFp32-1,1), (b_sp1_tmp.exp(0) || b_sp1_is_sn_tmp) )
	io.a_sp1_m := Cat(!a_sp1_is_sn_tmp, a_sp1_tmp.man)
	io.b_sp1_m := Cat(!b_sp1_is_sn_tmp, b_sp1_tmp.man)

	io.a_sp2_s := a_sp2_tmp.sign
	io.b_sp2_s := b_sp2_tmp.sign
	io.a_sp2_e := Cat(a_sp2_tmp.exp(expWidthFp32-1,1), (a_sp2_tmp.exp(0) || a_sp2_is_sn_tmp) )
	io.b_sp2_e := Cat(b_sp2_tmp.exp(expWidthFp32-1,1), (b_sp2_tmp.exp(0) || b_sp2_is_sn_tmp) )
	io.a_sp2_m := Cat(!a_sp2_is_sn_tmp, a_sp2_tmp.man)
	io.b_sp2_m := Cat(!b_sp2_is_sn_tmp, b_sp2_tmp.man)

	io.a_s := io.a.sign
	io.b_s := io.b.sign
	io.a_e := Cat(io.a.exp(expWidthFp64-1,1), (io.a.exp(0) || a_is_sn_tmp) )
	io.b_e := Cat(io.b.exp(expWidthFp64-1,1), (io.b.exp(0) || b_is_sn_tmp) )
	io.a_m := Cat(!a_is_sn_tmp, io.a.man)
	io.b_m := Cat(!b_is_sn_tmp, io.b.man)

}

object dinExtarct extends App {
  ChiselStage.emitSystemVerilogFile(
    new dinExtract(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}