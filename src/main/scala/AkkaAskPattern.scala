import AkkaAskPattern.DonutStoreProtocol1.CheckStock
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AkkaAskPattern extends App {
  val system = ActorSystem("DonutStoreActorSystem")

  object DonutStoreProtocol{
    case class Info(name: String)
  }

  class DonutInfoActor extends Actor with ActorLogging{

    import AkkaAskPattern.DonutStoreProtocol.Info

    override def receive: Receive = {
      case Info(name) if name equals "Vanilla" =>
        log.info("Found Vanilla Donut")
        sender ! true
      case Info(name) =>
        log.info(s"$name Donut is not supported")
        sender ! false
    }
  }

  val donutInfoActor = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActorAskPattern")
  /**
   * Ask Pattern
   * To use the Akka Ask Pattern, you have to make use of the ? operator.
   * As a reminder, for the Akka Tell Pattern, we used the ! operator.
   * The Ask Pattern will return a future
   */
  import DonutStoreProtocol._
  import akka.pattern._
  import scala.concurrent.ExecutionContext.Implicits.global
  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeOut: Timeout = Timeout(5 seconds)
  val vanillaDonutFound = donutInfoActor ? Info("Vanilla")

  println(vanillaDonutFound)
//  for {
//    found <- vanillaDonutFound
//  } yield (println(found))

  vanillaDonutFound.onComplete{
    case Success(value) => println(value)
    case Failure(exception) => println(exception)
  }

  val glazedDonutFound = donutInfoActor ? Info("glazed")
  glazedDonutFound.onComplete{
    case Success(value) => println(value)
    case Failure(exception) => println(exception)
  }
  Thread.sleep(2000)

  /**
   * Ask Pattern mapTo
   */
  val vanillaDonutFound1 = (donutInfoActor ? Info("Vanilla")).mapTo[Boolean]
  vanillaDonutFound1.onComplete{
    case Success(value) => println(value)
    case Failure(exception) => println(exception)
  }
  Thread.sleep(2000)

  /**
   * AskPattern pipeTo
   * We continue our discussion on the Akka Ask Pattern, and will show another handy utility named pipeTo().
   * It attaches to a Future operation by registering the Future andThen callback to allow you to easily send the result back to the sender
   */
  object DonutStoreProtocol1{
    case class Info(name: String)

    case class CheckStock(name: String)
  }

  class DonutStockActor extends Actor with ActorLogging {
    import DonutStoreProtocol._

    def findStock(name: String) : Future[Int] = Future {
      100
    }

    override def receive: Receive = {
      case CheckStock(name) =>
        log.info(s"Checking stock for $name")
        findStock(name).pipeTo(sender)
    }
  }

  val donutStockActor = system.actorOf(Props[DonutStockActor], name = "DonutStockActor")
  val vanillaDonutStock = (donutStockActor ? CheckStock("Vanilla")).mapTo[Int]
  println(vanillaDonutStock)

  vanillaDonutStock.onComplete{
    case Success(res) => println(res)
    case Failure(e) => println(e.getMessage)
  }
  Thread.sleep(2000)
  val isTerminated = system.terminate()
}
