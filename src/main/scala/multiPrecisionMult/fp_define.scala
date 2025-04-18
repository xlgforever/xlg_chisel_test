package multiPrecisionMult

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class fp_number (expWidth: Int, manWidth: Int) extends Bundle {
  val sign = Bool()
  val exp  = UInt(expWidth.W)
  val man  = UInt(manWidth.W)
}


