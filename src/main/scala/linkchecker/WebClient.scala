package linkchecker

import akka.actor.ActorSystem
import spray.http._
import scala.concurrent.Future
import spray.client.pipelining._
import akka.event.Logging
import scala.util.Success
import scala.util.Failure
import akka.io.IO
import spray.can.Http
import akka.util.Timeout
import scala.concurrent.duration._
import akka.util.Timeout.durationToTimeout
import akka.pattern._

trait WebClient {
  def get(url: String)(implicit system: ActorSystem): Future[String]
}

object SprayWebClientRequestLevel extends WebClient {
  def get(url: String)(implicit system: ActorSystem): Future[String] = {
    import system.dispatcher

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    val r = pipeline(Get(url))
    
    r.map(_.entity.asString)
  }
}

object SprayWebClientHostLevel extends WebClient {

  private implicit val timeout: Timeout = 5 seconds

  def get(url: String)(implicit system: ActorSystem): Future[String] = {

    import system.dispatcher
    
    val uri = Uri(url)
    val host = uri.authority.toString.replace("/", "")

    val pipeline: Future[SendReceive] =
      for (
        Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(host, port = 80)
      ) yield sendReceive(connector)

    val request = Get("/")
    pipeline.flatMap(f => f(request)).map(_.entity.asString)
  }
}

object SprayWebClientPipeline {

  def get(url: String)(implicit system: ActorSystem): Future[HttpResponse] = {
    import system.dispatcher // execution context for futures
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    pipeline(Get(url))

  }
}

object MainWebClient extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-spray-client")
  import system.dispatcher // execution context for futures below
  val log = Logging(system, getClass)

  val url = "http://www.google.it/"

  val future = SprayWebClientRequestLevel.get(url)

  future.onComplete {
    case Success(body) =>
      println(body)
      system.shutdown

    case Failure(ex) =>
      ex.printStackTrace()
      system.shutdown
  }

}


