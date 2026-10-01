# `questions.mask` is inert, and Author advertises it anyway

Carried over from the umbrella's `samples/FINDINGS.md` when Author moved to its own
repository. Recorded 2026-09-29; line references are to `Survey/` on `V3` and this repo
at that date. Not yet fixed.

`ElicitComponent.setInputMask` (line 212) is `private` and has no callers anywhere in
`Survey/src`. The column round-trips through export and import and is never applied.

Author shows an "Input mask" field (`QuestionDialog.java:68`) with the help text *"A pattern
that constrains what the respondent can type."* — so the UI offers a control with no runtime
effect. It is shown only for text-like types (`QuestionDialog.java:291`).

Neither FHHS nor the umbrella's census household sample sets a mask on any question.

Note that the method targets `HasAllowedCharPattern`, a character filter. It cannot display
a symbol, so it is not a route to currency formatting; Vaadin's `NumberField` has no format
hook, and its only currency affordance is `setPrefixComponent`.
