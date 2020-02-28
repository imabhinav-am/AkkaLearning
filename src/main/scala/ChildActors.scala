import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object ChildActors extends App {
  val system = ActorSystem("DonutActorSystem")

  object DonutStoreProtocol{
    case class Info(name: String)
  }

  class BakingActor extends Actor with ActorLogging{

    import DonutStoreProtocol._

    override def receive: Receive = {
      case Info(name) =>
        log.info(name)
    }
  }

  class DonutInfoActor extends Actor with ActorLogging {

    import DonutStoreProtocol._

    //By using the actor context value, the BakingActor will be a child actor of DonutInfoActor.
    val bakingActor: ActorRef = context.actorOf(Props[BakingActor], name = "BakingActor")

    //From a hierarchy point of view, we can assume that DonutInfoActor will reside under the /user hierarchy: /user/DonutInfoActor.
    // This would mean that the child BakingActor's hierarchy and path would be /user/DonutInfoActor/BakingActor.
    // To keep this example simple, inside the receive method of the DonutInfoActor, we will simply forward the same Info message to the BakingActor
    override def receive: Receive = {
      case msg@Info(name) =>
        log.info(name)
        bakingActor forward msg
    }
  }

  val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActor")

  import DonutStoreProtocol._
  donutInfoActor ! Info("Vanilla")

  Thread.sleep(3000)
  system.terminate()
}
