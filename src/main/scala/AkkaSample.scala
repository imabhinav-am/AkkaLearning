import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object AkkaSample extends App {
  // you can think of the ActorSystem as a 'black' box that will eventually hold your actors, and allow you to interact with them.
  val system = ActorSystem("DonutStoreActorSystem")
  //Obviously, you would perhaps want to close your actor system when your application is shutting down,
  // similar to how you would cleanly close any open resources such as database connections.
  val isTerminated = system.terminate()
  isTerminated.onComplete{
    case Success(value) => println(value)
    case Failure(exception) => println(exception.getMessage())
  }
  Thread.sleep(5000)
}
