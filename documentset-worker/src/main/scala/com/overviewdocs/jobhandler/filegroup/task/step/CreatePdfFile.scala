package com.overviewdocs.jobhandler.filegroup.task.step

import java.nio.file.{Files=>JFiles,Path}
import java.security.{DigestInputStream,MessageDigest}
import java.util.Locale
import org.overviewproject.pdfocr.PdfOcr
import scala.concurrent.{ExecutionContext,Future,blocking}

import com.overviewdocs.blobstorage.{BlobBucketId,BlobStorage}
import com.overviewdocs.database.HasBlockingDatabase
import com.overviewdocs.jobhandler.filegroup.task.FilePipelineParameters
import com.overviewdocs.models.{File,GroupedFileUpload,TempDocumentSetFile}
import com.overviewdocs.models.tables.{Files,TempDocumentSetFiles}
import com.overviewdocs.postgres.LargeObjectInputStream

/** Creates a [[File]] from a PDF document.
  *
  * Does this:
  *
  * 1. Downloads the file from Postgres LargeObject and calculates its sha1.
  * 2. Makes a searchable copy, using PdfOcr.
  * 3. Uploads both copies to BlobStorage.
  * 4. Writes a File and a TempDocumentSetFile to Postgres.
  * 5. Returns the File.
  *
  * If there's a recoverable error (i.e., the file is an invalid or
  * password-protected PDF), returns a String error message.
  *
  * TODO share some code with CreateOfficeFile.scala
  */
class CreatePdfFile(params: FilePipelineParameters)(implicit ec: ExecutionContext) extends HasBlockingDatabase {
  import database.api._

  private val CopyBufferSize = 1024 * 1024 * 5 // Copy 5MB at a time from database

  protected val blobStorage: BlobStorage = BlobStorage

  private def downloadLargeObjectAndCalculateSha1(destination: Path): Future[Array[Byte]] = {
    Future(blocking(JFiles.newOutputStream(destination))).flatMap { outputStream =>
      val loStream = new LargeObjectInputStream(params.inputOid, blockingDatabase)
      val digest = MessageDigest.getInstance("SHA-1")
      val digestStream = new DigestInputStream(loStream, digest)

      val buf = new Array[Byte](CopyBufferSize)
      def step: Future[Unit] = {
        Future(blocking(loStream.read(buf))).flatMap { nBytes =>
          if (nBytes == -1) {
            Future.successful(())
          } else {
            Future(blocking(outputStream.write(buf))).flatMap(_ => step)
          }
        }
      }

      for {
        _ <- step
        _ <- Future(blocking(outputStream.close))
      } yield digest.digest
    }
  }

  private def dummyProgress(nPages: Int, nTotalPages: Int): Future[Unit] = Future.successful(())

  private def withTempFiles[A](f: (Path, Path) => Future[A]): Future[A] = {
    Future(blocking {
      (JFiles.createTempFile("create-pdf-file-raw", ".pdf"), JFiles.createTempFile("create-pdf-file-pdf", ".pdf"))
    }).flatMap { case (rawPath, pdfPath) =>
      def delete: Future[Unit] = {
        Future(blocking {
          JFiles.delete(rawPath)
          JFiles.delete(pdfPath)
          ()
        })
      }

      f(rawPath, pdfPath)
        .recoverWith[A] { case ex: Exception => delete.flatMap(_ => Future.failed[A](ex)) }
        .flatMap(a => delete.map(_ => a))
    }
  }

  def execute: Future[Either[String,File]] = {
    withTempFiles { (rawPath, pdfPath) =>
      for {
        sha1 <- downloadLargeObjectAndCalculateSha1(rawPath)
        _ <- PdfOcr.makeSearchablePdf(rawPath, pdfPath, params.ocrLocales, dummyProgress)
        pdfNBytes <- Future(blocking(JFiles.size(pdfPath)))
        rawLocation <- BlobStorage.create(BlobBucketId.FileContents, rawPath)
        pdfLocation <- BlobStorage.create(BlobBucketId.FileContents, pdfPath)
        file <- writeDatabase(rawLocation, sha1, pdfLocation, pdfNBytes)
      } yield Right(file)
    }
  }

  private lazy val fileInserter = {
    Files
      .map(f => (f.referenceCount, f.name, f.contentsLocation, f.contentsSize, f.contentsSha1, f.viewLocation, f.viewSize))
      .returning(Files)
  }

  private lazy val tempDocumentSetFileInserter = (TempDocumentSetFiles returning TempDocumentSetFiles)

  private def writeDatabase(rawLocation: String, sha1: Array[Byte], pdfLocation: String, pdfNBytes: Long): Future[File] = {
    database.run((for {
      file <- fileInserter.+=((1, params.filename, rawLocation, params.inputSize, sha1, pdfLocation, pdfNBytes))
      _ <- TempDocumentSetFiles.+=(TempDocumentSetFile(params.documentSetId, file.id))
    } yield file).transactionally)
  }
}

object CreatePdfFile {
  def apply(params: FilePipelineParameters)(implicit ec: ExecutionContext): Future[Either[String,File]] = {
    new CreatePdfFile(params).execute
  }
}
