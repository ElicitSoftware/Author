---
-- ***LICENSE_START***
-- Elicit Author
-- %%
-- Copyright (C) 2026 Elicit Software LLC
-- %%
-- PolyForm Noncommercial License 1.0.0
-- <https://polyformproject.org/licenses/noncommercial/1.0.0>
-- ***LICENSE_END***
---

-- Test-only fixture (V9xxx, above every Survey migration version): one populated survey
-- and one empty survey. Every row is a plain working copy (version 0, epoch / sentinel
-- effective range) exactly as Author writes them. Keys are generated here; tests locate
-- rows by name / short_text, never by id.
DO $$
DECLARE
  v_survey_id   bigint;
  v_step1_id    bigint;
  v_step2_id    bigint;
  v_sec1_id     bigint;
  v_sec2_id     bigint;
  v_sec3_id     bigint;
  v_sg_id       bigint;
  v_q1_id       bigint;
  v_q2_id       bigint;
  v_q3_id       bigint;
  v_q4_id       bigint;
  v_q5_id       bigint;
  v_sq1_id      bigint;
  v_sq2_id      bigint;
  v_q4_dur      bigint;
  v_sq1_dur     bigint;
  v_ss2_dur     bigint;
  v_dim_id      bigint;
  v_ont_age     bigint;
  v_ont_child   bigint;
  v_ont_gender  bigint;
