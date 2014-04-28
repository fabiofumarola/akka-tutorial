package wordCount2

import akka.actor.ActorSystem
import akka.actor.Inbox
import scala.concurrent.duration._

object Main {

  def main(args: Array[String]) {
    //Use the system's dispatcher as ExecutionContext
    import system.dispatcher

    val system = ActorSystem("MapReduceApp")

    val master = system.actorOf(MasterActor.props)

    val lines = Vector("The quick brown fox tried to jump over the lazy dog and fell on the dog",
      "Dog is man's best friend", "Dog and Fox belong to the same family")

    val inbox = Inbox.create(system)

    inbox.send(master, MasterActor.Process(lines))

    val MasterActor.Result(data) = inbox.receive(1 seconds)

    println(s"final result $data")
    
    data.foreach { kv =>
      println(kv)
    }

    system.shutdown
  }
}