package com.insider
package parser

import scala.annotation.tailrec
import scala.collection.mutable

trait ItemsIterator extends Iterator[Map[String, List[String]]]

object AmazonDataSetParser {

  private[parser] sealed trait State

  private[parser] object ItemFound extends State

  private[parser] object NoLines extends State

  private[parser] object LookingForItem extends State


  private[parser] val propertyPattern = "^(\\w+)=(.*)$".r
  val itemPattern = "^[I|i][T|t][E|e][M|m] (\\d+)$".r
}

case class AmazonDataSetParser(lines: Iterator[String]) extends ItemsIterator {

  import AmazonDataSetParser._

  var state: State = LookingForItem //Initial state of iterator

  @tailrec
  private[this] def lookingForItem(): Unit = {
    if (state == LookingForItem) {
     if (lines.hasNext) {
       lines.next().toLowerCase match {
         case itemPattern(_) =>
           state = ItemFound
         case _              =>
           lookingForItem()
       }
     }
     else {
       state = NoLines
     }
   }
  }

  @tailrec
  private[this] def readProperties(values: mutable.Map[String, List[String]] = mutable.Map.empty): Map[String, List[String]] = {
    require(state == ItemFound)
    if (lines.hasNext) {
      lines.next() match {
        case propertyPattern(name, value) =>
          values.put(name, value :: (values.get(name) getOrElse Nil))
          readProperties(values)
        case itemPattern(_)               =>
          state = ItemFound
          values.toMap
        case _                            =>
          readProperties(values)
      }
    }
    else {
      state = NoLines
      values.toMap
    }
  }

  override def hasNext = {
    lookingForItem()
    state == ItemFound
  }

  override def next() = {
    lookingForItem()
    require(state == ItemFound)
    readProperties()
  }
}
