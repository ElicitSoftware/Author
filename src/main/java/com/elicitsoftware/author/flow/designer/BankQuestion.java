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

/** Drag payload of a question in the drawer's Questions panel: dropped into a card it is reused there (UC-011 A6). */
public record BankQuestion(int questionId, String label, String questionType) { }
