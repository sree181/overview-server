package com.overviewdocs.jobhandler.filegroup.task.step

import akka.actor.ActorRef
import akka.testkit.TestActor
import akka.testkit.TestProbe
import org.specs2.mock.Mockito
import org.specs2.mutable.Before
import org.specs2.mutable.Specification
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext,Future}

import com.overviewdocs.jobhandler.filegroup.DocumentIdSupplierProtocol._
import com.overviewdocs.models.Document
import com.overviewdocs.test.ActorSystemContext

class RequestDocumentIdsSpec extends Specification with Mockito {
  sequential

  "RequestDocumentIdsSpec" should {

    "request a document id" in new RequestingScope {
      requestDocumentIds.execute

      idSupplier.expectMsg(RequestIds(documentSetId, 2))
    }

    "return next step with document" in new RequestingScope {
      val NextStep(documents) = Await.result(requestDocumentIds.execute, Duration.Inf)

      documents must be equalTo (Seq(document, document))
    }
  }

  trait RequestingScope extends ActorSystemContext with Before {
    val documentSetId = 4l
    val documentId = 10l
    val document = smartMock[Document]
    val inputData = smartMock[DocumentWithoutIds]
    inputData.toDocument(documentSetId, documentId) returns document

    var idSupplier: TestProbe = _

    var requestDocumentIds: RequestDocumentIds = _

    override def before = {
      idSupplier = TestProbe()
      idSupplier.setAutoPilot(new SingleIdSupplier)

      requestDocumentIds = new TestRequestDocumentIds(documentSetId, idSupplier.ref)
    }

    class TestRequestDocumentIds(override val documentSetId: Long, override val documentIdSupplier: ActorRef) extends RequestDocumentIds {
      override protected val executor: ExecutionContext = implicitly
      override protected val documentData = Seq.fill(2)(inputData)
      override protected val nextStep = NextStep
      override protected val filename = "file"
    }

    class SingleIdSupplier extends TestActor.AutoPilot {
      override def run(sender: ActorRef, msg: Any) =
        msg match {
          case RequestIds(`documentSetId`, n) => {
            sender ! IdRequestResponse(Seq.fill(n)(documentId))
            TestActor.KeepRunning
          }
        }

    }
  }

  case class NextStep(documents: Seq[Document]) extends TaskStep {
    override def execute = Future.successful(this)
  }

}
