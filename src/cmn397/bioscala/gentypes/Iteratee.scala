/**
 *
 * Copyright (c) 2014 Chris Norman
 *
 * @author Chris Norman (cmn397@gmail.com)
 *
 */

package cmn397.bioscala.gentypes

import scala.util.{ Try, Success, Failure }

// TODO: separate Pending from Empty ??

/**
 * Input element for a computation represented by an Iteratee.
 */
trait Input[+E] {
  def map[U](f: E => U): Input[U] = this match {
    case Element(e) => Element(f(e))
    case Pending => Pending
    // TODO: Pending and EndOfInput should be objects defined in Object Input so there is only one instance
    case EndOfInput => EndOfInput
  }
}

case class Element[E](e: E) extends Input[E]
case object Pending extends Input[Nothing]
case object EndOfInput extends Input[Nothing]

/**
 * Consumer of a stream of Input elements of type E, eventually producing a result of type R.
 */
trait Iteratee[E, R] {

  /*
   * Retrieve the result of this Iteratee/computation. For continuations, this method
   * will push an EOF to force evaluation of the result.
   */
  def result: Try[R] = this match { // this is usually called "run"...
    case Done(res, rem) => Success(res)
    case Error(t) => Failure(t)
    case Continue(k) =>
      k(EndOfInput) match {
        case Done(res, _) => Success(res)
        case Continue(_) => Failure(new Exception("Diverging iteratee"))
        case e @ Error(t) => Failure(t)
      }
  }
  
  /*
   * Returns a modified Iteratee that maps the values from the enumerator BEFORE they are
   * passed to the input handler/continuation.
   */
  // TODO: is this rendered obsolete by Enumeratee ??
  def mapInput(inputTransform: E => E): Iteratee[E, R] = this match {
    case Continue(f) =>
      Continue { // map the input using the transform function
        case Element(e) => f(Element(e).map(inputTransform)).mapInput(inputTransform)
        case a @ _ => f(a)
      }
    case other @_ => other
  }

  /*
   * Debugging: display the state of this Iteratee on stdout.
   * 
   */ 
 def debugShowState() = this match {
    case Done(_, Pending) => println("Iteratee: Done, pending")
    case Done(_, EndOfInput) => println("Iteratee: Done, end of input")
    case Done(_, Element(e)) => println("Iteratee: Done, with remaining input")
    case Error(_) => println("Iteratee: Error")
    case Continue(_) => println("Iteratee: Continue")
    case _ => println("Iteratee: some other outcome")
  }

  /**
   * Transform the Iteratee's computed result when the Iteratee is Done.
   */
  def map[U](f: R => U): Iteratee[E, U]

  /**
   * The result of the completed computation of this Iteratee (of type R), is passed to the
   * function f and the resulting Iteratee can continue consuming input.
   */
  def flatMap[U](f: R => Iteratee[E, U]): Iteratee[E, U]

  def filter(p: (E) => Boolean): Iteratee[E, R]

  // TODO: def foreach[]
}

/**
 * Iteratee state representing an computation in progress and ready to accept more
 * input.
 */
case class Continue[E, R](k: Input[E] => Iteratee[E, R]) extends Iteratee[E, R] {
  def map[U](f: R => U): Iteratee[E, U] = Continue(e => k(e) map f)
  def flatMap[U](f: R => Iteratee[E, U]): Iteratee[E, U] = Continue(e => k(e) flatMap f)
  def filter(p: (E) => Boolean): Iteratee[E, R]	=
    Continue(e => {
      e match {
        case Element(el) =>
          if (p(el) == true) k(e)
          else k(EndOfInput)
        case o @ _ => k(o)
      }
    })
}

/**
 * Iteratee state representing a completed computation not requiring further input.
 */
case class Done[E, R](res: R, remainingInput: Input[E]) extends Iteratee[E, R] {
  //def fold[R](folder: Step[E, R] => folder(Done... James Rope slide 14:06
  def map[U](f: R => U): Iteratee[E, U] = Done(f(res), remainingInput)
  def flatMap[U](f: R => Iteratee[E, U]): Iteratee[E, U] = {
    f(res) match {
      case Continue(k) => k(remainingInput)
      case Done(res2, r3) => Done(res2, remainingInput)
      case other @ _ => other
    }
  }
  def filter(p: (E) => Boolean): Iteratee[E, R]	= { println("filter: done"); this }
}

/**
 * Iteratee state representing an error condition produced by an ongoing computation.
 */
case class Error[E, R](t: Throwable) extends Iteratee[E, R] {
  def map[U](f: R => U): Iteratee[E, U] = Error(t)
  def flatMap[U](f2: R => Iteratee[E, U]): Iteratee[E, U] = Error(t)
  def filter(p: (E) => Boolean): Iteratee[E, R]	= this
}

/**
 * Companion object for generation of simple Iteratee with a supplied continuation function.
 */
object Iteratee {

  /*
   * Create a basic Iteratee with state propagation and simple Done/Error processing,
   * parameterized with a continuation function "f" that will handle the (R, E) => R
   * (input to new state) transformation.
   * 
   * NOTE: the iteratee terminates the enumerator when the continuation receives "Pending" input.
   * TODO: Not sure if terminating on Pending is the right thing....
   */
  def fold[E, R](state: R)(f: (R, E) => R): Iteratee[E, R] = {
    def step(r: R): Input[E] => Iteratee[E, R] = in =>
      in match {
        case Element(e) => Continue(step(f(r, e)))
        case Pending => Done(r, Pending)  // terminate the enumerator on pending input ??
        case EndOfInput => Done(r, EndOfInput)
      }
    Continue(step(state))
  }

  /*
  def apply[E, R](e: Element[E] => Iteratee[E, R],
		  			empty: => Iteratee[E, R],
		  			eof: => Iteratee[E, R]): Iteratee[E, R] = {
    def step(n: Int): Input[A] => Iteratee[A, Int] = {
	    case Element(x)		=> Continue(step(n + 1))
	    case Pending		=> Continue(step(n))
	    case EndOfInput 	=> Done(n, EndOfInput)
	}
	Continue(step(0))
  }
*/
}
