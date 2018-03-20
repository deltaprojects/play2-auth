package controllers.ephemeral

import javax.inject.Inject
import jp.t2v.lab.play2.auth.sample.Account
import jp.t2v.lab.play2.auth.{CookieTokenAccessor, LoginLogout, TokenAccessor}
import play.Mode
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AbstractController, ControllerComponents}
import views.html

import scala.concurrent.{ExecutionContext, Future}

class Sessions @Inject() (val environment: Environment, controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(controllerComponents) with LoginLogout with AuthConfigImpl {
  lazy val tokenAccessor: TokenAccessor = new CookieTokenAccessor(
                                                                   cookieName = "PLAY2AUTH_SESS_ID",
                                                                   cookieSecureOption = environment.mode.asJava == Mode.PROD,
                                                                   cookieHttpOnlyOption = true,
                                                                   cookieDomainOption = None,
                                                                   cookiePathOption = "/",
                                                                   cookieMaxAge = None,
                                                                   secretKey="ZSn5z9l]1dhRTKM[iBjc_YJQlRH:M<RoFz5ZQ<]foaETnzb]QMn2lU6mK?8xxGGQ"
                                                                 )

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(html.ephemeral.login(loginForm))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded.map(_.flashing(
      "success" -> "You've been logged out"
    ))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.ephemeral.login(formWithErrors))),
      user           => gotoLoginSucceeded(user.get.id)
    )
  }

}