package tcp

import java.net.ServerSocket

class TcpListener(host: String, port: Int) {
  private val serverSocket = new ServerSocket(port)

  def accept(): TcpStream = {
    val clientSocket = serverSocket.accept()
    new TcpStream(clientSocket)
  }

  def close(): Unit = {
    serverSocket.close()
  }
}
