package com.elicitsoftware.author.survey;

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
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.model.Dimension;
import com.elicitsoftware.model.Metadata;
import com.elicitsoftware.model.Metadata.ElementRef;
import com.elicitsoftware.model.Ontology;
import com.elicitsoftware.model.Survey;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Reporting tags and their assignments (UC-021 assign, UC-022 reassign, UC-023 manage tags and
 * dimensions, UC-024 impact). {@code dimensions}, {@code ontology} and {@code metadata} are Type 1
 * tables: edits are in place, and a change to an assigned tag is one of the two sanctioned
 * retroactive edits, so it is recorded in the {@link ReportingChangeLedger} for the export warning.
 */
@ApplicationScoped
public class ReportingService {

    /** Table names used in {@link ValidationIssue#kind()} and ledger entries. */
    public static final String KIND_TAG = "ontology";
    public static final String KIND_ASSIGNMENT = "metadata";

    /** One metadata row resolved against the structure: the element it tags and where that element sits. */
    public record Assignment(Metadata metadata, ElementRef ref, String elementLabel, String where, boolean dangling, StructureNode node) {

        public String scopeLabel() {
            return scopeLabel(ref);
        }

        public static String scopeLabel(ElementRef ref) {
            if (ref == null) {
                return "invalid";
            }
            return switch (ref) {
                case STEPS_SECTIONS -> "Section in step";
                case QUESTION -> "Question everywhere";
                case SECTIONS_QUESTION -> "Question in section";
            };
        }

        public String valueLabel() {
            return metadata.value == null ? "(the answer)" : metadata.value;
        }
    }

    /** One dimension table the site's ETL will create. */
    public record DimTable(String name, String source, boolean sharedDimension, List<String> tags, int assignments) { }

    /** One {@code fact_sections} column the site's ETL will add. */
    public record FactColumn(String column, String dimTable, String tag) { }

    /** UC-024: everything the reporting schema gains from this survey, plus what would break it. */
    public record Impact(List<DimTable> tables, List<FactColumn> columns, List<ValidationIssue> problems) {

        public boolean hasErrors() {
            return problems.stream().anyMatch(ValidationIssue::isError);
        }
    }

    @Inject
    EntityManager em;

    @Inject
    SurveyStructureQuery structureQuery;

    @Inject
    ReportingChangeLedger ledger;

    // ---- dimensions and tags (UC-023) --------------------------------------------------------

    public List<Dimension> dimensions() {
        return Dimension.listAll(Sort.by("name"));
    }

    public List<Ontology> tags(int surveyId) {
        return Ontology.list("surveyId", Sort.by("tag"), surveyId);
    }

    public Optional<Ontology> tag(int surveyId, int ontologyId) {
        Ontology o = Ontology.findById(ontologyId);
        return o != null && Objects.equals(o.surveyId, surveyId) ? Optional.of(o) : Optional.empty();
    }

    public long assignmentCount(int ontologyId) {
        return Metadata.count("ontology.id", ontologyId);
    }

