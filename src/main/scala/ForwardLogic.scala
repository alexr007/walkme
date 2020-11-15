import LoadBalancer.Balancer
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.util.ByteString

object ForwardLogic {
  sealed trait Command
  case class RequestAJoke(sender: ActorRef[String]) extends Command

  def target(port: Int) = s"http://localhost:$port/get-fortune"

  def create(implicit system: ActorSystem[_]): Behavior[Command] = {
    import system.executionContext
    val http = Http()
    def doGet(n: Int) = http
      .singleRequest(HttpRequest(uri = target(n)))
      .flatMap(_.entity.dataBytes.runFold(ByteString(""))(_ ++ _))
      .map(_.utf8String)

    val clients = IndexedSeq(9551, 9552, 9553)

    val b = Balancer.create[Any, String](clients.length, { (_, i) =>
      val port = clients(i)
      system.log.info(s">> Forwarding request to the instance $port")
      doGet(port).map { rs =>
        system.log.info(s"<< Got Response from client at $port: $rs")
        rs
      }
    })

    Behaviors.receiveMessage {
      case RequestAJoke(sender) =>
        system.log.info("> Got request")
        b.handle((), { rs =>
          system.log.info(s"< Responding to originator")
          sender ! rs
        })
        Behaviors.same
    }
  }
}
