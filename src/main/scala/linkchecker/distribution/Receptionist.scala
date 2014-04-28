package linkchecker.distribution
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.ActorLogging
import akka.actor.SupervisorStrategy
import akka.actor.Terminated
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import scala.util.Random
import akka.actor.Address
import akka.actor.Deploy
import akka.remote.RemoteScope
import scala.concurrent.duration._
import akka.actor.ReceiveTimeout

object Receptionist {
  private case class Job(client: ActorRef, url: String)
  case class Get(url: String)
  case class Result(url: String, links: Set[String])
  case class Failed(url: String, reason: String)

  def props = Props[Receptionist]
}

class Receptionist extends Actor with ActorLogging {
  import Receptionist._

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  var reqNo = 0

  def controllerProps = Controller.props

  def receive = waiting

  val waiting: Receive = {
    case Get(url) =>
      log.debug("receive get for {}", url)
      context.become(runNext(Vector(Job(sender, url))))
  }

  def runNext(queue: Vector[Job]): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting
    else {
      val controller = context.actorOf(controllerProps, s"c$reqNo")
      context.watch(controller)
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }

  def running(queue: Vector[Job]): Receive = {

    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(context.unwatch(sender))
      context.become(runNext(queue.tail))

    case Terminated(_) =>
      val job = queue.head
      job.client ! Failed(job.url, "controller failed unexpectedly")
      context.become(runNext(queue.tail))

    case Get(url) =>
      log.debug("receive get for {}", url)
      context.become(enqueueJob(queue, Job(sender, url)))
  }

  def enqueueJob(queue: Vector[Job], job: Job): Receive = {
    if (queue.size > 3) {
      sender ! Failed(job.url, "queue overflow")
      running(queue)
    } else running(queue :+ job)
  }
}

class ClusterReceptionist extends Actor {
  import Receptionist._

  val cluster = Cluster(context.system)
  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
  cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  val randomGen = new Random
  def pick[A](coll: IndexedSeq[A]): A = coll(randomGen.nextInt(coll.size))

  def receive = awaitingMembers

  val awaitingMembers: Receive = {

    case Get(url) =>
      sender ! Failed(url, "no nodes available")

    case current: ClusterEvent.CurrentClusterState =>
      val notMe = current.members.toVector map (_.address) filter (_ != cluster.selfAddress)
      if (notMe.nonEmpty)
        context.become(active(notMe))

    case ClusterEvent.MemberUp(member) if member.address != cluster.selfAddress =>
      context.become(active(Vector(member.address)))
  }

  def active(addresses: Vector[Address]): Receive = {
    
    case Get(url) if context.children.size < addresses.size =>
      val client = sender
      val address = pick(addresses)
      context.actorOf(Worker.props(client, url, address))
      
    case Get(url) =>
      sender ! Failed(url, "too many parallel queries")
      
    case ClusterEvent.MemberUp(member) if member.address != cluster.selfAddress =>
      context.become(active(addresses :+ member.address))
      
    case ClusterEvent.MemberRemoved(member, _) =>
      val next = addresses filterNot (_ == member.address)
      if (next.isEmpty) context.become(awaitingMembers)
      else context.become(active(next))
  }

}

object Worker {
  def props(client: ActorRef, url: String, address: Address) = 
    Props(classOf[Worker],client, url, address)
}

class Worker(client: ActorRef, url: String, node: Address) extends Actor {
  implicit val s = context.parent

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy
  
  val controller = context.actorOf(Controller.remoteProps(node), "controller")
  context.watch(controller)

  context.setReceiveTimeout(60.seconds)
  controller ! Controller.Check(url, 2)

  def receive = ({
    case ReceiveTimeout =>
      context.unwatch(controller)
      client ! Receptionist.Failed(url, "controller timed out")
    case Terminated(_) =>
      client ! Receptionist.Failed(url, "controller died")
    case Controller.Result(links) =>
      context.unwatch(controller)
      client ! Receptionist.Result(url, links)
  }: Receive) andThen (_ => context.stop(self))
}