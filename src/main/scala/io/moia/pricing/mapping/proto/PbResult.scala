package io.moia.pricing.mapping.proto

import scala.util.{Failure, Success, Try}

/**
  * Models the attempt to read a Protocol Buffers case class into business model type `T`.
  */
sealed trait PbResult[+T] {

  val isSuccess: Boolean
  val isError: Boolean

  def get: T

  def getOrElse[U >: T](t: => U): U

  def map[B](f: T => B): PbResult[B]

  def flatMap[B](f: T => PbResult[B]): PbResult[B]

  def withPathPrefix(prefix: String): PbResult[T]

  def toTry: Try[T]
}

/**
  * Models the success to read a Protocol Buffers case class into business model type `T`.
  */
case class PbSuccess[T](value: T) extends PbResult[T] {

  val isSuccess = true
  val isError = false

  def get: T = value

  def getOrElse[U >: T](t: => U): U = value

  def map[B](f: T => B): PbResult[B] = PbSuccess(f(value))

  def flatMap[B](f: T => PbResult[B]): PbResult[B] = f(value)

  override def withPathPrefix(prefix: String): PbSuccess[T] = this

  def toTry: Try[T] = Success(get)
}

/**
  * Models the failure to read a Protocol Buffers case class into business model type `T`.
  * Provides error messages for one or more paths, e.g.
  * The path messages could be
  * /price Value must be a decimal number.      <- Simple field at top-level
  * /tripRequest/time Value is required.        <- Nested field
  * /prices(1) Value must be a decimal number.  <- Simple array
  * /tripRequests(1)/time Value is required.    <- Nested field in second array entry
  */
case class PbFailure(errors: Seq[(String, String)]) extends PbResult[Nothing] {

  val isSuccess = false
  val isError = true

  def get: Nothing = throw new NoSuchElementException(toString)

  def getOrElse[U >: Nothing](t: => U): U = t

  def map[B](f: Nothing => B): PbResult[B] = this.asInstanceOf[PbResult[B]]

  def flatMap[B](f: Nothing => PbResult[B]): PbResult[B] =
    this.asInstanceOf[PbResult[B]]

  override def withPathPrefix(prefix: String): PbFailure =
    PbFailure(for ((path, message) <- errors) yield (prefix + path, message))

  override def toString: String =
    errors.map(e => s"${e._1} ${e._2}".trim).mkString(" ")

  def toTry: Try[Nothing] = Failure(new Exception(toString))
}

object PbFailure {

  def apply(path: String, message: String): PbFailure =
    new PbFailure(Seq(path -> message))

  def apply(message: String): PbFailure =
    apply("", message)
}
