use rust::ThreadPool;
use std::io::prelude::*;
use std::net::TcpListener;
use std::net::TcpStream;
use student::*;

mod student;

fn main() {
    let listener = TcpListener::bind("127.0.0.1:7878").unwrap();
    let pool = ThreadPool::new(4);

    for stream in listener.incoming() {
        let stream = stream.unwrap();

        pool.execute(|| {
            handle_connection(stream);
        });
    }

    println!("Shutting down.\n");
}

fn handle_connection(mut stream: TcpStream) {
    let mut buffer = [0; 1024];
    stream.read(&mut buffer).unwrap();

    let get = b"GET / HTTP/1.1\r\n";
    let put = b"PUT / HTTP/1.1\r\n";

    let (status_line, data) = if buffer.starts_with(get) {
        let data = get_students("store/students.json");
        let json_data = serde_json::to_string(&data).expect("Failed to serialize to JSON");
        ("HTTP/1.1 200 OK", json_data)
    } else if buffer.starts_with(put) {
        let request_str = String::from_utf8_lossy(&buffer);
        let json_str = request_str
            .lines()
            .find(|line| line.starts_with('{'))
            .unwrap_or("");
        let json_str = json_str.trim_matches('\0');

        if let Ok(student) = serde_json::from_str::<StudentDto>(json_str) {
            let (status, message) = update_student(student);
            (status, message)
        } else {
            (
                "HTTP/1.1 400 Bad Request",
                String::from("Invalid student data"),
            )
        }
    } else {
        ("HTTP/1.1 404 NOT FOUND", String::from("404"))
    };

    let response = format!(
        "{}\r\nContent-Length: {}\r\n\r\n{}",
        status_line,
        data.len(),
        data
    );

    stream.write_all(response.as_bytes()).unwrap();
    stream.flush().unwrap();
}
