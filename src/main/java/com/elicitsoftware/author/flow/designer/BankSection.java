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

/** Drag payload of a section in the drawer's Sections panel: dropped into a lane it is mounted there (UC-015 A6). */
public record BankSection(int sectionId, String label) { }
