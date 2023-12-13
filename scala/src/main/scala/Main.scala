import play.api.libs.json.Json
import service.StudentService
import tcp.{TcpListener, TcpStream, ThreadPool}

object Main extends App {
  private val STUDENTS_STORE_PATH = "src/main/scala/store/students.json"
  private val TCP_ADDRES = "127.0.0.1"
  private val TCP_PORT = 8010
  private val POOL_SIZE = 4

  private val listener = new TcpListener(TCP_ADDRES, TCP_PORT)
  val pool = new ThreadPool(POOL_SIZE)

  private val studentService = new StudentService

  while (true) {
    val stream = listener.accept()
    pool.execute {
      handleConnection(stream)
    }
  }

  def handleConnection(stream: TcpStream): Unit = {
    val buffer = new Array[Byte](1024)
    stream.read(buffer)

    val get = "GET /students HTTP/1.1\r\n".getBytes
    val put = "PUT /students HTTP/1.1\r\n".getBytes

    if (buffer.startsWith(get)) {

      val studentsMap = studentService.getStudents(STUDENTS_STORE_PATH)

      val responseHeaders = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n"
      stream.write(responseHeaders.getBytes)
      stream.write(studentsMap.toString.getBytes)

      stream.close()
    }
    else if (buffer.startsWith(put)) {

      val requestBody = new String(buffer)
      val jsonDataStart = requestBody.indexOf("{")
      val jsonDataEnd = requestBody.indexOf("}")
      val jsonData = requestBody.substring(jsonDataStart, jsonDataEnd + 1)

      val json = Json.parse(jsonData)

      val index = (json \ "index").as[String]
      val name = (json \ "name").as[String]
      val surname = (json \ "surname").as[String]

      studentService.updateStudent(STUDENTS_STORE_PATH, index, name, surname)

      val responseHeaders = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n"
      val responseBody = Json.obj("message" -> "Student updated successfully").toString()
      stream.write(responseHeaders.getBytes)
      stream.write(responseBody.getBytes)
      stream.close()
    }
  }
}
