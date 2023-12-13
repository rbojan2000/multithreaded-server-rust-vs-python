package service

import scala.util.Using
import play.api.libs.json._

case class Student(name: String, surname: String)

object Student {
  implicit val studentWrites: Writes[Student] = Json.writes[Student]
}

class StudentService {
  def updateStudent(STUDENTS_STORE_PATH: String, index: String, name: String, surname: String): Unit = {
    val students = getStudents(STUDENTS_STORE_PATH)
    val updatedStudents = students.updated(index, Student(name, surname))

    saveStudents(STUDENTS_STORE_PATH, updatedStudents)
  }

  private def saveStudents(filePath: String, students: Map[String, Student]): Unit = {
    val json = Json.toJson(students)
    Using(java.nio.file.Files.newBufferedWriter(java.nio.file.Paths.get(filePath))) { writer =>
      writer.write(Json.prettyPrint(json))
    }.getOrElse {
      throw new RuntimeException(s"Failed to write file: $filePath")
    }
  }

  def getStudents(filePath: String): Map[String, Student] = {
    Using(scala.io.Source.fromFile(filePath)) { source =>
      val jsonContent = source.mkString
      implicit val studentReads: Reads[Student] = Json.reads[Student]
      val json: JsObject = Json.parse(jsonContent).as[JsObject]

      json.fields.map { case (index, value) =>
        val student = value.as[Student]
        index -> student
      }.toMap
    }.getOrElse {
      throw new RuntimeException(s"Failed to read file: $filePath")
    }
  }
}
