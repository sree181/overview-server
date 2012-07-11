package views.json.Tree

import play.api.Play.{start, stop}
import play.api.test.FakeApplication

import scala.collection.JavaConversions._

import com.avaje.ebean.Ebean

import org.specs2.mutable._

import play.api.libs.json.Json._

import models.{Document, DocumentSet, Node, Tree}
import helpers.DbContext

object JsonHelpersSpec extends Specification {

  
  "rootNodeToJsValue" should {
    "contain the nodes" in new DbContext {
    	val tree = new Tree()

    	val root = new Node()

    	for (i <- 10 to 12) {
    	  val level1ChildNode = new Node()
    	  root.children.add(level1ChildNode)

    	  for (j <- 1 to 2) {
    	    val level2ChildNode = new Node()
    	  	level1ChildNode.children.add(level2ChildNode)
    	  }
    	}

    	tree.root = root
    	tree.save
    	val treeJson = JsonHelpers.rootNodeToJsValue(tree.root)
    	treeJson.toString must /("nodes") */("id" -> root.id)

    	for (level1Node <- root.children.toSeq) {
    	  treeJson.toString must /("nodes") */("id" -> level1Node.id)
    	  for (level2Node <- level1Node.children.toSeq) {
    	    treeJson.toString must /("nodes") */("id" -> level2Node.id)
    	  }
    	}


    }

    "return documents in nodes" in new DbContext {
      val documentSet = new DocumentSet()

      val tree = new Tree()

      val root = new Node()

      for (i <- 10 to 30) {
        val document = new Document("document[" + i + "]", "textUrl-" + i, "viewUrl-" + i)
        documentSet.addDocument(document)

        root.addDocument(document)

      }

      for (i <- 0 to 2) {
      	val child = new Node()
      	documentSet.getDocuments.slice(5 * i, 5 * i + 5).foreach{child.addDocument}
      	root.addChild(child)
      }

      tree.root = root
      tree.save

      val treeJson = JsonHelpers.rootNodeToJsValue(tree.root)

      for (document <- documentSet.getDocuments.slice(0, 15)) {
        treeJson.toString must /("documents") */("id" -> document.id)
        treeJson.toString must /("documents") */("description" -> document.title)
    	  treeJson.toString.indexOf(document.title) must be equalTo(
    	      treeJson.toString.lastIndexOf(document.title))
      }

      for (document <- documentSet.getDocuments.slice(15, 21)) {
    	  treeJson.toString must not contain("document[" + document.id + "]")
      }

    }


    "fake Tags list" in new DbContext {

    	val tree = new Tree()

    	val root = new Node()
    	tree.root = root;

    	tree.save

    	val treeJson = JsonHelpers.rootNodeToJsValue(tree.root)

    	treeJson.toString must contain ("tags")
    }
  }

  "subNodeToJsValue" should {
    "contain the id" in new DbContext {
      val node = new Node()
      node.id = 5

      node.save
      val nodeJson = JsonHelpers.subNodeToJsValue(node)

      nodeJson.toString must / ("id" -> node.id)
    }

    "contain the description" in new DbContext {
      val node = new Node()
      node.id = 5
      node.description = "This is my description"

      node.save
      val nodeJson = JsonHelpers.subNodeToJsValue(node)

      nodeJson.toString must / ("description" -> node.description)
    }

    "contain children node Ids" in new DbContext {
      val rootNode = new Node()
      rootNode.id = 5
      for (i <- 1 to 3) {
        val childNode = new Node()
        childNode.id = i
        rootNode.children.add(childNode)
      }

      rootNode.save
      val nodeJson = JsonHelpers.subNodeToJsValue(rootNode)

      // I think Specs2 Json matcher should allow something like:
      //nodeJson.toString must /("children") "/(1.0)"
      // but I can't get it to work, so am using the fragile test below. - JK
      nodeJson.toString must contain ("\"children\":[1,2,3]" )

    }

    "contains doclist with docIds and count" in new DbContext {
      val rootNode = new Node()
      rootNode.id = 5
      for (i <- 1 to 3) {
        val childNode = new Node()
        childNode.id = i
        rootNode.children.add(childNode)
      }

      for (i <- 10 to 15) {
      	val document = new Document("title", "textUrl", "viewUrl")
      	document.id = i
      	rootNode.documents.add(document)
      }

      rootNode.save
      val nodeJson = JsonHelpers.subNodeToJsValue(rootNode)

      //nodeJson.toString must /("doclist") /("docids") /(10.0)
      nodeJson.toString must contain ("\"docids\":[10,11,12,13,14,15]")
      nodeJson.toString must /("doclist") /("n" -> 6)
    }

    "doclist lists at most first 10 docids" in new DbContext {
  	val rootNode = new Node()
      rootNode.id = 5

      for (i <- 10 to 35) {
      	val document = new Document("title" + i, "textUrl", "viewUrl")
      	document.id = i
      	rootNode.documents.add(document)
      }

  	rootNode.save
      val nodeJson = JsonHelpers.subNodeToJsValue(rootNode)


      nodeJson.toString must contain ("10,11,12,13,14,15,16,17,18,19]")
      nodeJson.toString must /("doclist") /("n" -> 26)
    }

    "fakes the tags list" in new DbContext {
      val rootNode = new Node()
      rootNode.id = 5

      rootNode.save
      val nodeJson = JsonHelpers.subNodeToJsValue(rootNode)

      nodeJson.toString must contain("\"taglist\":{\"full\":[],\"partial\":[]}")
    }
  }
}
