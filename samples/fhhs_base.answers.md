# FHHS through the Elicit Designer — a researcher's answers

These are the answers a researcher gave to `ElicitDesigner.elicit` while trying to
recreate the Family Health History Survey (FHHS) from memory of the running survey.
`generate_fhhs_base.py` transcribes them, plus the author notes, into `fhhs_base.elicit`.

## Survey identity

| Question | Answer |
|---|---|
| Short machine name | `FamilyHistorySurvey` (the designer forbids spaces; the deployed survey is named `Family History Survey`) |
| Title respondents see | Family History |
| What is this survey for? | This survey is designed to help you and your healthcare provider better understand your hereditary risk for cancer. By collecting information about your own health and the cancer history of your blood relatives - parents, siblings, children, grandparents, aunts, and uncles - this tool can support decisions about additional testing, personalize treatment, and ultimately improve care for you and your family. |
| Owner email | fhhs-owner@example.org (not stored in the file) |
| Where respondents go afterwards | *(blank — default thank-you page)* |

## Structure

**How many steps?** 15

| # | Step name | Report name | Sections (Heading=ReportName) | Reused from an earlier step |
|---|---|---|---|---|
| 1 | Welcome | Welcome | Introduction=Introduction | |
| 2 | Demographics | Demographics | Demographics=Demographics | |
| 3 | Proband | Proband | Proband=Proband | |
| 4 | Proband Cancer | Proband Cancer | {CS2's\|Your} Cancers=Cancers | |
| 5 | Children | Child | {G1's\|Your} Child {S#} - {S1}=Child | {CS2's\|Your} Cancers |
| 6 | Siblings | Sibling | {R1's\|Your} Sibling {S#} - {S1}=Sibling | {CS2's\|Your} Cancers |
| 7 | Mother | Mother | {G1's\|Your} Mother=Mother | {CS2's\|Your} Cancers |
| 8 | Maternal Grandmother | Maternal Grandmother | {G1's\|Your} Maternal Grandmother=Maternal Grandmother | {CS2's\|Your} Cancers |
| 9 | Maternal Grandfather | Maternal Grandfather | {G1's\|Your} Maternal Grandfather=Maternal Grandfather | {CS2's\|Your} Cancers |
| 10 | Mother's Siblings | Mother's Sibling | {G1's\|Your} Mother's Siblings=Mother's Siblings | {CS2's\|Your} Cancers |
| 11 | Father | Father | {G1's\|Your} Father=Father | {CS2's\|Your} Cancers |
| 12 | Paternal Grandmother | Paternal Grandmother | {G1's\|Your} Paternal Grandmother=Paternal Grandmother | {CS2's\|Your} Cancers |
| 13 | Paternal Grandfather | Paternal Grandfather | {G1's\|Your} Paternal Grandfather=Paternal Grandfather | {CS2's\|Your} Cancers |
| 14 | Father's Siblings | Father's Sibling | {G1's\|Your} Father's Siblings=Father's Siblings | {CS2's\|Your} Cancers |
| 15 | Thank you | Thank you | Thank you=Thank you | |

The headings use the token syntax the Welcome step explains: `{G1's|Your}` reads
"Anna's" when someone is filling the survey in for Anna and "Your" otherwise; `{S#}`
is the instance number and `{S1}` the name entered for that child or sibling.

## Delivery

| Question | Answer |
|---|---|
| How should completed responses be delivered? | PDF report for the respondent; Webhook / REST callback |
| What should the report be called? | Respondent |
| Report or webhook endpoint URL | http://host.docker.internal:8082/proband/report |

## Review

**Confirmed:** yes.

**Anything else the author should know?**

> There are three reports, not one. Besides "Respondent" at the URL above, add
> "Cancer Summary" at http://host.docker.internal:8082/casummary/report and
> "Family Pedigree" at http://host.docker.internal:8082/pedigree/report, in that order.
>
> The webhook is a post-survey action, not a report: "Generate Family History Report"
> at http://host.docker.internal:8082/familyhistory/generate. It builds the family
> history PDF and uploads it to the clinic's SFTP server as soon as a respondent finishes.
>
> Steps 5, 6, 10 and 14 repeat once per child, sibling, maternal aunt/uncle and paternal
> aunt/uncle; the counts are asked on the Proband step (for children and siblings) and on
> the Mother and Father steps (for their siblings). The Proband Cancer step only appears
> when the proband says they have had cancer, and the Cancers section under every
> relative only appears when that relative is marked as having had cancer.
>
> The Cancers section is the same block everywhere: a checkbox per cancer type, and for
> each ticked type a "more than once?" question and an age-at-diagnosis question.
