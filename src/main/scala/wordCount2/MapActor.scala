package wordCount2

import akka.actor.Actor
import akka.routing.RoundRobinPool
import akka.actor.Props
import akka.actor.ActorLogging

object MapActor {

  val STOP_WORDS_LIST = Set("a", "am", "an", "and", "are", "as", "at",
    "be", "do", "go", "if", "in", "is", "it", "of", "on", "the", "to")

  def routerProps() = RoundRobinPool(5).props(props)

  def props() = Props[MapActor]

  case class Process(id: Int, line: String)
  case class MapResult(id: Int, data: Seq[(String, Int)])

}

class MapActor extends Actor with ActorLogging {

  import MapActor._

  def receive: Receive = {
    case Process(id, line) =>
      log.debug("processing line for job {}, {}",id,line)
      sender ! MapResult(id, processExpression(line))
  }

  def processExpression(line: String): Seq[(String, Int)] = {
    line.split("\\s+").
      filter(w => !STOP_WORDS_LIST.contains(w.toLowerCase)).
      map(w => (w, 1))
  }

}