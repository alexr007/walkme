import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

object BalancerApp extends App {

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]) =
    Http()
      .newServerAt("localhost", 8080)
      .bind(routes)
      .onComplete {
        case Success(ServerBinding(la)) =>
          system.log.info("Server started at http://{}:{}/", la.getHostString, la.getPort)
        case Failure(ex) =>
          system.log.error("Server failed to start", ex)
          system.terminate()
      } (system.executionContext)

  val rootBehavior = Behaviors.setup[Nothing] { ctx =>
    implicit val as = ctx.system
    val logic = ForwardLogic.create
    val logicActor = ctx.spawn(logic, "ForwardLogic")
    val endpoints = new JokeRoutes(logicActor)
    startHttpServer(endpoints.routes)
    Behaviors.empty
  }

  ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
}
