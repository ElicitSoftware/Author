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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

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
    final Select<String> language = new Select<>();
    final Checkbox onlyOutstanding = new Checkbox();
    final Span counts = new Span();
    final Grid<Row> grid = new Grid<>();
    final Button request = new Button();
    final MemoryBuffer uploadBuffer = new MemoryBuffer();
    final Upload upload = new Upload(uploadBuffer);

    Survey survey;
    List<Row> rows = new ArrayList<>();

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
            event.rerouteToError(NotFoundException.class, "No survey with id " + id);
            return;
        }
        survey = found;
        build();
    }

    private void build() {
        removeAll();
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        overview.setTooltipText(getTranslation("translationsView.backToOverview"));
        overview.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        heading.setText(getTranslation("translationsView.heading",
                survey.title == null || survey.title.isBlank() ? survey.name : survey.title));
        heading.getElement().setAttribute("data-i18n-content", ""); // the survey's own title

        List<String> published = translations.contentLanguages(survey);
        if (published.isEmpty()) {
            add(new HorizontalLayout(overview, heading));
            Paragraph none = new Paragraph(getTranslation("translationsView.noLanguages"));
            Button openMetadata = new Button(getTranslation("translationsView.manageLanguages"),
                    e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                            new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
            openMetadata.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(none, openMetadata);
            return;
        }

        language.setLabel(getTranslation("translationsView.language"));
        language.setItems(published);
        if (language.getValue() == null || !published.contains(language.getValue())) {
            language.setValue(published.get(0));
        }
        language.addValueChangeListener(e -> load());

        onlyOutstanding.setLabel(getTranslation("translationsView.onlyOutstanding"));
        onlyOutstanding.addValueChangeListener(e -> load());

        request.setText(getTranslation("translationsView.request"));
        request.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        request.addClickListener(e -> downloadRequest());

        upload.setAcceptedFileTypes("application/json", ".json");
        upload.setMaxFiles(1);
        upload.setDropAllowed(false);
        upload.setUploadButton(new Button(getTranslation("translationsView.import")));
        upload.addSucceededListener(e -> importReturned());

        buildGrid();

        HorizontalLayout controls = new HorizontalLayout(language, onlyOutstanding, request, upload);
        controls.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        controls.setWidthFull();

        Paragraph intro = new Paragraph(getTranslation("translationsView.intro"));
        counts.addClassName(LumoUtility.TextColor.SECONDARY);

        add(new HorizontalLayout(overview, heading), intro, controls, counts, grid);
        load();
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
        StreamResource resource = new StreamResource(handoff.fileName(survey, tag),
                () -> new ByteArrayInputStream(bytes));
        Anchor download = new Anchor(resource, "");
        download.getElement().setAttribute("download", true);
        download.getStyle().set("display", "none");
        add(download);
        download.getElement().callJsFunction("click");
    }

    private void importReturned() {
        try {
            TranslationHandoff.ImportResult result = handoff.importFile(survey, uploadBuffer.getInputStream());
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
