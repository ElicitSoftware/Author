#!/usr/bin/env python3
"""
Generates the Elicit Survey Designer meta-survey as a .elicit import file.

Field layouts follow the post-Survey-V015 ELICIT_SURVEY_EXPORT_V1 contract (no trailing
is_draft field), as read by Author's ElicitFormat and Admin's SurveyDefinitionImportService.
Required field counts are asserted against the reader's own checks so the output cannot drift.
"""
import hashlib
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
    "surveys": 12, "select_groups": 10, "select_items": 11, "steps": 11,
    "sections": 11, "steps_sections": 12, "questions": 20,
    "sections_questions": 10, "relationships": 19, "translations": 13,
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
    "A meta-survey: it interviews you about the steps and sections of the survey you "
    "want to build, and its completed responses are the skeleton an author turns into "
    "a .elicit definition file for import into any Elicit deployment.",
    f"{SURVEY_PREFIX}-0001-0000-0001-0000-0000-0000",
    "", "elicit-designer", "Initial publication",
    "en", "es-419")

# ----------------------------------------------------------------- select groups
SG = {}
def select_group(gid, name, desc, data_type, items):
    SG[name] = gid
    add("select_groups", gid, ek(f"sg:{name}"), name, desc, data_type, *SCD)
    for i, (text, coded) in enumerate(items, start=1):
        add("select_items", gid*1000+i, ek(f"si:{name}:{coded}"), gid, text, i, coded, *SCD)

select_group(1, "DeliveryChannel", "How results leave the system", "Text",
    [("PDF report for the respondent","PDF"),("PDF report for staff","PDF_STAFF"),
     ("Webhook / REST callback","WEBHOOK"),("Email notification","EMAIL"),
     ("Nothing — data stays in Elicit","NONE")])

# ----------------------------------------------------------------- steps
STEPS = [(1,"Welcome"),(2,"Survey Identity"),(3,"Structure"),(4,"Delivery"),(5,"Generate")]
for order, name in STEPS:
    add("steps", order, ek(f"step:{name}"), order, name, "", f"{name} step", *SCD)

# ----------------------------------------------------------------- sections
# (section_display_order is global across the survey, matching the engine's display_key scheme)
SECTIONS = [
    (1,1,"Before You Begin"),      (2,1,"How a Survey Is Put Together"),
    (3,2,"Naming and Purpose"),
    (4,3,"How Many Steps"),        (5,3,"Describe Each Step"),
    (6,4,"Reports and Follow-up"), (7,5,"Review and Produce"),
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
    "<h2>Design a new Elicit survey</h2><p>This tool interviews you about the shape of the "
    "survey you want to build — its steps (pages) and the sections on each one. Every answer "
    "maps onto one row of the <code>.elicit</code> definition file that gets imported into an "
    "Elicit deployment.</p>"
    "<p><b>This interview sets up the structure only.</b> What you produce here is a skeleton: "
    "the survey's identity, its steps, the sections on each step, and how results are "
    "delivered. Everything that goes inside that skeleton is added afterwards in the "
    "<b>Author</b> tool:</p>"
    "<ul>"
    "<li>the <b>questions</b> in each section, with their wording, answer type, validation "
    "and help text;</li>"
    "<li>the <b>answer lists</b> (select groups and items) those questions draw on;</li>"
    "<li>the <b>rules</b> — which question is watched, the test applied to its answer "
    "(equals, is greater than, is checked, has been answered, contains) and the "
    "<b>action</b> taken: show, repeat, or replace text;</li>"
    "<li><b>tokens</b> in wording so text changes with earlier answers;</li>"
    "<li>the reporting <b>tags</b> (ontology and metadata) that shape exports and reports;</li>"
    "<li><b>preview and testing</b> of the logic before anything is published.</li>"
    "</ul>"
    "<p>You can stop and resume; nothing is published until an author reviews your answers.</p>",
    1, 1)


question("anatomy", "HTML",
    "<h3>How an Elicit survey is put together</h3>"
    "<p>Every survey is a small hierarchy. Knowing the names helps, because the rest of this "
    "interview and the author who builds the survey both use them.</p>"
    "<dl>"
    "<dt><b>Survey</b></dt><dd>The top-level record with name, title, description, and "
    "finish URL (Family History Survey, Library Card Registration, etc.).</dd>"
    "<dt><b>Steps</b></dt><dd>The pages in the progress bar; a step can repeat per person "
    "(Welcome, Demographics, etc.).</dd>"
    "<dt><b>Sections</b></dt><dd>Headed groups on a page; one section can be mounted under "
    "several steps and share its questions (Your Mother, Cancers, etc.).</dd>"
    "<dt><b>Questions</b></dt><dd>The answer kinds available and the fields each carries "
    "(\"What is your current age?\", \"Are you the patient?\", etc.).</dd>"
    "<dt><b>Select groups and select items</b></dt><dd>Reusable answer lists with display "
    "text and a stored coded value (Yes/No, Male/Female, etc.).</dd>"
    "<dt><b>Relationships</b></dt><dd>The upstream question, operator, and "
    "show/repeat/replace-text action (show the current-age question when a relative is "
    "Alive, repeat the child's-name question once per child, etc.). Replace-text uses "
    "tokens: \"What is &#123;S1's&#124;your&#125; age?\" reads \"What is Anna's age?\" "
    "when someone is answering for Anna.</dd>"
    "<dt><b>Reports and post-survey actions</b></dt><dd>What happens to a finished response "
    "(Family Pedigree, Cancer Summary, etc.).</dd>"
    "<dt><b>Ontology, dimensions and metadata</b></dt><dd>Tags that shape the reporting "
    "layer (Age, Breast Cancer, etc.).</dd>"
    "</dl>"
    "<p>This interview asks only for the <b>survey</b>, its <b>steps</b> and <b>sections</b>, "
    "and how results are delivered. An author adds the questions, answer lists, relationships "
    "and reporting tags afterwards, using your outline.</p>",
    2, 1)

