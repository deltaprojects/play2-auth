package controllers.csrf

import controllers.stack.TokenValidateElement
import javax.inject.Inject
import jp.t2v.lab.play2.auth.{AuthElement, CookieTokenAccessor}
import jp.t2v.lab.play2.auth.sample.Role._
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AbstractController, ControllerComponents}

class PreventingCsrfSample @Inject() (val environment: Environment, controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) with TokenValidateElement with AuthElement with AuthConfigImpl {
  def tokenAccessor = new CookieTokenAccessor(secretKey="ZSn5z9l]1dhRTKM[iBjc_YJQlRH:M<RoFz5ZQ<]foaETnzb]QMn2lU6mK?8xxGGQ")

  def formWithToken = StackAction(AuthorityKey -> NormalUser, IgnoreTokenValidation -> true) { implicit req =>
    Ok(views.html.csrf.formWithToken())
  }

  def formWithoutToken = StackAction(AuthorityKey -> NormalUser, IgnoreTokenValidation -> true) { implicit req =>
    Ok(views.html.csrf.formWithoutToken())
  }

  val form = Form { single("message" -> text) }

  def submitTarget = StackAction(AuthorityKey -> NormalUser) { implicit req =>
    form.bindFromRequest.fold(
      _       => throw new Exception,
      message => Ok(message).as("text/plain")
    )
  }

}
