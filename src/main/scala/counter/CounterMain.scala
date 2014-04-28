package counter

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem

object CounterMain {
  def props() = Props[CounterMain]
}

class CounterMain extends Actor {
  val counterActor = context.actorOf(Props[CounterBecome], "nameActor")

  counterActor ! "incr"
  counterActor ! "incr"
  counterActor ! "get"

  def receive = {
    case count: Int =>
      println(s"count was $count")
      context.stop(self)
  }
}

object Main {
  def main(args: Array[String]) {
	  val system = ActorSystem("CounterSystem")
	  
	  val counterMain = system.actorOf(CounterMain.props,"counterMain")
	  
	  system.shutdown
  }
}

