package wordCount1

import akka.actor.Actor
import akka.actor.Props

object AggregateActor {
  def props = Props[AggregateActor]
}

class AggregateActor extends Actor {

  var finalReduceMap = Map.empty[String, Int]

  def receive: Receive = {
    case ReduceData(data) =>
      aggregate(data)
    case Result =>
      sender ! finalReduceMap.toString
  }

  def aggregate(data: Map[String, Int]): Unit = {

    for ((key, value) <- data) {
      if (finalReduceMap contains key)
        finalReduceMap += key -> (finalReduceMap(key) + value)
      else
        finalReduceMap += (key -> value)
    }

  }
}