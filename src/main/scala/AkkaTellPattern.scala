import AkkaSample.system
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}

import scala.concurrent.Future

object AkkaTellPattern extends App {

  val system = ActorSystem("DonutStoreActorSystem")

  /**
   * Tell Pattern
   * Akka provides various interaction patterns, and the one which we will use in this tutorial is called the Tell Pattern.
   * This pattern is useful when you need to send a message to an actor, but do not expect to receive a response.
   * As a result, it is also commonly referred to as "fire and forget".
   */
  object DonutStoreProtocol{
    case class Info(name: String)
  }

  /**
   * Creating an Akka Actor is really easy. All you have to do is have a class extend the Actor trait.
   * Akka also comes built-in with a logging utility for actors, and you can access it by simply adding the ActorLogging trait.
   * Inside our actor, the primary method we are interested in at the moment is the receive method.
   * The receive method is the place where you instruct your actor which messages or protocols it is designed to react to. For our DonutInfoActor below, it will react to Info messages, where the actor will simply print the name property
   */
  class DonutInfoActor extends Actor with ActorLogging {

    import AkkaTellPattern.DonutStoreProtocol.Info

    override def receive: Receive = {
      case Info(name) => log.info(s"Found $name")
    }
  }

  import AkkaTellPattern.DonutStoreProtocol.Info

  //Creating Actor Reference
  val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActorTellPattern")
  donutInfoActor ! Info("vanilla")

  val isTerminated: Future[Terminated] = system.terminate()


}