    /** UC-023 step 3: a new tag; {@code dimensionName} null or blank gives the tag its own table. */
    @Transactional
    public Ontology createTag(int surveyId, String tag, String dimensionName) {
        Survey survey = survey(surveyId);
        String cleanTag = validateTag(tag, dimensionName);
        Ontology o = new Ontology();
        o.surveyId = surveyId;
        o.name = survey.name;
        o.tag = cleanTag;
        o.dimension = ensureDimension(dimensionName);
        try {
            o.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return o;
    }

    /** UC-023 A4: in-place edit; a changed tag or dimension on an assigned tag is retroactive. */
    @Transactional
    public Ontology updateTag(int surveyId, int ontologyId, String tag, String dimensionName) {
        Ontology o = tag(surveyId, ontologyId).orElseThrow(() -> new AuthoringException("The tag no longer exists"));
        String cleanTag = validateTag(tag, dimensionName);
        Dimension dimension = ensureDimension(dimensionName);
        String before = describe(o);
        boolean changed = !Objects.equals(o.tag, cleanTag)
                || !Objects.equals(o.dimension == null ? null : o.dimension.id, dimension == null ? null : dimension.id);
        o.tag = cleanTag;
        o.dimension = dimension;
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        if (changed) {
            for (Metadata m : Metadata.<Metadata>list("ontology.id", ontologyId)) {
                ledger.record(surveyId, KIND_ASSIGNMENT, m.id, "Tag " + before + " was redefined as " + describe(o)
                        + "; answers already recorded at deployed sites are reclassified");
            }
        }
        return o;
    }

    /** UC-023 A2: a tag in use cannot be removed. */
    @Transactional
    public void removeTag(int surveyId, int ontologyId) {
        Ontology o = tag(surveyId, ontologyId).orElseThrow(() -> new AuthoringException("The tag no longer exists"));
        long uses = assignmentCount(ontologyId);
        if (uses > 0) {
            throw new AuthoringException("Tag '" + o.tag + "' is assigned to " + uses + (uses == 1 ? " element" : " elements")
                    + "; remove those assignments first");
        }
        o.delete();
        em.flush();
    }

    /** Finds the dimension by name (case-insensitively, as the ETL lowercases) or creates it. */
    @Transactional
    public Dimension ensureDimension(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = validateDimension(name);
        for (Dimension d : dimensions()) {
            if (d.name != null && d.name.equalsIgnoreCase(clean)) {
                return d;
            }
        }
        Dimension d = new Dimension();
        d.name = clean;
        try {
            d.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return d;
    }

    // ---- assignments (UC-021, UC-022) --------------------------------------------------------

    public List<Assignment> assignments(int surveyId) {
        Labels labels = labels(surveyId);
        List<Assignment> out = new ArrayList<>();
        for (Metadata m : Metadata.<Metadata>list("surveyId", Sort.by("id"), surveyId)) {
            out.add(labels.resolve(m));
        }
        return out;
    }

    /** The assignments that concern one board node: both scopes for a question, the mount for a section. */
    public List<Assignment> assignmentsFor(int surveyId, StructureNode node) {
        return assignments(surveyId).stream().filter(a -> concerns(a.metadata(), node)).toList();
    }

    /** The assignments that concern one element by kind and durable id (the element detail page). */
    public List<Assignment> assignmentsFor(int surveyId, StructureNode.Kind kind, int durableId) {
        return assignments(surveyId).stream()
                .filter(a -> a.node() != null && a.node().kind() == kind && Objects.equals(a.node().durableId(), durableId))
                .toList();
    }

    /** UC-021 steps 2-5: one new assignment; {@code value} null or blank means the answer is the value. */
    @Transactional
    public Metadata assign(int surveyId, ElementRef ref, int durableId, int ontologyId, String value) {
        if (ref == null) {
            throw new AuthoringException("Choose what the tag applies to", "scope");
        }
        Ontology o = tag(surveyId, ontologyId).orElseThrow(() -> new AuthoringException("Choose a tag", "tag"));
        Labels labels = labels(surveyId);
        if (!labels.exists(ref, durableId)) {
            throw new AuthoringException("The element no longer exists in this survey", "scope");
        }
        for (Metadata existing : Metadata.<Metadata>list("surveyId", surveyId)) {
            if (existing.elementRef() == ref && Objects.equals(existing.elementId(), durableId)
                    && existing.ontology != null && Objects.equals(existing.ontology.id, ontologyId)) {
                throw new AuthoringException("This element already carries the tag '" + o.tag + "'", "tag");
            }
        }
        Metadata m = new Metadata();
        m.surveyId = surveyId;
        m.ontology = o;
        m.value = validateValue(value);
        switch (ref) {
            case STEPS_SECTIONS -> m.stepsSectionsId = durableId;
            case QUESTION -> m.questionId = durableId;
            case SECTIONS_QUESTION -> m.sectionsQuestionId = durableId;
        }
        try {
            m.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return m;
    }

    /** UC-022 steps 3-6: change the tag or value in place; the ledger carries the export warning. */
    @Transactional
    public Metadata reassign(int surveyId, int metadataId, int ontologyId, String value) {
        Metadata m = assignment(surveyId, metadataId);
        Ontology o = tag(surveyId, ontologyId).orElseThrow(() -> new AuthoringException("Choose a tag", "tag"));
        String cleanValue = validateValue(value);
        boolean changed = !Objects.equals(m.ontology == null ? null : m.ontology.id, ontologyId) || !Objects.equals(m.value, cleanValue);
        if (!changed) {
            return m;
        }
        String before = describe(m);
        m.ontology = o;
        m.value = cleanValue;
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        ledger.record(surveyId, KIND_ASSIGNMENT, m.id, labels(surveyId).resolve(m).elementLabel() + ": " + before
                + " was changed to " + describe(m) + "; answers already recorded at deployed sites are reclassified");
        return m;
    }

    /** UC-022 A2: removing an assignment stops classifying answers at deployed sites, so it is warned about too. */
    @Transactional
    public void unassign(int surveyId, int metadataId) {
        Metadata m = assignment(surveyId, metadataId);
        String message = labels(surveyId).resolve(m).elementLabel() + ": " + describe(m)
                + " was removed; answers already recorded at deployed sites stop being classified by it";
        m.delete();
        em.flush();
        ledger.record(surveyId, KIND_ASSIGNMENT, metadataId, message);
    }

    // ---- impact (UC-024) -----------------------------------------------------------------------

    public Impact impact(int surveyId) {
        List<Ontology> tags = tags(surveyId);
        List<Assignment> assignments = assignments(surveyId);
        List<ValidationIssue> problems = new ArrayList<>();

        Map<Integer, Integer> usesByTag = new HashMap<>();
        for (Assignment a : assignments) {
            Metadata m = a.metadata();
            if (m.ontology != null) {
                usesByTag.merge(m.ontology.id, 1, Integer::sum);
            }
            if (m.elementRef() == null) {
                problems.add(ValidationIssue.error(KIND_ASSIGNMENT, m.id, "An assignment of tag '" + tagOf(m)
                        + "' does not name exactly one element"));
            } else if (a.dangling()) {
                problems.add(ValidationIssue.warning(KIND_ASSIGNMENT, m.id, "Tag '" + tagOf(m) + "' is assigned to a removed or missing "
                        + a.scopeLabel().toLowerCase(Locale.ROOT) + (a.elementLabel().isBlank() ? "" : " (" + a.elementLabel() + ")")));
            }
        }
        Map<String, List<Assignment>> byElementAndTag = new HashMap<>();
        for (Assignment a : assignments) {
            Metadata m = a.metadata();
            if (m.elementRef() != null && m.ontology != null) {
                byElementAndTag.computeIfAbsent(m.elementRef() + ":" + m.elementId() + ":" + m.ontology.id, k -> new ArrayList<>()).add(a);
            }
        }
        byElementAndTag.values().stream().filter(l -> l.size() > 1).forEach(l -> problems.add(ValidationIssue.warning(
                KIND_ASSIGNMENT, l.get(0).metadata().id, "Tag '" + tagOf(l.get(0).metadata()) + "' is assigned "
                        + l.size() + " times to " + l.get(0).elementLabel() + " with different values")));

        Map<String, DimTableBuilder> tables = new TreeMap<>();
        Map<String, List<Ontology>> byColumn = new TreeMap<>();
        List<FactColumn> columns = new ArrayList<>();
        for (Ontology o : tags) {
            String table = ReportingNames.dimTable(o);
            String column = ReportingNames.factColumn(o.tag);
            ReportingNames.identifierProblem(table).ifPresent(p -> problems.add(ValidationIssue.error(KIND_TAG, o.id,
                    "Tag '" + o.tag + "': its dimension table " + p + "; the site's reporting build would fail")));
            ReportingNames.identifierProblem(column).ifPresent(p -> problems.add(ValidationIssue.error(KIND_TAG, o.id,
                    "Tag '" + o.tag + "': its report column " + p + "; the site's reporting build would fail")));
            int uses = usesByTag.getOrDefault(o.id, 0);
            if (uses == 0) {
                problems.add(ValidationIssue.warning(KIND_TAG, o.id, "Tag '" + o.tag + "' is not assigned to any element"));
            }
            tables.computeIfAbsent(table, t -> new DimTableBuilder(t, o.dimension != null ? o.dimension.name : o.tag, o.dimension != null))
                    .add(o.tag, uses);
            byColumn.computeIfAbsent(column, c -> new ArrayList<>()).add(o);
            columns.add(new FactColumn(column, table, o.tag));
        }
        byColumn.forEach((column, owners) -> {
            if (owners.size() > 1) {
                problems.add(ValidationIssue.error(KIND_TAG, owners.get(0).id, "Tags " + owners.stream().map(o -> "'" + o.tag + "'").toList()
                        + " all produce the report column '" + column + "'"));
            }
        });
        for (Ontology o : tags) {
            if (o.dimension == null) {
                String own = ReportingNames.dimTableForTag(o.tag);
                for (Ontology other : tags) {
                    if (other.dimension != null && own.equals(ReportingNames.dimTable(other))) {
                        problems.add(ValidationIssue.error(KIND_TAG, o.id, "Tag '" + o.tag + "' has no dimension, so it names its own table '"
                                + own + "', which is also the table of dimension '" + other.dimension.name + "'"));
                        break;
                    }
                }
            }
        }
        // fact_sections is one table per site: another survey defining the same column against a
        // different table would make the site's reporting build ambiguous.
        for (Ontology other : Ontology.<Ontology>list("surveyId <> ?1", surveyId)) {
            String column = ReportingNames.factColumn(other.tag);
            List<Ontology> mine = byColumn.get(column);
            if (mine != null) {
                String otherTable = ReportingNames.dimTable(other);
                for (Ontology o : mine) {
                    if (!otherTable.equals(ReportingNames.dimTable(o))) {
                        problems.add(ValidationIssue.warning(KIND_TAG, o.id, "Tag '" + o.tag + "' produces column '" + column
                                + "' against '" + ReportingNames.dimTable(o) + "', but another survey in this database maps the same column to '"
                                + otherTable + "'"));
                    }
                }
            }
        }
        columns.sort((a, b) -> a.column().compareTo(b.column()));
        return new Impact(tables.values().stream().map(DimTableBuilder::build).toList(), columns, problems);
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static final class DimTableBuilder {
        final String name;
        final String source;
        final boolean shared;
        final List<String> tags = new ArrayList<>();
        int assignments;

        DimTableBuilder(String name, String source, boolean shared) {
            this.name = name;
            this.source = source;
            this.shared = shared;
        }

        void add(String tag, int uses) {
            tags.add(tag);
            assignments += uses;
        }

        DimTable build() {
            return new DimTable(name, source, shared, List.copyOf(tags), assignments);
        }
    }

    /** Element labels resolved from the structure tree, retired rows included so nothing dangles silently. */
    private final class Labels {
        final Map<Integer, StructureNode> questionsByDurable = new LinkedHashMap<>();
        final Map<Integer, StructureNode> questionsByAssignment = new HashMap<>();
        final Map<Integer, StructureNode> sectionsByAssignment = new HashMap<>();

        Labels(int surveyId) {
            for (StructureNode n : structureQuery.tree(surveyId, true).all()) {
                switch (n.kind()) {
                    case QUESTION -> {
                        questionsByAssignment.put(n.assignmentId(), n);
                        // Prefer a current placement as the representative of the question.
                        questionsByDurable.merge(n.durableId(), n, (a, b) -> a.retired() && !b.retired() ? b : a);
                    }
                    case SECTION -> sectionsByAssignment.put(n.assignmentId(), n);
                    default -> { }
                }
            }
        }

        boolean exists(ElementRef ref, int durableId) {
            return switch (ref) {
                case QUESTION -> questionsByDurable.containsKey(durableId);
                case SECTIONS_QUESTION -> questionsByAssignment.containsKey(durableId);
                case STEPS_SECTIONS -> sectionsByAssignment.containsKey(durableId);
            };
        }

        Assignment resolve(Metadata m) {
            ElementRef ref = m.elementRef();
            if (ref == null) {
                return new Assignment(m, null, "", "", true, null);
            }
            StructureNode node = switch (ref) {
                case QUESTION -> questionsByDurable.get(m.questionId);
                case SECTIONS_QUESTION -> questionsByAssignment.get(m.sectionsQuestionId);
                case STEPS_SECTIONS -> sectionsByAssignment.get(m.stepsSectionsId);
            };
            if (node == null) {
                return new Assignment(m, ref, "", "", true, null);
            }
            String where = ref == ElementRef.QUESTION ? "wherever it appears" : path(node.parent());
            return new Assignment(m, ref, node.label(), where, node.retired(), node);
        }
    }

    private Labels labels(int surveyId) {
        return new Labels(surveyId);
    }

    public static String path(StructureNode node) {
        if (node == null) {
            return "";
        }
        String parent = path(node.parent());
        return parent.isEmpty() ? node.label() : parent + " › " + node.label();
    }

    private static boolean concerns(Metadata m, StructureNode node) {
        ElementRef ref = m.elementRef();
        if (ref == null || node == null) {
            return false;
        }
        return switch (node.kind()) {
            case QUESTION -> (ref == ElementRef.QUESTION && Objects.equals(m.questionId, node.durableId()))
                    || (ref == ElementRef.SECTIONS_QUESTION && Objects.equals(m.sectionsQuestionId, node.assignmentId()));
            case SECTION -> ref == ElementRef.STEPS_SECTIONS && Objects.equals(m.stepsSectionsId, node.assignmentId());
            case STEP -> false;
        };
    }

    private Metadata assignment(int surveyId, int metadataId) {
        Metadata m = Metadata.findById(metadataId);
        if (m == null || !Objects.equals(m.surveyId, surveyId)) {
            throw new AuthoringException("The assignment no longer exists");
        }
        return m;
    }

    private static Survey survey(int surveyId) {
        Survey s = Survey.findById(surveyId);
        if (s == null) {
            throw new AuthoringException("Survey " + surveyId + " no longer exists");
        }
        return s;
    }

    private static String tagOf(Metadata m) {
        return m.ontology == null ? "?" : m.ontology.tag;
    }

    private static String describe(Ontology o) {
        return "'" + o.tag + "' (" + ReportingNames.dimTable(o) + ")";
    }

    private static String describe(Metadata m) {
        return "tag '" + tagOf(m) + "'" + (m.value == null ? "" : " = '" + m.value + "'");
    }

    /** UC-023 BR-002/BR-003: a tag must survive the ETL's identifier rules for both the names it produces. */
    static String validateTag(String tag, String dimensionName) {
        if (tag == null || tag.isBlank()) {
            throw new AuthoringException("Tag is required", "tag");
        }
        String clean = tag.trim();
        if (clean.length() > 255) {
            throw new AuthoringException("Tag must be 255 characters or fewer", "tag");
        }
        if (!ReportingNames.TAG_CHARS.matcher(clean).matches()) {
            throw new AuthoringException("Tag may contain only letters, digits, spaces and hyphens, and must start with a letter or digit", "tag");
        }
        boolean ownTable = dimensionName == null || dimensionName.isBlank();
        if (ownTable && clean.contains("-")) {
            throw new AuthoringException("A tag without a dimension names its own table, and a hyphen is not allowed in a table name; "
                    + "choose a dimension or remove the hyphen", "tag");
        }
        ReportingNames.identifierProblem(ReportingNames.factColumn(clean)).ifPresent(p -> {
            throw new AuthoringException("The report column " + p, "tag");
        });
        if (ownTable) {
            ReportingNames.identifierProblem(ReportingNames.dimTableForTag(clean)).ifPresent(p -> {
                throw new AuthoringException("The dimension table " + p, "tag");
            });
        }
        return clean;
    }

    static String validateDimension(String name) {
        String clean = name.trim();
        if (clean.length() > 50) {
            throw new AuthoringException("Dimension must be 50 characters or fewer", "dimension");
        }
        if (!ReportingNames.DIMENSION_CHARS.matcher(clean).matches()) {
            throw new AuthoringException("Dimension may contain only letters, digits and underscores (it names a table)", "dimension");
        }
        ReportingNames.identifierProblem(ReportingNames.dimTableForDimension(clean)).ifPresent(p -> {
            throw new AuthoringException("The dimension table " + p, "dimension");
        });
        return clean;
    }

    static String validateValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim();
        if (clean.length() > 255) {
            throw new AuthoringException("Value must be 255 characters or fewer", "value");
        }
        return clean;
    }
}
