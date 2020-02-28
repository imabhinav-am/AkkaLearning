import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object ActorLifecycle extends App{

  val system = ActorSystem("DonutStoreActorSystem")

  object DonutStoreProtocol{
    case class Info(name: String)
  }

  class BakingActor extends Actor with ActorLogging{

    import DonutStoreProtocol._

    override def preStart(): Unit = log.info("preStart Baking")

    override def postStop(): Unit = log.info("postStop Baking")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart Baking")

    override def postRestart(reason: Throwable): Unit = log.info("postRestart Baking")

    override def receive: Receive = {
      case Info(name) => log.info(name)
    }
  }

  class DonutInfoActor extends Actor with ActorLogging{

    import DonutStoreProtocol._

    override def preStart(): Unit = log.info("preStart Info")

    override def postStop(): Unit = log.info("postStop Info")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart Info")

    override def postRestart(reason: Throwable): Unit = log.info("postRestart Info")

    val bakingActor: ActorRef = context.actorOf(Props[BakingActor], name = "BakingActor")

    override def receive: Receive = {
      case msg @ Info(name) =>
        log.info(name)
        bakingActor forward msg
    }
  }

  val donutInfoActor = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActor")

  import DonutStoreProtocol._

  donutInfoActor ! Info("Vanilla")

  Thread.sleep(3000)

  system.terminate()
}
