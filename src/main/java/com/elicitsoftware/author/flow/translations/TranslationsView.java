package com.elicitsoftware.author.flow.translations;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.LanguageTags;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.author.survey.TranslationBudget;
import com.elicitsoftware.author.survey.TranslationHandoff;
import com.elicitsoftware.author.survey.TranslationService;
import com.elicitsoftware.author.survey.TranslationService.Row;
import com.elicitsoftware.author.survey.TranslationService.Status;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UC-043: one page listing every translatable string of a survey beside its translation in a
 * chosen language, with its status.
 * <p>
 * The grid is for reviewing and fixing single strings; the bulk path is the hand-off file
 * (UC-045, UC-046), which is why the two buttons sit at the top of it. Language tabs inside the
 * five editor dialogs were rejected for this: each is a Binder over a detached working bean with a
 * caller-supplied persist function, so per-language fields would mean a second bean and a second
 * persist path in five places, and a translator would have to open every dialog to work through a
 * survey.
 */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)/translations", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class TranslationsView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    @Inject
    SurveyService surveyService;

    @Inject
    TranslationService translations;

    @Inject
    TranslationHandoff handoff;

    final H2 heading = new H2();
    final Button overview = new Button(VaadinIcon.ARROW_LEFT.create());
    final ComboBox<String> language = new ComboBox<>();
    /** UC-044 A2: takes the selected language out of the published set. */
    final Button withdraw = new Button();
    /** Open while the author confirms a withdrawal; for tests. */
    ConfirmDialog withdrawConfirm;
    final Checkbox onlyOutstanding = new Checkbox();
    final Span counts = new Span();
    final Grid<Row> grid = new Grid<>();
    final Button request = new Button();
    /**
     * The returned hand-off file arrives whole: the handler hands the bytes to the UI thread, where
     * the import and the result dialog run (UC-046). Upload performs the request outside the UI
     * lock, so the callback must go through {@code ui.access}; ImportDefinitionView does the same.
     */
    final Upload upload = new Upload(UploadHandler.inMemory((metadata, data) ->
            getUI().ifPresent(ui -> ui.access(() -> importReturned(data)))));

    Survey survey;
    List<Row> rows = new ArrayList<>();
    /** The languages the survey publishes, to tell an offered language from an adopted one. */
    Set<String> publishedTags = new HashSet<>();
    /** Set while this view is changing the picker itself, so its own listeners stand down. */
    private boolean updating;
    /** The listeners are registered once; {@code build()} runs again on every navigation in. */
    private boolean wired;

    public TranslationsView() {
        setWidthFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("translationsView.pageTitle");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int id = event.getRouteParameters().getInteger(SurveyEditorView.SURVEY_ID).orElse(-1);
        Survey found = surveyService.find(id).orElse(null);
        if (found == null) {
            event.rerouteToError(NotFoundException.class, getTranslation("surveyDesignerView.noSurvey", String.valueOf(id)));
            return;
        }
        survey = found;
        build();
    }

    private void build() {
        removeAll();
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        overview.setTooltipText(getTranslation("translationsView.backToOverview"));
        heading.setText(getTranslation("translationsView.heading",
                survey.title == null || survey.title.isBlank() ? survey.name : survey.title));
        heading.getElement().setAttribute("data-i18n-content", ""); // the survey's own title

        List<String> published = translations.contentLanguages(survey);
        publishedTags = new HashSet<>(published);

        // Every language the survey could be translated into, not only the ones it already
        // publishes: choosing one of the others publishes it (UC-044), which is what lets a
        // translation be started from the page where translating is done. The list is the curated
        // set (author.content.languages) -- Elicit only renders a survey in a language it also has
        // application text for, so offering more would invite work no respondent could ever see.
        // Custom values stay allowed, not to accept them but so that a tag typed anyway reaches
        // publish() and is refused with the route to getting one, rather than being ignored.
        language.setLabel(getTranslation("translationsView.language"));
        language.setPlaceholder(getTranslation("translationsView.language.placeholder"));
        language.setHelperText(getTranslation("translationsView.language.helper"));
        language.setAllowCustomValue(true);
        language.setItemLabelGenerator(this::languageLabel);
        // Wide enough for the placeholder and for a label like "es-419 - Spanish (Latin America)";
        // the default width truncates both and wraps the helper text onto two lines.
        language.setMinWidth("22em");
        updating = true;
        try {
            // A mutable copy: select() appends a typed tag through the list data view, which
            // writes into the collection setItems was handed.
            language.setItems(new ArrayList<>(translations.offerableLanguages(survey)));
            language.setValue(published.isEmpty() ? null : published.get(0));
        } finally {
            updating = false;
        }

        withdraw.setText(getTranslation("translationsView.withdraw"));
        withdraw.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        onlyOutstanding.setLabel(getTranslation("translationsView.onlyOutstanding"));

        request.setText(getTranslation("translationsView.request"));
        request.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Extension and MIME type are separate constraints, both enforced server-side as well as
        // in the file chooser; the hand-off file is JSON and is named *.json by the request button.
        upload.setAcceptedFileExtensions(".json");
        upload.setAcceptedMimeTypes("application/json");
        // UC-046 A4, NFR-023: refused before it is sent; TranslationHandoff checks what arrives.
        upload.setMaxFileSize(TranslationHandoff.MAX_FILE_BYTES);
        upload.setMaxFiles(1);
        upload.setDropAllowed(false);
        upload.setUploadButton(new Button(getTranslation("translationsView.import")));

        wireOnce();
        buildGrid();

        HorizontalLayout controls = new HorizontalLayout(language, withdraw, onlyOutstanding, request, upload);
        controls.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        controls.setWidthFull();

        Paragraph intro = new Paragraph(getTranslation("translationsView.intro"));
        counts.addClassName(LumoUtility.TextColor.SECONDARY);

        add(new HorizontalLayout(overview, heading), intro, controls, counts, grid);
        load();
    }

    /**
     * Listeners are registered exactly once. {@code build()} runs from {@code beforeEnter}, which
     * fires again on every navigation into a view instance the router has kept, so registering here
     * would stack a second copy of each listener and run {@code load()} once per copy.
     */
    private void wireOnce() {
        if (wired) {
            return;
        }
        wired = true;
        overview.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        language.addValueChangeListener(e -> {
            if (updating) {
                return;
            }
            String tag = e.getValue();
            if (tag == null || publishedTags.contains(tag)) {
                load();
            } else {
                publish(tag, e.getOldValue());
            }
        });
        // A tag the list does not carry is typed straight into the box, as a reporting dimension and
        // a rule token already are (TagDialog, RelationshipDialog).
        language.addCustomValueSetListener(e -> {
            if (!updating) {
                publish(e.getDetail(), language.getValue());
            }
        });
        onlyOutstanding.addValueChangeListener(e -> load());
        request.addClickListener(e -> downloadRequest());
        withdraw.addClickListener(e -> confirmWithdraw(language.getValue()));
    }

    /**
     * {@code fr — French}: the tag, because it is what a site's mount has to match, and the name.
     * <p>
     * The "not published yet" note is only added when the survey publishes something, where it
     * separates the author's own languages from the rest. On a survey that publishes nothing it
     * would sit on every row alike and say nothing.
     */
    private String languageLabel(String tag) {
        String key;
        if (!translations.isSupportedLanguage(tag)) {
            // Only reachable for a tag the survey already publishes -- an imported definition, or
            // one added before the set was curated. It stays in the list so that it can be seen
            // and taken back out (UC-044 BR-004).
            key = "translationsView.language.option.unsupported";
        } else if (publishedTags.isEmpty() || publishedTags.contains(tag)) {
            key = "translationsView.language.option";
        } else {
            key = "translationsView.language.option.new";
        }
        return getTranslation(key, tag, LanguageTags.displayName(tag, getLocale()));
    }

    /**
     * Adds the language to the survey's published set and shows it (UC-044 step 4, then UC-043
     * step 3). A tag the survey already publishes is simply selected.
     * <p>
     * Package-private so that the typed-tag paths can be tested without simulating the client's
     * {@code custom-value-set} event, which is the one line that reaches this.
     *
     * @param typed    the chosen or typed tag
     * @param previous the value to fall back to when the tag is refused
     */
    void publish(String typed, String previous) {
        String canonical = LanguageTags.canonical(typed);
        if (canonical != null && publishedTags.contains(canonical)) {
            select(canonical);
            load();
            return;
        }
        try {
            survey = surveyService.publishLanguage(survey.id, typed);
            publishedTags = new HashSet<>(translations.contentLanguages(survey));
            select(canonical);
            load();
            // UC-044 A1: publishing an incomplete language is allowed, and says how much of it
            // will fall back to the survey's own language at every site until it is translated.
            Notification.show(getTranslation("translationsView.language.published",
                            canonical, String.valueOf(translations.counts(survey.id, canonical).missing())),
                            6000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (AuthoringException e) {
            Notification n = Notification.show(e.getMessage(), 6000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            select(previous);
        }
    }

    /**
     * UC-044 A2: says what withdrawing keeps before anything changes. The page is the one place the
     * published set is managed (BR-007), so this is the only way a language leaves it.
     */
    private void confirmWithdraw(String tag) {
        if (tag == null || !publishedTags.contains(tag)) {
            return;
        }
        withdrawConfirm = new ConfirmDialog();
        withdrawConfirm.setHeader(getTranslation("translationsView.withdraw.header", languageName(tag)));
        withdrawConfirm.setText(getTranslation("translationsView.withdraw.text", tag));
        withdrawConfirm.setCancelable(true);
        withdrawConfirm.setCancelText(getTranslation("common.cancel"));
        withdrawConfirm.setConfirmText(getTranslation("translationsView.withdraw"));
        withdrawConfirm.addConfirmListener(e -> withdraw(tag));
        withdrawConfirm.open();
    }

    /**
     * Takes the language out of the published set and rebuilds the picker: the withdrawn language
     * is offered again as not yet published (or not at all, when Elicit does not carry it), and
     * the first language still published is selected, as when the page opens.
     */
    void withdraw(String tag) {
        try {
            survey = surveyService.withdrawLanguage(survey.id, tag);
        } catch (AuthoringException e) {
            Notification.show(e.getMessage(), 6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        List<String> published = translations.contentLanguages(survey);
        publishedTags = new HashSet<>(published);
        updating = true;
        try {
            language.setItems(new ArrayList<>(translations.offerableLanguages(survey)));
            language.setValue(published.isEmpty() ? null : published.get(0));
        } finally {
            updating = false;
        }
        load();
        Notification.show(getTranslation("translationsView.withdrawn", tag), 6000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String languageName(String tag) {
        return getTranslation("translationsView.language.option", tag, LanguageTags.displayName(tag, getLocale()));
    }

    /**
     * Selects a tag without re-entering the listeners. {@code setItems} would reset the value, so a
     * tag new to the list is appended to the data view instead; one already in it is refreshed, so
     * that its label drops the "not published yet" note.
     */
    private void select(String tag) {
        updating = true;
        try {
            if (tag != null) {
                if (language.getListDataView().getItems().anyMatch(tag::equals)) {
                    language.getListDataView().refreshItem(tag);
                } else {
                    language.getListDataView().addItem(tag);
                }
            }
            // Cleared first, so a rejected tag does not stay behind in the input as typed text.
            language.clear();
            language.setValue(tag);
        } finally {
            updating = false;
        }
    }

    private void buildGrid() {
        grid.removeAllColumns();
        // Base text and translations are authored survey content, not chrome (UC-040 BR-005).
        grid.getElement().setAttribute("data-i18n-content", "");
        grid.addColumn(Row::context).setHeader(getTranslation("translationsView.grid.element")).setAutoWidth(true);
        grid.addColumn(Row::field).setHeader(getTranslation("translationsView.grid.field")).setAutoWidth(true);
        grid.addColumn(Row::baseText).setHeader(getTranslation("translationsView.grid.baseText")).setFlexGrow(1);
        grid.addComponentColumn(this::translationField)
                .setHeader(getTranslation("translationsView.grid.translation")).setFlexGrow(1);
        grid.addComponentColumn(this::statusBadge)
                .setHeader(getTranslation("translationsView.grid.status")).setAutoWidth(true);
        grid.setAllRowsVisible(true);
    }

    /** The editable cell. Saved on blur, so a translator can work down the column. */
    private TextArea translationField(Row row) {
        TextArea field = new TextArea();
        field.setValue(row.translation() == null ? "" : row.translation());
        field.setWidthFull();
        field.setMaxRows(4);
        // The same budget the hand-off file states and the import enforces (UC-045 BR-004).
        field.setMaxLength(TranslationBudget.of(row.elementType(), row.field(), row.baseText()));
        field.addBlurListener(e -> save(row, field.getValue()));
        return field;
    }

    private Span statusBadge(Row row) {
        Span badge = new Span(switch (row.status()) {
            case TRANSLATED -> getTranslation("translationsView.status.translated");
            case MISSING -> getTranslation("translationsView.status.missing");
            case STALE -> getTranslation("translationsView.status.stale");
        });
        badge.getElement().getThemeList().add(switch (row.status()) { // i18n:ignore-start (Lumo theme names)
            case TRANSLATED -> "badge success";
            case MISSING -> "badge contrast";
            case STALE -> "badge error";
        }); // i18n:ignore-end
        if (row.status() == Status.STALE && row.previousBase() != null) {
            // Span is not HasTooltip; the title attribute is what a plain element offers, and the
            // sweep test reads it, which is why the wording is a chrome key.
            badge.getElement().setAttribute("title",
                    getTranslation("translationsView.status.staleTooltip", row.previousBase()));
        }
        return badge;
    }

    void load() {
        String tag = language.getValue();
        // A survey that publishes nothing yet selects nothing: the picker is the invitation, and
        // there is no language to count or to hand to a translator until one is chosen.
        if (tag == null) {
            rows = new ArrayList<>();
            counts.setText(getTranslation("translationsView.noLanguages"));
            grid.setItems(rows);
            // Hidden rather than disabled: Aura renders a disabled primary button at full opacity,
            // so it would read as a live control that silently does nothing.
            request.setVisible(false);
            upload.setVisible(false);
            withdraw.setVisible(false);
            return;
        }
        request.setVisible(true);
        upload.setVisible(true);
        withdraw.setVisible(publishedTags.contains(tag));
        rows = translations.rows(survey.id, tag);
        TranslationService.Counts c = translations.counts(survey.id, tag);
        counts.setText(getTranslation("translationsView.counts",
                String.valueOf(c.translated()), String.valueOf(c.missing()), String.valueOf(c.stale())));
        grid.setItems(onlyOutstanding.getValue()
                ? rows.stream().filter(r -> r.status() != Status.TRANSLATED).toList()
                : rows);
    }

    private void save(Row row, String value) {
        String current = row.translation() == null ? "" : row.translation();
        if (value.equals(current)) {
            return;
        }
        try {
            translations.upsert(survey.id, row.elementType(), row.targetKey(), row.field(),
                    language.getValue(), value);
            load();
        } catch (AuthoringException e) {
            Notification n = Notification.show(e.getMessage(), 6000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void downloadRequest() {
        String tag = language.getValue();
        byte[] bytes = handoff.export(survey, tag, onlyOutstanding.getValue());
        String fileName = handoff.fileName(survey, tag);
        Anchor download = new Anchor(DownloadHandler.fromInputStream(ev ->
                new DownloadResponse(new ByteArrayInputStream(bytes), fileName, "application/json", bytes.length)), "");
        download.getElement().setAttribute("download", true);
        download.getStyle().set("display", "none");
        add(download);
        download.getElement().callJsFunction("click");
    }

    /** Package-private so tests can feed a returned file without the browser upload. */
    void importReturned(byte[] data) {
        try {
            TranslationHandoff.ImportResult result = handoff.importFile(survey, new ByteArrayInputStream(data));
            new TranslationImportResultDialog(result).open();
            load();
        } catch (AuthoringException e) {
            Notification n = Notification.show(e.getMessage(), 8000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            upload.clearFileList();
        }
    }
}
