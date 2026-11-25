package com.elicitsoftware.model;

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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * One field of one survey element in one language (Survey V019; UC-043).
 * <p>
 * The ninth Type 2 table and the first that is not structural, so it extends
 * {@link StructuralElement} like the other eight and inherits retire, restore and the element-key
 * minting unchanged. What differs is how it attaches to the rest of the schema: it holds no durable
 * id of any other table, only the {@code *_key} of the element it translates and the name of the
 * base column. Versioning a question therefore leaves its translations attached, and retiring one
 * must close them explicitly ({@code ElementService}).
 * <p>
 * Author never renders a translation outside the Translations view: the designer, the dialogs and
 * the previews show the survey's base language, so an author always edits the text they see
 * (UC-040 BR-005).
 */
@Entity
@Table(name = "translations", schema = "survey")
public class Translation extends StructuralElement {

    @Id
    @SequenceGenerator(name = "TRANSLATIONS_ID_GENERATOR", schema = "survey", sequenceName = "translations_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSLATIONS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    /** {@code surveys}, {@code steps}, {@code sections}, {@code questions}, {@code select_items}, {@code relationships} or {@code reports}. */
    @Column(name = "element_type", nullable = false, length = 32)
    public String elementType;

    /**
     * The {@code *_key} of the <em>translated</em> element; never a surrogate or durable id. Named
     * {@code targetKey} rather than {@code elementKey} because {@link StructuralElement} already
     * uses "element key" for a row's own cross-instance identity, which here is
     * {@link #translationKey}. The database column keeps the name the file format uses.
     */
    @Column(name = "element_key", nullable = false)
    public UUID targetKey;

    /** Column name of the base text this row translates, from the translatable-field allowlist. */
    @Column(name = "field", nullable = false, length = 32)
    public String field;

    /** BCP-47 tag, as the mounted chrome bundles write it: {@code es-419}, {@code ar}. */
    @Column(name = "language", nullable = false, length = 35)
    public String language;

    /** The translated text. Removing a translation closes the row; it is never emptied. */
    @Column(name = "value", nullable = false)
    public String value;

    /**
     * Lower-case hex SHA-256 of the UTF-8 bytes of the base text this was translated from. It is
     * recomputed from the element's current base text on every write, so a translation imported
     * against unchanged text lands current and one imported against changed text lands stale.
     */
    @Column(name = "source_hash", nullable = false, length = 64)
    public String sourceHash;

    /**
     * Snapshot of that base text, for the out-of-date diff in the Translations view. Authoring-only
     * and never exported, on the same footing as {@code questions.sample} (FR-036).
     */
    @Column(name = "source_text")
    public String sourceText;

    @Column(name = "translation_id", nullable = false)
    public Integer translationId;

    @Column(name = "translation_key", nullable = false, updatable = false)
    public UUID translationKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return translationId; }
    @Override public void setDurableId(Integer durableId) { this.translationId = durableId; }
    @Override public UUID getElementKey() { return translationKey; }
    @Override public void setElementKey(UUID elementKey) { this.translationKey = elementKey; }
    @Override public String durableSequence() { return "survey.translations_durable_seq"; }
}
