package com.elicitsoftware.author.flow.designer;

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
import com.elicitsoftware.author.flow.ElementView;
import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.flow.SurveyModelGuideView;
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.DesignerAddress;
import com.elicitsoftware.author.survey.DesignerService;
import com.elicitsoftware.author.survey.ElementService;
import com.elicitsoftware.author.survey.RelationshipQuery;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.ListService;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.author.flow.reporting.ElementTagsDialog;
import com.elicitsoftware.author.survey.RuleService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.author.survey.TokenSamples;
import com.elicitsoftware.author.survey.StaleTranslationLedger;
import com.elicitsoftware.author.flow.translations.TranslationsView;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The designer board (UC-011, UC-015, UC-016, UC-018): steps as lanes, sections as collapsible
 * cards, questions as rows; every glyph a drag handle, every drag with a menu equivalent. The
 * board is rebuilt from {@link SurveyStructureQuery} after each change; selection is mirrored in
 * the breadcrumb, the drawer tree and the address (UC-006 BR-003).
 */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)/design", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveyDesignerView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    public static final String SELECT = "select";
    /** What {@code dragging-<what>} says when a rule glyph is in flight; the kinds use their own names. */
    private static final String RULE_DRAG = "rule";

    @Inject
    SurveyService surveyService;

    @Inject
    SurveyStructureQuery structureQuery;

    @Inject
    StaleTranslationLedger staleTranslations;

    @Inject
    DesignerService designer;

    @Inject
    ElementService elements;

    @Inject
    RuleService rules;

    @Inject
    RelationshipQuery relationshipQuery;

    @Inject
    ReportingService reporting;

    @Inject
    ListService listService;

    private static final ObjectMapper JSON = new ObjectMapper();

    Survey survey;
    SurveyStructureQuery.Tree tree;
    StructureNode selected;
    // Open (expanded) step and section nodes, keyed by kind so a step id and an assignment id
    // of the same number never collide. Steps have a null assignmentId, so they key on the step id.
    final Set<String> openNodes = new HashSet<>();

    final Div breadcrumb = new Div();
    final Div board = new Div();
    final Checkbox showRemoved = new Checkbox();
    final Checkbox showRules = new Checkbox(true);
    final Checkbox showPreview = new Checkbox(true);
    /** The board and, beside it, the respondent's view of the selection (the right-hand pane). */
    final Div split = new Div();
    final SectionPreview preview = new SectionPreview();
    final RuleArrowsOverlay overlay = new RuleArrowsOverlay();
    final Button overview = new Button(VaadinIcon.ARROW_LEFT.create());
    final Button addStep = new Button(VaadinIcon.PLUS.create());
    final List<StepLane> lanes = new ArrayList<>();
    DesignerPalette palette;
    DesignerToolbox toolbox;
    /** Reporting-tag labels per board node ("ss-"/"sq-" + assignment id), rebuilt with the board (UC-021). */
    final Map<String, List<String>> tagLabels = new java.util.HashMap<>();

    public SurveyDesignerView() {
        addClassName("designer");
        breadcrumb.addClassName("designer-breadcrumb");
        showRemoved.setLabel(getTranslation("surveyDesignerView.showRemoved"));
        showRules.setLabel(getTranslation("surveyDesignerView.showRules"));
        showPreview.setLabel(getTranslation("surveyDesignerView.showPreview"));
        overview.setText(getTranslation("surveyDesignerView.overview"));
        addStep.setText(getTranslation("surveyDesignerView.addStep"));
        breadcrumb.getElement().setAttribute("aria-label", getTranslation("surveyDesignerView.breadcrumb.aria"));
        board.addClassName("designer-board");
        showRemoved.addValueChangeListener(e -> load());
        showRules.addValueChangeListener(e -> render());
        showPreview.addValueChangeListener(e -> {
            preview.setVisible(Boolean.TRUE.equals(e.getValue()));
            refreshPreview(currentQuestions());
        });
        split.addClassName("designer-split");
        split.add(board, preview);
        overlay.addConnectRequestedListener(e -> connectRequested(e.sourceAnchor(), e.targetAnchor(), e.mountAnchor(), e.action()));
        overlay.addEdgeSelectedListener(e -> editRule(e.edgeId()));
        overlay.addEdgeRepointedListener(e -> repoint(e.edgeId(), e.end(), e.targetAnchor(), e.mountAnchor()));
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        overview.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        // Keyboard-reachable way to add a step (NFR-012): the palette glyph is drag-only.
        addStep.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addStep.addClickListener(e -> addStepFromSelection());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int id = event.getRouteParameters().getInteger(SurveyEditorView.SURVEY_ID).orElse(-1);
        Optional<Survey> found = surveyService.find(id);
        if (found.isEmpty()) {
            event.rerouteToError(NotFoundException.class, getTranslation("surveyDesignerView.noSurvey", String.valueOf(id)));
            return;
        }
        survey = found.get();
        if (palette == null) {
            palette = new DesignerPalette(this::paletteDragStarted, this::dragEnded);
            toolbox = new DesignerToolbox(this, palette);
            // The palette (add-tools + rule glyphs) now lives in the drawer, placed there by
            // MainLayout.syncWith; the in-view bar keeps only the back link and the view toggles.
            HorizontalLayout toolbar = new HorizontalLayout(overview, addStep, showRules, showRemoved, showPreview);
            toolbar.addClassName("designer-toolbar");
            toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
            toolbar.setWidthFull();
            add(breadcrumb, toolbar, split);
        }
        List<String> select = event.getLocation().getQueryParameters().getParameters().getOrDefault(SELECT, List.of());
        DesignerAddress address = select.isEmpty() ? null : DesignerAddress.parse(select.get(0)).orElse(null);
        tree = structureQuery.tree(survey.id, Boolean.TRUE.equals(showRemoved.getValue()));
        selected = address == null ? null : resolve(address);
        openAncestors(selected);
        render();
        scrollToSelected();
        if (address != null && DesignerAddress.RULE.equals(address.kind())) {
            // UC-007 BR-009: an address naming a rule opens the rule's editor over its target.
            editRule(address.id());
        } else if (address != null && selected == null) {
            // The board holds no such element. It may still be one the survey has but has placed
            // nowhere, which lives in the drawer panels (UC-006 BR-003a).
            revealUnplaced(address);
        }
    }

    /** UC-007 BR-009: show an element the board cannot draw in the panel that does hold it. */
    private void revealUnplaced(DesignerAddress address) {
        if (toolbox == null) {
            return;
        }
        boolean found = switch (address.kind()) {
            case DesignerAddress.SECTION -> toolbox.revealSection(address.id());
            case DesignerAddress.QUESTION -> toolbox.revealQuestion(address.id());
            default -> false;
        };
        if (found) {
            info(getTranslation(DesignerAddress.SECTION.equals(address.kind())
                    ? "surveyDesignerView.revealedUnplacedSection" : "surveyDesignerView.revealedUnplacedQuestion"));
        }
    }

    // ---- state -----------------------------------------------------------------------------

    public Survey survey() {
        return survey;
    }

    public StructureNode selected() {
        return selected;
    }

    /** The tools palette, placed in the drawer by {@link MainLayout} (built in {@code beforeEnter}). */
    public DesignerPalette palette() {
        return palette;
    }

    /** The drawer toolbox: palette, lists and questions panels above the tree. */
    public DesignerToolbox toolbox() {
        return toolbox;
    }

    void load() {
        tree = structureQuery.tree(survey.id, Boolean.TRUE.equals(showRemoved.getValue()));
        selected = selected == null ? null : find(selected.kind(), selected.durableId(), selected.assignmentId());
        openAncestors(selected);
        render();
        warnAboutStaleTranslations();
    }

    /**
     * FR-060: after a save that changed a translated wording, say which languages it invalidated.
     * <p>
     * The notification does not dismiss itself and carries a link to the Translations page, because
     * the consequence is invisible otherwise: every affected string quietly falls back to the base
     * language at every site until it is translated again.
     */
    private void warnAboutStaleTranslations() {
        for (String[] entry : staleTranslations.drain(survey.id)) {
            Notification notification = new Notification();
            notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            notification.setPosition(Notification.Position.BOTTOM_START);
            Button open = new Button(getTranslation("designer.staleTranslations.open"), e -> {
                notification.close();
                getUI().ifPresent(ui -> ui.navigate(TranslationsView.class,
                        new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id))));
            });
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            Button dismiss = new Button(getTranslation("common.close"), e -> notification.close());
            dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            notification.add(new HorizontalLayout(
                    new Span(getTranslation("designer.staleTranslations", entry[0], entry[2], entry[1])),
                    open, dismiss));
            notification.open();
        }
    }

    private void render() {
        rebuildBoard();
        Map<Integer, Question> questions = currentQuestions();
        refreshToolbox(questions);
        refreshPreview(questions);
        updateBreadcrumb();
        syncAddress();
        mainLayout().ifPresent(layout -> {
            layout.refreshStructureTree(); // the drawer tree reads the same tables; keep it in step (UC-006 BR-003)
            layout.syncWith(this);
        });
    }

    private void rebuildBoard() {
        // A drop rebuilds the board, so the dragged element (and its dragend) never comes back.
        dragEnded();
        board.removeAll();
        lanes.clear();
        collectTagLabels();
        List<StructureNode> roots = tree.roots();
        for (int i = 0; i < roots.size(); i++) {
            board.add(new DropZone(new DropSpot(null, i, StructureNode.Kind.STEP), this::onDrop));
            StepLane lane = new StepLane(this, roots.get(i), i + 1, childrenOf(roots.get(i)), i, roots.size());
            lanes.add(lane);
            board.add(lane);
        }
        board.add(new DropZone(new DropSpot(null, roots.size(), StructureNode.Kind.STEP), this::onDrop));
        overlay.setEdges(Boolean.TRUE.equals(showRules.getValue()) ? edgesJson() : "[]");
        overlay.setSelectedAnchor(anchorOf(selected));
        board.add(overlay);
        if (roots.isEmpty()) {
            RouterLink guide = new RouterLink(SurveyModelGuideView.class);
            guide.setText(getTranslation("designer.guideLink"));
            Div empty = new Div(new Span(getTranslation("surveyDesignerView.emptyBoard") + " "), guide);
            empty.addClassName("designer-empty-board");
            board.add(empty);
        }
    }

    private void updateBreadcrumb() {
        breadcrumb.removeAll();
        RouterLink root = new RouterLink(survey.title == null || survey.title.isBlank() ? survey.name : survey.title, SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)));
        root.addClassName("designer-crumb");
        breadcrumb.add(root);
        List<StructureNode> path = new ArrayList<>();
        for (StructureNode n = selected; n != null; n = n.parent()) {
            path.add(0, n);
        }
        for (int i = 0; i < path.size(); i++) {
            StructureNode n = path.get(i);
            Span separator = new Span("›");
            separator.addClassName("designer-crumb-separator");
            separator.getElement().setAttribute("aria-hidden", "true");
            breadcrumb.add(separator);
            if (i == path.size() - 1) {
                Span current = new Span(crumbLabel(n));
                current.addClassNames("designer-crumb", "designer-crumb-current");
                current.getElement().setAttribute("aria-current", "location");
                breadcrumb.add(current);
            } else {
                Button crumb = new Button(crumbLabel(n), e -> select(n));
                crumb.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
                crumb.addClassName("designer-crumb");
                breadcrumb.add(crumb);
            }
        }
    }

    private String crumbLabel(StructureNode n) {
        return switch (n.kind()) {
            case STEP -> getTranslation("surveyDesignerView.crumb.step", n.label());
            case SECTION -> getTranslation("surveyDesignerView.crumb.section", n.label());
            case QUESTION -> getTranslation("surveyDesignerView.crumb.question", n.label());
        };
    }

    @Override
    public String getPageTitle() {
        return getTranslation("surveyDesignerView.pageTitle");
    }

    private void syncAddress() {
        String path = "survey/" + survey.id + "/design" + (selected == null ? "" : "?" + SELECT + "=" + selected.routeKind() + ":" + selected.durableId());
        getUI().ifPresent(ui -> ui.getPage().executeJs("history.replaceState(history.state, '', $0)", path));
    }

    private Optional<MainLayout> mainLayout() {
        UI ui = getUI().orElse(UI.getCurrent());
        return ui == null ? Optional.empty() : ui.getChildren().filter(MainLayout.class::isInstance).map(MainLayout.class::cast).findFirst();
    }

    /** UC-006 BR-003: what the address selects — an element, an element in one container, or a rule's target. */
    private StructureNode resolve(DesignerAddress address) {
        switch (address.kind()) {
            case DesignerAddress.MOUNT:
                return tree.findByAssignment(StructureNode.Kind.SECTION, address.id());
            case DesignerAddress.ASSIGNMENT:
                return tree.findByAssignment(StructureNode.Kind.QUESTION, address.id());
            case DesignerAddress.RULE:
                return tree.target(rules.any(survey.id, address.id()).orElse(null));
            default:
                try {
                    return find(StructureNode.Kind.valueOf(address.kind().toUpperCase()), address.id(), null);
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }

    private StructureNode find(StructureNode.Kind kind, Integer durableId, Integer assignmentId) {
        return tree.find(kind, durableId, assignmentId);
    }

    private void openAncestors(StructureNode node) {
        for (StructureNode n = node; n != null; n = n.parent()) {
            String key = openKey(n);
            if (key != null) {
                openNodes.add(key);
            }
        }
    }

    /** The overlay anchor id for a node: step-/ss-/sq- as the board tags its boxes. */
    private static String anchorOf(StructureNode node) {
        if (node == null) {
            return "";
        }
        return switch (node.kind()) {
            case STEP -> "step-" + node.durableId();
            case SECTION -> "ss-" + node.assignmentId();
            case QUESTION -> "sq-" + node.assignmentId();
        };
    }

    /** Mark a collapsible node (step or section) expanded so a new or moved child is visible. */
    void open(StructureNode node) {
        String key = node == null ? null : openKey(node);
        if (key != null) {
            openNodes.add(key);
        }
    }

    /** Expansion key for a collapsible node (step or section); null for questions and leaves. */
    private static String openKey(StructureNode node) {
        return switch (node.kind()) {
            case STEP -> "step-" + node.durableId();
            case SECTION -> "ss-" + node.assignmentId();
            default -> null;
        };
    }

    // ---- queries used by the components ------------------------------------------------------

    public List<StructureNode> childrenOf(StructureNode node) {
        return tree.childrenOf(node);
    }

    public int mountCount(StructureNode section) {
        return (int) tree.all().stream().filter(n -> n.kind() == StructureNode.Kind.SECTION && !n.retired()
                && Objects.equals(n.durableId(), section.durableId())).count();
    }

    public boolean isOpen(StructureNode node) {
        String key = openKey(node);
        return key != null && openNodes.contains(key);
    }

    public boolean isSelected(StructureNode node) {
        return selected != null && selected.kind() == node.kind() && Objects.equals(selected.durableId(), node.durableId())
                && Objects.equals(selected.assignmentId(), node.assignmentId());
    }

    // ---- interaction -------------------------------------------------------------------------

    public void select(StructureNode node) {
        selected = node;
        openAncestors(node);
        render();
    }


    /** From the drawer tree: the tree's nodes are equal by value to the board's. */
    public void selectFromTree(StructureNode node) {
        select(find(node.kind(), node.durableId(), node.assignmentId()));
        scrollToSelected();
    }

    /**
     * Bring the selected card into view. The board was just rebuilt, so this runs after the new
     * DOM is attached; it looks only inside the board, since the preview tags its boxes alike.
     */
    private void scrollToSelected() {
        if (selected == null) {
            return;
        }
        board.getElement().executeJs(
                "const el = this.querySelector('[data-node-id=\"' + $0 + '\"]');"
                        + "if (el) el.scrollIntoView({block: 'nearest', inline: 'nearest', behavior: 'smooth'});",
                anchorOf(selected));
    }

    public void toggleAndSelect(StructureNode node) {
        String key = openKey(node);
        if (key != null && !openNodes.remove(key)) {
            openNodes.add(key);
        }
        selected = node;
        openAncestors(node.parent());
        render();
    }

    /** A drag whose payload nothing on the board highlights for (a list onto a choice question). */
    public void dragStarted() {
        markDrag(null);
    }

    /**
     * A drag of an element: only the insertion bars that accept this kind light up and lift.
     *
     * @see #markDrag(String)
     */
    public void dragStarted(StructureNode.Kind kind) {
        markDrag(kind == null ? null : kind.name().toLowerCase());
    }

    /**
     * A drag from the palette. A rule glyph is dropped on the question it reads from (UC-019), not
     * on an insertion bar — {@link #onDrop} turns a rule away, because its {@code kind()} is null
     * and so matches no spot — so it marks the board for rules and the question rows light up
     * instead. Every other glyph marks the board for the kind it creates.
     */
    void paletteDragStarted(PaletteItem item) {
        markDrag(item.isRule() ? RULE_DRAG : item.kind().name().toLowerCase());
    }

    /**
     * Marks the board for the duration of a drag: {@code dragging}, plus {@code dragging-<what>}
     * naming what is being dragged, when something on the board can take it.
     * <p>
     * That second class is what lets only the valid targets light up, and lift above the rule-arrow
     * overlay — whose arrow hit bands and glyphs are themselves drag handles, and would otherwise be
     * what the pointer finds over a target (UC-016, UC-019, UC-020).
     */
    private void markDrag(String what) {
        board.addClassName("dragging");
        if (what != null) {
            board.addClassName("dragging-" + what);
        }
    }

    public void dragEnded() {
        board.removeClassName("dragging");
        board.removeClassName("dragging-" + RULE_DRAG);
        for (StructureNode.Kind kind : StructureNode.Kind.values()) {
            board.removeClassName("dragging-" + kind.name().toLowerCase());
        }
    }

    /** Central drop dispatcher: palette items create, nodes reorder within or copy across containers. */
    public void onDrop(Object data, DropSpot spot) {
        try {
            if (data instanceof PaletteItem item) {
                if (item.kind() != spot.accepts()) {
                    return;
                }
                switch (item.kind()) {
                    case STEP -> createStepAt(spot.index());
                    case SECTION -> createSectionAt(spot.container(), spot.index());
                    case QUESTION -> createQuestionAt(spot.container(), spot.index(), typeNamed(item.questionType()));
                }
            } else if (data instanceof BankSection bank) {
                // UC-015 A6: a section from the drawer's Sections panel, mounted in the target step.
                if (spot.accepts() != StructureNode.Kind.SECTION || spot.container() == null) {
                    return;
                }
                StepsSections mount = designer.mountSection(survey.id, bank.sectionId(), spot.container().durableId(), spot.index());
                load();
                select(find(StructureNode.Kind.SECTION, bank.sectionId(), mount.stepsSectionsId));
                success(getTranslation("surveyDesignerView.sectionAlsoInStep", bank.label(), spot.container().label()));
            } else if (data instanceof BankQuestion bank) {
                // UC-011 A6: a question from the drawer's Questions panel, reused in the target section.
                if (spot.accepts() != StructureNode.Kind.QUESTION || spot.container() == null) {
                    return;
                }
                designer.assignQuestionInto(survey.id, bank.questionId(), spot.container().durableId(), spot.index());
                open(spot.container());
                load();
                select(find(StructureNode.Kind.QUESTION, bank.questionId(), null));
                success(getTranslation("surveyDesignerView.questionAlsoInSection", bank.label(), spot.container().label()));
            } else if (data instanceof StructureNode node) {
                if (node.kind() != spot.accepts() || node.retired()) {
                    return;
                }
                Set<Integer> backwardBefore = backwardRules();
                switch (node.kind()) {
                    case STEP -> {
                        int from = indexIn(tree.roots(), node);
                        designer.moveStep(survey.id, node.durableId(), targetIndex(from, spot.index()));
                        afterMove(node, getTranslation("surveyDesignerView.movedStep", node.label()), backwardBefore);
                    }
                    case SECTION -> {
                        if (Objects.equals(node.parent().durableId(), spot.container().durableId())) {
                            int from = indexIn(childrenOf(spot.container()), node);
                            designer.moveSectionWithinStep(survey.id, node.assignmentId(), targetIndex(from, spot.index()));
                            afterMove(node, getTranslation("surveyDesignerView.movedSection", node.label()), backwardBefore);
                        } else {
                            // UC-015 main flow: a card dragged across lanes mounts the same section there.
                            StepsSections mount = designer.mountSection(survey.id, node.durableId(), spot.container().durableId(), spot.index());
                            load();
                            select(find(StructureNode.Kind.SECTION, node.durableId(), mount.stepsSectionsId));
                            success(getTranslation("surveyDesignerView.sectionAlsoInStep", node.label(), spot.container().label()));
                        }
                    }
                    case QUESTION -> {
                        if (Objects.equals(node.parent().durableId(), spot.container().durableId())) {
                            int from = indexIn(childrenOf(spot.container()), node);
                            designer.moveQuestionWithinSection(survey.id, node.assignmentId(), targetIndex(from, spot.index()));
                            afterMove(node, getTranslation("surveyDesignerView.movedQuestion", node.label()), backwardBefore);
                        } else {
                            // UC-011 A4: a row dragged across cards places the same question there.
                            designer.assignQuestionInto(survey.id, node.durableId(), spot.container().durableId(), spot.index());
                            open(spot.container());
                            load();
                            select(find(StructureNode.Kind.QUESTION, node.durableId(), null));
                            success(getTranslation("surveyDesignerView.questionAlsoInSection", node.label(), spot.container().label()));
                        }
                    }
                }
            }
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    public void edit(StructureNode node) {
        try {
            switch (node.kind()) {
                case STEP -> ElementDialog.step(designer.currentStep(survey.id, node.durableId()),
                        e -> designer.rename(survey.id, e), saved -> load()).open();
                case SECTION -> ElementDialog.section(designer.currentSection(survey.id, node.durableId()),
                        e -> designer.rename(survey.id, e), saved -> load()).open();
                case QUESTION -> new QuestionDialog(survey.id, designer.currentQuestion(survey.id, node.durableId()), null,
                        q -> designer.update(survey.id, q), saved -> load(), listService).open();
            }
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    public void addChild(StructureNode node) {
        switch (node.kind()) {
            case STEP -> createSectionAt(node, childrenOf(node).size());
            case SECTION -> createQuestionAt(node, childrenOf(node).size(), palette.selectedQuestionType());
            default -> { }
        }
    }

    public void move(StructureNode node, int delta) {
        try {
            Set<Integer> backwardBefore = backwardRules();
            switch (node.kind()) {
                case STEP -> designer.moveStep(survey.id, node.durableId(), indexIn(tree.roots(), node) + delta);
                case SECTION -> designer.moveSectionWithinStep(survey.id, node.assignmentId(), indexIn(childrenOf(node.parent()), node) + delta);
                case QUESTION -> designer.moveQuestionWithinSection(survey.id, node.assignmentId(), indexIn(childrenOf(node.parent()), node) + delta);
            }
            afterMove(node, switch (node.kind()) {
                case STEP -> getTranslation("surveyDesignerView.movedStep", node.label());
                case SECTION -> getTranslation("surveyDesignerView.movedSection", node.label());
                case QUESTION -> getTranslation("surveyDesignerView.movedQuestion", node.label());
            }, backwardBefore);
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    /** UC-015 A1: a real copy of a section from the card's menu — a drag across lanes mounts instead. */
    public void copyTo(StructureNode node) {
        if (node.kind() != StructureNode.Kind.SECTION) {
            return;
        }
        new PickerDialog<>(getTranslation("surveyDesignerView.copySection.title", node.label()), getTranslation("surveyDesignerView.copySection.label"), tree.roots(), StructureNode::label, step -> {
            try {
                Section copy = designer.duplicateSectionInto(survey.id, node.durableId(), step.durableId(), childrenOf(step).size());
                copiedSection(copy, step);
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }).open();
    }

    /** UC-015 A5 keyboard route: the same section in another step. */
    public void mountIn(StructureNode section) {
        new PickerDialog<>(getTranslation("surveyDesignerView.mountSection.title", section.label()), getTranslation("surveyDesignerView.mountSection.label"), tree.roots(), StructureNode::label, step -> {
            try {
                designer.mountSection(survey.id, section.durableId(), step.durableId(), childrenOf(step).size());
                load();
                success(getTranslation("surveyDesignerView.sectionAlsoInStep", section.label(), step.label()));
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }).open();
    }

    /** The survey's current questions by durable id, read once per render for the panels and the preview. */
    private Map<Integer, Question> currentQuestions() {
        Map<Integer, Question> out = new java.util.LinkedHashMap<>();
        if (survey == null) {
            return out;
        }
        for (Question q : Question.<Question>list("surveyId = ?1 and effectiveTo = ?2", survey.id, com.elicitsoftware.model.StructuralElement.SENTINEL)) { // i18n:ignore JPQL
            out.put(q.questionId, q);
        }
        return out;
    }

    /** The Lists, Sections and Questions panels read the same tables as the board; keep them in step. */
    private void refreshToolbox(Map<Integer, Question> questions) {
        if (toolbox == null || survey == null || tree == null) {
            return;
        }
        java.util.Map<Integer, Long> users = new java.util.HashMap<>();
        java.util.Map<Integer, String> texts = new java.util.HashMap<>();
        for (Question q : questions.values()) {
            texts.put(q.questionId, q.text);
            if (q.selectGroupId != null) {
                users.merge(q.selectGroupId, 1L, Long::sum);
            }
        }
        java.util.Map<Integer, String> descriptions = new java.util.HashMap<>();
        for (Section sec : Section.<Section>list("surveyId = ?1 and effectiveTo = ?2", survey.id, com.elicitsoftware.model.StructuralElement.SENTINEL)) { // i18n:ignore JPQL
            descriptions.put(sec.sectionId, sec.description);
        }
        toolbox.lists.refresh(this, listService.lists(survey.id), listService.optionCounts(survey.id), users, listService.optionTexts(survey.id));
        // UC-006 BR-003a: the panels are the survey's contents, so they hold the elements the board
        // cannot draw -- a section in no step, a question in no section.
        toolbox.sections.refresh(this, tree.all(), structureQuery.unplacedSections(survey.id), descriptions);
        toolbox.questions.refresh(this, tree.all(), structureQuery.unplacedQuestions(survey.id), texts);
    }

    /** The right-hand pane: the selection as a respondent sees it, rebuilt with the board. */
    private void refreshPreview(Map<Integer, Question> questions) {
        if (survey == null || tree == null || !preview.isVisible()) {
            return;
        }
        Set<String> ruleShown = new HashSet<>();
        for (RelationshipQuery.RuleEdge edge : relationshipQuery.edges(survey.id, false)) {
            if ("SHOW".equals(edge.action())) {
                ruleShown.add(edge.target());
            }
        }
        preview.show(this, selected, questions, listService.optionTexts(survey.id), TokenSamples.load(survey.id), ruleShown);
    }

    // ---- lists (UC-014) ------------------------------------------------------------------------

    /** UC-014 step 6: a list dropped on a question row becomes its options. */
    public void useList(StructureNode question, PaletteList list) {
        if (question.kind() != StructureNode.Kind.QUESTION || question.retired()) {
            return;
        }
        try {
            designer.assignList(survey.id, question.durableId(), list.selectGroupId());
            load();
            select(find(StructureNode.Kind.QUESTION, question.durableId(), question.assignmentId()));
            success(getTranslation("surveyDesignerView.questionUsesList", question.label(), list.name()));
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    public void newList() {
        new SelectGroupDialog(listService, survey.id, null, saved -> load()).open();
    }

    public void editList(SelectGroup group) {
        try {
            new SelectGroupDialog(listService, survey.id, listService.list(survey.id, group.selectGroupId), saved -> load()).open();
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    /** UC-014 A2. */
    public void removeList(SelectGroup group) {
        new ConfirmDialog(getTranslation("surveyDesignerView.removeList.title"), getTranslation("surveyDesignerView.removeList.text", group.name), getTranslation("common.remove"), e -> {
            try {
                listService.remove(survey.id, group.selectGroupId);
                load();
                success(getTranslation("surveyDesignerView.removedList", group.name));
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }

    /** UC-011 A4/A6, keyboard route: the same question in a section chosen from a picker (row menu and Questions panel). */
    public void addBankQuestionTo(StructureNode question) {
        new PickerDialog<>(getTranslation("surveyDesignerView.addQuestion.title", question.label()), getTranslation("surveyDesignerView.addQuestion.label"), sectionsDistinct(), this::sectionPath, section -> {
            try {
                designer.assignQuestionInto(survey.id, question.durableId(), section.durableId(), childrenOf(section).size());
                open(section);
                load();
                success(getTranslation("surveyDesignerView.questionAlsoInSection", question.label(), section.label()));
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }).open();
    }

    /** The reporting tags shown as a badge on a row or card: "Tag" or "Tag = value". */
    public List<String> tagsOf(StructureNode node) {
        return tagLabels.getOrDefault(badgeKey(node), List.of());
    }

    private static String badgeKey(StructureNode node) {
        return (node.kind() == StructureNode.Kind.SECTION ? "ss-" : "sq-") + node.assignmentId();
    }

    /** One query per render: a question-wide tag lands on every placement of the question. */
    private void collectTagLabels() {
        tagLabels.clear();
        if (survey == null || tree == null) {
            return;
        }
        List<StructureNode> all = tree.all();
        for (ReportingService.Assignment a : reporting.assignments(survey.id)) {
            com.elicitsoftware.model.Metadata m = a.metadata();
            if (m.ontology == null || m.elementRef() == null) {
                continue;
            }
            String label = m.ontology.tag + (m.value == null ? "" : " = " + m.value);
            for (StructureNode n : all) {
                boolean hit = switch (m.elementRef()) {
                    case QUESTION -> n.kind() == StructureNode.Kind.QUESTION && Objects.equals(n.durableId(), m.questionId);
                    case SECTIONS_QUESTION -> n.kind() == StructureNode.Kind.QUESTION && Objects.equals(n.assignmentId(), m.sectionsQuestionId);
                    case STEPS_SECTIONS -> n.kind() == StructureNode.Kind.SECTION && Objects.equals(n.assignmentId(), m.stepsSectionsId);
                };
                if (hit) {
                    tagLabels.computeIfAbsent(badgeKey(n), k -> new ArrayList<>()).add(label);
                }
            }
        }
    }

    /** UC-021 step 1: the reporting tags a question or section carries, editable in place. */
    public void reportingTags(StructureNode node) {
        if (node.kind() == StructureNode.Kind.STEP) {
            return;
        }
        new ElementTagsDialog(reporting, survey, node, this::load).open();
    }

    public void openDetails(StructureNode node) {
        getUI().ifPresent(ui -> ui.navigate(ElementView.class, new RouteParameters(Map.of(
                SurveyEditorView.SURVEY_ID, String.valueOf(survey.id),
                ElementView.KIND, node.routeKind(),
                ElementView.DURABLE_ID, String.valueOf(node.durableId())))));
    }

    /** UC-015 A3 / UC-011 A5: remove one assignment; the last one retires the element. */
    public void removeAssignment(StructureNode node) {
        boolean last = node.kind() == StructureNode.Kind.SECTION ? mountCount(node) <= 1
                : tree.all().stream().filter(n -> n.kind() == StructureNode.Kind.QUESTION && !n.retired()
                && Objects.equals(n.durableId(), node.durableId())).count() <= 1;
        String what = node.kind() == StructureNode.Kind.SECTION
                ? getTranslation("surveyDesignerView.kind.section") : getTranslation("surveyDesignerView.kind.question");
        String where = node.kind() == StructureNode.Kind.SECTION
                ? getTranslation("surveyDesignerView.kind.step") : getTranslation("surveyDesignerView.kind.section");
        String text = getTranslation("surveyDesignerView.removeAssignment.text", what, node.label(), where, node.parent().label())
                + " " + (last ? getTranslation("surveyDesignerView.removeAssignment.last", what)
                : getTranslation("surveyDesignerView.removeAssignment.shared"));
        new ConfirmDialog(getTranslation("surveyDesignerView.removeAssignment.title", where), text, getTranslation("common.remove"), e -> {
            try {
                if (node.kind() == StructureNode.Kind.SECTION) {
                    designer.retireSectionAssignment(survey.id, node.assignmentId());
                } else {
                    designer.retireQuestionAssignment(survey.id, node.assignmentId());
                }
                selected = node.parent();
                load();
                success(getTranslation("surveyDesignerView.removedAssignment", what, node.label(), where, node.parent().label()));
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }

    /**
     * UC-015 A7 / UC-011 A7: remove an element that is placed nowhere. Everywhere else on the board
     * removal takes an assignment away and retires the element with the last one; an element with no
     * assignment has no such path, so this retires the element itself.
     */
    public void removeUnplaced(StructureNode node) {
        ElementService.RemovalPlan plan = elements.planRemoval(survey.id, node.kind(), node.durableId());
        String text = getTranslation("surveyDesignerView.removeUnplaced.text", node.label())
                + (plan.dependents().isEmpty() ? ""
                : " " + getTranslation("surveyDesignerView.removeStep.dependents", String.join("; ", plan.dependents())));
        new ConfirmDialog(getTranslation("surveyDesignerView.removeUnplaced.title"), text, getTranslation("common.remove"), e -> {
            try {
                elements.remove(survey.id, node.kind(), node.durableId());
                load();
                success(getTranslation("surveyDesignerView.removedUnplaced", node.label()));
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }

    public void removeStep(StructureNode step) {
        ElementService.RemovalPlan plan = elements.planRemoval(survey.id, StructureNode.Kind.STEP, step.durableId());
        String text = getTranslation("surveyDesignerView.removeStep.text", step.label(), plan.dependents().isEmpty() ? ""
                : " " + getTranslation("surveyDesignerView.removeStep.dependents", String.join("; ", plan.dependents())));
        new ConfirmDialog(getTranslation("surveyDesignerView.removeStep.title"), text, getTranslation("common.remove"), e -> {
            try {
                designer.removeStep(survey.id, step.durableId());
                selected = null;
                load();
                success(getTranslation("surveyDesignerView.removedStep", step.label()));
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }

    // ---- rules (UC-019, UC-020) ------------------------------------------------------------

    String edgesJson() {
        try {
            return JSON.writeValueAsString(relationshipQuery.edges(survey.id, Boolean.TRUE.equals(showRemoved.getValue())));
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /** The row a pending rule hangs from, so the rule learns the mount it fires from (UC-019 BR-001). */
    private StructureNode pendingSource;

    /** UC-019 step 1-2: a rule glyph landed on a question row; the overlay now follows the pointer. */
    public void startRule(String action, StructureNode question) {
        showRules.setValue(true);
        selected = question;
        pendingSource = question;
        openAncestors(question);
        render();
        overlay.setPending("sq-" + question.assignmentId(), action);
    }

    /** UC-019 A1: the keyboard path — upstream fixed, target chosen in the dialog. */
    public void addRule(StructureNode question) {
        Relationship preset = new Relationship();
        preset.upstreamSqId = question.assignmentId();
        new RelationshipDialog(survey.id, tree, rules, null, preset, saved -> ruleSaved(saved), this::load).open();
    }

    void connectRequested(String sourceAnchor, String targetAnchor, String mountAnchor, String action) {
        overlay.setPending("", "");
        Integer sq = anchorId(sourceAnchor, "sq");
        if (sq == null || targetAnchor == null) {
            return;
        }
        Relationship preset = new Relationship();
        preset.upstreamSqId = sq;
        // The row the glyph landed on names the mount the rule fires from (UC-019 BR-001).
        StructureNode source = pendingSource != null && sq.equals(pendingSource.assignmentId()) ? pendingSource : null;
        StructureNode sourceStep = source == null ? null : stepOf(source);
        preset.upstreamStepId = sourceStep == null ? null : sourceStep.durableId();
        pendingSource = null;
        preset.actionType = action == null || action.isBlank() ? null : ActionType.find("name", action).firstResult();
        Integer step = anchorId(targetAnchor, "step");
        Integer ss = anchorId(targetAnchor, "ss");
        Integer tsq = anchorId(targetAnchor, "sq");
        if (step != null) {
            preset.downstreamStepId = step;
        } else if (ss != null) {
            preset.downstreamSsId = ss;
        } else if (tsq != null) {
            preset.downstreamSqId = tsq;
            preset.downstreamSsId = anchorId(mountAnchor, "ss"); // the card the row was dropped in
        } else {
            info(getTranslation("surveyDesignerView.dropArrowHint"));
            return;
        }
        new RelationshipDialog(survey.id, tree, rules, null, preset, saved -> ruleSaved(saved), this::load).open();
    }

    void editRule(int relationshipId) {
        Optional<Relationship> rule = rules.any(survey.id, relationshipId);
        if (rule.isEmpty()) {
            info(getTranslation("surveyDesignerView.ruleGone"));
            return;
        }
        // Clicking a rule glyph shifts focus to the rule's target, then opens the rule editor.
        StructureNode target = targetNode(rule.get());
        if (target != null) {
            select(target);
        }
        overlay.setSelectedEdgeId(relationshipId);
        new RelationshipDialog(survey.id, tree, rules, rule.get(), null, saved -> ruleSaved(saved), () -> {
            overlay.setSelectedEdgeId(null);
            load();
        }).open();
    }

    /** The downstream element a rule points at — the innermost of its path, in the mount the path names — resolved to a board node. */
    private StructureNode targetNode(Relationship r) {
        return tree.target(r);
    }

    void repoint(int relationshipId, String end, String targetAnchor, String mountAnchor) {
        try {
            rules.repoint(survey.id, relationshipId, "SOURCE".equals(end) ? RuleService.End.SOURCE : RuleService.End.TARGET, targetAnchor, mountAnchor);
            load();
            success(getTranslation("surveyDesignerView.ruleRepointed"));
        } catch (AuthoringException ae) {
            error(ae.getMessage());
            load();
        }
    }

    private void ruleSaved(Relationship saved) {
        overlay.setSelectedEdgeId(saved.relationshipId);
        showRules.setValue(true);
        load();
    }

    private static Integer anchorId(String anchor, String prefix) {
        if (anchor == null || !anchor.startsWith(prefix + "-")) {
            return null;
        }
        try {
            return Integer.parseInt(anchor.substring(prefix.length() + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- creation --------------------------------------------------------------------------

    void createStepAt(int index) {
        ElementDialog.step(null, draft -> designer.createStep(survey.id, draft, index), saved -> {
            load();
            select(find(StructureNode.Kind.STEP, saved.stepId, null));
        }).open();
    }

    /** NFR-012 keyboard path: add a step immediately after the given one. */
    public void addStepAfter(StructureNode step) {
        int i = indexIn(tree.roots(), step);
        createStepAt(i < 0 ? tree.roots().size() : i + 1);
    }

    /** The "Add step" button: after the selected step (or the step holding the selection), else at the end. */
    public void addStepFromSelection() {
        StructureNode step = stepOf(selected);
        if (step != null) {
            addStepAfter(step);
        } else {
            createStepAt(tree.roots().size());
        }
    }

    private static StructureNode stepOf(StructureNode node) {
        for (StructureNode n = node; n != null; n = n.parent()) {
            if (n.kind() == StructureNode.Kind.STEP) {
                return n;
            }
        }
        return null;
    }

    void createSectionAt(StructureNode step, int index) {
        ElementDialog.section(null, draft -> designer.createSectionIn(survey.id, step.durableId(), draft, index), saved -> {
            load();
            StructureNode node = find(StructureNode.Kind.SECTION, saved.sectionId, null);
            if (node != null) {
                open(node);
            }
            select(node);
        }).open();
    }

    void createQuestionAt(StructureNode section, int index, QuestionType type) {
        new QuestionDialog(survey.id, null, type, draft -> designer.createQuestionIn(survey.id, section.durableId(), draft, index), saved -> {
            open(section);
            load();
            select(find(StructureNode.Kind.QUESTION, saved.questionId, null));
        }, listService).open();
    }

    // ---- helpers ---------------------------------------------------------------------------

    private void copiedSection(Section copy, StructureNode targetStep) {
        load();
        StructureNode node = find(StructureNode.Kind.SECTION, copy.sectionId, null);
        select(node);
        Button undo = new Button(getTranslation("surveyDesignerView.undo"), e -> {
            try {
                designer.removeSection(survey.id, copy.sectionId);
                selected = null;
                load();
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        });
        undo.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Notification n = new Notification(new Span(getTranslation("surveyDesignerView.copiedSection", copy.name, targetStep.label()) + " "), undo);
        n.setDuration(8000);
        n.setPosition(Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.open();
    }

    private void afterChange(StructureNode node, String message) {
        selected = node;
        load();
        success(message);
    }

    /**
     * UC-033 step 4 / UC-016 A4: a reorder is never refused for a rule, but when it turns rules
     * backwards the author is told how many, and the rebuilt board draws them red.
     */
    private void afterMove(StructureNode node, String message, Set<Integer> backwardBefore) {
        selected = node;
        load();
        Set<Integer> newlyBackward = new HashSet<>(backwardRules());
        newlyBackward.removeAll(backwardBefore);
        if (newlyBackward.isEmpty()) {
            success(message);
            return;
        }
        int n = newlyBackward.size();
        warning(n == 1 ? getTranslation("surveyDesignerView.backward.one", message)
                : getTranslation("surveyDesignerView.backward.many", message, n));
    }

    /** The ids of the current rules whose target is not after their origin (UC-033 BR-003: derived, never stored). */
    private Set<Integer> backwardRules() {
        Set<Integer> ids = new HashSet<>();
        for (RelationshipQuery.RuleEdge e : relationshipQuery.edges(survey.id, false)) {
            if (e.invalid()) {
                ids.add(e.relationshipId());
            }
        }
        return ids;
    }

    private static int indexIn(List<StructureNode> siblings, StructureNode node) {
        for (int i = 0; i < siblings.size(); i++) {
            StructureNode s = siblings.get(i);
            if (Objects.equals(s.durableId(), node.durableId()) && Objects.equals(s.assignmentId(), node.assignmentId())) {
                return i;
            }
        }
        return -1;
    }

    /** A zone index counts the dragged element itself when it sits before the zone. */
    private static int targetIndex(int from, int zoneIndex) {
        return from >= 0 && zoneIndex > from ? zoneIndex - 1 : zoneIndex;
    }

    private List<StructureNode> sectionsDistinct() {
        List<StructureNode> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (StructureNode n : tree.all()) {
            if (n.kind() == StructureNode.Kind.SECTION && !n.retired() && seen.add(n.durableId())) {
                out.add(n);
            }
        }
        return out;
    }

    private String sectionPath(StructureNode section) {
        return section.parent().label() + " › " + section.label();
    }

    private static QuestionType typeNamed(String name) {
        return name == null ? null : QuestionType.find("name", name).firstResult();
    }

    private static void success(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void info(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START);
    }

    private static void warning(String message) {
        Notification.show(message, 8000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private static void error(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
