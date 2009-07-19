package com.novocode.squery.combinator.sql

import scala.collection.mutable.ArrayBuffer
import com.novocode.squery.session.PositionedParameters
import com.novocode.squery.session.TypeMapper.StringTypeMapper

final class SQLBuilder extends SQLBuilder.Segment {
  import SQLBuilder._

  private val segments = new ArrayBuffer[Segment]
  private var currentStringSegment: StringSegment = null

  private def ss = {
    if(currentStringSegment eq null) {
      if(segments.isEmpty || segments.last.isInstanceOf[SQLBuilder]) {
        currentStringSegment = new StringSegment
        segments += currentStringSegment
      }
      else currentStringSegment = segments.last.asInstanceOf[StringSegment]
    }
    currentStringSegment
  }

  def +=(s: String) = { ss.sb append s; this }

  def +=(c: Char) = { ss.sb append c; this }

  def +=(s: SQLBuilder) = { ss.sb append s; this }

  def +?=(f: Setter) = { ss.setters append f; ss.sb append '?'; this }

  def createSlot = {
    val s = new SQLBuilder
    segments += s
    currentStringSegment = null
    s
  }

  def appendTo(res: StringBuilder, setters: ArrayBuffer[Setter]): Unit =
    for(s <- segments) s.appendTo(res, setters)

  def build = {
    val sb = new StringBuilder(64)
    val setters = new ArrayBuffer[Setter]
    appendTo(sb, setters)
    (sb.toString, new CombinedSetter(setters))
  }
}

object SQLBuilder {
  type Setter = (PositionedParameters => PositionedParameters)

  private class CombinedSetter(b: Seq[Setter]) extends Setter {
    def apply(p: PositionedParameters): PositionedParameters = {
      for(s <- b) s(p)
      p
    }
  }

  trait Segment {
    def appendTo(res: StringBuilder, setters: ArrayBuffer[Setter]): Unit
  }

  class StringSegment extends Segment {
    val sb = new StringBuilder(32)
    val setters = new ArrayBuffer[Setter]

    def appendTo(res: StringBuilder, setters: ArrayBuffer[Setter]) {
      res append sb
      setters ++ this.setters
    }
  }
}
