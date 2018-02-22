package controllers.stack

import scala.concurrent.Future
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import scala.util.Random
import java.security.SecureRandom

import play.api.libs.typedmap.TypedKey

trait TokenValidateActionFunction extends ActionFunction[Request, Request] with Results {

  import TokenValidateActionFunction._

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      if (isIgnoreTokenValidation(request) || validateToken(request)) {
        implicit val ctx = this.executionContext
        val newToken = generateToken
        block(request.addAttr(PreventingCsrfTokenKey, newToken)) map {
          _.withSession(PreventingCsrfTokenSessionKey -> newToken.value)
        }
      } else {
        Future.successful(BadRequest("Invalid preventing CSRF token"))
      }
  }

  private val tokenForm: Form[PreventingCsrfToken] = Form {
    PreventingCsrfToken.FormKey -> text.transform(PreventingCsrfToken.apply, (p: PreventingCsrfToken) => p.value)
  }

  private val random = new Random(new SecureRandom)
  private val table = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "^`~:/?,.{[}}|+_()*^%$#@!"

  private def generateToken: PreventingCsrfToken = PreventingCsrfToken {
    Iterator.continually(random.nextInt(table.size)).map(table).take(32).mkString
  }

  private def validateToken(implicit request: Request[_]): Boolean = (for {
    tokenInForm    <- tokenForm.bindFromRequest().value
    tokenInSession <- request.session.get(PreventingCsrfTokenSessionKey)
  } yield tokenInForm.value == tokenInSession) getOrElse false

  private def isIgnoreTokenValidation(implicit request: Request[_]): Boolean =
    request.attrs.get(IgnoreTokenValidation).exists(identity)

}
object TokenValidateActionFunction {
  val PreventingCsrfTokenKey: TypedKey[PreventingCsrfToken] = TypedKey("preventingCsrfToken")
  val IgnoreTokenValidation: TypedKey[Boolean] = TypedKey("ignoreTokenValidation")
  private val PreventingCsrfTokenSessionKey = "preventingCsrfToken"
}

case class PreventingCsrfToken(value: String)

object PreventingCsrfToken {

  val FormKey = "preventingCsrfToken"

}