# --- Naming and Purpose
question("identity_intro", "HTML",
    "<h3>Survey identity</h3>"
    "<p><b>What this captures.</b> The single <code>surveys</code> record that everything else "
    "hangs off: the machine name, the title respondents see, a description of its purpose, and "
    "where respondents go when they finish.</p>"
    "<p><b>How it is used.</b> The machine name is how administrators find the survey when "
    "they generate access codes and pull exports, so it must be unique in the deployment. The "
    "title appears at the top of every page respondents see. The description is shown to "
    "administrators, not respondents. The owner's email is for the author to reach you with "
    "questions; it is not stored in the survey.</p>"
    "<p><b>Why it matters.</b> The name and title are the hardest things to change once "
    "respondents have been invited, because access codes, emails and exports all refer to "
    "them. Pick them as if they were permanent.</p>",
    3, 1)
question("name", "TEXT", "Short machine name for the survey (letters and digits, no spaces)",
    3, 2, required="true", short="SurveyName",
    tip="Becomes surveys.name. Must be unique within the deployment.",
    validation="Use letters and digits only, no spaces.", placeholder="LibraryCardReg")
question("title", "TEXT", "Title respondents will see at the top of every page",
    3, 3, required="true", short="SurveyTitle", placeholder="Public Library Card Registration")
question("purpose", "TEXTAREA", "What is this survey for? Who fills it in, and what happens to the answers?",
    3, 4, required="true", short="Purpose")
question("owner_email", "EMAIL", "Email address of the person who owns this survey",
    3, 5, required="true", short="OwnerEmail", validation="Enter a valid email address.")
question("post_url", "TEXT", "Where should respondents be sent after they finish? (leave blank for the default thank-you page)",
    3, 6, short="PostSurveyUrl", placeholder="https://example.org/thank-you")

# --- Structure
question("structure_intro", "HTML",
    "<h3>Steps and sections</h3>"
    "<p><b>What this captures.</b> The skeleton of the survey. A <b>step</b> is one page the "
    "respondent moves through; the progress bar lists the steps by name. A <b>section</b> is a "
    "headed group of questions on a step. Each step you describe becomes a <code>steps</code> "
    "record, each heading a <code>sections</code> record, and each pairing a "
    "<code>steps_sections</code> record.</p>"
    "<p><b>How it is used.</b> Every question the author adds later is placed in one of these "
    "sections, so the outline you give here decides where everything lives. The short "
    "<i>report names</i> become the dimension names the reporting tables and pedigree drawings "
    "are built from — keep them short, plain and stable. A section can be re-mounted under "
    "several steps; it then shares its questions between them, which is how one \"Cancers\" "
    "block can serve a mother, a father and every sibling without being written eight times.</p>"
    "<p><b>Why it matters.</b> Step and section positions are baked into every stored answer's "
    "key. Adding a step later is easy; reordering or removing one after respondents have "
    "started is not. Get the order right here, even if the wording changes.</p>",
    4, 1)
question("step_count", "INTEGER", "How many steps (pages) should the survey have?",
    4, 2, required="true", short="StepCount", minv="1", maxv="20",
    tip="Each step is one page the respondent moves through.",
    validation="Enter a whole number between 1 and 20.")
question("step_name", "TEXT", "Name of this step, as shown in the progress bar",
    5, 1, required="true", short="StepName")
question("step_report_name", "TEXT", "Short name used for this step in reports and data exports",
    5, 2, short="StepReportName",
    tip="Becomes steps.dimension_name. Leave blank to reuse the step name.",
    placeholder="Mother")
question("step_sections", "TEXTAREA",
    "The sections on this step, one per line and in order, written as Heading=ReportName",
    5, 3, required="true", short="SectionHeadings",
    tip="Example: Your Mother=Mother on one line. Leave out =ReportName to reuse the heading. "
        "A step needs at least one section.")
question("step_reused_sections", "TEXTAREA",
    "If any of these sections already appear on an earlier step, list their headings here, one per line",
    5, 4, short="ReusedSections",
    tip="The same section can sit under several steps and shares its questions between them. "
        "Write the heading exactly as it appeared on the earlier step.")

