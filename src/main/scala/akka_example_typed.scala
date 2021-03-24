import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Props}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object akka_example_typed {
  import BookingSystem._
  val persId = "001"

  object BookingSystem {
    sealed trait Command
    case class Book(number: Int)     extends Command
    case class Refund(number: Int)   extends Command

    // 2 создаем handlecommand
    sealed trait Event
    case class Booked(id: Int, number: Int)     extends Event
    case class Refunded(id: Int, number: Int)   extends Event

    // 3 создаем handlecommand
    object State {
      val empty = State(100)
    }

    final case class State(value: Int) {
      def book(number: Int): State     = copy(value = value - number)
      def refund(number: Int): State   = copy(value = value + number)
    }

    def handleCommand(
        persistenceId: String,
        state: State,
        command: Command,
        ctx: ActorContext[Command]
    ): Effect[Event, State] =
      command match {
        case Book(number) =>
          ctx.log.info(s"1. Получено сообщение - бронь билетов $number шт. Количество билетов ${state.value} шт.")
          val booked = Booked(persistenceId.toInt, number)
          Effect
            .persist(booked)
            .thenRun { x =>
              ctx.log.info(s"3. После брони билетов, количество оставшихся ${x.value} шт.")
            }
        case Refund(number) =>
          ctx.log.info(s"1. Получено сообщение - возврат билетов $number шт. Количество билетов ${state.value} шт.")
          Effect
            .persist(Booked(persistenceId.toInt, number))
            .thenRun { x =>
              ctx.log.info(s"3. После возврата билетов, количество оставшихся ${x.value} шт.")
            }
      }

    def handleEvent(state: State, event: Event, ctx: ActorContext[Command]): State =
      event match {
        case Booked(_, number) =>
          ctx.log.info(s"2. Перехвачено событие бронь билетов в количестве $number шт. Количество билетов ${state.value} шт.")
          state.book(number)
        case Refunded(_, number) =>
          ctx.log.info(s"2. Перехвачено событие возврат билетов в количестве $number шт. Количество билетов ${state.value} шт.")
          state.refund(number)
      }

    def apply(): Behavior[Command] =
      Behaviors.setup { ctx =>
        // 1 про Command, Event, State
        EventSourcedBehavior[Command, Event, State](
          PersistenceId("ShoppingCart", persId),
          State.empty,
          (state, command) => handleCommand(persId, state, command, ctx),
          (state, event) => handleEvent(state, event, ctx)
        )
      }
  }

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val writeActorRef = ctx.spawn(BookingSystem(), "BookingSystem", Props.empty)

      writeActorRef ! Book(10)
      writeActorRef ! Refund(5)

      Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(akka_example_typed(), "akka_typed")
  }

}
