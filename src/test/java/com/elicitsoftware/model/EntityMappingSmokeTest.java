package com.elicitsoftware.model;

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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every entity in {@code com.elicitsoftware.model} must load from the real Survey schema
 * (replayed from the test mirror) and the fixture rows must carry the identities Author relies on.
 * Traceability: UC-003 (create), UC-005 (import), UC-006 (browse), UC-008 (export).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class EntityMappingSmokeTest {

    @Inject
    EntityManager em;

    @ParameterizedTest
    @ValueSource(classes = {Survey.class, QuestionType.class, OperatorType.class, ActionType.class,
            Dimension.class, Ontology.class, Metadata.class,
            Step.class, Section.class, Question.class, SelectGroup.class, SelectItem.class,
            StepsSections.class, SectionsQuestion.class, Relationship.class})
    void everyEntityQueriesAgainstTheSchema(Class<?> entity) {
        List<?> rows = em.createQuery("select e from " + entity.getSimpleName() + " e", entity)
                .setMaxResults(1).getResultList();
        assertNotNull(rows);
    }

    @Test
    void fixtureQuestionIsAWorkingCopyWithKeyAndDurableId() {
        Question q = em.createQuery("select q from Question q where q.shortText = :st", Question.class)
                .setParameter("st", "FixtureQ3").getSingleResult();
        assertNotNull(q.questionKey, "element key");
        assertNotNull(q.questionId, "durable key");
        assertEquals(0, q.version);
        assertEquals(StructuralElement.EPOCH, q.effectiveFrom);
        assertTrue(q.isCurrent());
        assertFalse(q.isRetired());
        assertEquals("RADIO", q.questionType.name);
        assertNotNull(q.selectGroupId, "durable select group reference");
        assertEquals(0, q.selectGroupVersion);
    }

    @Test
    void fixtureSurveyHasStructureAndKey() {
        Survey s = em.createQuery("select s from Survey s where s.name = :n", Survey.class)
                .setParameter("n", "AuthorFixture").getSingleResult();
        assertNotNull(s.surveyKey);
        long steps = em.createQuery("select count(x) from Step x where x.surveyId = :sid", Long.class)
                .setParameter("sid", s.id).getSingleResult();
        long relationships = em.createQuery("select count(r) from Relationship r where r.surveyId = :sid", Long.class)
                .setParameter("sid", s.id).getSingleResult();
        assertEquals(2, steps);
        assertEquals(1, relationships);
        Relationship r = em.createQuery("select r from Relationship r where r.surveyId = :sid", Relationship.class)
                .setParameter("sid", s.id).getSingleResult();
        assertTrue(r.hasDownstreamTarget());
        assertEquals("SHOW", r.actionType.name);
    }
}
