#!/usr/bin/env python3
"""
Generates the Elicit Survey Designer meta-survey as a .elicit import file.

Field layouts follow the post-Survey-V015 ELICIT_SURVEY_EXPORT_V1 contract (no trailing
is_draft field), as read by Author's ElicitFormat and Admin's SurveyDefinitionImportService.
Required field counts are asserted against the reader's own checks so the output cannot drift.
"""
import uuid, sys

# Leading component of every display key. Cosmetic since the importer rebases it onto the
# id the target deployment allocates (SurveyDefinitionFileFields#rebaseDisplayKey); kept
# parameterised so the generated file is readable for whichever survey id you expect.
SURVEY_PREFIX = sys.argv[1] if len(sys.argv) > 1 else "0001"
NS = uuid.UUID("6f1d5b60-0e2a-5c31-9a44-2b7e0d6c8f10")          # stable namespace for element_keys
SURVEY_KEY = "b3a7c2e4-5d18-4f92-8a60-1c9e3f75d2b4"

def ek(name): return str(uuid.uuid5(NS, name))

# importer's required field counts (ElicitFormat.REQUIRED_FIELDS in Author, mirrored by Admin).
# Since Survey V015 the Type 2 records end with published_comment; the old trailing is_draft
# field is gone (readers still accept it on files written before V015).
COUNTS = {
    "surveys": 10, "select_groups": 10, "select_items": 11, "steps": 11,
    "sections": 11, "steps_sections": 12, "questions": 20,
    "sections_questions": 10, "relationships": 19,
}

def esc(v):
    if v is None: return ""
    s = str(v)
    return s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "\\r")

records = {k: [] for k in COUNTS}

def add(table, *fields):
    assert len(fields) == COUNTS[table], f"{table}: got {len(fields)}, need {COUNTS[table]}"
    records[table].append("|".join(esc(f) for f in fields))

# SCD tail shared by every versioned record: version, effective_from, effective_to,
# published_by, published_comment.  The importer does not read these on a create-import
# (DB defaults apply) but the field count must still be exact.
SCD = (0, "", "", "elicit-designer", "Initial publication")

# ----------------------------------------------------------------- question types
QTYPE = {"CHECKBOX":1,"DATE_PICKER":2,"COMBOBOX":3,"HTML":4,"INTEGER":5,"DOUBLE":6,
         "RADIO":7,"TEXT":8,"TEXTAREA":9,"MULTI_SELECT":10,"CHECKBOX_GROUP":11,
         "DATETIME":12,"EMAIL":13,"PASSWORD":14}
OPERATOR = {"BOOLEAN":1,"GTE":2,"EQUAL":3,"NOT_EQUAL":4,"FIELD_EXIST":5,"CONTAINS":6}
ACTION = {"SHOW":1,"REPEAT":2,"TEXT":3}

# ----------------------------------------------------------------- survey
add("surveys", 1, SURVEY_KEY, "ElicitDesigner", 1,
    "Elicit Survey Designer",
    "A meta-survey: it interviews you about the survey you want to build, and its "
    "completed responses are the specification an author turns into a .elicit "
    "definition file for import into any Elicit deployment.",
    f"{SURVEY_PREFIX}-0001-0000-0001-0000-0000-0000",
    "", "elicit-designer", "Initial publication")

# ----------------------------------------------------------------- select groups
SG = {}
def select_group(gid, name, desc, data_type, items):
    SG[name] = gid
    add("select_groups", gid, ek(f"sg:{name}"), name, desc, data_type, *SCD)
    for i, (text, coded) in enumerate(items, start=1):
        add("select_items", gid*1000+i, ek(f"si:{name}:{coded}"), gid, text, i, coded, *SCD)

select_group(1, "AnswerType", "Elicit question types, coded to the engine's type_id", "Text",
    [("Short text box","8"),("Long text area","9"),("Whole number","5"),("Decimal number","6"),
     ("Yes / no checkbox","1"),("Single choice — dropdown","3"),("Single choice — radio buttons","7"),
     ("Multiple choice — checkboxes","11"),("Multiple choice — dropdown","10"),
     ("Date","2"),("Date and time","12"),("Email address","13"),("Password","14"),
     ("Informational text (no answer)","4")])

