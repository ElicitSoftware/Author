package com.elicitsoftware.author.flow.designer;

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

/**
 * Drag payload of a list in the drawer's Lists panel: dropped on a choice question it becomes its options (UC-014).
 *
 * @param selectGroupId the list's select group id
 * @param name          the list's name
 */
public record PaletteList(int selectGroupId, String name) { }
