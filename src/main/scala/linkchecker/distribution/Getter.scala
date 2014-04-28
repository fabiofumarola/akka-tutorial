package linkchecker.distribution

import akka.actor.Actor
import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext
import akka.pattern.pipe
import scala.util.{ Success, Failure }
import akka.actor.Props
import scala.concurrent.Promise
import scala.util.Success
import linkchecker._
import akka.actor.PoisonPill

object Getter {

  def props(url: String, depth: Int) = Props(classOf[Getter], url, depth)
}

class Getter(url: String, depth: Int) extends Actor {
  import Getter._

  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]

  def client: WebClient = SprayWebClientRequestLevel

  //implicit val sys = context.system
  //client.get(url)(context.system) pipeTo self

  val future = client.get(url)(context.system)

  //bind to current self
  val s = self

  future.onComplete {
    case Success(body) =>
      s ! body
    case Failure(ex) =>
      s ! Failure(ex)
  }

  def receive = {

    case body: String =>
      findLinks(body.toString).foreach { l =>
        context.parent ! Controller.Check(l, depth)
      }
      context.stop(self)

    case Failure(ex) =>
      self ! PoisonPill

  }

  val A_TAG = "(?i)<a ([^>]+)>.+?</a>".r
  val HREF_ATTR = """\s*(?i)href\s*=\s*(?:"([^"]*)"|'([^']*)'|([^'">\s]+))\s*""".r

  def findLinks(body: String): Iterator[String] = {
    for {
      anchor <- A_TAG.findAllMatchIn(body)
      HREF_ATTR(dquot, quot, bare) <- anchor.subgroups
    } yield if (dquot != null) dquot
    else if (quot != null) quot
    else bare
  }
}