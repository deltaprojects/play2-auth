package jp.t2v.lab.play2.auth

import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait AuthActionBuilders extends AsyncAuth { self: AuthConfig with AbstractController =>


  final case class GenericOptionalAuthRequest[+A, R[+_] <: Request[_]](user: Option[User], underlying: R[A]) extends WrappedRequest[A](underlying.asInstanceOf[Request[A]])
  final case class GenericAuthRequest[+A, R[+_] <: Request[_]](user: User, underlying: R[A]) extends WrappedRequest[A](underlying.asInstanceOf[Request[A]])

  final case class GenericOptionalAuthFunction[R[+_] <: Request[_]](
    override protected val executionContext: ExecutionContext
  ) extends ActionFunction[R, ({type L[+A] = GenericOptionalAuthRequest[A, R]})#L] {
    def invokeBlock[A](request: R[A], block: GenericOptionalAuthRequest[A, R] => Future[Result]) = {
      implicit val ec: ExecutionContext = executionContext
      restoreUser(request, executionContext) recover {
        case _ => None -> identity[Result] _
      } flatMap { case (user, cookieUpdater) =>
        block(GenericOptionalAuthRequest[A, R](user, request)).map(cookieUpdater)
      }
    }
  }

  final case class GenericAuthenticationRefiner[R[+_] <: Request[_]](
    override protected val executionContext: ExecutionContext
  ) extends ActionRefiner[({type L[+A] = GenericOptionalAuthRequest[A, R]})#L, ({type L[+A] = GenericAuthRequest[A, R]})#L] {
    override protected def refine[A](request: GenericOptionalAuthRequest[A, R]): Future[Either[Result, GenericAuthRequest[A, R]]] = {
      request.user map { user =>
        Future.successful(Right[Result, GenericAuthRequest[A, R]](GenericAuthRequest[A, R](user, request.underlying)))
      } getOrElse {
        implicit val ctx = executionContext
        authenticationFailed(request).map(Left.apply[Result, GenericAuthRequest[A, R]])
      }
    }
  }

  final case class GenericAuthorizationFilter[R[+_] <: Request[_]](
    authority: Authority,
    override protected val executionContext: ExecutionContext
  ) extends ActionFilter[({type L[+B] = GenericAuthRequest[B, R]})#L] {
    override protected def filter[A](request: GenericAuthRequest[A, R]): Future[Option[Result]] = {
      implicit val ctx = executionContext
      authorize(request.user, authority) collect {
        case true => None
      } recoverWith {
        case _ => authorizationFailed(request, request.user, Some(authority)).map(Some.apply)
      }
    }
  }

  final def composeOptionalAuthAction[R[+_] <: Request[_], B](builder: ActionBuilder[R, B])(implicit ec: ExecutionContext): ActionBuilder[({type L[+A] = GenericOptionalAuthRequest[A, R]})#L, B] = {
    builder.andThen[({type L[A] = GenericOptionalAuthRequest[A, R]})#L](GenericOptionalAuthFunction[R](ec))
  }

  final def composeAuthenticationAction[R[+_] <: Request[_], B](builder: ActionBuilder[R, B])(implicit ec: ExecutionContext): ActionBuilder[({type L[+A] = GenericAuthRequest[A, R]})#L, B] = {
    composeOptionalAuthAction[R, B](builder).andThen[({type L[+A] = GenericAuthRequest[A, R]})#L](GenericAuthenticationRefiner[R](ec))
  }

  final def composeAuthorizationAction[R[+_] <: Request[_], B](builder: ActionBuilder[R, B])(authority: Authority)(implicit ec: ExecutionContext): ActionBuilder[({type L[+A] = GenericAuthRequest[A, R]})#L, B] = {
    composeAuthenticationAction(builder).andThen[({type L[+A] = GenericAuthRequest[A, R]})#L](GenericAuthorizationFilter[R](authority, ec))
  }

  final type OptionalAuthRequest[+A] = GenericOptionalAuthRequest[A, Request]
  final type AuthRequest[+A] = GenericAuthRequest[A, Request]
  final def OptionalAuthFunction(implicit ec: ExecutionContext): ActionFunction[Request, OptionalAuthRequest] = GenericOptionalAuthFunction[Request](ec)
  final def AuthenticationRefiner(implicit ec: ExecutionContext): ActionRefiner[OptionalAuthRequest, AuthRequest] = GenericAuthenticationRefiner[Request](ec)
  final def AuthorizationFilter(authority: Authority)(implicit ec: ExecutionContext): ActionFilter[AuthRequest] = GenericAuthorizationFilter[Request](authority, ec)

  final def OptionalAuthAction(implicit ec: ExecutionContext): ActionFunction[Request, OptionalAuthRequest] = composeOptionalAuthAction(Action)
  final def AuthenticationAction(implicit ec: ExecutionContext): ActionFunction[Request, AuthRequest] = composeAuthenticationAction(Action)
  final def AuthorizationAction(authority: Authority)(implicit ec: ExecutionContext): ActionFunction[Request, AuthRequest] = composeAuthorizationAction(Action)(authority)

}