BEGIN
  INSERT INTO survey.surveys(id, name, display_order, title, description, initial_display_key, post_survey_url, survey_key)
  VALUES (NEXTVAL('survey.surveys_seq'), 'AuthorFixture', 901,
          'Author Fixture', 'Populated survey used by the Author test suite.',
          NULL, NULL, gen_random_uuid())
  RETURNING id INTO v_survey_id;

  UPDATE survey.surveys
     SET initial_display_key = LPAD(v_survey_id::text, 4, '0') || '-0001-0000-0001-0000-0000-0000'
   WHERE id = v_survey_id;

  INSERT INTO survey.steps(id, survey_id, display_order, name, dimension_name, description, step_key)
  VALUES (NEXTVAL('survey.steps_seq'), v_survey_id, 1, 'Fixture Step One', 'Fixture Step One', 'First step', gen_random_uuid())
  RETURNING id INTO v_step1_id;
  INSERT INTO survey.steps(id, survey_id, display_order, name, dimension_name, description, step_key)
  VALUES (NEXTVAL('survey.steps_seq'), v_survey_id, 2, 'Fixture Step Two', 'Fixture Step Two', 'Second step', gen_random_uuid())
  RETURNING id INTO v_step2_id;

  INSERT INTO survey.sections(id, survey_id, display_order, name, dimension_name, description, section_key)
  VALUES (NEXTVAL('survey.sections_seq'), v_survey_id, 1, 'Fixture Section A', 'Fixture Section A', 'Section A', gen_random_uuid())
  RETURNING id INTO v_sec1_id;
  INSERT INTO survey.sections(id, survey_id, display_order, name, dimension_name, description, section_key)
  VALUES (NEXTVAL('survey.sections_seq'), v_survey_id, 2, 'Fixture Section B', 'Fixture Section B', 'Section B', gen_random_uuid())
  RETURNING id INTO v_sec2_id;
  INSERT INTO survey.sections(id, survey_id, display_order, name, dimension_name, description, section_key)
  VALUES (NEXTVAL('survey.sections_seq'), v_survey_id, 3, 'Fixture Section C', 'Fixture Section C', 'Section C', gen_random_uuid())
  RETURNING id INTO v_sec3_id;

  INSERT INTO survey.steps_sections(id, survey_id, step_id, step_display_order, section_id, section_display_order, display_key, steps_sections_key)
  VALUES (NEXTVAL('survey.steps_sections_seq'), v_survey_id, v_step1_id, 1, v_sec1_id, 1,
          LPAD(v_survey_id::text, 4, '0') || '-0001-0000-0001-0000-0000-0000', gen_random_uuid());
  INSERT INTO survey.steps_sections(id, survey_id, step_id, step_display_order, section_id, section_display_order, display_key, steps_sections_key)
  VALUES (NEXTVAL('survey.steps_sections_seq'), v_survey_id, v_step1_id, 1, v_sec2_id, 2,
          LPAD(v_survey_id::text, 4, '0') || '-0001-0000-0002-0000-0000-0000', gen_random_uuid())
  RETURNING steps_sections_id INTO v_ss2_dur;
  INSERT INTO survey.steps_sections(id, survey_id, step_id, step_display_order, section_id, section_display_order, display_key, steps_sections_key)
  VALUES (NEXTVAL('survey.steps_sections_seq'), v_survey_id, v_step2_id, 2, v_sec3_id, 1,
          LPAD(v_survey_id::text, 4, '0') || '-0002-0000-0001-0000-0000-0000', gen_random_uuid());

  INSERT INTO survey.select_groups(id, survey_id, name, description, data_type, select_group_key)
  VALUES (NEXTVAL('survey.select_groups_seq'), v_survey_id, 'FixtureYesNo', 'Yes / No', 'Text', gen_random_uuid())
  RETURNING id INTO v_sg_id;
  INSERT INTO survey.select_items(id, survey_id, select_group_id, display_text, display_order, coded_value, select_item_key)
  VALUES (NEXTVAL('survey.select_items_seq'), v_survey_id, v_sg_id, 'Yes', 1, 'Y', gen_random_uuid());
  INSERT INTO survey.select_items(id, survey_id, select_group_id, display_text, display_order, coded_value, select_item_key)
  VALUES (NEXTVAL('survey.select_items_seq'), v_survey_id, v_sg_id, 'No', 2, 'N', gen_random_uuid());

  -- type_id 8 = TEXT, 5 = INTEGER, 7 = RADIO, 4 = HTML (survey.question_types, seeded by V003).
  INSERT INTO survey.questions(id, survey_id, type_id, text, short_text, required, question_key)
  VALUES (NEXTVAL('survey.questions_seq'), v_survey_id, 4, '<p>Welcome to the fixture survey.</p>', 'FixtureQ1', false, gen_random_uuid())
  RETURNING id INTO v_q1_id;
  INSERT INTO survey.questions(id, survey_id, type_id, text, short_text, required, question_key)
  VALUES (NEXTVAL('survey.questions_seq'), v_survey_id, 8, 'What is your name?', 'FixtureQ2', true, gen_random_uuid())
  RETURNING id INTO v_q2_id;
  INSERT INTO survey.questions(id, survey_id, type_id, text, short_text, required, select_group_id, question_key)
  VALUES (NEXTVAL('survey.questions_seq'), v_survey_id, 7, 'Do you have children?', 'FixtureQ3', true, v_sg_id, gen_random_uuid())
  RETURNING id INTO v_q3_id;
  INSERT INTO survey.questions(id, survey_id, type_id, text, short_text, required, min_value, max_value, question_key)
  VALUES (NEXTVAL('survey.questions_seq'), v_survey_id, 5, 'How many children do you have?', 'FixtureQ4', false, 0, 20, gen_random_uuid())
  RETURNING id, question_id INTO v_q4_id, v_q4_dur;
  INSERT INTO survey.questions(id, survey_id, type_id, text, short_text, required, question_key)
  VALUES (NEXTVAL('survey.questions_seq'), v_survey_id, 8, 'Anything else?', 'FixtureQ5', false, gen_random_uuid())
  RETURNING id INTO v_q5_id;

  INSERT INTO survey.sections_questions(id, survey_id, question_id, section_id, display_order, sections_question_key)
  VALUES (NEXTVAL('survey.sections_questions_seq'), v_survey_id, v_q1_id, v_sec1_id, 1, gen_random_uuid());
  INSERT INTO survey.sections_questions(id, survey_id, question_id, section_id, display_order, sections_question_key)
  VALUES (NEXTVAL('survey.sections_questions_seq'), v_survey_id, v_q2_id, v_sec1_id, 2, gen_random_uuid());
  INSERT INTO survey.sections_questions(id, survey_id, question_id, section_id, display_order, sections_question_key)
  VALUES (NEXTVAL('survey.sections_questions_seq'), v_survey_id, v_q3_id, v_sec2_id, 1, gen_random_uuid())
  RETURNING id, sections_question_id INTO v_sq1_id, v_sq1_dur;
  INSERT INTO survey.sections_questions(id, survey_id, question_id, section_id, display_order, sections_question_key)
  VALUES (NEXTVAL('survey.sections_questions_seq'), v_survey_id, v_q4_id, v_sec2_id, 2, gen_random_uuid())
  RETURNING id INTO v_sq2_id;
  INSERT INTO survey.sections_questions(id, survey_id, question_id, section_id, display_order, sections_question_key)
  VALUES (NEXTVAL('survey.sections_questions_seq'), v_survey_id, v_q5_id, v_sec3_id, 1, gen_random_uuid());

  -- operator_id 3 = EQUAL, action_id 1 = SHOW: show "how many children" when "children" = Y.
  INSERT INTO survey.relationships(id, survey_id, upstream_step_id, upstream_sq_id,
                                    downstream_step_id, downstream_ss_id, downstream_sq_id,
                                    operator_id, action_id, description, token, reference_value, relationship_key)
  VALUES (NEXTVAL('survey.relationships_seq'), v_survey_id, v_step1_id, v_sq1_id,
          NULL, NULL, v_sq2_id,
          3, 1, 'Show child count when children = Y', NULL, 'Y', gen_random_uuid());

  -- Reporting tags (UC-021..UC-024): one shared dimension, one tag on it, two tags with their own
  -- tables, and one assignment per element scope. Element columns carry durable ids.
  INSERT INTO survey.dimensions(id, name) VALUES (NEXTVAL('survey.dimensions_seq'), 'fixture_age')
  RETURNING id INTO v_dim_id;
  INSERT INTO survey.ontology(id, survey_id, name, tag, dimension)
  VALUES (NEXTVAL('survey.ontology_seq'), v_survey_id, 'AuthorFixture', 'Age', v_dim_id)
  RETURNING id INTO v_ont_age;
  INSERT INTO survey.ontology(id, survey_id, name, tag, dimension)
  VALUES (NEXTVAL('survey.ontology_seq'), v_survey_id, 'AuthorFixture', 'Has Children', NULL)
  RETURNING id INTO v_ont_child;
  INSERT INTO survey.ontology(id, survey_id, name, tag, dimension)
  VALUES (NEXTVAL('survey.ontology_seq'), v_survey_id, 'AuthorFixture', 'Gender', NULL)
  RETURNING id INTO v_ont_gender;
  -- FixtureQ4 wherever it appears -> Age (the answer is the value)
  INSERT INTO survey.metadata(id, survey_id, steps_sections_id, question_id, sections_question_id, ontology_id, value)
  VALUES (NEXTVAL('survey.metadata_seq'), v_survey_id, NULL, v_q4_dur, NULL, v_ont_age, NULL);
  -- FixtureQ3 only in Section B -> Has Children (the answer is the value)
  INSERT INTO survey.metadata(id, survey_id, steps_sections_id, question_id, sections_question_id, ontology_id, value)
  VALUES (NEXTVAL('survey.metadata_seq'), v_survey_id, NULL, NULL, v_sq1_dur, v_ont_child, NULL);
  -- Section B as mounted in Step One -> Gender = Female (a constant)
  INSERT INTO survey.metadata(id, survey_id, steps_sections_id, question_id, sections_question_id, ontology_id, value)
  VALUES (NEXTVAL('survey.metadata_seq'), v_survey_id, v_ss2_dur, NULL, NULL, v_ont_gender, 'Female');

  -- Empty survey for create / delete / import tests.
  INSERT INTO survey.surveys(id, name, display_order, title, description, survey_key)
  VALUES (NEXTVAL('survey.surveys_seq'), 'AuthorFixtureEmpty', 902,
          'Author Fixture (empty)', 'Survey with no structure.', gen_random_uuid());
END $$;
