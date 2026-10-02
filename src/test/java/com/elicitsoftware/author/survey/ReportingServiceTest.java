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
import com.elicitsoftware.author.definition.ExportValidation;
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.author.survey.ReportingService.Assignment;
import com.elicitsoftware.author.survey.ReportingService.Impact;
import com.elicitsoftware.model.Dimension;
import com.elicitsoftware.model.Metadata;
import com.elicitsoftware.model.Metadata.ElementRef;
import com.elicitsoftware.model.Ontology;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-021 Assign, UC-022 Reassign, UC-023 Manage Entries, UC-024 Preview Impact. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReportingServiceTest {

    @Inject
    ReportingService service;

    @Inject
    ReportingChangeLedger ledger;

    @Inject
    ExportValidation validation;

    @Inject
    SurveyService surveys;

    @Inject
    SurveyStructureQuery structure;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private Ontology tag(Survey s, String tag) {
        return service.tags(s.id).stream().filter(o -> o.tag.equals(tag)).findFirst().orElseThrow();
    }

    private StructureNode node(Survey s, StructureNode.Kind kind, String label) {
        return structure.tree(s.id, false).all().stream()
                .filter(n -> n.kind() == kind && n.label().equals(label)).findFirst().orElseThrow();
    }

    @AfterEach
    void clearLedger() {
        // The ledger lives outside the rolled-back transaction.
        ledger.clear(fixture().id);
    }

    // ---- UC-021 --------------------------------------------------------------------------------

    @Test
    void fixtureAssignmentsResolveToElementsAndScopes() {
        List<Assignment> rows = service.assignments(fixture().id);
        assertEquals(3, rows.size(), "fixture: one assignment per scope");
        Assignment age = rows.stream().filter(a -> a.metadata().ontology.tag.equals("Age")).findFirst().orElseThrow();
        assertEquals(ElementRef.QUESTION, age.ref());
        assertEquals("FixtureQ4", age.elementLabel(), "UC-021 BR-002: question-wide scope resolves the question");
        assertNull(age.metadata().value, "UC-021 BR-003: null value reports the answer");
        Assignment children = rows.stream().filter(a -> a.metadata().ontology.tag.equals("Has Children")).findFirst().orElseThrow();
        assertEquals(ElementRef.SECTIONS_QUESTION, children.ref());
        assertEquals("FixtureQ3", children.elementLabel());
        assertTrue(children.where().contains("Fixture Section B"), children.where());
        Assignment gender = rows.stream().filter(a -> a.metadata().ontology.tag.equals("Gender")).findFirst().orElseThrow();
        assertEquals(ElementRef.STEPS_SECTIONS, gender.ref());
        assertEquals("Fixture Section B", gender.elementLabel());
        assertEquals("Female", gender.metadata().value);
        assertFalse(rows.stream().anyMatch(Assignment::dangling));
    }

    @Test
    void assignmentsForABoardNodeCoverBothQuestionScopes() {
        Survey s = fixture();
        StructureNode q3 = node(s, StructureNode.Kind.QUESTION, "FixtureQ3");
        assertEquals(1, service.assignmentsFor(s.id, q3).size(), "FixtureQ3 is tagged in its section only");
        StructureNode q4 = node(s, StructureNode.Kind.QUESTION, "FixtureQ4");
        assertEquals(1, service.assignmentsFor(s.id, q4).size(), "FixtureQ4 is tagged wherever it appears");
        StructureNode sectionB = node(s, StructureNode.Kind.SECTION, "Fixture Section B");
        assertEquals(1, service.assignmentsFor(s.id, sectionB).size(), "Section B as mounted in Step One carries Gender");
        assertEquals(1, service.assignmentsFor(s.id, StructureNode.Kind.SECTION, sectionB.durableId()).size());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "reporting.author", roles = {"elicit_author"})
    void assign_storesTheChosenScopeAndMintsAKey() {
        Survey s = fixture();
        StructureNode q2 = node(s, StructureNode.Kind.QUESTION, "FixtureQ2");
        Metadata m = service.assign(s.id, ElementRef.SECTIONS_QUESTION, q2.assignmentId(), tag(s, "Age").id, "  ");
        assertNotNull(m.metadataKey, "UC-021 step 5: element key minted");
        assertEquals(ElementRef.SECTIONS_QUESTION, m.elementRef(), "UC-021 BR-001: exactly one element column");
        assertEquals(q2.assignmentId(), m.sectionsQuestionId);
        assertNull(m.value, "blank value means the answer");
        assertEquals(4, service.assignments(s.id).size());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "reporting.author", roles = {"elicit_author"})
    void assign_refusesADuplicateOnTheSameScope() {
        Survey s = fixture();
        StructureNode q4 = node(s, StructureNode.Kind.QUESTION, "FixtureQ4");
        AuthoringException ae = assertThrows(AuthoringException.class,
                () -> service.assign(s.id, ElementRef.QUESTION, q4.durableId(), tag(s, "Age").id, "x"));
        assertEquals("tag", ae.field(), "UC-021 A1: marked on the tag field");
        // The other scope of the same question is a different assignment.
        Metadata m = service.assign(s.id, ElementRef.SECTIONS_QUESTION, q4.assignmentId(), tag(s, "Age").id, null);
        assertEquals(ElementRef.SECTIONS_QUESTION, m.elementRef());
    }

    @Test
    @TestTransaction
    void assign_refusesAMissingElementOrScope() {
        Survey s = fixture();
        assertEquals("scope", assertThrows(AuthoringException.class,
                () -> service.assign(s.id, ElementRef.QUESTION, 999999, tag(s, "Age").id, null)).field());
        assertEquals("scope", assertThrows(AuthoringException.class,
                () -> service.assign(s.id, null, 1, tag(s, "Age").id, null)).field());
    }

    // ---- UC-022 --------------------------------------------------------------------------------

    @Test
    @TestTransaction
    @TestSecurity(user = "reporting.author", roles = {"elicit_author"})
    void reassign_recordsTheRetroactiveChangeUntilExport() {
        Survey s = fixture();
        Assignment age = service.assignments(s.id).stream().filter(a -> a.metadata().ontology.tag.equals("Age")).findFirst().orElseThrow();
        Metadata m = service.reassign(s.id, age.metadata().id, tag(s, "Gender").id, "unknown");
        assertEquals("Gender", m.ontology.tag, "UC-022 step 6: updated in place");
        assertEquals("unknown", m.value);
        List<ValidationIssue> issues = validation.validate(s);
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && "metadata".equals(i.kind())
                && i.message().contains("reported under the new tag")), "UC-022 BR-002: warned at export: " + issues);
        surveys.stampExport(s.id, "release");
        assertTrue(ledger.since(s.id).isEmpty(), "UC-008 step 4 clears the record");
    }

    @Test
    @TestTransaction
    void reassign_withoutAChangeRecordsNothing() {
        Survey s = fixture();
        Assignment gender = service.assignments(s.id).stream().filter(a -> a.metadata().ontology.tag.equals("Gender")).findFirst().orElseThrow();
        service.reassign(s.id, gender.metadata().id, gender.metadata().ontology.id, "Female");
        assertTrue(ledger.since(s.id).isEmpty(), "UC-022 A1");
    }

    @Test
    @TestTransaction
    void unassign_deletesAndRecordsTheChange() {
        Survey s = fixture();
        Assignment children = service.assignments(s.id).stream().filter(a -> a.metadata().ontology.tag.equals("Has Children")).findFirst().orElseThrow();
        service.unassign(s.id, children.metadata().id);
        assertEquals(2, service.assignments(s.id).size());
        assertEquals(1, ledger.since(s.id).size(), "UC-022 A2: removal is retroactive at the site");
        assertTrue(ledger.since(s.id).get(0).message().contains("FixtureQ3"));
    }

    // ---- UC-023 --------------------------------------------------------------------------------

    @Test
    @TestTransaction
    void createTag_defaultsTheNamespaceAndMintsAKey() {
        Survey s = fixture();
        Ontology o = service.createTag(s.id, "  Vital Status ", null);
        assertEquals("Vital Status", o.tag);
        assertEquals("AuthorFixture", o.name, "UC-023 BR-004: namespace is the survey name");
        assertNotNull(o.ontologyKey, "C-017: element key minted");
        assertNull(o.dimension, "no dimension: own table");
        assertEquals(4, service.tags(s.id).size());
    }

    @Test
    @TestTransaction
    void createTag_withANewDimensionCreatesItAndReusesItCaseInsensitively() {
        Survey s = fixture();
        int before = service.dimensions().size();
        Ontology o = service.createTag(s.id, "Breast Cancer", "cancer");
        assertNotNull(o.dimension.dimensionKey, "UC-023 BR-001: dimension minted with its key");
        assertEquals(before + 1, service.dimensions().size());
        Ontology again = service.createTag(s.id, "Lung Cancer", "Cancer");
        assertEquals(o.dimension.id, again.dimension.id, "UC-023 BR-001: reused, not duplicated");
        assertEquals(before + 1, service.dimensions().size());
        Dimension existing = service.ensureDimension("FIXTURE_AGE");
        assertEquals("fixture_age", existing.name);
    }

    @Test
    @TestTransaction
    void createTag_refusesNamesTheSiteWouldReject() {
        Survey s = fixture();
        assertEquals("tag", assertThrows(AuthoringException.class, () -> service.createTag(s.id, "Non-Melanoma", null)).field(),
                "UC-023 A3: hyphen without a dimension");
        assertNotNull(service.createTag(s.id, "Non-Melanoma", "cancer"), "with a dimension the hyphen only affects the column");
        assertEquals("tag", assertThrows(AuthoringException.class, () -> service.createTag(s.id, "Age (years)", null)).field());
        assertEquals("tag", assertThrows(AuthoringException.class, () -> service.createTag(s.id, "x".repeat(64), "cancer")).field(),
                "UC-024 BR-002: column longer than 63");
        assertEquals("dimension", assertThrows(AuthoringException.class, () -> service.createTag(s.id, "Height", "body size")).field(),
                "UC-023 BR-003: dimension must be identifier-safe");
        assertEquals("tag", assertThrows(AuthoringException.class, () -> service.createTag(s.id, " ", null)).field());
    }

    @Test
    @TestTransaction
    void createTag_refusesADuplicateInTheNamespace() {
        Survey s = fixture();
        AuthoringException ae = assertThrows(AuthoringException.class, () -> service.createTag(s.id, "Age", null));
        assertEquals("tag", ae.field(), "UC-023 A1: " + ae.getMessage());
    }

    @Test
    @TestTransaction
    void removeTag_isBlockedWhileAssignedAndAllowedAfterwards() {
        Survey s = fixture();
        Ontology gender = tag(s, "Gender");
        assertThrows(AuthoringException.class, () -> service.removeTag(s.id, gender.id), "UC-023 A2");
        Assignment a = service.assignments(s.id).stream().filter(x -> x.metadata().ontology.id.equals(gender.id)).findFirst().orElseThrow();
        service.unassign(s.id, a.metadata().id);
        service.removeTag(s.id, gender.id);
        assertEquals(2, service.tags(s.id).size());
    }

    @Test
    @TestTransaction
    void updateTag_onAnAssignedTagRecordsTheChange() {
        Survey s = fixture();
        Ontology age = tag(s, "Age");
        service.updateTag(s.id, age.id, "Age", "fixture_age");
        assertTrue(ledger.since(s.id).isEmpty(), "unchanged: nothing recorded");
        Ontology updated = service.updateTag(s.id, age.id, "Age at Diagnosis", "fixture_age");
        assertEquals("Age at Diagnosis", updated.tag);
        assertEquals(1, ledger.since(s.id).size(), "UC-023 A4: one record per assignment");
    }

    // ---- UC-024 --------------------------------------------------------------------------------

    @Test
    void impact_derivesTablesAndColumnsForTheFixture() {
        Impact impact = service.impact(fixture().id);
        assertEquals(List.of("dim_fixture_age", "dim_gender", "dim_has_children"),
                impact.tables().stream().map(ReportingService.DimTable::name).toList(), "UC-024 BR-001");
        assertTrue(impact.tables().get(0).sharedDimension());
        assertEquals(List.of("age_key", "gender_key", "has_children_key"),
                impact.columns().stream().map(ReportingService.FactColumn::column).toList());
        assertFalse(impact.hasErrors(), impact.problems().toString());
        assertTrue(impact.problems().isEmpty(), "every tag is assigned and every element exists: " + impact.problems());
    }

    @Test
    @TestTransaction
    void impact_flagsColumnCollisionsAndUnassignedTags() {
        Survey s = fixture();
        // (name, tag) uniqueness is case-sensitive, but the derived column is not: a collision.
        service.createTag(s.id, "has children", "fixture_age");
        Impact impact = service.impact(s.id);
        assertTrue(impact.hasErrors(), "UC-024 BR-002: two tags on one column");
        assertTrue(impact.problems().stream().anyMatch(p -> p.isError() && p.message().contains("has_children_key")), impact.problems().toString());
        assertTrue(impact.problems().stream().anyMatch(p -> !p.isError() && p.message().contains("is not used")), "UC-024 step 3");
        List<ValidationIssue> issues = validation.validate(s);
        assertTrue(issues.stream().anyMatch(i -> i.isError() && "ontology".equals(i.kind())), "UC-024 A2: blocks the export");
    }
}
