Deferred = jQuery.Deferred

# Stores a possibly-incomplete list of selected documents
#
# When you create a DocumentList (from a Store, a Selection and a
# NeedsResolver), its @documents property is empty and @n is undefined.
# Call get_placeholder_documents() to get some documents we know to exist that
# match the selection; call .slice() to get a Deferred that will (or has been)
# resolved to the Documents. When documents have been found, @documents will
# be populated with this (possibly-incomplete) list, and @n will be the total
# number of documents.
class DocumentList
  constructor: (@store, @selection, @resolver) ->
    @documents = []
    @deferreds = {}
    @n = undefined

  get_placeholder_documents: () ->
    ids_so_far = (document.id? && document.id || document for document in (@selection.documents || []))
    excludes_something = @selection.documents?.length

    for key in [ 'tags', 'nodes' ]
      store = @store[key]
      for object in @selection[key]
        object = store.get(object) if !object.id?

        return [] if !object?.doclist?

        doclist = object.doclist

        if !excludes_something
          ids_so_far = doclist.docids
          excludes_something = true
        else
          new_ids_so_far = []
          for docid in (doclist.docids || [])
            if ids_so_far.indexOf(docid) >= 0
              new_ids_so_far.push(docid)
          ids_so_far = new_ids_so_far

    (@store.documents.get(docid) for docid in ids_so_far)

  # Returns a Deferred which, when resolved, will be a slice of this.documents
  slice: (start=0, end=20) ->
    deferred_key = "#{start}..#{end}"

    return @deferreds[deferred_key] if @deferreds[deferred_key]?

    deferred = if end < @documents.length
      new Deferred().resolve(@documents.slice(start, end))
    else
      @resolver.get_selection_documents_slice(@selection, start, end)

    deferred.done (documents, n) =>
      for document, i in documents
        @documents[start+i] = document
      @n = n

    @deferreds[deferred_key] = deferred.pipe((documents, n) -> documents)


exports = require.make_export_object('models/document_list')
exports.DocumentList = DocumentList
