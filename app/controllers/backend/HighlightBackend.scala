package controllers.backend

import akka.actor.{ActorSelection,ActorSystem}
import com.google.inject.ImplementedBy
import com.typesafe.config.ConfigFactory
import javax.inject.{Inject,Singleton}
import scala.concurrent.Future

import com.overviewdocs.messages.DocumentSetReadCommands
import com.overviewdocs.query.Query
import com.overviewdocs.searchindex.{Utf16Highlight,Utf16Snippet}
import modules.RemoteActorSystemModule

/** Finds highlights of a search term in a document.
  */
@ImplementedBy(classOf[RemoteActorHighlightBackend])
trait HighlightBackend extends Backend {
  /** Lists all highlights of a given term in the document.
    *
    * The query may be well-formed but be inappropriate for the given document
    * (e.g., a boolean query on too many terms). In that case, the server will
    * respond with a Left.
    *
    * @param documentSetId DocumentSet ID
    * @param documentId Document ID
    * @param query Parsed search query
    */
  def highlight(documentSetId: Long, documentId: Long, query: Query): Future[Seq[Utf16Highlight]]

  /** Lists short phrases matching the given query in each document.
    *
    * The query may be well-formed but be inappropriate for the given document
    * (e.g., a boolean query on too many terms). In that case, the server will
    * respond with a Left.
    *
    * @param documentSetId DocumentSet ID
    * @param documentIds Document ID
    * @param query Parsed search query
    */
  def highlights(documentSetId: Long, documentIds: Seq[Long], query: Query): Future[Map[Long, Seq[Utf16Snippet]]]
}

/** Akka RemoteActor-backed search backend.
  */
@Singleton
class RemoteActorHighlightBackend @Inject() (remoteActorSystemModule: RemoteActorSystemModule)
extends HighlightBackend {
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  private implicit val system = remoteActorSystemModule.remoteActorSystem
  private implicit val timeout = Timeout(30.seconds)
  private val workerActor = remoteActorSystemModule.workerActor

  override def highlight(documentSetId: Long, documentId: Long, query: Query) = {
    workerActor.ask(DocumentSetReadCommands.Highlight(documentSetId, documentId, query)).mapTo[Seq[Utf16Highlight]]
  }

  override def highlights(documentSetId: Long, documentIds: Seq[Long], query: Query) = {
    workerActor.ask(DocumentSetReadCommands.Highlights(documentSetId, documentIds, query)).mapTo[Map[Long, Seq[Utf16Snippet]]]
  }
}
