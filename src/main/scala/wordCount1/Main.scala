package wordCount1

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.Inbox
import akka.pattern._
import scala.concurrent.Await

object Main {

  def main(args: Array[String]) {

    //Use the system's dispatcher as ExecutionContext
    import system.dispatcher

    val system = ActorSystem("MapReduceApp")
    val master = system.actorOf(MasterActor.props)

    implicit val timeout = Timeout(5 minutes)

    master ! "The quick brown fox tried to jump over the lazy dog and fell on the dog"
    master tell ("Dog is man's best friend", master)
    master ! "Dog and Fox belong to the same family"
    
    Thread.sleep(500)
    //val future = (master ? Result).mapTo[String]

    val response = for (
      result <- (master ? Result).mapTo[String]
    ) yield result
    
    val result = Await.result(response, 5 seconds)
    println(result)
    
    system.shutdown
  }

}