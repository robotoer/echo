package com.github.oetzi.echo.core

import com.github.oetzi.echo.Echo._
import com.github.oetzi.echo.Control._
import com.github.oetzi.echo.util.Cache

/**Behaviour provides an implementation of FRP Behaviours.
 */
sealed class Behaviour[T](private val rule: Time => T) {

  /**Holds last computed value and time it was computed at
   * to prevent redundant evaluation.
   */
  private var cache = new Cache[Time, T](time => rule(time))

  /**Evaluates the Behaviour at the current time. The function is atomic
   * with respect to the run-time group of FRP objects so evaluation times
   * are guaranteed to be monotonically increasing (even for Behaviours part
   * of more than one composite Behaviour and for concurrently evaluated
   * Behaviours.)
   */
  def eval(): T = {
    groupLock synchronized {
      this.at(now())
    }
  }

  /**Returns a Event[T] that occurs every time the given
   * Event occurs with the value of the Behaviour at that time.
   */
  def sample[U](sourceEvent: Event[U]): Event[(U, T)] = {
    frp {
      val source = new EventSource[(U, T)] {
        sourceEvent.hook {
          occ => occur((occ.value, Behaviour.this.at(occ.time)))
        }
      }

      source.event()
    }
  }

  /**Returns a Behaviour that behaves as the callee until the
   * Event occurs. It then switches to behaving as the passed
   * Behaviour.
   */
  def until[U](event: Event[U], behaviour: Behaviour[T]): Behaviour[T] = {
    frp {
      val rule: Time => T = {
        time =>
          val occ = event.top()

          if (occ == None) {
            this.at(time)
          }

          else {
            behaviour.at(time)
          }
      }

      new Behaviour(rule)
    }
  }

  /**Returns a Behaviour that toggles between behaving as the callee
   * and the passed Behaviour whenever the passed Event occurs.
   */
  def toggle[A](event: Event[A], behaviour: Behaviour[T]): Behaviour[T] = {
    frp {
      val rule: Time => T = {
        time =>
          val occ = event.top()

          if (occ == None || occ.get.num % 2 == 0) {
            this.at(time)
          }

          else {
            behaviour.at(time)
          }
      }

      new Behaviour(rule)
    }
  }

  /**Returns a Behaviour that transforms the callee's
   * value with the passed function.
   */
  def map[B](func: T => B): Behaviour[B] = {
    frp {
      new Behaviour(time => func(this.at(time)))
    }
  }

  /**Returns a Behaviour that transforms the callee's
   * and passed Behavior's value with the passed function.
   */
  def map2[U, V](behaviour: Behaviour[U])(func: (T, U) => V): Behaviour[V] = {
    frp {
      new Behaviour(time => func(this.at(time), behaviour.at(time)))
    }
  }

  /**Returns a Behaviour that transforms the callee's
   * and passed Behaviors' value with the passed function.
   */
  def map3[U, V, W](beh1: Behaviour[U], beh2: Behaviour[V])(func: (T, U, V) => W): Behaviour[W] = {
    frp {
      new Behaviour(time => func(this.at(time), beh1.at(time), beh2.at(time)))
    }
  }

  /**Evaluates the Behaviour at the specified time.
   */
  private[core] def at(time: Time): T = {
    cache.get(time)
  }
}

object Behaviour {
  def apply[T](rule: Time => T): Behaviour[T] = {
    new Behaviour(rule)
  }
}

/**Switcher represents a Behaviour that's value is always the latest evaluated occurrence in
 * a given Event[Behaviour].
 */
class Switcher[T](behaviour: Behaviour[T], val event: Event[Behaviour[T]]) extends Behaviour[T](
  Switcher.construct(behaviour, event)) {
}

object Switcher {
  def apply[T](initial: Behaviour[T], event: Event[Behaviour[T]]): Switcher[T] = {
    new Switcher(initial, event)
  }

  private def construct[T](initial: Behaviour[T], event: Event[Behaviour[T]]): Time => T = {
    frp {
      {
        time =>
          val occ = event.top()

          if (occ == None) {
            initial.at(time)
          }

          else {
            occ.get.value.at(time)
          }
      }
    }
  }
}

/**Stepper is a a static valued version of Switcher that represents the latest
 * occurrence in an Event[T].
 */
class Stepper[T](initial: T, event: Event[T]) extends Switcher[T](initial, event.map((t, v) => new Constant(v))) {}

object Stepper {
  def apply[T](initial: T, event: Event[T]): Stepper[T] = {
    new Stepper(initial, event)
  }
}

/**Constant is a Behaviour that's value never changes. It is optimised so
 * so it only returns the value rather than evaluating it needlessly with respect
 * to time.
 */
protected[echo] class Constant[T](private val value: T) extends Behaviour[T](time => value) {
  override def eval(): T = {
    value
  }

  override private[core] def at(time: Time): T = {
    value
  }
}