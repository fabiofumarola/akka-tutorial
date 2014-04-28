package linkchecker.distribution

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ReceiveTimeout
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import akka.actor.ActorLogging
import akka.actor.RootActorPath
import akka.actor.Identify
import akka.actor.ActorIdentity
import akka.actor.Terminated
import com.typesafe.config.ConfigFactory

object ClusterMain {
  def props = Props[ClusterMain]
}

class ClusterMain extends Actor {
  import Receptionist._

  val cluster = Cluster(context.system)
  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
  cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])
  cluster.join(cluster.selfAddress)

  val receptionist = context.actorOf(Receptionist.props, "receptionist")
  context.watch(receptionist) // sign death pact

  def getLater(d: FiniteDuration, url: String): Unit = {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(d, receptionist, Get(url))
  }

  //getLater(Duration.Zero, "http://www.google.com")
  //getLater(Duration.Zero, "http://www.repubblica.it")

  def receive = {
    case ClusterEvent.MemberUp(member) =>
      if (member.address != cluster.selfAddress) {
        getLater(1.seconds, "http://www.google.com")
        getLater(2.seconds, "http://www.google.com/0")
        getLater(2.seconds, "http://www.google.com/1")
        getLater(3.seconds, "http://www.google.com/2")
        getLater(4.seconds, "http://www.google.com/3")
        getLater(5.seconds, "http://www.repubblica.it")
        context.setReceiveTimeout(20.seconds)
      }
    case Result(url, set) =>
      println(set.toVector.sorted.mkString(s"Results for '$url':\n", "\n", "\n"))
    case Failed(url, reason) =>
      println(s"Failed to fetch '$url': $reason\n")
    case ReceiveTimeout =>
      cluster.leave(cluster.selfAddress)
    case ClusterEvent.MemberRemoved(m, _) =>
      context.stop(self)
  }
}

object ClusterWorker {
  def props() = Props(classOf[ClusterWorker])
}

class ClusterWorker() extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
  cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])
  val main = cluster.selfAddress.copy(port = Some(2552))
  cluster.join(main)

  def receive = {
    case ClusterEvent.MemberUp(member) =>
      if (member.address == main)
        context.actorSelection(RootActorPath(main) / "user" / "app" / "receptionist") ! Identify("42")
    case ActorIdentity("42", None) => context.stop(self)
    case ActorIdentity("42", Some(ref)) =>
      log.info("receptionist is at {}", ref)
      context.watch(ref)
    case Terminated(_) => context.stop(self)
    case ClusterEvent.MemberRemoved(m, _) =>
      if (m.address == main) context.stop(self)
  }

}