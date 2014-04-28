package wordCount1

import akka.actor.Actor
import akka.routing.RoundRobinPool
import akka.actor.Props
import akka.actor.ActorLogging

object ReducerActor {
    def routerProps = RoundRobinPool(5).props(Props[ReducerActor])
}

class ReducerActor extends Actor with ActorLogging{

  def receive: Receive = {
    case MapData(wordCounts) =>
      sender ! reduce(wordCounts)
  }
  
  def reduce(words: IndexedSeq[WordCount]): ReduceData = {
    val mapData = words.foldLeft(Map.empty[String,Int]){ (map,wordCount) =>
    	val count = map.getOrElse(wordCount.word,0) + wordCount.count
    	map + (wordCount.word -> count)
    }
    ReduceData(mapData)
  }
}