# --- Delivery
question("delivery_intro", "HTML",
    "<h3>Reports and follow-up</h3>"
    "<p><b>What this captures.</b> What happens to a completed response. Each report you name "
    "becomes a <code>reports</code> record; a webhook becomes a <code>post_survey_actions</code> "
    "record.</p>"
    "<p><b>How it is used.</b> Reports appear as buttons on the respondent's final page and in "
    "the Admin app, and are rendered on demand by the service at the URL you give. "
    "Post-survey actions run automatically the moment a respondent finishes — for example, "
    "generating a PDF and dropping it on an SFTP server for the clinic. Email notification and "
    "\"nothing\" need no further detail.</p>"
    "<p><b>Why it matters.</b> This is the only part of the survey that reaches people other "
    "than the respondent. If you are unsure, choose \"Nothing\": data stays in Elicit and can "
    "be exported by an administrator later, whereas a wrong endpoint fails silently at "
    "completion time.</p>",
    6, 1)
question("channels", "CHECKBOX_GROUP", "How should completed responses be delivered?",
    6, 2, short="DeliveryChannels", group="DeliveryChannel")
question("report_name", "TEXT", "What should the report be called?", 6, 3, short="ReportName")
question("report_url", "TEXT", "Report or webhook endpoint URL", 6, 4, short="ReportUrl",
    placeholder="http://report-service:8080/report")

# --- Review
question("outro", "HTML",
    "<h3>Review and hand-off</h3>"
    "<p><b>What this captures.</b> Your confirmation that the outline is complete, plus "
    "anything the author needs that the earlier questions had no room for.</p>"
    "<p><b>How it is used.</b> Nothing is built until you confirm. An author then maps each "
    "answer onto the matching step and section record, adds the questions, answer lists and "
    "conditional rules, and uploads the finished <code>.elicit</code> file through "
    "<em>Apply Survey Definition</em> in the Admin app.</p>"
    "<p><b>Why it matters.</b> The notes box is the right place for things this interview "
    "cannot express: which steps should repeat per family member, wording that must change "
    "depending on who is answering, or sections that only appear for some respondents. "
    "The author reads it before touching anything else.</p>",
    7, 1)
question("confirm", "CHECKBOX", "I confirm the details above are complete and ready for review",
    7, 2, required="true", short="Confirmed")
question("notes", "TEXTAREA", "Anything else the author should know before building this survey?",
    7, 3, short="AuthorNotes")

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
# Report name/URL only matter if a report channel was chosen.
rel("Ask for a report name only when a PDF report was requested", "channels", "CONTAINS",
    "SHOW", ref="PDF", down_sq="report_name")
rel("Ask for an endpoint only when a webhook was requested", "channels", "CONTAINS",
    "SHOW", ref="WEBHOOK", down_sq="report_url")

# ----------------------------------------------------------------- translations
# A handful of es-419 rows, enough to exercise the record type end to end: one of each
# element type that carries respondent-facing text, plus the survey's own title and
# description.  source_hash is the sha256 the runtime compares against the base text; a
# row whose base text is later edited stops matching and is not served.
BASE = {}   # (element_type, element_key, field) -> the base text it was written from

def base_text(element_type, key, field, text):
    BASE[(element_type, key, field)] = text

def translate(element_type, key, field, language, value):
    source = BASE[(element_type, key, field)]
    digest = hashlib.sha256(source.encode("utf-8")).hexdigest()
    add("translations", len(records["translations"]) + 1,
        ek(f"tr:{element_type}:{key}:{field}:{language}"),
        element_type, key, field, language, value, digest, *SCD)

SURVEY_TITLE = "Elicit Survey Designer"
base_text("surveys", SURVEY_KEY, "title", SURVEY_TITLE)
translate("surveys", SURVEY_KEY, "title", "es-419", "Dise\u00f1ador de encuestas Elicit")

base_text("steps", ek("step:Welcome"), "name", "Welcome")
translate("steps", ek("step:Welcome"), "name", "es-419", "Bienvenida")

base_text("sections", ek("section:Naming and Purpose"), "name", "Naming and Purpose")
translate("sections", ek("section:Naming and Purpose"), "name", "es-419", "Nombre y prop\u00f3sito")

base_text("select_items", ek("si:DeliveryChannel:EMAIL"), "display_text", "Email notification")
translate("select_items", ek("si:DeliveryChannel:EMAIL"), "display_text", "es-419",
          "Notificaci\u00f3n por correo electr\u00f3nico")

# ----------------------------------------------------------------- emit
ORDER = ["surveys","select_groups","select_items","steps","sections","steps_sections",
         "questions","sections_questions","relationships","translations"]
EMPTY = ["reports","post_survey_actions","dimensions","ontology","metadata"]

out = ["# ELICIT_SURVEY_EXPORT_V1",
       "# survey_id: 1",
       f"# survey_key: {SURVEY_KEY}",
       "# survey_name: ElicitDesigner"]
# Header counts follow the file's record order (ElicitFormat.TABLES): translations last,
# because a translation references every other table's element keys.
for t in ORDER:
    if t != "translations":
        out.append(f"# {t}: {len(records[t])}")
for t in EMPTY:
    out.append(f"# {t}: 0")
out.append(f"# translations: {len(records['translations'])}")
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
