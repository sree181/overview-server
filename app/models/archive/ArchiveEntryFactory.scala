package models.archive

import models.DocumentFileInfo
import java.io.InputStream
import org.overviewproject.tree.orm.File
import controllers.util.PlayLargeObjectInputStream

trait ArchiveEntryFactory {

  def create(document: DocumentFileInfo): Option[ArchiveEntry] = {
    
    for {
      fileId <- document.fileId
      file <- storage.findFile(fileId)
      size <- file.contentsSize
    } yield {
      ArchiveEntry(file.name, size, largeObjectInputStream(file.contentsOid) _)
    }
  }
  
  protected val storage: Storage 
  
  protected trait Storage {
    def findFile(fileId: Long): Option[File]
  }
  
  private def largeObjectInputStream(oid: Long)(): InputStream = new PlayLargeObjectInputStream(oid)
}