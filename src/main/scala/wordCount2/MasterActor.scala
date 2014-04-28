package wordCount2

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import scala.concurrent.duration._

object MasterActor {
  case class Process(data: Seq[String])
  case class Result(data: Map[String, Int])

  private case class Job(sender: ActorRef, resultCount: Int, reduceData: Seq[Map[String, Int]] = Seq.empty[Map[String,Int]])

  implicit def symbolToString(s: Symbol) = s.toString
  
  def props = Props[MasterActor]
}

class MasterActor extends Actor with ActorLogging {
  
  context.setReceiveTimeout(10 seconds)

  import MasterActor._

  private var jobs = Map.empty[Int, Job]

  var counter = 0

  val mappers = context.actorOf(MapActor.routerProps, 'mappers)
  val reducers = context.actorOf(ReduceActor.props, 'reducers)

  def receive: Receive = {

    case Process(lines) =>
      val id = counter
      jobs = jobs + (id -> Job(sender, lines.size))
      counter += 1

      lines.foreach { l =>
        mappers ! MapActor.Process(id, l)
      }

    case res: MapActor.MapResult =>
      reducers ! res

    case ReduceActor.ReduceData(id, map) =>
      val job = jobs(id)
      val resCount = job.resultCount - 1
      val redData = job.reduceData :+ map

      if (resCount == 0) {
        //compute the final result
        val aggregateActor = context.actorOf(AggregateActor.props(id, redData))
      } else {
        //update the job
        jobs += (id -> job.copy(resultCount = resCount, reduceData = redData))
      }

    case AggregateActor.AggregateResult(id, map) =>
      val job = jobs(id)

      job.sender ! Result(map)

      sender ! PoisonPill

  }
}