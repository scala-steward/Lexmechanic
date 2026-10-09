package lexmechanic.servitor

import lexmechanic.noosphere.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.file.{Files, Path}
import java.util.zip.{CRC32, Deflater, DeflaterOutputStream, Inflater, InflaterInputStream}

object Png:
  private val Signature = Array[Byte](-119, 80, 78, 71, 13, 10, 26, 10)

  def read(path: Path, region: Option[Region]): RgbImage =
    val bytes = Files.readAllBytes(path)
    if !bytes.startsWith(Signature) then throw RuntimeException(s"$path is not a PNG file")
    val buffer = ByteBuffer.wrap(bytes)

    var width, height, channels = 0
    val compressed = ByteArrayOutputStream()
    var pos = Signature.length
    var ended = false
    while !ended do
      val length = buffer.getInt(pos)
      val start = pos + 8
      String(bytes, pos + 4, 4, US_ASCII) match
        case "IHDR" =>
          width = buffer.getInt(start)
          height = buffer.getInt(start + 4)
          channels = (bytes(start + 8).toInt, bytes(start + 9).toInt, bytes(start + 12).toInt) match
            case (8, 2, 0)                     => 3
            case (8, 6, 0)                     => 4
            case (depth, colorType, interlace) =>
              throw RuntimeException(s"Unsupported PNG: bit depth $depth, color type $colorType, interlace $interlace")
        case "IDAT" => compressed.write(bytes, start, length)
        case "IEND" => ended = true
        case _      => ()
      pos = start + length + 4

    val Region(x, y, w, h) = clamp(region, width, height)
    val inflater = Inflater()
    val data = DataInputStream(InflaterInputStream(ByteArrayInputStream(compressed.toByteArray), inflater, 1 << 16))
    var previous = new Array[Byte](width * channels)
    var current = new Array[Byte](width * channels)
    val out = new Array[Byte](w * h * 3)
    try
      // Filters reference the row above, so every row up to the crop's bottom edge must be decoded.
      for row <- 0 until y + h do
        val filter = data.readUnsignedByte()
        data.readFully(current)
        unfilter(filter, current, previous, channels)
        if row >= y then copyRgb(current, x, channels, out, (row - y) * w * 3, w)
        val swap = previous
        previous = current
        current = swap
    finally inflater.end()
    RgbImage(w, h, out)

  private def copyRgb(row: Array[Byte], x: Int, channels: Int, out: Array[Byte], at: Int, count: Int): Unit =
    if channels == 3 then System.arraycopy(row, x * 3, out, at, count * 3)
    else
      var i = 0
      while i < count do
        val src = (x + i) * channels
        out(at + i * 3) = row(src)
        out(at + i * 3 + 1) = row(src + 1)
        out(at + i * 3 + 2) = row(src + 2)
        i += 1

  def write(image: RgbImage): Array[Byte] =
    val stride = image.width * 3
    val compressed = ByteArrayOutputStream()
    val deflater = Deflater(Deflater.BEST_SPEED)
    val deflate = DeflaterOutputStream(compressed, deflater, 1 << 16)
    val row = new Array[Byte](stride + 1)
    row(0) = 1 // Sub
    for y <- 0 until image.height do
      val src = y * stride
      var i = 0
      while i < stride do
        val left = if i >= 3 then image.pixels(src + i - 3) else 0
        row(i + 1) = (image.pixels(src + i) - left).toByte
        i += 1
      deflate.write(row)
    deflate.finish()
    deflater.end()

    val out = ByteArrayOutputStream()
    val png = DataOutputStream(out)
    png.write(Signature)
    writeChunk(
      png,
      "IHDR",
      ByteBuffer.allocate(13).putInt(image.width).putInt(image.height).put(8.toByte).put(2.toByte).array()
    )
    writeChunk(png, "IDAT", compressed.toByteArray)
    writeChunk(png, "IEND", Array.emptyByteArray)
    out.toByteArray

  private def clamp(region: Option[Region], width: Int, height: Int): Region = region match
    case None                     => Region(0, 0, width, height)
    case Some(Region(x, y, w, h)) =>
      val (left, top) = (x.max(0).min(width), y.max(0).min(height))
      val (right, bottom) = ((x + w).min(width), (y + h).min(height))
      if right <= left || bottom <= top then
        throw RuntimeException(s"Calibrated region lies outside the ${width}x$height screenshot - calibrate again")
      Region(left, top, right - left, bottom - top)

  private def unfilter(kind: Int, row: Array[Byte], above: Array[Byte], bpp: Int): Unit =
    var i = 0
    kind match
      case 0 => ()
      case 1 =>
        i = bpp
        while i < row.length do
          row(i) = (row(i) + row(i - bpp)).toByte
          i += 1
      case 2 =>
        while i < row.length do
          row(i) = (row(i) + above(i)).toByte
          i += 1
      case 3 =>
        while i < row.length do
          val left = if i >= bpp then row(i - bpp) & 0xff else 0
          row(i) = (row(i) + ((left + (above(i) & 0xff)) >> 1)).toByte
          i += 1
      case 4 =>
        while i < row.length do
          val a = if i >= bpp then row(i - bpp) & 0xff else 0
          val c = if i >= bpp then above(i - bpp) & 0xff else 0
          row(i) = (row(i) + paeth(a, above(i) & 0xff, c)).toByte
          i += 1
      case k => throw RuntimeException(s"Corrupt PNG: unknown filter $k")

  private def paeth(a: Int, b: Int, c: Int): Int =
    val p = a + b - c
    val pa = math.abs(p - a)
    val pb = math.abs(p - b)
    val pc = math.abs(p - c)
    if pa <= pb && pa <= pc then a else if pb <= pc then b else c

  private def writeChunk(out: DataOutputStream, kind: String, data: Array[Byte]): Unit =
    val kindBytes = kind.getBytes(US_ASCII)
    val crc = CRC32()
    crc.update(kindBytes)
    crc.update(data)
    out.writeInt(data.length)
    out.write(kindBytes)
    out.write(data)
    out.writeInt(crc.getValue.toInt)
