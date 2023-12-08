use std::collections::HashMap;

use serde::{Deserialize, Serialize};

#[derive(Debug, Deserialize, Serialize)]
pub struct Student {
    name: String,
    surname: String,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct StudentDto {
    index: String,
    name: String,
    surname: String,
}

pub fn get_students(file_path: &str) -> HashMap<String, Student> {
    let file = std::fs::File::open(file_path).expect("File not found");
    let reader = std::io::BufReader::new(file);

    let students: HashMap<String, Student> =
        serde_json::from_reader(reader).expect("Error parsing JSON");

    students
}

pub fn update_student(student: StudentDto) -> (&'static str, String) {
    let mut students = get_students("store/students.json");
    let mut success_message = String::new();

    if let Some(existing_student) = students.get_mut(&student.index) {
        
        existing_student.name = student.name.clone();
        existing_student.surname = student.surname.clone();

        println!("Student updated: {:?}", existing_student);
        success_message = format!("Student with index {} updated successfully.", student.index);

        if let Ok(file) = std::fs::File::create("store/students.json") {
            serde_json::to_writer(file, &students).expect("Error writing JSON to file");
        } else {
            println!("Error opening file for writing.");
        }
        ("HTTP/1.1 200 OK", success_message)
    } else {
        
        println!("Student with index {} not found.", student.index);
        success_message = format!("Student with index {} not found.", student.index);
        ("HTTP/1.1 404 NOT FOUND", success_message)
    }
}
