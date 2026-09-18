package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-005 through the Import definition view. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ImportDefinitionViewTest extends QuarkusBrowserlessTest {

    @Test
    @TestTransaction
    @TestSecurity(user = "import.author", roles = {"elicit_author"})
    void uploadingTheSampleShowsCountsAndOpenButton() throws IOException {
        ImportDefinitionView view = navigate(ImportDefinitionView.class);
        byte[] sample = Files.readAllBytes(Path.of("samples", "ElicitDesigner.elicit"));

        view.handleUpload(sample, "ElicitDesigner.elicit");

        String text = find(Paragraph.class, view.result).all().stream().map(Paragraph::getText).reduce("", String::concat);
        int expected = com.elicitsoftware.author.definition.SurveyDefinitionImporterTest.headerCounts().values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(text.contains(expected + " records imported"), text);
        Button open = find(Button.class, view.result).withText("Open in editor").single();
        assertTrue(open.isEnabled());
        assertEquals(1, Survey.count("name", "ElicitDesigner"));
    }

    @Test
    @TestSecurity(user = "import.author", roles = {"elicit_author"})
    void rejectedFileShowsTheReason() {
        ImportDefinitionView view = navigate(ImportDefinitionView.class);
        view.handleUpload("not a definition".getBytes(StandardCharsets.UTF_8), "junk.elicit");
        String text = find(Paragraph.class, view.result).single().getText();
        assertTrue(text.startsWith("Import failed"), text);
    }
}
