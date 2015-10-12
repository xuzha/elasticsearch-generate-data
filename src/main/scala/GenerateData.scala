
import akka.actor.ActorSystem
import com.beust.jcommander.{JCommander, _}
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}
import spray.util._

import scala.concurrent.Future
import scala.util._

class DataGenerater(esUrl: String,
                    indexName: String,
                    indexType: String,
                    batchSize: Int,
                    numOfShards: Int,
                    count: Int,
                    format: String,
                    numOfReplicas: Int,
                    setRefresh: Boolean,
                    deleteIndex: Boolean,
                    username: Option[String],
                    password: Option[String]) {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  var id_counter = 0

  def deleteIndex(indexName: String) = {
    val url = s"${esUrl}$indexName?refresh=true"
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val response: Future[HttpResponse] = pipeline(Delete(url))

    response.onComplete(res => {
      println(s"${res.get.message}")
    })
    response.await()
  }

  def createIndex(indexName: String) = {
    val schema = """{"settings": { "number_of_shards": """ + numOfShards + """, "number_of_replicas": """ + numOfReplicas + """ }, "refresh": true } """
    val url = s"${esUrl}$indexName"

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    val response: Future[HttpResponse] = pipeline(Put(url, schema))

    response.onComplete(res =>
      println(s"${res.get.message}")
    )

    response.await()
  }

  def uploadBatch(body: String, count: Int): Unit = {
    val url = s"${esUrl}_bulk"
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    val response: Future[HttpResponse] = pipeline(Post(url, body))


    response.onComplete(res => {
      val mes = res.get.message.toString
      val index1 = mes.indexOf("\"took\":")
      val index2 = mes.indexOf(",", index1)
      if (res.get.message.status.isSuccess) {
        val took = mes.substring(index1, index2).split(":")(1)
        println(s"Upload: OK - upload took: ${took}, total docs uploaded: ${count}")
      } else {
        println(s"Upload: failed -")
      }

    })
    response.await()
  }


  def setIndexRefresh(interval: String): Unit = {

    val params = s""" {"index":{"refresh_interval": "${interval}"}}"""
    val url = s"$esUrl$indexName/_settings"

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    val response: Future[HttpResponse] = pipeline(Put(url, params))

    response.onComplete(res =>
      println(s"${res.get.message}")
    )

    response.await()
  }

  def getDataFromFormat(format: String): Option[(String, Any)] = {

    val vals = format.split(":")
    var returnValue: Any = 0

    if (vals.length > 1) {
      vals(1) match {
        case "String" =>
          returnValue = Random.alphanumeric.take(Random.nextInt(30)).mkString

        case "Int" =>
          returnValue = Random.nextInt(1000 * 1000)

        case "ts" =>
          val current = System.currentTimeMillis
          returnValue = current + Random.nextInt(24 * 60 * 60 * 1000).toLong
      }

      return Some((vals(0), returnValue))
    }

    return None
  }

  def generate_random_data(formats: List[String]): Map[String, Any] = {
    var dataMap = Map[String, Any]()

    formats.foreach { format =>
      val data = getDataFromFormat(format)
      data foreach { case (key, value) =>
        dataMap += (key -> value)
      }
    }
    dataMap
  }

  def start() = {

    // clean index
    if (deleteIndex) {
      deleteIndex(indexName)
    }

    createIndex(indexName)

    if (setRefresh) {
      setIndexRefresh("-1")
    }

    val formats = format.split(",")

    val currTs = System.currentTimeMillis()
    val total = 0
    var cmd = ""
    var upload_data_count = -1

    for (i <- 0 to count) {
      val item = generate_random_data(formats.toList)
      cmd += s"""{"index":{"_index":"${indexName}", "_type":"${indexType}"}}"""
      cmd += "\n"
      cmd += scala.util.parsing.json.JSONObject(item)
      cmd += "\n"

      upload_data_count += 1
      if (upload_data_count % batchSize == 0) {
        uploadBatch(cmd, upload_data_count)
        cmd = ""
      }
    }

    if (cmd != "") {
      uploadBatch(cmd, upload_data_count)
    }

    if (setRefresh) {
      setIndexRefresh("1s")
    }
  }
}


object GenerateData extends App {

  val params = new GenerateDataParameters
  val commander = new JCommander(params, args: _*)


  var env: collection.mutable.Map[String, String] = collection.mutable.Map(sys.env.toSeq: _*)
  var appendData = false

  commander.setProgramName("GenerateData")

  if (params.help) {
    commander.usage()
    System.exit(0)
  }

  //println(s"${params.es_url}, ${params.index_name}, ${params.index_type}, ${params.batch_size}, ${params.num_of_shards}, ${params.count}, ${params.format}, ${params.num_of_replicas}, ${params.set_refresh}, ${params.delete_index}")

  new DataGenerater(
    params.es_url,
    params.index_name,
    params.index_type,
    params.batch_size,
    params.num_of_shards,
    params.count,
    params.format,
    params.num_of_replicas,
    params.set_refresh,
    params.delete_index,
    Option(params.user_name),
    Option(params.password)
  ).start()


  @Parameters(separators = "=")
  class GenerateDataParameters {
    @Parameter(names = Array("-es_url"), description = "URL of your Elasticsearch node")
    val es_url: String = """http://localhost:9200/"""

    @Parameter(names = Array("-index_name"), description = "Name of the index to store your messages")
    val index_name: String = "test_data"

    @Parameter(names = Array("-index_type"), description = "Type")
    val index_type: String = "test_type"

    @Parameter(names = Array("-batch_size"), description = "Elasticsearch bulk index batch size")
    val batch_size: Integer = 500

    @Parameter(names = Array("-num_of_shards"), description = "Number of shards for ES index")
    val num_of_shards: Integer = 2

    @Parameter(names = Array("-count"), description = "Number of docs to generate")
    val count: Integer = 10 * 1000

    @Parameter(names = Array("-format"), description = "message format")
    val format: String = "name:String,age:Int,last_updated:ts"

    @Parameter(names = Array("-num_of_replicas"), description = "Number of replicas for ES index")
    val num_of_replicas: Integer = 0

    @Parameter(names = Array("-set_refresh"), description = "Set refresh rate to -1 before starting the upload")
    val set_refresh: Boolean = false

    @Parameter(names = Array("-delete_index"), description = "Set delete index first")
    val delete_index: Boolean = false

    @Parameter(names = Array("-user_name"), description = "For Shield")
    val user_name: String = null

    @Parameter(names = Array("-password"), description = "For Shield")
    val password: String = null

    @Parameter(names = Array("-h", "--help"), help = true, description = "Print help text")
    var help: Boolean = false
  }

}