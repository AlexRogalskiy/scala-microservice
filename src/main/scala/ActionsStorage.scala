import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import spray.json.{DefaultJsonProtocol, enrichAny, _}

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

case class Action(id: Int, name: String)

trait ActionJsonProtocol extends DefaultJsonProtocol {
  implicit val actionJson = jsonFormat2(Action)
}

object ActionsStorage extends ActionJsonProtocol{
  var actions = List(
    Action(1, "Add"),
    Action(2, "Multiply"),
    Action(3, "Divide")
  )

  def main(args: Array[String]): Unit = {
    implicit val system           = ActorSystem(Behaviors.empty, "my-system")
    implicit val executionContext = system.executionContext

    val personServerRoute =
      pathPrefix("api" / "action") {
        get {
          (path(IntNumber) | parameter('id.as[Int])) { id =>
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                actions.find(_.id == id).toJson.prettyPrint
              )
            )
          } ~
          pathEndOrSingleSlash {
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                actions.toJson.prettyPrint
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

                actions = actions :+ action
                complete(StatusCodes.OK)
              case Failure(ex) =>
                failWith(ex)
            }
          }
      }

    val bindingFuture = Http().newServerAt("localhost", 8000).bind(personServerRoute)

    println(s"Server online at http://localhost:8000/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind())                 // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}