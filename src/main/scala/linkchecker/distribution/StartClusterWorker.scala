package linkchecker.distribution

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object StartClusterWorker {

  def main(args: Array[String]) {
	  startup(0)
  }

  def startup(port: Int): Unit = {
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem("ClusterSystem", config)
    // Create an actor that handles cluster domain events
    system.actorOf(ClusterWorker.props, name = "ClusterMain")
  }
}