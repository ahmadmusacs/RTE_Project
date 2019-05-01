

import java.io.File

import org.slf4j.{Logger, LoggerFactory}
import NaiveBayes._

import scala.collection.mutable
import scala.io.Source
import scala.math._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import java.util.Calendar
import scala.io._
// import scala.util.parsing.json._
import scala.util.parsing.json._

import java.io._
import java.io.File
import java.nio.charset.Charset
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{LemmaAnnotation, PartOfSpeechAnnotation, SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._

import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
// import org.scalanlp.chalk.lang.eng

class NaiveBayes {

  /**
    * Trains a multinomial Naive Bayes using the spam-train and nonspam-train directories
    * @param corpusDir the overall spamDataset folder
    */
    
    // def trainMultinomial(corpusDir:String): Unit = 

  

}

object NaiveBayes {
  val NOT_SPAM = 0
  val SPAM = 1

  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  var PriorsMaps: scala.collection.mutable.Map[String, Double] = scala.collection.mutable.Map()
  var PriorsValues: scala.collection.mutable.Map[Int, Double] = scala.collection.mutable.Map()
	var TCcountMapInNeutral :  scala.collection.mutable.Map[String , Int] = scala.collection.mutable.Map()
	var TCcountMapInEntailment :  scala.collection.mutable.Map[String , Int] = scala.collection.mutable.Map()
	var TCcountMapInContradiction :  scala.collection.mutable.Map[String , Int] = scala.collection.mutable.Map()
	
	var ProbabilityMapinNeutral : scala.collection.mutable.Map[ String, Double ] = scala.collection.mutable.Map()
	var ProbabilityMapinEntailment : scala.collection.mutable.Map[ String, Double ] = scala.collection.mutable.Map()
	var ProbabilityMapinContradiction : scala.collection.mutable.Map[ String, Double ] = scala.collection.mutable.Map()



	var ProbabilityMapinSpam : scala.collection.mutable.Map[ String, Double ] = scala.collection.mutable.Map()
	var ProbabilityMapinNonSpam : scala.collection.mutable.Map[ String, Double ] = scala.collection.mutable.Map()
	var TCcountMapInSpam : scala.collection.mutable.Map[String , Int] = scala.collection.mutable.Map()
	var TCcountMapInNonSpam : scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map()
	var numberofTokensinSpam : Double = 0.0

	var numberofTokensinNeutral : Double = 0.0
	var numberofTokensinEntailment : Double = 0.0 
	var numberofTokensinContradiction : Double = 0.0

	var numberofTokensinNonspam : Double = 0.0

	var stopwords = Array[String]()

   def main(args:Array[String]) : Unit = {
   		evaluateUnaryLemmatization(true)
   		// println("completed")
   		// trainMultinomial("dataset")

   }

    def process(sentence: String) : List[String] = {
    // val proc = new FastNLPProcessor()
    val props: Properties = new Properties()
    props.put("annotators", "tokenize, ssplit, pos, lemma")

    val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)
    // read the whole text from a file; concatenate all sentences into one String
    // val doc = proc.annotate(Source.fromFile(fileToProcess).getLines().mkString("\n"))
    // just a for loop over the sentences in this text
    
      // mkString converts any collection to a string,
      // where the elements are separated by the given separator string
      val document: Annotation = new Annotation(sentence)

    // run all Annotator - Tokenizer on this text
      pipeline.annotate(document)

      val sentences: List[CoreMap] = document.get(classOf[SentencesAnnotation]).asScala.toList
      var lemmaList : List[String] = List[String]()

      (for {
        sentence: CoreMap <- sentences
        token: CoreLabel <- sentence.get(classOf[TokensAnnotation]).asScala.toList
        word: String = token.get(classOf[TextAnnotation])
        pos: String = token.get(classOf[PartOfSpeechAnnotation])
        lemma: String = token.get(classOf[LemmaAnnotation])
        //println("token :" + t._1 + " |||  lemma : " + t._2
      } yield (word, lemma)) foreach(t => lemmaList = lemmaList :+ t._2 )

      // println("")
      // println("Size of lemma" + lemmaList.size)
      // for (w <- lemmaList)
      // {
      //   println(w)
      // }
      return lemmaList
  }

  def generateBigram(senetence  : String) : String= {
  	var WordList = senetence.split(" ")
			 var itr = 0
			 var BigramList = Array[String]()
			 while(itr < WordList.length -1 )
			 {
			 	BigramList = BigramList :+ (WordList(itr) + " " + WordList(itr+1))
			 	itr += 1
			 }

			 var newBigram = BigramList.mkString(" ")
	return newBigram
  }
  def generateTrigram(senetence  : String) : String= {
  	var WordList = senetence.split(" ")
			 var itr = 0
			 var BigramList = Array[String]()
			 while(itr < WordList.length -2 )
			 {
			 	BigramList = BigramList :+ (WordList(itr) + " " + WordList(itr+1) + " " + WordList(itr+2))
			 	itr += 1
			 }

			 var newBigram = BigramList.mkString(" ")
	return newBigram
  }

  def filterStopwords(sentence : String) : String = {
  	var WordList = sentence.split(" ")
  	var NewWordList  = Array[String]()

  	for ( w <- WordList){
  		if ( stopwords.contains(w))
  		{

  		}
  		else
  		{
  			NewWordList = NewWordList :+ w
  		}
  	}

  	return NewWordList.mkString(" ")
  }
   def trainMultinomial(corpusDir:String): Unit = {
    // var time = System.currentTimeMillis / 1000
    // println(time)
    // all the spam training docs will be in this folder

    
 //    var source = Source.fromFile("dataset/snli_1.0_train.jsonl")
 //    // parse
 //    // val mapper = new ObjectMapper() with ScalaObjectMapper
 //    // mapper.registerModule(DefaultScalaModule)
 //    // val parsedJson = mapper.readValue[Map[String, Object]](json.reader())
 //    // println(parsedJson)
 //    // all the non-spam training docs will be in this folder
 // //    val result = JSON.parseFull(json)
	// // result match {
	// // 	  // Matches if jsonStr is valid JSON and represents a Map of Strings to Any
	// // 	  case Some(map: Map[String, Any]) => println(map)
	// // 	  case None => println("Parsing failed")
	// // 	  case other => println("Unknown data structure: " + other)
	// // }

	// val pw = new PrintWriter(new File("train_full_word.txt"))
	// for(line <- source.getLines()) { 
 //      		//println(line)
	// 	val jsonResult = JSON.parseFull(line)
	// 	val map:Map[String,Any] = jsonResult.get.asInstanceOf[Map[String, Any]]

	// 	val goldLabel = map.get("gold_label").get.asInstanceOf[String]
	// 	val sentence1 = map.get("sentence1").get.asInstanceOf[String]
	// 	val sentence2 = map.get("sentence2").get.asInstanceOf[String]
	// 	// println(goldLabel)
	// 	// println(sentence1)
	// 	var lemmas1 = sentence1.trim().split(" ")
	// 	var lemmas2 = sentence2.trim().split(" ")
		
	// 	// lemmas1.foreach(println)
	// 	// lemmas2.foreach(println)
	// 	for ( word <- lemmas1)
	// 	{
	// 		pw.write(word + " ")
	// 	}
	// 	pw.write("\n")
	// 	for ( word <- lemmas2)
	// 	{
	// 		pw.write(word + " ")

	// 	}
	// 	pw.write("\n")
	// 	pw.write(goldLabel + "\n")
	// }

	// pw.close()

 //    println("getting input")

 //    return()

    // val nonSpamTrainDir = new File(this.getClass.getClassLoader.getResource(corpusDir + "/nonspam-train").getFile)
    

    //
    // compute class PriorsValues
    //
    // TODO: compute priors here!
    
    var source = Source.fromFile("train_full_word.txt")
    // source = Source.fromFile("dataset/snli_1.0_train.jsonl")



    var lineNum = 1
    var countNeutral = 0
    var countContradiction = 0
    var countEntailment = 0

    for ( line <- source.getLines)
    {
  //   	val jsonResult = JSON.parseFull(line)
		// val map:Map[String,Any] = jsonResult.get.asInstanceOf[Map[String, Any]]

		// val goldLabel = map.get("gold_label").get.asInstanceOf[String]
		// val sentence1 = map.get("sentence1").get.asInstanceOf[String]
		// val sentence2 = map.get("sentence2").get.asInstanceOf[String]
		// if( lineNum % 3 == 0)
  //   	{
		// 	// println(line)
		// 	// println(line.getClass)
		// 	if( goldLabel == "neutral")
		// 	{
		// 		countNeutral += 1
		// 	}
		// 	else if( goldLabel == "contradiction")
		// 	{
		// 		countContradiction += 1
		// 	}
		// 	else if( goldLabel == "entailment")
		// 	{
		// 		countEntailment += 1
		// 	}
    		
  //   	}
    	if( lineNum % 3 == 0)
    	{
			// println(line)
			// println(line.getClass)
			if( line == "neutral")
			{
				countNeutral += 1
			}
			else if( line == "contradiction")
			{
				countContradiction += 1
			}
			else if( line == "entailment")
			{
				countEntailment += 1
			}
    		
    	}
    	lineNum += 1
    }

    println(countEntailment, countContradiction, countNeutral)
    var allLabelSum : Double = countNeutral + countEntailment + countContradiction

    PriorsMaps("neutral") = countNeutral / allLabelSum
    PriorsMaps("contradiction") = countContradiction / allLabelSum
    PriorsMaps("entailment") = countEntailment / allLabelSum
	println(PriorsMaps)



	var vocab = new ArrayBuffer[String]()
	var vocabPresent : scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map()
	lineNum = 1
	var premise = ""
	var hypothesis = ""
	source.close()

	var stopwordsSource = Source.fromFile("stopwords.txt")
	

	for (line <- stopwordsSource.getLines)
	{
		stopwords = stopwords :+ (line.trim())
	}

	stopwordsSource.close()

	// val stemmer = new PorterStemmer
    
	source = Source.fromFile("train_full_word.txt")
    // source = Source.fromFile("dataset/snli_1.0_train.jsonl")
    // for ( line <- source.getLines)
    // {
  //   	val jsonResult = JSON.parseFull(line)
		// val map:Map[String,Any] = jsonResult.get.asInstanceOf[Map[String, Any]]

		// val goldLabel = map.get("gold_label").get.asInstanceOf[String]
		// val sentence1 = map.get("sentence1").get.asInstanceOf[String]
		// val sentence2 = map.get("sentence2").get.asInstanceOf[String]
	 // 	var concatenatedSentence = premise.trim() + " " + hypothesis.trim()
		// var words = concatenatedSentence.split(" ")
		// // println(premise)
		// // println(hypothesis)
  //       for(word <- words)
  //       {
  //           // println(word)
  //           if ( goldLabel == "neutral"){
  //           	numberofTokensinNeutral += 1
  //           	TCcountMapInNeutral(word) = TCcountMapInNeutral.getOrElse(word, 0) + 1
            	
  //           }
  //           else if( goldLabel == "entailment")
  //           {
  //           	numberofTokensinEntailment += 1
  //           	TCcountMapInEntailment(word) = TCcountMapInEntailment.getOrElse(word, 0) + 1
            	
  //           }
  //           else if( goldLabel == "contradiction")
  //           {
  //           	numberofTokensinContradiction += 1
  //           	TCcountMapInContradiction(word) = TCcountMapInContradiction.getOrElse(word, 0) + 1
            	
  //           }

  //           var isPresent = vocabPresent.getOrElse(word, 0)

  //           if ( isPresent == 0)
  //           {
  //               vocab += word
  //               vocabPresent(word) = vocabPresent.getOrElse(word,0) + 1
  //           }
            
  //       }
  //   }

    
	for ( line <- source.getLines)
	{
		if( lineNum % 3 == 1)
		{
			premise = line.trim()
		}
		else if( lineNum % 3 == 2)
		{
			hypothesis = line.trim()
		}
		else
		{

			/*
			for ( w1 <- premise)
			{
				for ( w2 <- hypothesis)
				{
					var word = w1 + " " + w2
					if ( line == "neutral"){
	                	numberofTokensinNeutral += 1
	                	TCcountMapInNeutral(word) = TCcountMapInNeutral.getOrElse(word, 0) + 1
		                if ( vocab.exists(w => w == word)  == false)
		                {
		                    vocab += word
		                }
		            }
		            else if( line == "entailment")
		            {
		            	numberofTokensinEntailment += 1
		            	TCcountMapInEntailment(word) = TCcountMapInEntailment.getOrElse(word, 0) + 1
		            	if ( vocab.exists(w => w == word) == false)
		            	{
		            		vocab += word
		            	}
		            }
		            else if( line == "contradiction")
		            {
		            	numberofTokensinContradiction += 1
		            	TCcountMapInContradiction(word) = TCcountMapInContradiction.getOrElse(word, 0) + 1
		            	if ( vocab.exists(w => w == word) == false)
		            	{
		            		vocab += word
		            	}
		            }
				}
			}
			*/

		
			var concatenatedSentence = (premise) +  " " + generateBigram(hypothesis)
			// println(concatenatedSentence)
			 // var bigramPremise = ""
			 // var premiseWords = premise.split(" ")
			 // var premisebiwordList = Array[String]()

			 // for (w <- premiseWords.length-1)
			 // {
			 // 	premisebiwordList = premisebiwordList :+ (w+)
			 // }
			 /// BIGRAM 

			 


			 /// BIGRAM ENDS
			var words = concatenatedSentence.split(" ")
			// println(premise)
			// println(hypothesis)
            for(word <- words)
            {
                // println(word)
                if ( line == "neutral"){
                	numberofTokensinNeutral += 1
                	TCcountMapInNeutral(word) = TCcountMapInNeutral.getOrElse(word, 0) + 1
                	
                }
                else if( line == "entailment")
                {
                	numberofTokensinEntailment += 1
                	TCcountMapInEntailment(word) = TCcountMapInEntailment.getOrElse(word, 0) + 1
                	
                }
                else if( line == "contradiction")
                {
                	numberofTokensinContradiction += 1
                	TCcountMapInContradiction(word) = TCcountMapInContradiction.getOrElse(word, 0) + 1
                	
                }

                var isPresent = vocabPresent.getOrElse(word, 0)

                if ( isPresent == 0)
                {
                    vocab += word
                    vocabPresent(word) = vocabPresent.getOrElse(word,0) + 1
                }
                
            }
            
		}
		lineNum += 1
	}

	


	println(vocab.length)
	ProbMapCalculation(vocab)
	// return()
	evaluate()

	// vocab.foreach(println)


  }

  def ProbMapCalculation(vocab: ArrayBuffer[String] ) {
    for (word <- vocab)
    {
        var TCinNeutral = TCcountMapInNeutral.getOrElse(word, 0)
        var TCfloatingPoint : Double = TCinNeutral.asInstanceOf[Number].longValue
        var vocabSize = vocab.size
        // var TCinBoth = TCcountMapInSpam.getOrElse(word, 0) + TCcountMapInNonSpam.getOrElse(word, 0)
        ProbabilityMapinNeutral(word) = (TCfloatingPoint + 1 )/ ( TCcountMapInNeutral.keySet.size + numberofTokensinNeutral)

    }

    
    // non Spam calculation
    
    for (word <- vocab)
    {
        var TCinEntailment = TCcountMapInEntailment.getOrElse(word, 0)
        var TCfloatingPoint : Double = TCinEntailment.asInstanceOf[Number].longValue
        var vocabSize = vocab.size
        // var TCinBoth = TCcountMapInSpam.getOrElse(word, 0) + TCcountMapInNonSpam.getOrElse(word, 0)
        ProbabilityMapinEntailment(word) = (TCfloatingPoint + 1 )/ ( TCcountMapInEntailment.keySet.size + numberofTokensinEntailment)

    }


    for (word <- vocab)
    {
        var TCinContradiction = TCcountMapInContradiction.getOrElse(word, 0)
        var TCfloatingPoint : Double = TCinContradiction.asInstanceOf[Number].longValue
        var vocabSize = vocab.size
        // var TCinBoth = TCcountMapInSpam.getOrElse(word, 0) + TCcountMapInNonSpam.getOrElse(word, 0)
        ProbabilityMapinContradiction(word) = (TCfloatingPoint + 1 )/ ( TCcountMapInContradiction.keySet.size + numberofTokensinContradiction)

    }

    new PrintWriter("probMapWordBigram.txt") {
	vocab.foreach {
	    case (k) =>
	      write(k + "\t" + ProbabilityMapinNeutral.getOrElse(k, 0) + "\t" + ProbabilityMapinEntailment.getOrElse(k, 0) + "\t" +
	      ProbabilityMapinContradiction.getOrElse(k, 0) )
	      write("\n")
	  }
	  close()
	}

	println(TCcountMapInNeutral.keySet.size, TCcountMapInEntailment.keySet.size , TCcountMapInContradiction.keySet.size)
	println(numberofTokensinNeutral, numberofTokensinEntailment, numberofTokensinContradiction)
	println(vocab.size)
  }


  def SetPriors(countSpamFiles : Double, countNonspamFiles : Double) {
    PriorsValues(SPAM) = countSpamFiles / (countSpamFiles + countNonspamFiles)
    PriorsValues(NOT_SPAM) = countNonspamFiles / ( countNonspamFiles + countSpamFiles)
    println(countSpamFiles, countNonspamFiles)
    println(PriorsValues(SPAM) , PriorsValues(NOT_SPAM))
  }

  /**
    * Evaluates a multinomial Naive Bayes using the spam-test and nonspam-test directories
    * @param corpusDir the overall spamDataset folder
    * @return A Score object containing all relevant counts and scores
    */

  def evaluateUnaryLemmatization(isBigram : Boolean) = {


  	var source = Source.fromFile("probMapLemmatizationUnary.txt")
  	if( isBigram == true)
  	{
  		source = Source.fromFile("probMapWordBigram.txt")
  	}
  	for (line <- source.getLines)
  	{
  		var strings = line.split("\t")
  		var word = strings(0)
  		var PMapN = strings(1).toDouble
  		var PMapE = strings(2).toDouble
  		var PMapC = strings(3).toDouble
  		ProbabilityMapinNeutral(word) = PMapN
  		ProbabilityMapinEntailment(word) = PMapE
  		ProbabilityMapinContradiction(word) = PMapC
  	}

  	var truePositives = 0
    var falsePositives = 0
    var trueNegatives = 0
    var falseNegatives = 0


  	source = Source.fromFile("dataset/snli_1.0_test.jsonl")

  	var correct : Double = 0
  	var incorrect : Double = 0

  	var neutralCorrect : Double  = 0
  	var neutralIncorrect : Double = 0

  	var entailmentCorrect : Double = 0
  	var entailmentIncorrect : Double = 0

  	var contradictionCorrect : Double = 0
  	var contradictionIncorrect : Double = 0

  	var tpNeutral : Double = 0
  	var fpNeutral : Double = 0
  	var tnNeutral : Double = 0
  	var fnNeutral : Double = 0

  	var tpEntailment : Double = 0
  	var fpEntailment : Double = 0
  	var tnEntailment : Double = 0
  	var fnEntailment : Double = 0

  	var tpContradiction : Double = 0
  	var fpContradiction : Double = 0
  	var tnContradiction : Double = 0
  	var fnContradiction : Double = 0
 //  	val conf = new SparkConf().setAppName("Ahmad").setMaster("local")
	// var sc = new SparkContext(conf)

 //  	var predictionAndLabels = Array[(Double, Double)]()

  	for(line <- source.getLines()) { 
      		//println(line)
		val jsonResult = JSON.parseFull(line)
		val map:Map[String,Any] = jsonResult.get.asInstanceOf[Map[String, Any]]

		val goldLabel = map.get("gold_label").get.asInstanceOf[String]
		val sentence1 = map.get("sentence1").get.asInstanceOf[String]
		val sentence2 = map.get("sentence2").get.asInstanceOf[String]
		// println(goldLabel)
		// println(sentence1)

		

		// var lemmas1 = process(sentence1.trim())
		// var lemmas2 = process(sentence2.trim())


		/*
		var neutralScore : Double = Math.log(PriorsMaps("neutral"))
        var entailmentScore : Double = Math.log(PriorsMaps("entailment"))
        var contradictionScore : Double = Math.log(PriorsMaps("contradiction"))
		for ( w1 <- lemmas1)
		{
			for (w2 <- lemmas2)
			{
				var word = w1 + " " + w2;
				neutralScore += Math.log(ProbabilityMapinNeutral.getOrElse(word, 1/(TCcountMapInNeutral.keySet.size + numberofTokensinNeutral)))
		    	entailmentScore += Math.log(ProbabilityMapinEntailment.getOrElse(word, 1/(TCcountMapInEntailment.keySet.size + numberofTokensinEntailment)))
		    	contradictionScore += Math.log(ProbabilityMapinContradiction.getOrElse(word, 1/(TCcountMapInContradiction.keySet.size + numberofTokensinContradiction)))

			}
		}

		*/
		var lemmas = process(sentence1.trim() + " " + sentence2.trim())
		var neutralScore : Double = Math.log(0.3326810674831215)
        var entailmentScore : Double = Math.log(0.33386788795104183)
        var contradictionScore : Double = Math.log(0.3334510445658367)
        var TCcountMapInNeutralSize : Double = 25006
        var TCcountMapInEntailmentSize : Double = 20137
        var TCcountMapInContradictionSize : Double = 23830
        numberofTokensinNeutral = 4227658.0
        numberofTokensinEntailment = 3940101.0
        numberofTokensinContradiction = 4073027.0
		if( isBigram == true){
		
		    lemmas = ((sentence1) + " " + generateBigram(sentence2)).split(" ").toList
			neutralScore  = Math.log(0.3326810674831215)
		    entailmentScore  = Math.log(0.33386788795104183)
		    contradictionScore  = Math.log(0.3334510445658367)
		    TCcountMapInNeutralSize  = 50175
		    TCcountMapInEntailmentSize  = 41034
		    TCcountMapInContradictionSize  = 47481
		    numberofTokensinNeutral = 5002346.0
		    numberofTokensinEntailment = 4423155.0
		    numberofTokensinContradiction = 4686235.0
    	}

		for ( word <- lemmas)
		{
			
            neutralScore += Math.log(ProbabilityMapinNeutral.getOrElse(word, 1/(TCcountMapInNeutralSize + numberofTokensinNeutral)))
		    entailmentScore += Math.log(ProbabilityMapinEntailment.getOrElse(word, 1/(TCcountMapInEntailmentSize + numberofTokensinEntailment)))
		    contradictionScore += Math.log(ProbabilityMapinContradiction.getOrElse(word, 1/(TCcountMapInContradictionSize + numberofTokensinContradiction)))

        }
        
      	var predictedLabel : Double = 0
      	if( neutralScore >= entailmentScore && neutralScore >= contradictionScore)
		{
			predictedLabel = 0
		}
		else if( entailmentScore >= neutralScore && entailmentScore >= contradictionScore )
		{
			predictedLabel = 1
		}
		else if( contradictionScore >= entailmentScore && contradictionScore >= neutralScore )
		{
			predictedLabel = 2
		}

		var actualLabel : Double = 0

		if( goldLabel == "neutral")
		{
			actualLabel = 0
			if( neutralScore >= entailmentScore && neutralScore >= contradictionScore)
			{
				correct += 1
				neutralCorrect += 1
			}
			else 
			{

				incorrect += 1
				neutralIncorrect += 1
			}
		}
		else if( goldLabel == "entailment")
		{
			actualLabel = 1
			if( entailmentScore >= neutralScore && entailmentScore >= contradictionScore)
			{
				correct += 1
				entailmentCorrect += 1
			}
			else 
			{
				incorrect += 1
				entailmentIncorrect += 1
			}
		}
		else if( goldLabel == "contradiction")
		{
			actualLabel = 2
			if( contradictionScore >= entailmentScore && contradictionScore >= neutralScore)
			{
				correct += 1
				contradictionCorrect += 1
			}
			else 
			{
				incorrect += 1
				contradictionIncorrect += 1
			}
		}

		if( actualLabel == 0)
		{
			if(predictedLabel == 0)
			{
				tpNeutral += 1
				tnEntailment += 1
				tnContradiction += 1
			}
			else if( predictedLabel == 1)
			{
				fnNeutral += 1
				fpEntailment += 1

			}
			else if( predictedLabel == 2)
			{
				fnNeutral += 1
				fpContradiction += 1
			}
		}
		else if( actualLabel == 1)
		{
			if(predictedLabel == 0)
			{
				fnEntailment += 1
				fpNeutral += 1
			}
			else if( predictedLabel == 1)
			{
				tpEntailment += 1
				tnNeutral += 1
				tnContradiction += 1

			}
			else if( predictedLabel == 2)
			{
				fnEntailment += 1
				fpContradiction += 1
			}
		}
		else if( actualLabel == 2)
		{
			if(predictedLabel == 0)
			{
				fnContradiction += 1
				fpNeutral += 1
			}
			else if( predictedLabel == 1)
			{
				fnContradiction += 1
				fpEntailment += 1
			}
			else if( predictedLabel == 2)
			{
				tpContradiction += 1
				tnNeutral += 1
				tnEntailment += 1
			}
		}
		// predictionAndLabels = predictionAndLabels :+ (predictedLabel, actualLabel)

		// lemmas1.foreach(println)
		// lemmas2.foreach(println)
		
	}
	
	
	println(correct, incorrect)
	var accuracy = correct/ (correct + incorrect)
	println(accuracy)

	// println(neutralCorrect, neutralIncorrect)
	// accuracy = neutralCorrect/ (neutralIncorrect +  neutralCorrect)
	// println(accuracy)

	// println(entailmentCorrect, entailmentIncorrect)
	// accuracy = entailmentCorrect/ (entailmentCorrect + entailmentIncorrect)
	// println(accuracy)

	// println(contradictionCorrect, contradictionIncorrect)
	// accuracy = contradictionCorrect/ (contradictionIncorrect + contradictionCorrect)
	// println(accuracy)

	// println(tpNeutral, fpNeutral, tnNeutral, fnNeutral)
	// println(tpEntailment, fpEntailment, tnEntailment, fnEntailment)
	// println(tpContradiction, fpContradiction, tnContradiction, fnContradiction)

	println("Neutral Scores")

	var (p1,r1) = F1_Score(tpNeutral, tnNeutral, fpNeutral, fnNeutral)
	println("Entailment Scores")
	var (p2,r2) = F1_Score(tpEntailment, tnEntailment, fpEntailment, fnEntailment)
	println("contradiction Scores")
	var (p3,r3) = F1_Score(tpContradiction, tnContradiction, fpContradiction, fnContradiction)
	println("Macro F1")
	var pOverall : Double = (p1+p2+p3)/3
	var rOverall : Double = (r1+r2+r3)/3
	println(2*(pOverall*rOverall) / (pOverall + rOverall))
  }

  def evaluate(): Unit = {


  	var truePositives = 0
    var falsePositives = 0
    var trueNegatives = 0
    var falseNegatives = 0


  	var source = Source.fromFile("dataset/snli_1.0_test.jsonl")

  	var correct : Double = 0
  	var incorrect : Double = 0

  	var neutralCorrect : Double  = 0
  	var neutralIncorrect : Double = 0

  	var entailmentCorrect : Double = 0
  	var entailmentIncorrect : Double = 0

  	var contradictionCorrect : Double = 0
  	var contradictionIncorrect : Double = 0

  	var tpNeutral : Double = 0
  	var fpNeutral : Double = 0
  	var tnNeutral : Double = 0
  	var fnNeutral : Double = 0

  	var tpEntailment : Double = 0
  	var fpEntailment : Double = 0
  	var tnEntailment : Double = 0
  	var fnEntailment : Double = 0

  	var tpContradiction : Double = 0
  	var fpContradiction : Double = 0
  	var tnContradiction : Double = 0
  	var fnContradiction : Double = 0
 //  	val conf = new SparkConf().setAppName("Ahmad").setMaster("local")
	// var sc = new SparkContext(conf)

 //  	var predictionAndLabels = Array[(Double, Double)]()

  	for(line <- source.getLines()) { 
      		//println(line)
		val jsonResult = JSON.parseFull(line)
		val map:Map[String,Any] = jsonResult.get.asInstanceOf[Map[String, Any]]

		val goldLabel = map.get("gold_label").get.asInstanceOf[String]
		var sentence1 = map.get("sentence1").get.asInstanceOf[String]
		var sentence2 = map.get("sentence2").get.asInstanceOf[String]
		// println(goldLabel)
		// println(sentence1)
			// sentence1 = filterStopwords(sentence1)
			// sentence2 = filterStopwords(sentence2)
		var lemmas = ((sentence1) + " " + generateBigram(sentence2)).split(" ")

		// var lemmas1 = process(sentence1.trim())
		// var lemmas2 = process(sentence2.trim())


		/*
		var neutralScore : Double = Math.log(PriorsMaps("neutral"))
        var entailmentScore : Double = Math.log(PriorsMaps("entailment"))
        var contradictionScore : Double = Math.log(PriorsMaps("contradiction"))
		for ( w1 <- lemmas1)
		{
			for (w2 <- lemmas2)
			{
				var word = w1 + " " + w2;
				neutralScore += Math.log(ProbabilityMapinNeutral.getOrElse(word, 1/(TCcountMapInNeutral.keySet.size + numberofTokensinNeutral)))
		    	entailmentScore += Math.log(ProbabilityMapinEntailment.getOrElse(word, 1/(TCcountMapInEntailment.keySet.size + numberofTokensinEntailment)))
		    	contradictionScore += Math.log(ProbabilityMapinContradiction.getOrElse(word, 1/(TCcountMapInContradiction.keySet.size + numberofTokensinContradiction)))

			}
		}

		*/
		
		var neutralScore : Double = Math.log(PriorsMaps("neutral"))
        var entailmentScore : Double = Math.log(PriorsMaps("entailment"))
        var contradictionScore : Double = Math.log(PriorsMaps("contradiction"))

		for ( word <- lemmas)
		{
			
            neutralScore += Math.log(ProbabilityMapinNeutral.getOrElse(word, 1/(TCcountMapInNeutral.keySet.size + numberofTokensinNeutral)))
		    entailmentScore += Math.log(ProbabilityMapinEntailment.getOrElse(word, 1/(TCcountMapInEntailment.keySet.size + numberofTokensinEntailment)))
		    contradictionScore += Math.log(ProbabilityMapinContradiction.getOrElse(word, 1/(TCcountMapInContradiction.keySet.size + numberofTokensinContradiction)))

        }
        
      	var predictedLabel : Double = 0
      	if( neutralScore >= entailmentScore && neutralScore >= contradictionScore)
		{
			predictedLabel = 0
		}
		else if( entailmentScore >= neutralScore && entailmentScore >= contradictionScore )
		{
			predictedLabel = 1
		}
		else if( contradictionScore >= entailmentScore && contradictionScore >= neutralScore )
		{
			predictedLabel = 2
		}

		var actualLabel : Double = 0

		if( goldLabel == "neutral")
		{
			actualLabel = 0
			if( neutralScore >= entailmentScore && neutralScore >= contradictionScore)
			{
				correct += 1
				neutralCorrect += 1
			}
			else 
			{

				incorrect += 1
				neutralIncorrect += 1
			}
		}
		else if( goldLabel == "entailment")
		{
			actualLabel = 1
			if( entailmentScore >= neutralScore && entailmentScore >= contradictionScore)
			{
				correct += 1
				entailmentCorrect += 1
			}
			else 
			{
				incorrect += 1
				entailmentIncorrect += 1
			}
		}
		else if( goldLabel == "contradiction")
		{
			actualLabel = 2
			if( contradictionScore >= entailmentScore && contradictionScore >= neutralScore)
			{
				correct += 1
				contradictionCorrect += 1
			}
			else 
			{
				incorrect += 1
				contradictionIncorrect += 1
			}
		}

		if( actualLabel == 0)
		{
			if(predictedLabel == 0)
			{
				tpNeutral += 1
				tnEntailment += 1
				tnContradiction += 1
			}
			else if( predictedLabel == 1)
			{
				fnNeutral += 1
				fpEntailment += 1

			}
			else if( predictedLabel == 2)
			{
				fnNeutral += 1
				fpContradiction += 1
			}
		}
		else if( actualLabel == 1)
		{
			if(predictedLabel == 0)
			{
				fnEntailment += 1
				fpNeutral += 1
			}
			else if( predictedLabel == 1)
			{
				tpEntailment += 1
				tnNeutral += 1
				tnContradiction += 1

			}
			else if( predictedLabel == 2)
			{
				fnEntailment += 1
				fpContradiction += 1
			}
		}
		else if( actualLabel == 2)
		{
			if(predictedLabel == 0)
			{
				fnContradiction += 1
				fpNeutral += 1
			}
			else if( predictedLabel == 1)
			{
				fnContradiction += 1
				fpEntailment += 1
			}
			else if( predictedLabel == 2)
			{
				tpContradiction += 1
				tnNeutral += 1
				tnEntailment += 1
			}
		}
		// predictionAndLabels = predictionAndLabels :+ (predictedLabel, actualLabel)

		// lemmas1.foreach(println)
		// lemmas2.foreach(println)
		
	}
	
	// println(predictionAndLabels)
	// var PredAndLbl = sc.parallelize(predictionAndLabels)
	// val metrics = new MulticlassMetrics(PredAndLbl)

	// // Confusion matrix
	// println("Confusion matrix:")
	// println(metrics.confusionMatrix)

	// // Overall Statistics
	// val accuracy = metrics.accuracy
	// println("Summary Statistics")
	// println(s"Accuracy = $accuracy")

	// // Precision by label
	// val labels = metrics.labels
	// labels.foreach { l =>
	//   println(s"Precision($l) = " + metrics.precision(l))
	// }

	// // Recall by label
	// labels.foreach { l =>
	//   println(s"Recall($l) = " + metrics.recall(l))
	// }

	// // False positive rate by label
	// labels.foreach { l =>
	//   println(s"FPR($l) = " + metrics.falsePositiveRate(l))
	// }

	// // F-measure by label
	// labels.foreach { l =>
	//   println(s"F1-Score($l) = " + metrics.fMeasure(l))
	// }

	// // Weighted stats
	// println(s"Weighted precision: ${metrics.weightedPrecision}")
	// println(s"Weighted recall: ${metrics.weightedRecall}")
	// println(s"Weighted F1 score: ${metrics.weightedFMeasure}")
	// println(s"Weighted false positive rate: ${metrics.weightedFalsePositiveRate}")

	println(correct, incorrect)
	var accuracy = correct/ (correct + incorrect)
	println(accuracy)

	println(neutralCorrect, neutralIncorrect)
	accuracy = neutralCorrect/ (neutralIncorrect +  neutralCorrect)
	println(accuracy)

	println(entailmentCorrect, entailmentIncorrect)
	accuracy = entailmentCorrect/ (entailmentCorrect + entailmentIncorrect)
	println(accuracy)

	println(contradictionCorrect, contradictionIncorrect)
	accuracy = contradictionCorrect/ (contradictionIncorrect + contradictionCorrect)
	println(accuracy)

	println(tpNeutral, fpNeutral, tnNeutral, fnNeutral)
	println(tpEntailment, fpEntailment, tnEntailment, fnEntailment)
	println(tpContradiction, fpContradiction, tnContradiction, fnContradiction)

	F1_Score(tpNeutral, tnNeutral, fpNeutral, fnNeutral)
	F1_Score(tpEntailment, tnEntailment, fpEntailment, fnEntailment)
	F1_Score(tpContradiction, tnContradiction, fpContradiction, fnContradiction)
/*

    // all the spam testing docs will be in this folder
    val spamTestDir = new File(this.getClass.getClassLoader.getResource(corpusDir + "/spam-test").getFile)
    // all the non-spam testing docs will be in this folder
    val nonSpamTestDir = new File(this.getClass.getClassLoader.getResource(corpusDir + "/nonspam-test").getFile)

    var truePositives = 0
    var falsePositives = 0
    var trueNegatives = 0
    var falseNegatives = 0

    

    // TODO: evaluate on all spam-test and nonspam-test docs here; update the counts above during the evaluation
    var testFiles = spamTestDir.listFiles
    for (fileX <- testFiles)
    {
        var spamScore : Double = Math.log(PriorsValues(SPAM))
        var nonspamScore : Double = Math.log(PriorsValues(NOT_SPAM))
        
        var source = Source.fromFile(fileX)
        for ( line <- source.getLines())
        {
            var words = line.split(" ")
            for(word <- words)
            {
                spamScore += Math.log(ProbabilityMapinSpam.getOrElse(word, 1/(TCcountMapInSpam.keySet.size + numberofTokensinSpam)))
               
            }
        }

        source = Source.fromFile(fileX)
        for ( line <- source.getLines())
        {
            var words = line.split(" ")
            for(word <- words)
            {
                nonspamScore += Math.log(ProbabilityMapinNonSpam.getOrElse(word, 1/(TCcountMapInNonSpam.keySet.size + numberofTokensinNonspam)))
            }
        }
      
        if(spamScore > nonspamScore)
        {
            truePositives += 1
        }
        else 
        {
            falseNegatives += 1
        }
    }


    testFiles = nonSpamTestDir.listFiles
    for (fileX <- testFiles)
    {
        var spamScore = Math.log(PriorsValues(SPAM))
        var nonspamScore = Math.log(PriorsValues(NOT_SPAM))
        var source = Source.fromFile(fileX)
        for ( line <- source.getLines())
        {
            var words = line.split(" ")
            for(word <- words)
            {
                spamScore += Math.log(ProbabilityMapinSpam.getOrElse(word, 1/(TCcountMapInSpam.keySet.size + numberofTokensinSpam)))
            }
        }

        source = Source.fromFile(fileX)
        for ( line <- source.getLines())
        {
            var words = line.split(" ")
            for(word <- words)
            {
                nonspamScore += Math.log(ProbabilityMapinNonSpam.getOrElse(word, 1/(TCcountMapInNonSpam.keySet.size + numberofTokensinNonspam)))
   
            }
        }

        if(spamScore < nonspamScore)
        {
            trueNegatives += 1
        }
        else 
        {
            falsePositives += 1
        }
    }
*/

    // println("Accuracy metric using F1 : (the values true+ve, false+ve, true-ve, false-ve) "+ truePositives, falsePositives , trueNegatives, falseNegatives)

    // Score(truePositives, falsePositives, trueNegatives, falseNegatives)
  }

  def F1_Score(tp:Double, tn:Double, fp:Double, fn:Double) : (Double, Double) = {
  	var precisionScore = (tp/(tp+fp))
  	var recallScore =  tp/(tp+fn)
  	println("Precision : " + (tp/(tp+fp)))
  	println("Recall :  " + tp/(tp+fn))
  	println("F1 : " + (2*precisionScore * recallScore)/ (precisionScore + recallScore))

  	return (precisionScore, recallScore)
  }
}
