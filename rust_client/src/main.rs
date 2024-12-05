use std::sync::Arc;
use tokio::sync::Semaphore;
use std::time::Instant;

const MOCK_SERVER_URL: &str = "http://127.0.0.1:8000/api";
const MAX_CONCURRENT_REQUESTS: usize = 50;
const NUM_REQUESTS: usize = 1000;
const MAX_RETRIES: usize = 3;

async fn send_request(url: &str) -> Result<(), String> {
    let mut attempts = 0;

    while attempts < MAX_RETRIES {
        let response = reqwest::get(url).await;

        match response {
            Ok(res) if res.status().is_success() => {
                println!("Success: {}", res.status());
                return Ok(());
            }
            Ok(res) => {
                println!("Failed (attempt {}): {}", attempts + 1, res.status());
            }
            Err(_) => {
                println!("Request failed (attempt {}): Network error or timeout", attempts + 1);
            }
        }
        attempts += 1;
    }
    Err("Max retries reached".to_string())
}

async fn send_requests_concurrently() -> (usize, usize) {
    let semaphore = Arc::new(Semaphore::new(MAX_CONCURRENT_REQUESTS));
    let mut tasks = Vec::new();

    let successful_requests = Arc::new(tokio::sync::Mutex::new(0));
    let failed_requests = Arc::new(tokio::sync::Mutex::new(0));

    for _ in 0..NUM_REQUESTS {
        let permit = semaphore.clone().acquire_owned().await.unwrap();
        let successful_requests = successful_requests.clone();
        let failed_requests = failed_requests.clone();

        let task = tokio::spawn(async move {
            match send_request(MOCK_SERVER_URL).await {
                Ok(_) => {
                    let mut count = successful_requests.lock().await;
                    *count += 1;
                }
                Err(_) => {
                    let mut count = failed_requests.lock().await;
                    *count += 1;
                }
            }
            drop(permit);
        });

        tasks.push(task);
    }

    for task in tasks {
        task.await.unwrap();
    }

    let successful = *successful_requests.lock().await;
    let failed = *failed_requests.lock().await;

    (successful, failed)
}

#[tokio::main]
async fn main() {
    let start_time = Instant::now();

    println!("Sending requests to mock server...");

    let (successful, failed) = send_requests_concurrently().await;

    println!("Total Successful Requests: {}", successful);
    println!("Total Failed Requests: {}", failed);

    let duration = start_time.elapsed();
    println!("Total execution time: {:?}", duration);
}
