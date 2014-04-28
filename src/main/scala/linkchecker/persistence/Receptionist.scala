package linkchecker.persistence
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.ActorLogging
import akka.actor.SupervisorStrategy
import akka.actor.Terminated
import akka.persistence.Processor
import akka.persistence.Persistent

object Receptionist {
  private case class Job(client: ActorRef, url: String)
  case class Get(url: String)
  case class Result(url: String, links: Set[String])
  case class Failed(url: String)

  def props = Props[Receptionist]
}

class Receptionist extends Actor with ActorLogging with Processor {
  import Receptionist._

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  var reqNo = 0

  def controllerProps = Controller.props

  def receive = waiting

  val waiting: Receive = {

    case Persistent(payload, sequenceNr) =>
      log.debug("receive persistente get for {}", payload)
      self ! payload

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
      job.client ! Failed(job.url)
      context.become(runNext(queue.tail))

    case Get(url) =>
      log.debug("receive get for {}", url)
      context.become(enqueueJob(queue, Job(sender, url)))
  }

  def enqueueJob(queue: Vector[Job], job: Job): Receive = {
    if (queue.size > 3) {
      sender ! Failed(job.url)
      running(queue)
    } else running(queue :+ job)
  }
}