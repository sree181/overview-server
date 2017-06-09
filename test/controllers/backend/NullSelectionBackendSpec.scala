package controllers.backend

import java.util.Date
import org.specs2.mock.Mockito
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import com.overviewdocs.query.Field
import com.overviewdocs.searchindex.SearchWarning
import models.{InMemorySelection,Selection,SelectionRequest,SelectionWarning}

class NullSelectionBackendSpec extends NullBackendSpecification with Mockito {
  trait BaseScope extends NullScope {
    def resultIds: Array[Long] = Array.empty
    def warnings: List[SelectionWarning] = Nil
    val dsBackend = mock[DocumentSelectionBackend]
    val backend = new NullSelectionBackend(dsBackend)
    dsBackend.createSelection(any[SelectionRequest]) returns Future.successful(InMemorySelection(resultIds, warnings))

    val userEmail: String = "user@example.org"
    val documentSetId: Long = 1L
  }

  "NullSelectionBackend" should {
    "#create" should {
      trait CreateScope extends BaseScope {
        lazy val request = SelectionRequest(documentSetId, Seq(), Seq(), Seq(), Seq(), None, None)
        def create = await(backend.create(userEmail, request))
        lazy val result = create
      }

      "return a Selection with the returned document IDs" in new CreateScope {
        override def resultIds = Array(1L, 2L, 3L)
        await(result.getAllDocumentIds) must beEqualTo(Array(1L, 2L, 3L))
      }

      "return warnings" in new CreateScope {
        override def warnings = SelectionWarning.SearchIndexWarning(SearchWarning.TooManyExpansions(Field.Text, "foo", 2)) :: Nil
        result.warnings must beEqualTo(warnings)
      }

      "return a different Selection each time" in new CreateScope {
        create.id must not(beEqualTo(create.id))
      }

      "pass the SelectionRequest to the dsBackend" in new CreateScope {
        create
        there was one(dsBackend).createSelection(request)
      }

      "pass a failure back" in new CreateScope {
        val t = new Throwable("moo")
        dsBackend.createSelection(any[SelectionRequest]) returns Future.failed(t)
        create must throwA[Throwable](message="moo")
      }
    }
  }
}
