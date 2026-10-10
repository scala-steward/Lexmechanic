package lexmechanic.servitor.image

final case class RgbImage(width: Int, height: Int, pixels: Array[Byte]):

  def fit(maxEdge: Int, maxPixels: Int): RgbImage =
    val scale = math.min(
      maxEdge.toDouble / math.max(width, height),
      math.sqrt(maxPixels.toDouble / (width.toLong * height))
    )
    if scale >= 1 then this else resize(math.max(1, (width * scale).toInt), math.max(1, (height * scale).toInt))

  // Area averaging: each output pixel is the mean of the source area it covers, which keeps small text legible.
  private def resize(w: Int, h: Int): RgbImage =
    val (rowStart, rowWeights) = RgbImage.coverage(height, h)
    val (colStart, colWeights) = RgbImage.coverage(width, w)
    val stride = width * 3
    val blended = new Array[Float](stride)
    val out = new Array[Byte](w * h * 3)
    var y = 0
    while y < h do
      java.util.Arrays.fill(blended, 0f)
      val rw = rowWeights(y)
      var r = 0
      while r < rw.length do
        val src = (rowStart(y) + r) * stride
        var i = 0
        while i < stride do
          blended(i) += (pixels(src + i) & 0xff) * rw(r)
          i += 1
        r += 1
      var x = 0
      while x < w do
        val cw = colWeights(x)
        var c = 0
        while c < 3 do
          var sum = 0f
          var k = 0
          while k < cw.length do
            sum += blended((colStart(x) + k) * 3 + c) * cw(k)
            k += 1
          out((y * w + x) * 3 + c) = math.min(255, math.round(sum)).toByte
          c += 1
        x += 1
      y += 1
    RgbImage(w, h, out)

object RgbImage:
  /** For each of `to` output pixels: the first source pixel it covers and the weights of the ones it covers. */
  private def coverage(from: Int, to: Int): (Array[Int], Array[Array[Float]]) =
    val ratio = from.toDouble / to
    val starts = new Array[Int](to)
    val weights = new Array[Array[Float]](to)
    var o = 0
    while o < to do
      val begin = o * ratio
      val end = math.min(begin + ratio, from)
      val first = begin.toInt
      val covered = new Array[Float](math.ceil(end).toInt - first)
      var i = 0
      while i < covered.length do
        covered(i) = ((math.min(end, first + i + 1) - math.max(begin, first + i)) / ratio).toFloat
        i += 1
      starts(o) = first
      weights(o) = covered
      o += 1
    (starts, weights)