select_group(2, "YesNo", "Simple yes/no", "Boolean", [("Yes","1"),("No","0")])

select_group(3, "RuleOperator", "Comparison operators, coded to the engine's operator_id", "Text",
    [("Is checked / is true","1"),("Is greater than or equal to","2"),("Is exactly equal to","3"),
     ("Is not equal to","4"),("Has been answered at all","5"),("Contains the value","6")])

select_group(4, "RuleAction", "Rule actions, coded to the engine's action_id", "Text",
    [("Show the target","1"),("Repeat the target N times","2"),("Replace the target's text","3")])

select_group(5, "Requirement", "Whether an answer is mandatory", "Text",
    [("Required — cannot continue without it","1"),("Optional","0")])

select_group(6, "DeliveryChannel", "How results leave the system", "Text",
    [("PDF report for the respondent","PDF"),("PDF report for staff","PDF_STAFF"),
     ("Webhook / REST callback","WEBHOOK"),("Email notification","EMAIL"),
     ("Nothing — data stays in Elicit","NONE")])

# ----------------------------------------------------------------- steps
STEPS = [(1,"Welcome"),(2,"Survey Identity"),(3,"Structure"),(4,"Questions"),
         (5,"Answer Lists"),(6,"Conditional Logic"),(7,"Delivery"),(8,"Generate")]
for order, name in STEPS:
    add("steps", order, ek(f"step:{name}"), order, name, "", f"{name} step", *SCD)

# ----------------------------------------------------------------- sections
# (section_display_order is global across the survey, matching the engine's display_key scheme)
SECTIONS = [
    (1,1,"Before You Begin"),      (2,2,"Naming and Purpose"),
    (3,3,"How Many Steps"),        (4,3,"Describe Each Step"),
    (5,4,"How Many Questions"),    (6,4,"Describe Each Question"),
    (7,5,"How Many Answer Lists"), (8,5,"Describe Each Answer List"),
    (9,6,"Conditional Logic"),     (10,6,"Describe Each Rule"),
    (11,7,"Reports and Follow-up"),(12,8,"Review and Produce"),
]
for order, step_order, name in SECTIONS:
    add("sections", order, ek(f"section:{name}"), order, name, "", f"{name} section", *SCD)

# ----------------------------------------------------------------- steps_sections
SS = {}
for order, step_order, name in SECTIONS:
    ssid = 100 + order
    SS[name] = ssid
    dk = f"{SURVEY_PREFIX}-{step_order:04d}-0000-{order:04d}-0000-0000-0000"
    add("steps_sections", ssid, ek(f"ss:{name}"), step_order, step_order, order, order, dk, *SCD)

# ----------------------------------------------------------------- questions
QID = {}
_next = [1000]
def question(tag, qtype, text, section, order, *, required="false", short=None, tip="",
             minv="", maxv="", validation="", group=None, placeholder="", default="", variant=""):
    qid = _next[0]; _next[0] += 1
    QID[tag] = qid
    add("questions", qid, ek(f"q:{tag}"), QTYPE[qtype], text, short or tag, tip, required,
        minv, maxv, validation, SG[group] if group else "", "", placeholder, default, variant, *SCD)
    sqid = 5000 + qid
    QID["sq:" + tag] = sqid
    add("sections_questions", sqid, ek(f"sq:{tag}"), qid, section, order, *SCD)
    return qid

question("intro", "HTML",
    "<h2>Design a new Elicit survey</h2><p>This tool interviews you about the survey you "
    "want to build. Answer as much as you can — every answer maps onto one row of the "
    "<code>.elicit</code> definition file that gets imported into an Elicit deployment.</p>"
    "<p>You can stop and resume; nothing is published until an author reviews your answers.</p>",
    1, 1)

# --- Naming and Purpose
question("name", "TEXT", "Short machine name for the survey (letters and digits, no spaces)",
    2, 1, required="true", short="SurveyName",
    tip="Becomes surveys.name. Must be unique within the deployment.",
    validation="Use letters and digits only, no spaces.", placeholder="LibraryCardReg")
question("title", "TEXT", "Title respondents will see at the top of every page",
    2, 2, required="true", short="SurveyTitle", placeholder="Public Library Card Registration")
