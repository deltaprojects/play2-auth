package controllers.rememberme

import controllers.stack.Pjax
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import jp.t2v.lab.play2.auth.sample.Account
import jp.t2v.lab.play2.auth.sample.Role._
import play.api.Environment
import play.api.mvc.{AbstractController, ControllerComponents}
import views.html

class Messages @Inject() (val environment: Environment, controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) with Pjax with AuthElement with AuthConfigImpl {

  if (Account.findAll.isEmpty) {
    Seq(
         Account(1, "alice@example.com", "secret", "Alice", Administrator),
         Account(2, "bob@example.com",   "secret", "Bob",   NormalUser),
         Account(3, "chris@example.com", "secret", "Chris", NormalUser)
       ) foreach Account.create
  }
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

  protected val fullTemplate: User => Template = html.rememberme.fullTemplate.apply

}
