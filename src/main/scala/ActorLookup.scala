import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object ActorLookup extends App {
  val system = ActorSystem("DonutActorSystem")

  object DonutStoreProtocol{
    case class Info(name: String)
  }

  class DonutInfoActor extends Actor with ActorLogging{

    import DonutStoreProtocol._

    override def receive: Receive = {
      case Info(name) =>
        log.info(name)
    }
  }

  val donutInfoActor = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActorLookup")

  import DonutStoreProtocol._
  donutInfoActor ! Info("Vanilla")
  system.actorSelection("/user/DonutInfoActorLookup") ! Info("Chocolate")

  //send a message to all the actors in an actor system by making use of the wildcard: /user/*
  system.actorSelection("/user/*") ! Info("vanilla and chocolate")

  val isTerminated = system.terminate()
}
