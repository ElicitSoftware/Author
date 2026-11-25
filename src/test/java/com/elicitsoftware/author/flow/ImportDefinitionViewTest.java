package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.definition.ElicitFormat;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
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
        assertTrue(text.contains(view.getTranslation("importDefinitionView.recordsImported", String.valueOf(expected), "ElicitDesigner.elicit")), text);
        Button open = find(Button.class, view.result).withText(view.getTranslation("importDefinitionView.openInEditor")).single();
        assertTrue(open.isEnabled());
        assertEquals(1, Survey.count("name", "ElicitDesigner"));
    }

    /**
     * UC-005 step 6: an import writes rows, it does not judge them. The same checks the editor and
     * the export dialog run are shown where the file lands, so a draft that is well-formed as a file
     * but holds a survey no respondent can use says so before anyone opens the editor.
     */
    @Test
    @TestTransaction
    @TestSecurity(user = "import.author", roles = {"elicit_author"})
    void uploadedDefinitionIsValidatedInPlace() throws IOException {
        ImportDefinitionView view = navigate(ImportDefinitionView.class);

        view.handleUpload(Files.readAllBytes(Path.of("samples", "ElicitDesigner.elicit")), "ElicitDesigner.elicit");

        assertTrue(find(H3.class, view.result).all().stream().map(H3::getText)
                        .anyMatch(view.getTranslation("importDefinitionView.validation")::equals),
                "the validation heading is shown after an import");
    }

    @Test
    @TestSecurity(user = "import.author", roles = {"elicit_author"})
    void rejectedFileShowsTheReason() {
        ImportDefinitionView view = navigate(ImportDefinitionView.class);
        view.handleUpload("not a definition".getBytes(StandardCharsets.UTF_8), "junk.elicit");
        String text = find(Paragraph.class, view.result).single().getText();
        assertTrue(text.startsWith(view.getTranslation("importDefinitionView.importFailed", "")), text);
    }

    /** UC-005 A6, NFR-023: the 5 MB limit holds for what reaches the server, whatever the browser allowed. */
    @Test
    @TestTransaction
    @TestSecurity(user = "import.author", roles = {"elicit_author"})
    void aFileOverTheLimitIsRefusedOnTheServerAndNothingIsImported() {
        long before = Survey.count();
        ImportDefinitionView view = navigate(ImportDefinitionView.class);
        view.handleUpload(new byte[ElicitFormat.MAX_FILE_BYTES + 1], "large.elicit");
        assertEquals(view.getTranslation("error.handoff.tooLarge", "5"),
                find(Paragraph.class, view.result).single().getText());
        assertTrue(find(Button.class, view.result).all().isEmpty(), "nothing to open");
        assertEquals(before, Survey.count(), "nothing was imported");
    }
}
