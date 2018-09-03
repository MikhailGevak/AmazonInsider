package com.insider.item

object Item {
  val pricePattern = "(\\d+)USD\\$\\d+\\.\\d+".r

  def getFirstElement(properties: Map[String, List[String]])(key: String, default: String): String = {
    properties get key flatMap { prices => prices.headOption } getOrElse default
  }

  def apply(properties: Map[String, List[String]]): Item = {
    val get = getFirstElement(properties) _

    val price = get("ListPrice", "0") match {
      case pricePattern(price) => price.toLong
      case _                   => 0
    }

    val title = get("Title", "")
    val eanList = get("EANList", "")
    val ean = get("EAN", "")
    val label = get("Label", "")

    Item(price, title, eanList, ean, label, properties)
  }
}

case class Item(price: Long,
                title: String,
                eanList: String,
                ean: String,
                label: String,
                properties: Map[String, List[String]])
