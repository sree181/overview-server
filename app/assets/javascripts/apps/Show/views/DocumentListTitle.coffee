define [ 'jquery', 'underscore', 'backbone', 'i18n' ], ($, _, Backbone, i18n) ->
  t = i18n.namespaced('views.DocumentSet.show.DocumentListTitle')

  # Shows what's currently selected
  #
  # Usage:
  #
  #   state = new State(...)
  #   view = new DocumentListTitleView(state: state)
  #
  # Listens to State's `change:document-list`. Doesn't call anything or emit
  # any events.
  class DocumentListTitleView extends Backbone.View
    tagName: 'h3'

    events:
      'click a[data-sort-by-metadata-field]': 'onClickSortByMetadataField'

    templates:
      main: _.template('''
        <h3><%= t('title_html', length) %></h3>
        <div class="sort-by"><%= t('sort_by_FIELD').replace('FIELD', sortByFieldHtml) %></div>
      ''')

      sortByField: _.template('''
        <div class="sort-by-field dropdown">
          <a id="DocumentListTitle-sort-by-field" data-target="#" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
            <%- currentSortBy %>
            <span class="caret"></span>
          </a>
          <ul class="dropdown-menu" role="menu" aria-labelledby="DocumentListTitle-sort-by-field">
            <li><a href="#" data-sort-by-metadata-field=""><%- sortByTitle %></a></li>
            <% metadataFields.forEach(function(field) { %>
              <li><a href="#" data-sort-by-metadata-field="<%- field %>"><%- field %></a></li>
            <% }) %>
          </ul>
        </div>
      ''')

    initialize: ->
      throw new Error('Must supply options.state, a State') if !@options.state

      @state = @options.state
      @documentSet = @state.documentSet
      @listenTo(@documentSet, 'change:metadataFields', @render)
      @listenTo(@state, 'change:documentList', @_attachDocumentList)

      @_attachDocumentList()

    _attachDocumentList: ->
      documentList = @state.get('documentList')

      if documentList != @documentList
        @stopListening(@documentList) if @documentList
        @documentList = documentList
        @listenTo(@documentList, 'change:length', @render) if @documentList
        @render()

    render: ->
      length = @documentList?.get('length')

      if length?
        fields = @documentSet.get('metadataFields')
        sortByTitle = t('sort_by.title')
        sortByFieldHtml = @templates.sortByField
          currentSortBy: sortByTitle
          sortByTitle: sortByTitle
          metadataFields: fields
        @$el.html(@templates.main(t: t, length: length, sortByFieldHtml: sortByFieldHtml))
      else
        @$el.text(t('loading'))

    onClickSortByMetadataField: (ev) ->
      ev.preventDefault()

      oldParams = @state.get('documentList')?.params
      return if !oldParams
      @state.setDocumentListParams(oldParams.sortedByMetadataField(ev.target.getAttribute('data-sort-by-metadata-field')))
