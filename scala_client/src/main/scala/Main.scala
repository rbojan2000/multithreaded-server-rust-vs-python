import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}
import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import java.util.concurrent.{Executors, Semaphore}
import scala.concurrent.duration._

object ConcurrentClient {

  val mockServerUrl = "http://127.0.0.1:8000/api"
  val maxConcurrentRequests = 50
  val numRequests = 50
  val maxRetries = 3

  val executor = Executors.newFixedThreadPool(maxConcurrentRequests)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
  
  val backend = AsyncHttpClientFutureBackend()

  @volatile var successCount = 0
  @volatile var failureCount = 0
  val semaphore = new Semaphore(maxConcurrentRequests)

  def sendRequest(retriesLeft: Int): Future[Response[Either[String, String]]] = {
    semaphore.acquire()

    val requestFuture = basicRequest
      .get(uri"$mockServerUrl")
      .send(backend)
      .recoverWith {
        case _ if retriesLeft > 0 =>
          println(s"Retrying request, remaining retries: $retriesLeft")
          sendRequest(retriesLeft - 1)
      }

    requestFuture.onComplete { _ =>
      semaphore.release()
    }

    requestFuture
  }

  def sendConcurrentRequests(): Future[Unit] = {
    val requestFutures = (1 to numRequests).map { _ =>
      sendRequest(maxRetries).map {
        case response if response.code.isSuccess =>
          synchronized {
            successCount += 1
          }
          response.body match {
            case Right(body) => println(s"Success: $body")
            case Left(error) => println(s"Error: $error")
          }
        case response =>
          synchronized {
            failureCount += 1
          }
          println(s"Request failed with status ${response.code}: ${response.statusText}")
      }
    }

    Future.sequence(requestFutures).map(_ => ()).andThen {
      case Success(_) =>
        println(s"All requests completed. Success: $successCount, Failure: $failureCount")
      case Failure(exception) =>
        println(s"Error occurred: $exception")
    }
  }

  def main(args: Array[String]): Unit = {
    val startTime = System.nanoTime()

    sendConcurrentRequests().onComplete {
      case Success(_) =>
        val endTime = System.nanoTime()
        val duration = (endTime - startTime) / 1e9
        println(s"All requests completed successfully in $duration seconds.")
      case Failure(exception) =>
        println(s"Error: $exception")
    }

    scala.concurrent.Await.result(sendConcurrentRequests(), Duration.Inf)
  }
}
