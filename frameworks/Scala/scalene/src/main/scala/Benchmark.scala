package scalene.benchmark

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scalene.actor.Pool
import scalene.routing._
import scalene.http._
import scalene._
import scalene.util._
import scalene.sql._
import BasicConversions._

object Main extends App {

  Class.forName("org.postgresql.Driver");

  trait JsonMessage
  case class JsonRouteMessage(message: String) extends JsonMessage
  case class DBRouteMessage(id: Int, randomnumber: Int) extends JsonMessage
  case class MultiDBRouteMessage(items: Array[DBRouteMessage]) extends JsonMessage

  implicit val messageFormatter = new BodyFormatter[JsonMessage] {
    val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
    def apply(msg: JsonMessage) = {
      val obj = msg match {
        case MultiDBRouteMessage(items) => items
        case other => other
      }
      scalene.http.Body(mapper.writeValueAsBytes(obj), Some(ContentType.`application/json`))
    }
  }

  val settings = Settings.basic(
    serverName = "scalene",
    port = 8080,
    server = scalene.ServerSettings.Default
  )

  
  implicit val pool = new Pool

  def basicServer = {

  val worldClient = MiniSQL.client(
    "world-client",
    "jdbc:postgresql://tfb-database:5432/hello_world",
    "benchmarkdbuser",
    "benchmarkdbpass"
  )

  val random = new java.util.Random
  
  def randomWorld(session: MiniSQLSession): Option[DBRouteMessage] = {
    val stmt = session.prepared("SELECT id, randomnumber FROM world WHERE id = (?)")
    stmt.setInt(1, math.abs(random.nextInt) % 10000 + 1)
    val rs = stmt.executeQuery()
    if (rs.next()) {
      Some(DBRouteMessage(rs.getInt(1), rs.getInt(2)))
    } else {
      None
    }      
  }

  val dbRoute = GET / "db" to {_ =>
    worldClient.query{session =>
      randomWorld(session).map{_.ok}.getOrElse("N/A".notFound)
    }
  }

  val QueryNum = ![Int]
    .map{i => if (i < 1) 1 else if (i > 500) 500 else i}
    .recover{_ => 1}

  val multiRoute = GET / "queries" / QueryNum to {num =>
    worldClient.query{session =>
      MultiDBRouteMessage(Array.fill(num)(randomWorld(session).get)).ok
    }
  }


  val routes = Routes(
    GET / "plaintext" to {_ => plainBody.ok},
    GET / "json"      to {_ => JsonRouteMessage("Hello, World!").ok},
    dbRoute,
    multiRoute
  )


  }

  val plainBody = scalene.http.Body.plain("Hello, World!")

  def coreServer = HttpServer.start(settings, implicit context => new RequestHandler[HttpRequest, HttpResponse] {

      val matchUrl = "GET /plaintext".getBytes
      def onInitialize(context: RequestHandlerContext){
      }

      def handleRequest(request: HttpRequest): Async[HttpResponse] = {
        if (java.util.Arrays.equals(request.firstLine, 0, matchUrl.length, matchUrl, 0, matchUrl.length)) {
          Async.successful(HttpResponse(ResponseCode.Ok, plainBody))
        } else {
          Async.successful(HttpResponse(ResponseCode.NotFound, http.Body.plain("not found")))
        }
      }

      def handleError(request: Option[HttpRequest], error: Throwable) =
        HttpResponse(ResponseCode.Error, http.Body.plain(error.toString))

    })

  //Routing.start(settings, routes)
  //

  def minimalCoreServer = Server.start(scalene.ServerSettings.Default.copy(port = 8080), context => new ServerConnectionHandler {
    def onInitialize(env: AsyncContext){}

    val codec = new HttpServerCodec(processRequest, context.time, List(new DateHeader, scalene.http.Header("Server", "scalene")).toArray)
    val matchUrl = "GET /plaintext".getBytes

    var wOpt: Option[WriteBuffer] = None

    def processRequest(request: HttpRequest): Unit = {
      if (java.util.Arrays.equals(request.firstLine, 0, matchUrl.length, matchUrl, 0, matchUrl.length)) {
        codec.encode(HttpResponse(ResponseCode.Ok, plainBody), wOpt.get)
      } else {
        codec.encode(HttpResponse(ResponseCode.NotFound, http.Body.plain("not found")), wOpt.get)
      }
    }

    var n = 0
    val arr = new Array[Byte](1024)
    var arrPos = 0

    def onReadData(buffer: ReadBuffer, wopt: Option[WriteBuffer]): Unit = {
      while (buffer.buffer.hasRemaining) {
        val b = buffer.buffer.get
        arr(arrPos) = b
        arrPos += 1
        if (b == '\n'.toByte || b == '\r'.toByte) {
          n += 1
          if (n == 4) {
            if (java.util.Arrays.equals(arr, 0, matchUrl.length, matchUrl, 0, matchUrl.length)) {
              codec.encode(HttpResponse(ResponseCode.Ok, plainBody), wopt.get)
            } else {
              codec.encode(HttpResponse(ResponseCode.NotFound, http.Body.plain("not found")), wopt.get)
            }
            n = 0
            arrPos = 0
          }
        } else {
          n = 0
        }

      }
      //wOpt = wopt
      //codec.decode(buffer)
    }

    def onWriteData(buffer: WriteBuffer): Boolean = false

    def onConnected(handle: ConnectionHandle) = {}

    def onDisconnected(reason: DisconnectReason) = {}

    def idleTimeout: scala.concurrent.duration.Duration = scala.concurrent.duration.Duration.Inf

  },new RefreshOnDemandTimeKeeper(new RealTimeKeeper) )


  val server = minimalCoreServer
  pool.join


}

