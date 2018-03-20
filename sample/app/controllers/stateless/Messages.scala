package controllers.stateless

import controllers.stack.Pjax
import javax.inject.Inject
import jp.t2v.lab.play2.auth.sample.Role._
import jp.t2v.lab.play2.auth.{AuthElement, CookieTokenAccessor}
import play.api.Environment
import play.api.mvc.{AbstractController, ControllerComponents}
import views.html

class Messages @Inject() (val environment: Environment, controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) with Pjax with AuthElement with AuthConfigImpl {
  def tokenAccessor = new CookieTokenAccessor(secretKey="ZSn5z9l]1dhRTKM[iBjc_YJQlRH:M<RoFz5ZQ<]foaETnzb]QMn2lU6mK?8xxGGQ")

  def main = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val title = "message main"
    Ok(html.message.main(title))
  }

  def list = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val title = "all messages"
    Ok(html.message.list(title))
  }

  def detail(id: Int) = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val title = "messages detail "
    Ok(html.message.detail(title + id))
  }

  def write = StackAction(AuthorityKey -> Administrator) { implicit request =>
    val title = "write message"
    Ok(html.message.write(title))
  }

  protected val fullTemplate: User => Template = html.stateless.fullTemplate.apply

}