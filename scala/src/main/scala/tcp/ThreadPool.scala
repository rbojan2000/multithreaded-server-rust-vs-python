package tcp

import java.util.concurrent.{Callable, Executors}

class ThreadPool(size: Int) {
  private val pool = Executors.newFixedThreadPool(size)

  def execute(f: => Unit): Unit = {
    pool.submit(new Callable[Unit] {
      override def call(): Unit = f
    })
  }

  def shutdown(): Unit = {
    pool.shutdown()
  }
}
