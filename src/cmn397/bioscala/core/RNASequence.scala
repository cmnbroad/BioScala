/**
 *
 * Copyright (c) 2014 Chris Norman
 *
 * @author Chris Norman (cmn397@gmail.com)
 *
 */

package cmn397.bioscala.core

import scala.util.{ Try, Success, Failure }

import cmn397.bioscala.gentypes._

/**
 * A sequence of RNA nucleotides.
 * 
 */

object RNASequence {
  def apply(id: String, seq: String) = {
    new RNASequence(id, new SequenceSourceString(seq))
  }

  def apply(fName: String) = {
    // TODO: PARSING - FASTA file sequence is named after the file rather than the tag
    new RNASequence(fName, new SequenceSourceFASTA(fName))
  }
}

class RNASequence(id: String, src: SequenceSource) extends NucleotideSequence(id, src) {
  val alpha = RNAAlphabet

  override final def countBases: Try[(Long, Long, Long, Long)] = {
    def countNucleotides: Iteratee[Char, (Long, Long, Long, Long)] = {
      def step(r: (Long, Long, Long, Long)): Input[Char] => Iteratee[Char, (Long, Long, Long, Long)] = {
        case Element(e) =>
          e.toLower match {
            case 'a' => Continue(step((r._1 + 1, r._2, r._3, r._4)))
            case 'c' => Continue(step((r._1, r._2 + 1, r._3, r._4)))
            case 'g' => Continue(step((r._1, r._2, r._3 + 1, r._4)))
            case 'u' => Continue(step((r._1, r._2, r._3, r._4 + 1)))
            case _ => Error(new IllegalArgumentException)
          }
        case Pending => Done(r, Pending) // ?????????????
        case EndOfInput => Done(r, EndOfInput)
      }
      Continue(step(0, 0, 0, 0))
    }
    src.enumerate(countNucleotides).result
  }

  /**
   * Returns true if we can find a valid start codon for this alignment.
   */
  /*
  private def hasValidStartCodon: Boolean = {
    val startORF = getS.grouped(3).toVector.dropWhile(c => (c.length < 3) || (!RNACodonTable.isStartCodon(c.mkString)))
    if (startORF.isEmpty || startORF.head.length < 3) false
    else {
      require(RNACodonTable.isStartCodon(startORF.head.mkString))
      true
    }
  }
*/
  /**
   * Returns true if we can align on a valid start codon followed by a valid stop codon.
   */
  /*
  private def hasValidStopCodon : Boolean = {
    val startORF = getS.grouped(3).toVector.dropWhile(c => (c.length < 3) || (!RNACodonTable.isStartCodon(c.mkString)))
    if (startORF.isEmpty || startORF.head.length < 3) false // can't even align on start codon
    else {
      // should be aligned on a start codon
      val stopORF = startORF.dropWhile(c => (c.length == 3) && (!RNACodonTable.isStopCodon(c.mkString)))
      if (stopORF.isEmpty || stopORF.head.length < 3) false
      else {
        require(RNACodonTable.isStopCodon(stopORF.head.mkString))
        true
      }
    }
  }
*/
  /**
   * True iff the sequence contains a start codon followed by a stop codon.
   */
  /*
  def isValidORF: Boolean = hasValidStopCodon // test is sufficient since this first aligns on a start codon
*/
  /**
   * Returns index of the locations of all start codons
   */
  /*
  private def getAllStartCodonIndices: List[Long] = {
    def loop(acc: List[Long], seqstr: Vector[Char], curIndex: Long): List[Long] = {
      if (seqstr.isEmpty) acc
      else {
        val nextCodon = seqstr.take(3).mkString
        if (nextCodon.length == 3 && RNACodonTable.isStartCodon(nextCodon))
          loop(curIndex :: acc, seqstr.tail, curIndex + 1)
        else
          loop(acc, seqstr.tail, curIndex + 1)     
      }  
    }
    loop(Nil, getS.toVector, 0).reverse // reverse to keep the lower indices ordered from small to large
  }
*/
  // FIX: get rid of toInts
  /**
   * Return a list of all possible proteins into which this sequence could be translated.
   */
  /*
  def getValidProteins: List[ProteinSequence] = {
    val allSequences = getAllStartCodonIndices.map((i: Long) => RNASequence(id + ", drop:" + i, getS.drop(i.toInt).mkString))
    allSequences.filter((seq: RNASequence) => seq.hasValidStopCodon).map(seq => seq.translate)
   }
*/
  // TODO: This doesn't properly test if there was a stop codon??
  /*
   * Locate the first start codon and translate until an end codon is hit. Assumes the sequence is aligned at
   * and the beginning of the ORF (it need not begin at the first codon, but there should be a start codon).
   */
  /*
  def translate: ProteinSequence = {
    val startORF = getS.grouped(3).toVector.dropWhile(c => (c.length < 3) || (!RNACodonTable.isStartCodon(c.mkString)))
    if (startORF.isEmpty || startORF.head.length < 3)
      throw new Exception("Can't align on start codon")
    else { // should be aligned on a start codon
      val psVec = startORF.takeWhile(c => (c.length == 3) && !RNACodonTable.isStopCodon(c.mkString))
      if (psVec.isEmpty || psVec.head.length < 3)
        throw new Exception("Sequence contains a valid start codon but no valid stop codon")
      else
        ProteinSequence(id + ", as protein", psVec.map(c => RNACodonTable.getAminoAcidShortName(c.mkString)).mkString)
    }
  }
  */
}