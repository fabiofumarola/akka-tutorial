package linkchecker.design

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import scala.concurrent.Future
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.Actor
import linkchecker.WebClient

class StepParent(child: Props, fwd: ActorRef) extends Actor {
  context.actorOf(child, "child")
  def receive = {
    case msg => fwd.tell(msg, sender)
  }
}

object GetterSpec {

  val firstLink = "http://www.test.info/1"

  val bodies = Map(
    firstLink ->
      """<html>
        |  <head><title>Page 1</title></head>
        |  <body>
        |    <h1>A Link</h1>
        |   <a href="http://test.info/2">click here</a>
        |  </body>
        |</html>""".stripMargin)

  val links = Map(
    firstLink -> Seq("http://test.info/2"))

  object FakeWebClient extends WebClient {
    def get(url: String)(implicit system: ActorSystem): Future[String] = {
      bodies.get(url) match {
        case None => Future.failed(new Exception("404"))
        case Some(body) => Future.successful(body)
      }
    }
  }

  def fakeGetter(url: String, depth: Int): Props =
    Props(new Getter(url, depth) {
      override def client = FakeWebClient
    })

}

class GetterSpec extends TestKit(ActorSystem("GetterSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  import GetterSpec._

  override def afterAll(): Unit = {
    system.shutdown()
  }

  "A Getter" must {

    "return the right body" in {
      val getter = system.actorOf(Props(new StepParent(fakeGetter(firstLink, 2), testActor)), "rightBody")
      for (link <- links(firstLink))
        expectMsg(Controller.Check(link, 2))
      expectMsg(Getter.Done)
    }

    "properly finish in case of errors" in {
      val getter = system.actorOf(Props(new StepParent(fakeGetter("unknown", 2), testActor)), "wrongLink")
      expectMsg(Getter.Done)
    }

  }

}