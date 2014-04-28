package linkchecker.errorKernel

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ReceiveTimeout
import akka.actor.ActorSystem
import akka.actor.Props

object MainActor {
  def props = Props[MainActor]
}

class MainActor extends Actor {

  import Receptionist._

  val receptionist = context.actorOf(Receptionist.props, "receptionist")
  context.watch(receptionist) // sign death pact

//  receptionist ! Get("http://www.google.it")
//  receptionist ! Get("http://www.google.it/1")
//  receptionist ! Get("http://www.google.it/2")
//  receptionist ! Get("http://www.google.it/3")
//  receptionist ! Get("http://www.google.it/4")
  receptionist ! Get("http://www.repubblica.it")

  context.setReceiveTimeout(60.seconds)

  def receive = {
    case Result(url, set) =>
      println(set.toVector.sorted.mkString(s"Results for '$url':\n", "\n", "\n"))
    case Failed(url) =>
      println(s"Failed to fetch '$url'\n")
    case ReceiveTimeout =>
      context.stop(self)
  }
}

object Main {
  def main(args: Array[String]) {
    
    val system = ActorSystem("LinkChecker")
    val mainActor = system.actorOf(MainActor.props)

  }
}