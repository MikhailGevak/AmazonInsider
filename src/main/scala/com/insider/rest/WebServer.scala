package com.insider.rest

import java.io.StringWriter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.insider.dsl.Flows
import com.insider.item.Item
import com.insider.parser.AmazonDataSetParser

import scala.io.StdIn

object WebServer extends Json {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("Insider-Amazon")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    def parse(filter: Item => Boolean)(comparator: Option[(Item, Item) => Boolean], count: Option[Int] = None) =
      Flows.parser(transformIterator = AmazonDataSetParser(_), parallelism = 6, filter = filter, comparator = comparator, count = count)
        .map {
          toJson(_)
        }

    val route =
      path("amazon" / "all") {
        get {
          complete(parse { _ => true } {
            None
          })
        }
      } ~ path("amazon" / "parser1") {
        get {
          complete(parse {
            _.price > 3900
          } {
            None
          })
        }
      } ~ path("amazon" / "parser2") {
        get {
          complete(parse {
            _.title.toLowerCase.contains("player")
          } {
            None
          })
        }
      } ~ path("amazon" / "parser3") {
        get {
          complete(parse {
            _.eanList.length > 15
          } {
            None
          })
        }
      } ~ path("amazon" / "parser4") {
        get {
          def eanToLong(ean: String) = if(!ean.isEmpty){ean.toLong}else{0}

          def comparator(item1: Item, item2: Item) = {
            eanToLong(item1.ean).compareTo(eanToLong(item2.ean)) match {
              case 1  => true
              case -1 => false
              case 0  => item1.price < item2.price
            }
          }

          complete(parse( _ => true )(Some(comparator(_, _)), Some(10)))
        }
      } ~ path("amazon" / "parser5") {
        get {
          def comparator(item1: Item, item2: Item) = item1.price > item2.price

          complete(parse{_.label.equals("Viewtv")}(Some(comparator(_, _))))
        }
      } ~ path("amazon"){
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
            """<a href = "/amazon/parser1">Parse the products which are more expesive than $39.</a><hr><br>
              |<a href = "/amazon/parser2">Parse the products which have the word “player” in their title.</a><hr><br>
              |<a href = "/amazon/parser3">Parse the product which have EANlist digit numbers more then 15 digits.</a><hr><br>
              |<a href = "/amazon/parser4">Parse the first 10 products with Highest EAN number (Primary) and Lowest Price.(Secondary)</a><hr><br>
              |<a href = "/amazon/parser5">Parse and list only “Viewtv” labels according to their Price - High to Low.</a><hr><br>
            """.stripMargin))
        }
      }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}


trait Json {
  lazy val mapper = (new ObjectMapper).registerModule(DefaultScalaModule)

  def toJson(obj: Any): String = {
    val outWriter = new StringWriter
    mapper.writeValue(outWriter, obj)
    outWriter.toString
  }
}