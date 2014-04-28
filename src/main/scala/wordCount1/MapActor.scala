package wordCount1

import akka.actor.Actor
import akka.routing.RoundRobinPool
import akka.actor.Props
import akka.actor.ActorLogging

object MapActor {

  val STOP_WORDS_LIST = Set("a", "am", "an", "and", "are", "as", "at",
    "be", "do", "go", "if", "in", "is", "it", "of", "on", "the", "to")

  def routerProps = RoundRobinPool(5).props(Props[MapActor])

}

class MapActor extends Actor with ActorLogging {

  import MapActor._

  def receive: Receive = {
    case message: String =>
      sender ! processExpression(message)
  }

  def processExpression(line: String): MapData = {
    
    MapData(line.split("\\s+").
      filter(w => !STOP_WORDS_LIST.contains(w.toLowerCase)).
      map(w => WordCount(w, 1)))
  }

}