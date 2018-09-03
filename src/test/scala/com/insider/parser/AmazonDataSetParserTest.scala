package com.insider.parser

import org.scalatest.{FlatSpec, Matchers}

class AmazonDataSetParserTest extends FlatSpec with Matchers{
    it should "parse one simple item" in {
      val lines = Seq("ITEM 1",
        "Author=Bruce Barnbaum",
        "Binding=Paperback",
        "Brand=Brand: Rocky Nook",
        "")

      val parser = AmazonDataSetParser(lines.toIterator)

      val items = parser.toSeq
      items.size should be(1)
      items(0) should be(Map("Author"->List("Bruce Barnbaum"), "Binding"->List("Paperback"), "Brand"->List("Brand: Rocky Nook")))
    }

  it should "parse one simple item and ignore `white noise`" in {
    val lines = Seq("ITEM 1",
      "Author=Bruce Barnbaum",
      "Binding=Paperback",
      "White NOIse!!!",
      "Brand=Brand: Rocky Nook",
      "")

    val parser = AmazonDataSetParser(lines.toIterator)

    val items = parser.toSeq
    items.size should be(1)
    items(0) should be(Map("Author"->List("Bruce Barnbaum"), "Binding"->List("Paperback"), "Brand"->List("Brand: Rocky Nook")))
  }

  it should "parse not correct file (return empty Iterator)" in {
    val lines = Seq("Author=Bruce Barnbaum",
      "Binding=Paperback",
      "White NOIse!!!",
      "Brand=Brand: Rocky Nook",
      "")

    val parser = AmazonDataSetParser(lines.toIterator)

    val items = parser.toSeq
    items.size should be(0)
  }

  it should "parse some simple items" in {
    val lines = Seq("ITEM 1",
      "Binding=Kitchen",
      "Brand=Calphalon",
      "CatalogNumberList=ZPV-2247IB0601A",
      "EAN=0016853039487",
      "EANList=0016853039487",
      "Feature=Innovative stainless steel bakeware has interlocking non-stick layers deliver long-lasting, high-performance clean release with even the stickiest or most delicate baked goods.",
      "Feature=Heavy-gauge stainless steel core heats evenly without hot spots so your cookies and cakes come out of the oven perfectly and evenly browned.",
      "Feature=Toffee-colored nonstick finish is beautiful and functional. The lighter color makes it easier to prevent overbrowning.",
      "Feature=Reinforced rolled edges prevent warping.",
      "Feature=Full 10-year warranty. Calphalon will replace any item found defective in material or workmanship when put to normal household use and cared for according to the instructions.",
      "Feature=Available as individual baking pans or bakeware sets.",
      "",
      "ITEM 2",
      "Binding=Kitchen",
      "Brand=Rachael Ray",
      "CatalogNumberList=OC-5567355673WIR-101",
      "Color=Grey",
      "Department=Kitchen & Dining",
      "EAN=0013038001659",
      "")

    val parser = AmazonDataSetParser(lines.toIterator)

    val items = parser.toSeq

    items.size should be(2)
    val item1 = items(0)
    val item2 = items(1)

    item1.get("Binding").get should be("Kitchen" :: Nil)
    item1.get("CatalogNumberList").get should be("ZPV-2247IB0601A" :: Nil)
    item1.get("Feature").get.size should be(6)

    item2.get("EAN").get should be("0013038001659" :: Nil)
  }
}
