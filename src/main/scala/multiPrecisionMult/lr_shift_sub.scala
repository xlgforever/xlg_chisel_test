package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class lr_shift_sub(shiftSize:Int=8,totalWidth:Int=64, shiftMode:Int=0) extends Module {
	val halfWidth = totalWidth / 2
  val io = IO(new Bundle {
	val mode = Input(Bool())
	val shift_in = Input(UInt(totalWidth.W))
	val dp_shift_en = Input(Bool())
	val sp2_shift_en = Input(Bool())
	val sp1_shift_en = Input(Bool())

	val shift_out = Output(UInt(totalWidth.W))
  })
	val r_shift_out_tmp1 = Mux(
		io.sp1_shift_en || io.dp_shift_en,
		Cat( Fill(shiftSize, 0.U(1.W)), io.shift_in(halfWidth-1-shiftSize, 0) ),
		io.shift_in(halfWidth-1, 0)
						)
	val r_shift_out_tmp2 = Cat( io.shift_in(halfWidth+shiftSize-1, halfWidth), r_shift_out_tmp1(halfWidth-1-shiftSize, 0) )

	val l_shift_out_tmp1 = Mux(
		io.sp2_shift_en || io.dp_shift_en,
		Cat( io.shift_in(totalWidth-1-shiftSize, halfWidth), Fill(shiftSize, 0.U(1.W)) ),
		io.shift_in(totalWidth-1, halfWidth)
						)
	val l_shift_out_tmp2 = Cat(  l_shift_out_tmp1(halfWidth-1, shiftSize), io.shift_in(halfWidth-1, halfWidth-shiftSize) )

	val shift_out_hi = Wire(UInt(halfWidth.W))
	val shift_out_lo = Wire(UInt(halfWidth.W))

	if (shiftMode == 0) {//右移模式
		shift_out_hi := Mux( io.dp_shift_en || io.sp2_shift_en, 
							Cat( Fill(shiftSize, 0.U(1.W)), io.shift_in(totalWidth-1-shiftSize, halfWidth) ), 
							io.shift_in(totalWidth-1, halfWidth) 
							)
		
		shift_out_lo := Mux(io.mode && io.dp_shift_en,
							r_shift_out_tmp2,
							r_shift_out_tmp1
							)
	} else  {//左移模式
		shift_out_hi := Mux(io.mode && io.dp_shift_en,
							l_shift_out_tmp2,
							l_shift_out_tmp1
							)
		
		shift_out_lo := Mux(io.sp1_shift_en || io.dp_shift_en,
							Cat( io.shift_in(halfWidth-1, shiftSize), Fill(shiftSize, 0.U(1.W)) ),
							io.shift_in(halfWidth-1, 0)
							)
	}

	io.shift_out := Cat(shift_out_hi, shift_out_lo)

}
object lr_shift_sub extends App {
  ChiselStage.emitSystemVerilogFile(
    new lr_shift_sub(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}