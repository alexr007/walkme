import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.{ExecutionContext, Future}

object LoadBalancer {

  /** isolated state: A - request, B - response */
  case class State[A, B] private(queue: List[(A, B => Any)], busy: Vector[Boolean]) {
    def occupy(i: Int) = copy(busy = busy.updated(i, true))
    def release(i: Int) = copy(busy = busy.updated(i, false))
    def nextAvailable = busy.indexWhere(_ == false) match {
      case x if x >= 0 => Some(x)
      case _           => None
    }
    def nonEmpty = queue.nonEmpty
    def enqueue(rq: A, cb: B => Any) = copy(queue = (rq, cb) :: queue)
    def dequeueAndOccupy = (queue, nextAvailable) match {
      case (as :+ _, Some(i)) => copy(queue = as).occupy(i)
      case _                  => this
    }
    def takeNext = (queue, nextAvailable) match {
      case (_ :+ x, Some(i))  => Some((x, i))
      case _                  => None
    }
  }
  /** only one way to build initial state */
  object State {
    def initial[A, B](n: Int) = new State[A, B](List.empty, Vector.fill(n)(false))
  }
  /** thread-safe proxy to state */
  class StateRef[A, B] private(n: Int) {
    private val ref = new AtomicReference(State.initial[A, B](n))
    def enqueue(rq: A, cb: B => Any): Unit = ref.updateAndGet(_.enqueue(rq, cb))
    def release(i: Int): Unit = ref.updateAndGet(_.release(i))
    def dequeueAndOccupy =
      ref.getAndUpdate(_.dequeueAndOccupy).takeNext
    def nonEmpty = ref.get.nonEmpty
  }
  /** the only way to create it */
  object StateRef {
    def create[A, B](n: Int) = new StateRef[A, B](n)
  }
  /** actual implementation */
  class Balancer[A, B] private(n: Int, f: (A, Int) => Future[B])(implicit ec: ExecutionContext) {
    private val st = StateRef.create[A, B](n)

    private def process: Unit =
      st.dequeueAndOccupy
        .foreach { case ((rq, g), i) =>
          f(rq, i)
            .map(g)
            .andThen(_ => st.release(i))
            .andThen(_ => process)
        }

    def handle(rq: A, cb: B => Any) =
      Future { st.enqueue(rq, cb) }
        .andThen(_ => process)

  }
  /** the only way to create a balancer */
  object Balancer {
    def create[A, B](n: Int, f: (A, Int) => Future[B])(implicit ec: ExecutionContext) = new Balancer[A, B](n, f)
  }

}
