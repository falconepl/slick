package scala.slick.jdbc

import java.sql.PreparedStatement
import scala.slick.session.{PositionedParameters, PositionedResult}

/**
 * Invoker for raw SQL queries.
 * The companion object contains utility methods for building static queries.
 */
abstract class StaticQuery[-P,+R](query: String, rconv: GetResult[R], pconv: SetParameter[P])
extends StatementInvoker[P,R] {
  protected def getStatement = query
  protected def setParam(param: P, st: PreparedStatement) = pconv(param, new PositionedParameters(st))
  protected def extractValue(rs: PositionedResult): R = rconv(rs)

  protected[this] type Self <: StaticQuery[P, R]
  protected[this] def copy(query: String = this.query, pconv: SetParameter[P] = this.pconv): Self

  def + (s: String) = copy(query + s)
  def +? [T](v: T)(implicit p: SetParameter[T]) = copy(query + '?', new SetParameter[P] {
    def apply(param: P, pp: PositionedParameters) {
      pconv(param, pp)
      p(v, pp)
    }
  })
}

object StaticQuery {
  def apply[R](implicit conv: GetResult[R]) = queryNA("")
  def apply[P, R](implicit pconv1: SetParameter[P],  rconv: GetResult[R]) = query[P,R]("")
  def u = updateNA("")
  def u1[P](implicit pconv1: SetParameter[P]) = update[P]("")

  def query[P,R](query: String)(implicit rconv: GetResult[R], pconv: SetParameter[P]) =
    new StaticQuery1[P, R](query, rconv, pconv)

  def queryNA[R](query: String)(implicit conv: GetResult[R]) =
    new StaticQuery0[R](query, conv, SetParameter.SetUnit)

  def update[P](query: String)(implicit pconv: SetParameter[P]) =
    new StaticQuery1[P, Int](query, GetResult.GetUpdateValue, pconv)

  def updateNA(query: String) =
    new StaticQuery0[Int](query, GetResult.GetUpdateValue, SetParameter.SetUnit)

  implicit def interpolation(s: StringContext) = new SQLInterpolation(s)
}

class SQLInterpolation(val s: StringContext) extends AnyVal {
  def sql[P](param: P)(implicit pconv: SetParameter[P]) =
    new SQLInterpolationResult[P](s.parts, param, pconv)
  def sqlu[P](param: P)(implicit pconv: SetParameter[P]) =
    new SQLInterpolationResult[P](s.parts, param, pconv).as[Int](GetResult.GetUpdateValue)
}

case class SQLInterpolationResult[P](strings: Seq[String], param: P, pconv: SetParameter[P]) {
  def as[R](implicit rconv: GetResult[R]): UnitInvoker[R] =
    new StaticQuery1[P, R](strings.mkString("?"), rconv, pconv)(param)
  def u = as[Int](GetResult.GetUpdateValue)
}

class StaticQuery0[R](query: String, rconv: GetResult[R], pconv: SetParameter[Unit]) extends StaticQuery[Unit, R](query, rconv, pconv) with UnitInvokerMixin[R] {
  protected[this] type Self = StaticQuery0[R]
  protected[this] def copy(query: String, pconv: SetParameter[Unit]): Self = new StaticQuery0(query, rconv, pconv)
}

class StaticQuery1[P1, R](query: String, rconv: GetResult[R], pconv: SetParameter[P1]) extends StaticQuery[P1, R](query, rconv, pconv) {
  protected[this] type Self = StaticQuery1[P1, R]
  protected[this] def copy(query: String, pconv: SetParameter[P1]): Self = new StaticQuery1(query, rconv, pconv)
}
