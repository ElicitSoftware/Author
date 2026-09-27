#!/usr/bin/env python3
"""
Transcribes a researcher's ElicitDesigner answers (fhhs_base.answers.md) into a skeleton
.elicit file for the Family Health History Survey: survey identity, steps, sections,
step/section mounts, reports and post-survey actions. No questions, answer lists, rules
or metadata -- those are the author's job after import.

Same record layouts and escaping as generate_elicit_designer.py.
"""
import uuid, sys

SURVEY_PREFIX = sys.argv[1] if len(sys.argv) > 1 else "0001"
NS = uuid.UUID("2c8e7a41-9b3d-5f60-8e12-4a7c1d9b5e33")           # stable namespace for element_keys
SURVEY_KEY = "7d4f2a9c-1e6b-5c83-9f47-2b8e6d1a4c55"

def ek(name): return str(uuid.uuid5(NS, name))

COUNTS = {"surveys": 12, "steps": 11, "sections": 11, "steps_sections": 12,
          "reports": 6, "post_survey_actions": 6}

def esc(v):
    if v is None: return ""
    s = str(v)
    return s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "\\r")

records = {k: [] for k in COUNTS}
def add(table, *fields):
    assert len(fields) == COUNTS[table], f"{table}: got {len(fields)}, need {COUNTS[table]}"
    records[table].append("|".join(esc(f) for f in fields))

SCD = (0, "", "", "fhhs-researcher", "Transcribed from ElicitDesigner answers")

# ----------------------------------------------------------------- survey identity
add("surveys", 1, SURVEY_KEY, "FamilyHistorySurvey", 1, "Family History",
    "This survey is designed to help you and your healthcare provider better understand your "
    "hereditary risk for cancer. By collecting information about your own health and the cancer "
    "history of your blood relatives - parents, siblings, children, grandparents, aunts, and "
    "uncles - this tool can support decisions about additional testing, personalize treatment, "
    "and ultimately improve care for you and your family.",
    f"{SURVEY_PREFIX}-0001-0000-0001-0000-0000-0000",
    "", "fhhs-researcher", "Transcribed from ElicitDesigner answers",
    # V019: the language this content is written in, and the set it is published in. This
    # skeleton publishes none; translations are authored in Author against the finished survey.
    "en", "")

# ----------------------------------------------------------------- structure
# One row per step, straight from the answers table:
#   (step name, report name, [Heading=ReportName, ...], [reused heading, ...])
CANCERS = "{CS2's|Your} Cancers"
STEPS = [
    ("Welcome",              "Welcome",              ["Introduction=Introduction"], []),
    ("Demographics",         "Demographics",         ["Demographics=Demographics"], []),
    ("Proband",              "Proband",              ["Proband=Proband"], []),
    ("Proband Cancer",       "Proband Cancer",       [f"{CANCERS}=Cancers"], []),
    ("Children",             "Child",                ["{G1's|Your} Child {S#} - {S1}=Child"], [CANCERS]),
    ("Siblings",             "Sibling",              ["{R1's|Your} Sibling {S#} - {S1}=Sibling"], [CANCERS]),
    ("Mother",               "Mother",               ["{G1's|Your} Mother=Mother"], [CANCERS]),
    ("Maternal Grandmother", "Maternal Grandmother", ["{G1's|Your} Maternal Grandmother=Maternal Grandmother"], [CANCERS]),
    ("Maternal Grandfather", "Maternal Grandfather", ["{G1's|Your} Maternal Grandfather=Maternal Grandfather"], [CANCERS]),
    ("Mother's Siblings",    "Mother's Sibling",     ["{G1's|Your} Mother's Siblings=Mother's Siblings"], [CANCERS]),
    ("Father",               "Father",               ["{G1's|Your} Father=Father"], [CANCERS]),
    ("Paternal Grandmother", "Paternal Grandmother", ["{G1's|Your} Paternal Grandmother=Paternal Grandmother"], [CANCERS]),
    ("Paternal Grandfather", "Paternal Grandfather", ["{G1's|Your} Paternal Grandfather=Paternal Grandfather"], [CANCERS]),
    ("Father's Siblings",    "Father's Sibling",     ["{G1's|Your} Father's Siblings=Father's Siblings"], [CANCERS]),
    ("Thank you",            "Thank you",            ["Thank you=Thank you"], []),
]

sections = {}            # heading -> (section_id, report_name)
ss_id = 100
for step_no, (name, report, new_sections, reused) in enumerate(STEPS, start=1):
    add("steps", step_no, ek(f"step:{name}"), step_no, name, report, "", *SCD)
    mounts = []
    for entry in new_sections:
        heading, _, sec_report = entry.partition("=")
        if heading in sections:
            raise SystemExit(f"step {step_no}: section '{heading}' was already defined; list it under reused")
        sections[heading] = (len(sections) + 1, sec_report or heading)
        mounts.append(heading)
    for heading in reused:
        if heading not in sections:
            raise SystemExit(f"step {step_no}: reused section '{heading}' has not been defined yet")
        mounts.append(heading)
    for order, heading in enumerate(mounts, start=1):
        sec_id = sections[heading][0]
        ss_id += 1
        dk = f"{SURVEY_PREFIX}-{step_no:04d}-0000-{order:04d}-0000-0000-0000"
        add("steps_sections", ss_id, ek(f"ss:{name}:{heading}"), step_no, step_no, sec_id, order, dk, *SCD)

for heading, (sec_id, report) in sections.items():
    add("sections", sec_id, ek(f"section:{heading}"), sec_id, heading, report, "", *SCD)

# ----------------------------------------------------------------- delivery + author notes
REPORTS = [("Respondent",      "http://host.docker.internal:8082/proband/report"),
           ("Cancer Summary",  "http://host.docker.internal:8082/casummary/report"),
           ("Family Pedigree", "http://host.docker.internal:8082/pedigree/report")]
for order, (rname, url) in enumerate(REPORTS):
    add("reports", order + 1, ek(f"report:{rname}"), rname, rname, url, order)

add("post_survey_actions", 1, ek("psa:Generate Family History Report"),
    "Generate Family History Report",
    "Builds the family history PDF and uploads it to the clinic's SFTP server after survey completion",
    "http://host.docker.internal:8082/familyhistory/generate", 1)

# ----------------------------------------------------------------- emit
ORDER = ["surveys", "steps", "sections", "steps_sections", "reports", "post_survey_actions"]
HEADER = ["surveys","select_groups","select_items","steps","sections","steps_sections",
          "questions","sections_questions","relationships","reports","post_survey_actions",
          "dimensions","ontology","metadata","translations"]

out = ["# ELICIT_SURVEY_EXPORT_V1", "# survey_id: 1", f"# survey_key: {SURVEY_KEY}",
       "# survey_name: FamilyHistorySurvey"]
out += [f"# {t}: {len(records.get(t, []))}" for t in HEADER]
out += ["# generated: 2026-09-17T21:00:00-04:00", "# survey_revision: 2026-09-17T21:00:00-04:00", ""]
for t in ORDER:
    out += [f"{t}: {line}" for line in records[t]]

open(sys.argv[2] if len(sys.argv) > 2 else "fhhs_base.elicit", "w").write("\n".join(out) + "\n")
for t in ORDER:
    print(f"  {t:22} {len(records[t]):3}  ({COUNTS[t]} fields each)")
print(f"\nTotal records: {sum(len(records[t]) for t in ORDER)}")