question("purpose", "TEXTAREA", "What is this survey for? Who fills it in, and what happens to the answers?",
    2, 3, required="true", short="Purpose")
question("owner_email", "EMAIL", "Email address of the person who owns this survey",
    2, 4, required="true", short="OwnerEmail", validation="Enter a valid email address.")
question("post_url", "TEXT", "Where should respondents be sent after they finish? (leave blank for the default thank-you page)",
    2, 5, short="PostSurveyUrl", placeholder="https://example.org/thank-you")

# --- Structure
question("step_count", "INTEGER", "How many steps (pages) should the survey have?",
    3, 1, required="true", short="StepCount", minv="1", maxv="20",
    tip="Each step is one page the respondent moves through.",
    validation="Enter a whole number between 1 and 20.")
question("step_name", "TEXT", "Name of this step, as shown in the progress bar",
    4, 1, required="true", short="StepName")
question("step_section_count", "INTEGER", "How many sections (headed groups of questions) are on this step?",
    4, 2, required="true", short="SectionCount", minv="1", maxv="10",
    validation="Enter a whole number between 1 and 10.")
question("section_heading", "TEXT", "Heading for the first section on this step",
    4, 3, short="SectionHeading")

# --- Questions
question("question_count", "INTEGER", "How many questions does the whole survey have?",
    5, 1, required="true", short="QuestionCount", minv="1", maxv="200",
    validation="Enter a whole number between 1 and 200.")
question("q_text", "TEXTAREA", "Question wording, exactly as the respondent should read it",
    6, 1, required="true", short="QuestionText")
question("q_type", "COMBOBOX", "What kind of answer does this question take?",
    6, 2, required="true", short="AnswerType", group="AnswerType")
question("q_required", "RADIO", "Must this question be answered?",
    6, 3, required="true", short="Required", group="Requirement")
question("q_short", "TEXT", "Short label for exports, reports and column headings",
    6, 4, required="true", short="ShortLabel",
    tip="Becomes questions.short_text — keep it under about 30 characters.")
question("q_list", "TEXT", "Which answer list does this question use? (name it from step 5)",
    6, 5, short="AnswerList",
    tip="Only applies to choice questions.")
question("q_help", "TEXT", "Optional help text or tooltip shown beside the question",
    6, 6, short="HelpText")
question("q_validation", "TEXT", "Message to show when the answer fails validation",
    6, 7, short="ValidationText", placeholder="Enter a whole number between 1 and 10.")

# --- Answer Lists
question("list_count", "INTEGER", "How many reusable answer lists does the survey need?",
    7, 1, required="true", short="ListCount", minv="0", maxv="50",
    tip="An answer list is a set of choices shared by one or more questions.",
    validation="Enter a whole number between 0 and 50.")
question("list_name", "TEXT", "Name of this answer list", 8, 1, required="true", short="ListName",
    placeholder="PreferredContactMethod")
question("list_options", "TEXTAREA",
    "The options, one per line, written as Label=StoredValue",
    8, 2, required="true", short="ListOptions",
    tip="Example: Email=EMAIL on one line, Phone=PHONE on the next.")

# --- Conditional Logic
question("needs_logic", "CHECKBOX", "Does any part of this survey appear, repeat or change based on an earlier answer?",
    9, 1, short="NeedsLogic")
question("rule_count", "INTEGER", "How many such rules are there?",
    9, 2, short="RuleCount", minv="1", maxv="50",
    validation="Enter a whole number between 1 and 50.")
question("rule_when", "TEXT", "When this question is answered…  (give its short label)",
    10, 1, required="true", short="RuleTrigger")
question("rule_operator", "COMBOBOX", "…and the answer…", 10, 2, required="true",
    short="RuleOperator", group="RuleOperator")
question("rule_value", "TEXT", "…this value…", 10, 3, short="RuleValue",
    tip="Leave blank for 'is checked' or 'has been answered at all'.")
question("rule_action", "COMBOBOX", "…then do this:", 10, 4, required="true",
    short="RuleAction", group="RuleAction")
question("rule_target", "TEXT", "…to this question, section or step (give its short label or name)",
    10, 5, required="true", short="RuleTarget")

