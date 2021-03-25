import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Props}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object akka_example_typed {
  import BookingSystem._

  object BookingSystem {
    def apply(): Behavior[Command] = Behaviors.setup { ctx =>
      /**
       * !!!!!!!!!!!!!  1
       * Акторы с состоянием, которое персистется в журнал и может быть восстановлено, при сбоях в JVM, ручном рестарте или при миграции на кластере
       * Сохраняются только события, не состояние. Хотя иногда можно сохранить состояние.
       * События персистятся путем добавления в хранилище (НЕ ИЗМЕНЕНИЯ), оптимизированное под запись, что повышает скорость записи
       *
       * Поведение персистентного актора типизировано типов сообщения, которое актор может принимать.
       * В состав персистентного актора входят:
       * - персистенс id - идентифицирует события в журнале актора или инстансе актора
       * - начальное состояние актора
       * - handleCommand - в методе описана логика работы с сообщениями. Данный метод возвращает эффект, например запись события в журнал или остановка актора
       * - handleEvent - возвращает новое состояние на основе текущего и после тог как событие было возвращено
       */
      val persId = "BookingSystem"
      EventSourcedBehavior[Command, Event, State](
        PersistenceId.ofUniqueId(persId),
        State.empty,
        (state, command) => handleCommand(persId, state, command, ctx),
        (state, event) => handleEvent(state, event, ctx)
      )
    }

    sealed trait Command
    case class Book(number: Int)   extends Command
    case class Refund(number: Int) extends Command

    // 2 создаем handlecommand
    sealed trait Event
    case class Booked(id: String, number: Int)   extends Event
    case class Refunded(id: String, number: Int) extends Event

    // 3 создаем handlecommand
    // Состояние обычно определяется как неизменяемый класс
    final case class State(value: Int) {
      def book(number: Int): State   = copy(value = value - number)
      def refund(number: Int): State = copy(value = value + number)
    }
    object State {
      val empty = State(100)
    }


    /**
     * 4.
     * Перехватывает входящее сообщение и создает эффект, например сохранение эвента или остановка актора
     * В .persist(booked) сохраняется одно или несколько событий атомарно.
     * После успешного сохранения и обновления стейта выполняется каллбек функция thenRun, в которой можно добавить другие сайд эффекты.
     *
     * */
    def handleCommand(persistenceId: String, state: State, command: Command, ctx: ActorContext[Command]): Effect[Event, State] = command match {
        case Book(number) =>
          ctx.log.info(s"1. Получено сообщение - бронь билетов $number шт. Количество билетов ${state.value} шт.")
          val booked = Booked(persistenceId, number)
          Effect
            .persist(booked)
            .thenRun { x =>
              ctx.log.info(s"3. После брони билетов, количество оставшихся ${x.value} шт.")
            }
        case Refund(number) =>
          ctx.log.info(s"1. Получено сообщение - возврат билетов $number шт. Количество билетов ${state.value} шт.")
          Effect
            .persist(Booked(persistenceId, number))
            .thenRun { x =>
              ctx.log.info(s"3. После возврата билетов, количество оставшихся ${x.value} шт.")
            }
      }

    /**
     * 5.
     * После успешного сохранения события, запускается метод handleEvent, который обновляет состояние актора.
     * Также handleEvent используется при запуске объекта для восстановления своего состояния из сохраненных событий.
     * handleEvent предназначен только для обновления стейта и не должен создавать эффекты. Для эффектов должен использовать
     * ся метод thenRun
    * */
    def handleEvent(state: State, event: Event, ctx: ActorContext[Command]): State = event match {
        case Booked(_, number) =>
          ctx.log.info(s"2. Перехвачено событие бронь билетов в количестве $number шт. Количество билетов ${state.value} шт.")
          state.book(number)
        case Refunded(_, number) =>
          ctx.log.info(s"2. Перехвачено событие возврат билетов в количестве $number шт. Количество билетов ${state.value} шт.")
          state.refund(number)
      }

  }

  def apply(): Behavior[NotUsed] = Behaviors.setup { ctx =>
      val bookingSystem = ctx.spawn(BookingSystem(), "BookingSystem", Props.empty)

      bookingSystem ! Book(5)
      bookingSystem ! Refund(2)
      bookingSystem ! Book(4)

      Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(akka_example_typed(), "akka_typed")
  }
}
