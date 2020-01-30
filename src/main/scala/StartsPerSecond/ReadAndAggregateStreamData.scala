package StartsPerSecond

import scala.concurrent.Await

import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.execution.ExecutionModel.AlwaysAsyncExecution
import monix.reactive.observables.ObservableLike.Transformer
import scala.concurrent.duration._
import scala.util.Try

import java.io.{ BufferedReader, InputStreamReader }
import java.net.URL

import com.fasterxml.jackson.annotation.{ JsonIgnoreProperties, JsonProperty }

// Example: 'data: {"device":"xbox_one","sev":"error","title":"orange is the new black","country":"IND","time":1515445354624}'
@JsonIgnoreProperties(ignoreUnknown = true)
case class StreamData(@JsonProperty(required=true) device: String,
                      @JsonProperty(required=true) sev: String,
                      @JsonProperty(required=true) title: String,
                      @JsonProperty(required=true) country: String,
                      @JsonProperty(required=true) time: Long) {

  def isSuccessful(): Boolean = (sev == "success" || sev == "successinfo")

  def isValid(): Boolean = {
    val strFields = Seq(device, sev, title, country)
    val anyBustedData = (strFields.map(field => field.contains("busted data")))
    !anyBustedData.exists(_ == true)
  }

  def timeInSec(): Long = time / 1000

}

// Example: '{“device”: “xbox_one”, “sps”: 1, “title”: “cuervos”, “country”: “BR”}'
@JsonIgnoreProperties(ignoreUnknown = true)
case class StartsPerSecond(@JsonProperty(required=true) device: String,
                           @JsonProperty(required=true) sps: Long,
                           @JsonProperty(required=true) title: String,
                           @JsonProperty(required=true) country: String,
                           @JsonProperty(required=true) timeInSec: Long) {

  override def toString(): String = JSONUtil.encodeAsString(this)

  def incrementSPS(): StartsPerSecond = copy(sps = sps + 1L)
}

object ReadAndAggregateStreamData {

  implicit val scheduler = Scheduler(executionModel = AlwaysAsyncExecution)
  val PARSE_STREAM_DATA_BATCH_SIZE = 10
  val inputStreamURL = "https://tweet-service.herokuapp.com/sps"

  /*
    Reads data from stream, splitting up the work of reading and parsing among
    number of tasks determined by PARSE_STREAM_DATA_BATCH_SIZE, and emitting
    this data in order as an observable (note that no actual work is done
    till the observable is executed in the main method)
   */
  def readAndParseStreamData(): Observable[Option[StreamData]] = {
    val connection = new URL(inputStreamURL).openConnection
    val reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))

    Observable.fromLinesReader(reader)
      .transform(mapParallelOrdered[String, Option[StreamData]](PARSE_STREAM_DATA_BATCH_SIZE)(parseStreamData(_)))
  }

  /*
    Creates a task that parses the input data into StreamData if possible (nothing gets executed
    since Monix task is lazy)
   */
  def parseStreamData(line: String): Task[Option[StreamData]] = {
    val stripNumChars = new String("data:").length
    val decoded = if(line.isEmpty()) {
      Try(None)
    } else {
      Try(Some(JSONUtil.decode(line.substring(stripNumChars), classOf[StreamData])))
    } recoverWith {
      // Ignore invalid input that cannot be parsed
      case _ => Try(None)
    }
    Task.fromTry(decoded)
  }

  /*
    Group observable of input StreamData by device, title, country and time (in seconds).
    Iterate over each stream data event, and for successful and valid start events per group,
    increment count of SPS. Emit each group aggregated starts per second (note that no actual
    work is done till the observable is executed in the main method).
   */
  def aggregateStartsPerSecond(in : Observable[Option[StreamData]]): Observable[Option[StartsPerSecond]] = {
    in.filter(b => b.nonEmpty)
      .groupBy(sd => (sd.get.device, sd.get.title, sd.get.country, sd.get.timeInSec))
      // Concurrently merge the observables grouped by device, title, country, time
      // into a single observable.
      .mergeMap { case grouped =>
      val (device, title, country, timeInSec) = grouped.key
      val observedEvents : Observable[Option[StartsPerSecond]] =
        grouped.foldLeftF[Option[StartsPerSecond]](
          Some(StartsPerSecond(
            device = device,
            sps = 0L,
            title = title,
            country = country,
            timeInSec = timeInSec))) {
          case (Some(start), Some(streamData)) if (streamData.isSuccessful && streamData.isValid) =>
            val updatedStart = Some(start.incrementSPS)
            System.out.println(updatedStart.get.toString)
            updatedStart
          case _ => None
        }
      observedEvents
    }
  }

  /*
     Creates one Task per computation (Monix Tasks are lazy, so nothing gets executed yet),
     groups the tasks into batches of size determined by size, using bufferTumbling,
     waits for entire batch to complete using Task.gather, which returns a Task[Seq[Q]]
     Then creates Observable using the above Task and flattens Seq[Q], so that Qs get
     emitted downstream one by one (preserving order).
     Note - Referred to code here:
     https://softwaremill.com/reactive-streams-in-scala-comparing-akka-streams-and-monix-part-2/
  */
  def mapParallelOrdered[P, Q](size: Int)(f: P => Task[Q]): Transformer[P, Q] = {
    _.map(f).bufferTumbling(size).flatMap { tasks =>
      Observable
        .fromTask(Task.gather(tasks))
        .concatMap(Observable.fromIterable(_))
    }
  }

  def main(args: Array[String]): Unit = {
    val read: Observable[Option[StreamData]] = ReadAndAggregateStreamData.readAndParseStreamData()
    val aggregated: Observable[Option[StartsPerSecond]] = ReadAndAggregateStreamData.aggregateStartsPerSecond(read)

    // Actual execution of observable happens below
    val job = Observable.fromTask(aggregated.completedL).completedL.runAsync
    Await.result(job, Duration.Inf)
  }
}


