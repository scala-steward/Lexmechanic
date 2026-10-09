package lexmechanic.servitor

import sttp.model.{StatusCode, StatusText}

import java.io.{BufferedInputStream, ByteArrayOutputStream, InputStream}
import java.net.{InetAddress, InetSocketAddress, ServerSocket, Socket}
import java.nio.charset.StandardCharsets.UTF_8

/** Minimal HTTP/1.1 server for a single local browser; one thread and one request per connection. */
object HttpServer:
  private val MaxBody = 1 << 20
  private val MaxHead = 1 << 16

  def serve(port: Int)(handler: Request => Response): Unit =
    val server = ServerSocket()
    server.setReuseAddress(true)
    server.bind(InetSocketAddress(InetAddress.getLoopbackAddress, port))
    Log.info(s"UI: http://127.0.0.1:$port")
    while true do
      val socket = server.accept()
      Background.run("http")(handle(socket, handler))

  private def handle(socket: Socket, handler: Request => Response): Unit =
    try
      // Browsers open speculative connections that never send a request.
      socket.setSoTimeout(10000)
      val in = BufferedInputStream(socket.getInputStream)
      val head = readHead(in).split("\r\n")
      val Array(method, target, _*) = head.head.split(" "): @unchecked
      val headers = head.tail.flatMap { line =>
        line.split(":", 2) match
          case Array(k, v) => Some(k.trim.toLowerCase -> v.trim)
          case _           => None
      }.toMap
      val length = headers.get("content-length").map(_.toInt).getOrElse(0)
      if length > MaxBody then throw RuntimeException("Request body too large")
      val response =
        try handler(Request(method, target.takeWhile(_ != '?'), headers, in.readNBytes(length)))
        catch case e: Exception => Response.text(500, e.reason)
      write(socket, response)
    catch case _: Exception => ()
    finally socket.close()

  private def readHead(in: InputStream): String =
    val out = ByteArrayOutputStream()
    var tail = 0
    while tail != 0x0d0a0d0a do
      val b = in.read()
      if b < 0 then throw RuntimeException("Connection closed")
      out.write(b)
      if out.size > MaxHead then throw RuntimeException("Request head too large")
      tail = (tail << 8) | b
    String(out.toByteArray, UTF_8).trim

  private def write(socket: Socket, response: Response): Unit =
    val head =
      s"HTTP/1.1 ${response.status} ${StatusText.default(StatusCode(response.status)).getOrElse("")}\r\n" +
        s"Content-Type: ${response.contentType}\r\n" +
        s"Content-Length: ${response.body.length}\r\n" +
        "Cache-Control: no-store\r\nConnection: close\r\n\r\n"
    val out = ByteArrayOutputStream(head.length + response.body.length)
    out.write(head.getBytes(UTF_8))
    out.write(response.body)
    socket.getOutputStream.write(out.toByteArray)
