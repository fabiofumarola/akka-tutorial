package wordCount1

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorLogging

object MasterActor {
  def props = Props(classOf[MasterActor])
}

class MasterActor extends Actor with ActorLogging{

  val mapActor = context.actorOf(MapActor.routerProps, "map")
  val reduceActor = context.actorOf(ReducerActor.routerProps, "reduce")
  val aggregateActor = context.actorOf(AggregateActor.props, "aggregate")

  def receive: Receive = {
    case line: String =>
      mapActor ! line

    case mapData: MapData =>
      reduceActor ! mapData
      
    case reduceData: ReduceData =>
      aggregateActor ! reduceData

    case Result =>
      aggregateActor forward Result
  }

}