import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.{DefaultResizer, RoundRobinPool}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RoundRobinPoolExample extends App{
  val system = ActorSystem("DonutStoreActorSystem")

  object DonutStoreProtocol {

    case class Info(name: String)

    case class CheckStock(name: String)

    case class WorkerFailedException(error: String) extends Exception(error)
  }

  class DonutStockActor extends Actor with ActorLogging{
    import DonutStoreProtocol._

    override def supervisorStrategy: SupervisorStrategy = {
      OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 seconds){
        case _: WorkerFailedException =>
          log.error("Worker failed exception, will restart")
          Restart

        case _: Exception =>
          log.error("Worker failed, will need to escalate up the hierarchy")
          Escalate
      }
    }
    //We will not create worker actor
    //val workerActor: ActorRef = context.actorOf(Props[DonutStockWorkerActor], name = "DonutStockWorkerActor")

    //We'll use resizeable RoundRobinPool
    val resizer: DefaultResizer = DefaultResizer(lowerBound = 5, upperBound = 10)
    val props = RoundRobinPool(5, Some(resizer), supervisorStrategy = supervisorStrategy).props(Props[DonutStockWorkerActor])
    val donutStockWorkerRouterPool: ActorRef = context.actorOf(props, "DonutStockWorkerRouter")

    override def receive: Receive = {
      case checkStock @ CheckStock(name) =>
        log.info(s"Checking stock for $name donut")
        donutStockWorkerRouterPool forward checkStock
    }
  }

  println("\ntep 4: Worker Actor called DonutStockWorkerActor")
  class DonutStockWorkerActor extends Actor with ActorLogging {

    import DonutStoreProtocol._

    override def postRestart(reason: Throwable): Unit = {
      log.info(s"restarting ${self.path.name} because of $reason")
    }

    def receive: Receive = {
      case CheckStock(name) =>
        sender ! findStock(name)
    }

    def findStock(name: String): Int = {
      log.info(s"Finding stock for donut = $name, thread = ${Thread.currentThread().getId}")
      100
    }
  }

  val donutStockActor = system.actorOf(Props[DonutStockActor], name = "DonutStockActor")

  import DonutStoreProtocol._
  import akka.pattern._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout: Timeout = Timeout(5 second)

  val vanillaStockRequests = (1 to 10).map(i => (donutStockActor ? CheckStock("vanilla")).mapTo[Int])

    vanillaStockRequests.foreach(x => x.onComplete{
    case Success(res) => println(res)
    case Failure(exception) => println(exception.getMessage)
  })

  //  for {
//    results <- Future.sequence(vanillaStockRequests)
//  } yield println(s"vanilla stock results = $results")

  Thread.sleep(5000)
  system.terminate()
}
