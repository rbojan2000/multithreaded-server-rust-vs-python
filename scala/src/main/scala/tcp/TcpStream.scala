package tcp

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.Socket

class TcpStream(socket: Socket) {
  private val reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
  private val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))

  def read(buffer: Array[Byte]): Int = {
    socket.getInputStream.read(buffer)
  }

  def write(data: Array[Byte]): Unit = {
    socket.getOutputStream.write(data)
    socket.getOutputStream.flush()
  }

  def close(): Unit = {
    reader.close()
    writer.close()
    socket.close()
  }
}