# --- Delivery
question("channels", "CHECKBOX_GROUP", "How should completed responses be delivered?",
    11, 1, short="DeliveryChannels", group="DeliveryChannel")
question("report_name", "TEXT", "What should the report be called?", 11, 2, short="ReportName")
question("report_url", "TEXT", "Report or webhook endpoint URL", 11, 3, short="ReportUrl",
    placeholder="http://report-service:8080/report")

# --- Review
question("outro", "HTML",
    "<h3>What happens next</h3><p>Your answers become the specification for a "
    "<code>.elicit</code> file. An author maps each answer onto the matching record — "
    "steps, sections, questions, answer lists and rules — and the finished file is "
    "uploaded through <em>Apply Survey Definition</em> in the Admin app.</p>",
    12, 1)
question("confirm", "CHECKBOX", "I confirm the details above are complete and ready for review",
    12, 2, required="true", short="Confirmed")
question("notes", "TEXTAREA", "Anything else the author should know before building this survey?",
    12, 3, short="AuthorNotes")

# ----------------------------------------------------------------- relationships
_rel = [1]
def rel(desc, upstream_tag, operator, action, *, ref="", down_sq=None, down_ss=None,
        down_step=None, up_step=None, token=""):
    rid = _rel[0]; _rel[0] += 1
    add("relationships", rid, ek(f"rel:{desc}"),
        up_step or "", QID["sq:" + upstream_tag], down_step or "",
        SS[down_ss] if down_ss else "", QID["sq:" + down_sq] if down_sq else "",
        OPERATOR[operator], ACTION[action], desc, token, ref, "", "", *SCD)

# Repeat the per-step block once for every step the author asked for.
rel("Repeat the step description block once per step", "step_count", "GTE", "REPEAT",
    ref="1", down_ss="Describe Each Step")
# Repeat the per-question block once for every question.
rel("Repeat the question description block once per question", "question_count", "GTE", "REPEAT",
    ref="1", down_ss="Describe Each Question")
# Repeat the per-list block once for every answer list.
rel("Repeat the answer-list block once per list", "list_count", "GTE", "REPEAT",
    ref="1", down_ss="Describe Each Answer List")
# Only ask how many rules there are if the author said logic is needed.
rel("Ask for a rule count only when conditional logic is needed", "needs_logic", "BOOLEAN",
    "SHOW", down_sq="rule_count")
# Repeat the per-rule block once for every rule.
rel("Repeat the rule description block once per rule", "rule_count", "GTE", "REPEAT",
    ref="1", down_ss="Describe Each Rule")
# The choice-list question only matters for choice-typed questions.
rel("Ask which answer list to use only for single-choice dropdown questions", "q_type",
    "EQUAL", "SHOW", ref="3", down_sq="q_list")
# Report name/URL only matter if a report channel was chosen.
rel("Ask for a report name only when a PDF report was requested", "channels", "CONTAINS",
    "SHOW", ref="PDF", down_sq="report_name")
rel("Ask for an endpoint only when a webhook was requested", "channels", "CONTAINS",
    "SHOW", ref="WEBHOOK", down_sq="report_url")

# ----------------------------------------------------------------- emit
ORDER = ["surveys","select_groups","select_items","steps","sections","steps_sections",
         "questions","sections_questions","relationships"]
EMPTY = ["reports","post_survey_actions","dimensions","ontology","metadata"]

out = ["# ELICIT_SURVEY_EXPORT_V1",
       "# survey_id: 1",
       f"# survey_key: {SURVEY_KEY}",
       "# survey_name: ElicitDesigner"]
for t in ORDER:
    out.append(f"# {t}: {len(records[t])}")
for t in EMPTY:
    out.append(f"# {t}: 0")
out += ["# generated: 2026-09-17T19:00:00-04:00",
        "# survey_revision: 2026-09-17T19:00:00-04:00",
        ""]
for t in ORDER:
    for line in records[t]:
        out.append(f"{t}: {line}")

text = "\n".join(out) + "\n"
open(sys.argv[2] if len(sys.argv) > 2 else "ElicitDesigner.elicit", "w").write(text)

for t in ORDER:
    print(f"  {t:22} {len(records[t]):3}  ({COUNTS[t]} fields each)")
print(f"\nTotal records: {sum(len(records[t]) for t in ORDER)}")
