package com.insider.dsl

import java.io.File
import java.net.URI

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.insider.item.Item
import com.insider.parser.ItemsIterator

import scala.concurrent.{ExecutionContext, Future}

object Flows {
  private[this] def mainFlow(transformIterator: Iterator[String] => ItemsIterator,
                             filter: Item => Boolean,
                             parallelism: Int): Flow[File, Item, _] =
    Flow.fromFunction(identity[File])
      .map { path =>  scala.io.Source.fromFile(path, "Cp1252").getLines()}
      .flatMapMerge(parallelism, stringIterator => Source.fromIterator(() => transformIterator(stringIterator))
      .map {
        Item(_)
      }
      .filter(filter))


  def parser(transformIterator: Iterator[String] => ItemsIterator,
             filter: Item => Boolean = (_ => true),
             parallelism: Int,
             comparator: Option[(Item, Item) => Boolean] = None,
             count: Option[Int] = None)(implicit mat: Materializer, ex: ExecutionContext): Future[Seq[Item]] =
    Source.single(getClass.getResource("/datasets").toURI)
      .mapConcat { path => new File(path).listFiles().toStream }
      .via(mainFlow(transformIterator, filter, parallelism))
      .runWith(Sink.seq) map { items => comparator map {
      items.sortWith(_)
    } getOrElse items
    } map {items => count map {items.take(_)} getOrElse items}
}


