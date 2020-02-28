import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ErrorKernelSupervision extends App {
  //We introduce the Error Kernel Pattern, which mandates that failures are isolated and localized,
  //as opposed to crashing an entire system

  val system = ActorSystem("DonutStoreActorSystem")

  object DonutStoreProtocol {

    case class Info(name: String)

    case class CheckStock(name: String)

    //we'll create a custom exception (WorkerFailedException),
    // that we will use to show how to isolate failures by following the Error Kernel Approach.
    case class WorkerFailedException(error: String) extends Exception(error)

  }

  //we'll create a custom exception (WorkerFailedException),
  //that we will use to show how to isolate failures by following the Error Kernel Approach.
  //Our actor will react to exceptions of type WorkerFailedException, and attempt to restart the child actor DonutStockWorkerActor.
  //For all other exceptions, we assume that DonutStockActor is unable to handle those and, in turn, it will escalate those exceptions up the actor hierarchy.
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

    val workerActor: ActorRef = context.actorOf(Props[DonutStockWorkerActor], name = "DonutStockWorkerActor")

    override def receive: Receive = {
      case checkStock @ CheckStock(name) =>
        log.info(s"Checking stock for $name donut")
        workerActor forward checkStock
      }
    }

  class DonutStockWorkerActor extends Actor with ActorLogging{

    import DonutStoreProtocol._

    @throws[Exception](classOf[Exception])
    override def postRestart(reason: Throwable): Unit = {
      log.info(s"restarting ${self.path.name} because of $reason")
    }

    override def receive: Receive = {
      case CheckStock(name) =>
        findStock(name)
        context.stop(self)
    }

    def findStock(name: String): Int = {
      log.info(s"Finding stock for $name")
      //throw new IllegalStateException("boom") // Will Escalate the exception up the hierarchy
      throw new WorkerFailedException("boom") // Will Restart DonutStockWorkerActor
      100
    }


  }

  val donutStockActor = system.actorOf(Props[DonutStockActor], name = "DonutStockActor")


  import DonutStoreProtocol._
  import akka.pattern._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout: Timeout = Timeout(5 second)

  val vanillaDonutStock: Future[Int] = (donutStockActor ? CheckStock("Vanilla")).mapTo[Int]
  vanillaDonutStock.onComplete{
    case Success(value) => println(value)
    case Failure(exception) => println(exception.getMessage)
  }

  Thread.sleep(5000)
  system.terminate()
}
