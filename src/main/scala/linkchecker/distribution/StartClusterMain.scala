package linkchecker.distribution

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object StartClusterMain {

  def main(args: Array[String]) {
	  startup(2552)
  }

  def startup(port: Int): Unit = {
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem("ClusterSystem", config)
    // Create an actor that handles cluster domain events
    system.actorOf(ClusterMain.props, name = "ClusterMain")
  }
}