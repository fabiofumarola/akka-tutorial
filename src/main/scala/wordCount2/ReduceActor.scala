package wordCount2

import akka.actor.Actor
import akka.actor.Props
import akka.routing.RoundRobinPool
import akka.actor.ActorLogging

object ReduceActor {
  case class ReduceData(id: Int, data: Map[String, Int])

  def props = Props[ReduceActor]
  def routerProps = RoundRobinPool(5).props(props)
}

class ReduceActor extends Actor with ActorLogging {

  import MapActor._
  import ReduceActor._

  def receive: Receive = {
    case MapResult(id, tuples) =>
      log.debug("reducing tuple for job {}, {}", id, tuples)
      sender ! reduce(id, tuples)
  }

  def reduce(id: Int, words: Seq[(String, Int)]): ReduceData = {
    val mapData = words.foldLeft(Map.empty[String, Int]) { (map, wordCount) =>
      val count = map.getOrElse(wordCount._1, 0) + wordCount._2
      map + (wordCount._1 -> count)
    }
    ReduceData(id, mapData)
  }
}