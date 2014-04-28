package linkchecker.distribution

import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.duration._
import akka.actor.ReceiveTimeout
import akka.actor.Props
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy
import akka.actor.Terminated
import akka.actor.Deploy
import akka.remote.RemoteScope
import akka.actor.Address

object Controller {
  case class Check(url: String, depth: Int)
  case class Result(links: Set[String])

  def props = Props[Controller]
  def remoteProps(node: Address) = props.withDeploy(Deploy(scope = RemoteScope(node)))
}

class Controller extends Actor with ActorLogging {
  import Controller._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
    case _: Exception => SupervisorStrategy.Restart
  }

  var cache = Set.empty[String]

  context.setReceiveTimeout(10.seconds)

  def receive = {

    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0) {
        val getter = context.actorOf(Getter.props(url, depth - 1))
        context.watch(getter)
      }

      cache += url

    case ReceiveTimeout =>
      context.children foreach context.stop

    case Terminated(_) =>
      if (context.children.isEmpty)
        context.parent ! Result(cache)
  }
}