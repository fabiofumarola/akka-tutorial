package wordCount2

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef

object AggregateActor {

  def props(id: Int, reduceData: Seq[Map[String, Int]]) = Props(classOf[AggregateActor], id, reduceData)
  
  case class AggregateResult(id: Int, data: Map[String,Int])
}

class AggregateActor(id: Int, reduceData: Seq[Map[String, Int]]) extends Actor {
  
  import AggregateActor._

  val aggregation = reduceData.foldLeft(Map.empty[String, Int]) { (finalMap, elem) =>

    val updatedValues = elem.map { kv =>
      val count = finalMap.getOrElse(kv._1, 0) + kv._2
      kv._1 -> count
    }
    finalMap ++ updatedValues
  }

  context.parent ! AggregateResult(id, aggregation)

  def receive: Receive = {
    case _ =>
      
  }
}