/**
 *
 * Copyright (c) 2014 Chris Norman
 *
 * @author Chris Norman (cmn397@gmail.com)
 *
 */

package cmn397.bioscala.core

import scala.annotation.tailrec
import scala.util.{ Try, Success, Failure }
import cmn397.bioscala.gentypes._
import cmn397.bioscala.filehandlers.FASTAFileSource

/**
 * Base trait for representing a source of sequence data (i.e., in-memory strings or FASTA files)
 * which can be processed by enumerator/iteratee pairs.
 *
 */
trait SequenceSource {

  def enumerate[R]: Iteratee[Char, R] => Iteratee[Char, R]

  // if nChars == None, then enumerate all characters
  def getSequenceString(nChars: Option[Long] = None): String = {
    val sb: StringBuffer = new StringBuffer;
    def getChars(n: Long): Iteratee[Char, String] = {
      def step(sbuf: StringBuffer, count: Long): Input[Char] => Iteratee[Char, String] = {
        case Element(e) =>
          nChars match {
            case None => Continue(step(sbuf.append(e), count + 1))
            case Some(n) => if (count < n)
            				  Continue(step(sbuf.append(e), count + 1))
            			    else
            			      Done(sbuf.toString, Pending)
          }
        case Pending => Done(sbuf.toString, Pending) // ?????????????
        case EndOfInput => Done(sbuf.toString, EndOfInput)
      }
      Continue(step(sb, 1))
    }
    enumerate(getChars(nChars.getOrElse(0))).result match {
      case Success(s) => s
      case Failure(t) => "getChars failed: " + t.getMessage
    }
  }

  override def toString: String = getSequenceString(Some(20))

  @tailrec
  protected final def loop[R](itr: Iterator[Char], ite: Iteratee[Char, R]): Iteratee[Char, R] = {
    ite match {
      case d @ Done(_, _) => d
      case e @ Error(t) => e
      case c @ Continue(f) =>
        if (itr.hasNext) loop(itr, f(Element(itr.next)))
        else loop(itr, f(EndOfInput))
    }
  }
}

object SequenceSource {

  /*
   * Return an iteratee which feeds it's input into a packed vector suitable for acting as
   * a backing store for a SequenceSourceVector
   */ 
  def packedVectorGenerator: Iteratee[Char, Vector[Char]] = {
    def step(v: Vector[Char]): Input[Char] => Iteratee[Char, Vector[Char]] = {
      case Element(e) => Continue(step(v :+ e))
      case Pending => Done(v, Pending) // ?????????????
      case EndOfInput => Done(v, EndOfInput)
    }
    Continue(step(Vector[Char]()))
  }
}

class SequenceSourceString(val seqStr: String) extends SequenceSource {
  override def enumerate[R]: Iteratee[Char, R] => Iteratee[Char, R] = loop(seqStr.iterator, _)
}

class SequenceSourceFASTA(fileName: String) extends SequenceSource {

  override def enumerate[R]: Iteratee[Char, R] => Iteratee[Char, R] = {
    val ffSource = new FASTAFileSource(fileName)
    ffSource.enumerate(_)
  }
}

/*
 * This source is represented by another source that is (lazily) transformed via a 1:1 transformation
 * function (ie, this might represent an RNA sequence which is transformed from a DNA sequence via
 * a transcription function). The original (DNA sequence) source is maintained as the source, and
 * the enumerator just "lifts" any supplied iteratee so that the step function's input is transformed
 * on demand.
 *  
 */ 
class SequenceSourceMappedLinear(val src: SequenceSource, transform: Char => Char) extends SequenceSource {
  override def enumerate[R]: Iteratee[Char, R] => Iteratee[Char, R] = {
    ite => src.enumerate(Iteratee.liftInput[Char, R](ite, transform))
  }
}

// TODO: actually pack the bits...
class SequenceSourceVector(val cache: Vector[Char]) extends SequenceSource {
  override def enumerate[R]: Iteratee[Char, R] => Iteratee[Char, R] = loop(cache.iterator, _)
}

class SequenceSourceReverseVector(val cache: Vector[Char]) extends SequenceSource {
  override def enumerate[R]: Iteratee[Char, R] => Iteratee[Char, R] = loop(cache.reverseIterator, _)
}