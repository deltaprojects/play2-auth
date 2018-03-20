package controllers.rememberme

import javax.inject.Inject
import jp.t2v.lab.play2.auth.sample.Account
import jp.t2v.lab.play2.auth.sample.Role.{Administrator, NormalUser}
import jp.t2v.lab.play2.auth.{CookieTokenAccessor, LoginLogout}
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{AbstractController, ControllerComponents}
import views.html

import scala.concurrent.Future

class Sessions @Inject() (val environment: Environment, controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) with LoginLogout with AuthConfigImpl {
  if (Account.findAll.isEmpty) {
    Seq(
         Account(1, "alice@example.com", "secret", "Alice", Administrator),
         Account(2, "bob@example.com",   "secret", "Bob",   NormalUser),
         Account(3, "chris@example.com", "secret", "Chris", NormalUser)
       ) foreach Account.create
  }
  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }
  val remembermeForm = Form {
    "rememberme" -> boolean
  }

  def login = Action { implicit request =>
    Ok(html.rememberme.login(loginForm, remembermeForm.fill(request.session.get("rememberme").exists("true" ==))))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded.map(_.flashing(
      "success" -> "You've been logged out"
    ))
  }

  def authenticate = Action.async { implicit request =>
    val rememberme = remembermeForm.bindFromRequest()
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.rememberme.login(formWithErrors, rememberme))),
      { user =>
        val req = request.copy(tags = request.tags + ("rememberme" -> rememberme.get.toString))
        gotoLoginSucceeded(user.get.id)(req, defaultContext).map(_.withSession("rememberme" -> rememberme.get.toString))
      }
    )
  }

}