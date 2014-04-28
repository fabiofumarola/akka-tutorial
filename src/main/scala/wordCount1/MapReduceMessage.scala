package wordCount1

sealed trait MapReduceMessage

case class WordCount(word: String, count: Int) extends MapReduceMessage
case class MapData(dataList: IndexedSeq[WordCount]) extends MapReduceMessage
case class ReduceData(reduceDataMap: Map[String, Int]) extends MapReduceMessage
case class Result() extends MapReduceMessage