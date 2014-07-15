/**
 *
 * Copyright (c) 2014 Chris Norman
 * 
 * @author Chris Norman (cmn397@gmail.com)
 * 
 */

import scala.io.Source

//import bioscala.core.{DNASequence, RNASequence, ProteinSequence, ProteinAlphabet, SequenceAnalysis}
import cmn397.bioscala.core._

import cmn397.bioscala.gentypes._

//import bioscala.filehandlers.FASTAFileReader
//import bioscala.populations.Population
//import bioscala.graph._

/**
 * Test driver application for exercising  BioScala functions.
 */

object BioScalaTestDriver {

  // location of test data files
  val getTestFileDir = "src\\TestData\\"
    
  def doBioScala(args: Int): Unit = {
	args match {
      case 1 =>
       			val seq = DNASequence("id", "acgt")
       			println(seq.countBases)

      case 2 => // TestID: rna
        
       			val seq1 = DNASequence("TestID: rna", "GATGGAACTTGACTACGTAAATT").transcribe
       			println(seq1)

       			val inputFile = getTestFileDir + "trna.txt" // called tRNA, but it contains a DNA string
      			val seq2 = DNASequence("Test transcribe", Source.fromFile(inputFile).getLines.mkString)
      			val tseq = seq2.transcribe
     			println(tseq)
      
     case 3 => // TestID: revc
        		val inputFile = getTestFileDir + "Testrevc.txt"
      			val seq = DNASequence("test", Source.fromFile(inputFile).getLines.mkString)
     			println(seq.reverseComplement.toString)
/*   
      case 4 => // TestID: gc
		        val ff = new FASTAFileReader(getTestFileDir + "tgc.fasta")
		        val mList = ff.getSequenceList.map(_.getGCContent)
		        mList.map(println)
		        println (mList.max)

      case 5 => // TestID: hamm
		        val List(s1, s2) = Source.fromFile(getTestFileDir + "thamm.txt").getLines.toList
		        val seq1 = DNASequence("hamming 1", s1)
		        val seq2 = DNASequence("hamming 2", s2)
      			println(seq1.getHammingDistance(seq2))

      case 6 => // TestID: perm
      			val l = (1 to 5).toList.permutations
      			println(l)

      case 7 => // TestID: prot
        		val inputFile = getTestFileDir + "tprot.txt"
      			val seq = RNASequence("test", Source.fromFile(inputFile).getLines.mkString)
      			println(seq.translate)

      case 8 => //TestID: subs
        		val List(s, p) = Source.fromFile(getTestFileDir + "tsubs.txt").getLines.toList
        		val dnaSeq = DNASequence("id", s)
      			println(dnaSeq.findLiteralMotif(p))

      case 9 => //TestID: cons
		         val ff = new FASTAFileReader(getTestFileDir + "tcons.fasta")
		         val fList = ff.getSequenceList
		         val (profile, consensus) = SequenceAnalysis.getConsensusProfileAndString(fList)
		         SequenceAnalysis.printProfile(profile)
		         println(consensus)

      case 10 => // TestID: grph
		         val ff = new FASTAFileReader(getTestFileDir + "tgrph.fasta")
		         val dbg = DeBruijn.graphFromSequenceList(3, ff.getSequenceList)
		         dbg.findOverlapPairs.foreach((t) => println(t._1 + " " + t._2))

      case 11 => // testID: lcsm
        		 val ff = new FASTAFileReader(getTestFileDir + "tlcsm.fasta")
		         val mList = ff.getSequenceList
		         val g = mList.foldLeft(new SuffixTree)((g: SuffixTree, d: DNASequence) => g.updated(d.getS.mkString + "$", d.id))
		         val lst = mList.foldLeft(List[String]())((s: List[String], d: DNASequence) => d.id.mkString :: s)
		         val result = g.LongestCommonSubstring
		         result.foreach(println(_))

      case 12 => // TestID: mprt
        		FindNGlycosylationMotifLocations
      
      case 13 => // TestID: mrna
        		val inputFile = getTestFileDir + "tmrna.txt"
        	    val protSeq = ProteinSequence("mrna", Source.fromFile(inputFile).getLines.mkString)
        		val n = protSeq.numSourceRNAStrings(1000000)
        		println(n)
        		 
      case 14 => // TestID: prtm
        		val inputFile = getTestFileDir + "tprtm.txt"
        	    val protSeq = ProteinSequence("mrna", Source.fromFile(inputFile).getLines.mkString)
        		val n = protSeq.totalMass
        		println(n)
        		 
      case 15 => // TestID: orf
		        val ff = new FASTAFileReader(getTestFileDir + "torf.fasta")
		        val dnaSeqList = ff.getSequenceList
		        dnaSeqList match {
		          case seq :: Nil => val proteins = dnaSeqList.head.candidateProteins.map(_.getS.mkString).toSet.toList.sortWith(_ < _)
		        		  			 proteins.foreach(println(_))
		          case _ => println("Expected a single sequence")
		        }
		
*/
      case other => println(other + ": unrecognized problem number")
    }
   }

  def main(args: Array[String]): Unit = {
    doBioScala(2)
  }

/*
  // TestID: mprt find n glycosylation motif locations in a series of proties speciied by UniPtot ID
  def FindNGlycosylationMotifLocations = {
    import scala.io.BufferedSource
	import java.io.{InputStreamReader, BufferedReader, ByteArrayInputStream}
	val fis = new java.io.FileInputStream(getTestFileDir + "tmprt.txt")  // contains a list of UniProt protein IDs
	val bs = new BufferedSource(fis)
	val proteinIDList = bs.getLines().toList
	def getUniProtFastaFile(id: String) : FASTAFileReader = {
	  val baseURL = "http://www.uniprot.org/uniprot/" + id + ".fasta"
	  val result = scala.io.Source.fromURL(baseURL).mkString
	  println("Protein: " + id + "Length: " + result.length)
	  new FASTAFileReader(new ByteArrayInputStream(result.getBytes))
	}
	val fastaList = proteinIDList.map(getUniProtFastaFile(_))
	val proteinAlphabet = "ACDEFGHIKLMNPQRSTVWY".toVector
	def getNGlycosylationMotifLocations(ff: FASTAFileReader) : List[(Long, Long)] = {
	  require (ff.getSequenceList.length == 1)
	  val DNASeq = ff.getSequenceList.head
	  val st = new SuffixTree().updated(DNASeq.getS.mkString + "#", DNASeq.id)
	  st.substringsFromPattern(ProteinAlphabet, "N{P}[ST]{P}")
	}
	val resultList = fastaList.map(getNGlycosylationMotifLocations)
	fis.close
	def printList(e: (String, List[(Long, Long)])) = {
	  println(e._1);
	  e._2.sorted.foreach(g => print((g._1 + 1) + " "))
	  println
	}
	proteinIDList.zip(resultList).foreach(printList)
  }
  */
}