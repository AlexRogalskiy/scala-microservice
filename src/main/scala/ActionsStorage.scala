import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, enrichAny, _}

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

case class Action(id: Int, value: String)

trait JsonProtocol extends DefaultJsonProtocol {
  implicit val action = jsonFormat2(Action)
}

object ActionsStorage extends JsonProtocol{
  var dbMock = List(
    Action(1, "add"),
    Action(2, "divide")
  )

  def executeAction(input: String): Unit = {
    input.parseJson.convertTo[Action]
  }


  implicit val system           = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext = system.executionContext



  val route: Route = pathPrefix("api" / "action") {
    get {
      pathEndOrSingleSlash {
        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            dbMock.toJson.prettyPrint
          )
        )
      } ~
        (path(IntNumber) | parameter('id.as[Int])) { id =>
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              dbMock.find(_.id == id).toJson.prettyPrint
            )
          )
        }
    } ~
      (post & pathEndOrSingleSlash & extractRequest & extractLog) { (request, log) =>
        val entity             = request.entity
        val strictEntityFuture = entity.toStrict(2 seconds)
        val actionFuture       = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[Action])

        onComplete(actionFuture) {
          case Success(action) =>
            log.info(s"Got action: $action")

            dbMock = dbMock :+ action   // db write

            complete(StatusCodes.OK)
          case Failure(ex) =>
            failWith(ex)
        }
      }
  }


  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt("localhost", 8000).bind(route)

    println(s"Server online at http://localhost:8000/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind())                 // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}