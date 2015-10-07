package com.overviewdocs.jobhandler.filegroup.task.process

import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import com.overviewdocs.jobhandler.filegroup.task.step.DocumentWithoutIds
import com.overviewdocs.jobhandler.filegroup.task.step.RequestDocumentIds
import com.overviewdocs.jobhandler.filegroup.task.step.TaskStep
import com.overviewdocs.models.Document


object DoRequestDocumentIds {
  def apply(documentIdSupplier: ActorRef, documentSetId: Long, filename: String)(implicit executor: ExecutionContext) =
    new StepGenerator[Seq[DocumentWithoutIds], Seq[Document]] {
      override def generate(d: Seq[DocumentWithoutIds]): TaskStep = {
        RequestDocumentIds(documentIdSupplier, documentSetId, filename, d, nextStepFn)
      }
    }
}
