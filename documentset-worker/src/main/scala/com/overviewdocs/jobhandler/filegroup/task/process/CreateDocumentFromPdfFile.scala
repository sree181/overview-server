package com.overviewdocs.jobhandler.filegroup.task.process

import scala.concurrent.ExecutionContext
import com.overviewdocs.util.BulkDocumentWriter
import akka.actor.ActorRef

object CreateDocumentFromPdfFile {
  def apply(documentSetId: Long, filename: String,
            documentIdSupplier: ActorRef, bulkDocumentWriter: BulkDocumentWriter)(implicit executor: ExecutionContext) =
    new UploadedFileProcess {
      override protected val steps =
        DoCreatePdfFile(documentSetId, filename).andThen(
          DoExtractTextFromPdf(documentSetId).andThen(
            DoRequestDocumentIds(documentIdSupplier, documentSetId, filename).andThen(
              DoWriteDocuments(documentSetId, filename, bulkDocumentWriter))))

    }
}