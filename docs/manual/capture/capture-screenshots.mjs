/*
 * UC-039 BR-002: every stage of the manual carries a screenshot of the screen it describes.
 *
 * Drives a real Chromium against a running Author (the umbrella compose stack) and writes
 * the manual's figures to docs/manual/images/. See docs/manual/README.md for how to run it.
 */
import { chromium } from 'playwright';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';

const BASE = process.env.AUTHOR_URL || 'http://localhost:8084';
const PREVIEW = process.env.PREVIEW_URL || 'http://localhost:8085';
const USER = process.env.AUTHOR_USER || 'author';
const PASS = process.env.AUTHOR_PASSWORD || 'author';
const OUT = process.env.MANUAL_IMAGES || path.resolve(import.meta.dirname, '../images');

/**
 * The survey the figures walk, with the structure the manual describes. It ships as a fixture so
 * the run depends on nothing outside this repository: the walk installs it through UC-005 when the
 * deployment does not hold it yet. MANUAL_SURVEY_DEFINITION points the run at another definition.
 */
const FIXTURES = path.resolve(import.meta.dirname, 'fixtures');
const DEFINITION = process.env.MANUAL_SURVEY_DEFINITION
  ? path.resolve(process.env.MANUAL_SURVEY_DEFINITION)
  : path.join(FIXTURES, 'family-health-history.elicit');

/** The survey name a definition installs, as the Surveys grid lists it; an export carries it. */
function surveyNameFrom(definition) {
  if (!existsSync(definition)) return null;
  return readFileSync(definition, 'utf8').slice(0, 2000).match(/^# survey_name:\s*(.+)$/m)?.[1].trim() || null;
}

const SURVEY = process.env.MANUAL_SURVEY || surveyNameFrom(DEFINITION) || 'Family Health History';

/**
 * The manual's figures, in numbering order. `shot()` refuses a name that is not here, and the run
 * ends with a report of anything it could not capture and a non-zero exit.
 */
const FIGURES = [
  '01-sign-in', '02-surveys-list', '03-new-survey', '04-survey-overview', '05-designer-board',
  '06-new-step', '07-step-menu', '08-new-section', '09-list-of-options', '10-question-editor',
  '11-choice-question', '12-section-opened', '13-question-menu', '14-new-rule', '15-question-bank',
  '16-reporting', '17-assign-tag', '18-export', '19-guide', '20-ai-draft', '21-reports',
  '22-post-survey-actions', '23-delete-survey', '24-translations', '25-preview',
];

const captured = new Set();
const skipped = [];

const FIELD_TAGS = [
  'vaadin-text-field', 'vaadin-text-area', 'vaadin-integer-field', 'vaadin-number-field',
  'vaadin-combo-box', 'vaadin-select', 'vaadin-checkbox',
];

let page;

/** Vaadin 25 slots the label as a <label> child of the field host, so match on that. */
const host = (label) =>
  page.locator(FIELD_TAGS.map((t) => `${t}:has(> label:text-is("${label}"))`).join(',')).first();
const field = (label) => host(label).locator('input, textarea').first();

const pause = (ms) => page.waitForTimeout(ms);

/*
 * #132: a figure ends a little below the last thing on its screen instead of carrying the empty
 * band under a short page, and a figure a procedure points into carries that procedure's step
 * numbers. `marks` is a list of [step number, locator]; their positions are written to
 * <name>.markers.tex beside the image, which the manual's \screenshot reads, so a recapture moves
 * the markers with the screen.
 */
const MIN_FIGURE_HEIGHT = 420;

/** The lowest visible text, control, image or ruled box: where the screen's content ends. */
async function contentBottom(fullPage) {
  return page.evaluate((whole) => {
    const limit = whole ? document.documentElement.scrollHeight : window.innerHeight;
    const media = /^(INPUT|TEXTAREA|BUTTON|IMG|SVG|CANVAS|VAADIN-BUTTON|VAADIN-ICON)$/i;
    let bottom = 0;
    const walk = (root) => {
      for (const el of root.querySelectorAll('*')) {
        if (el.shadowRoot) walk(el.shadowRoot);
        const r = el.getBoundingClientRect();
        const top = r.top + window.scrollY;
        // A container nearly as tall as the window is layout, not content.
        if (r.width < 2 || r.height < 2 || r.height > window.innerHeight * 0.6) continue;
        if (top >= limit || r.bottom + window.scrollY <= 0) continue;
        const st = getComputedStyle(el);
        if (st.visibility === 'hidden' || st.display === 'none' || Number(st.opacity) === 0) continue;
        const leaf = el.children.length === 0 || media.test(el.tagName);
        const drawn = el.textContent.trim() || media.test(el.tagName) || st.backgroundImage !== 'none'
          || st.borderBottomWidth !== '0px';
        if (leaf && drawn) bottom = Math.max(bottom, Math.min(r.bottom + window.scrollY, limit));
      }
    };
    walk(document);
    return bottom;
  }, fullPage);
}

/**
 * Where a step's disc goes: beside the control, on whichever side the screen is empty, so it
 * covers neither the control's label nor a neighbour's; on its corner when both sides are taken.
 * The disc is about 48 CSS pixels across at the manual's figure width.
 */
async function markerSpot(box) {
  return page.evaluate((b) => {
    const media = /^(INPUT|TEXTAREA|BUTTON|IMG|SVG|CANVAS|VAADIN-BUTTON|VAADIN-ICON|LABEL)$/i;
    const deepest = (x, y) => {
      let el = document.elementFromPoint(x, y);
      while (el && el.shadowRoot) {
        const inner = el.shadowRoot.elementFromPoint(x, y);
        if (!inner || inner === el) break;
        el = inner;
      }
      return el;
    };
    const occupied = (x, y) => {
      if (x < 4 || y < 4 || x > window.innerWidth - 4 || y > window.innerHeight - 4) return true;
      const el = deepest(x, y);
      if (!el) return false;
      return media.test(el.tagName) || (el.children.length === 0 && el.textContent.trim() !== '');
    };
    const free = (cx, cy) => [[0, 0], [-18, 0], [18, 0], [0, -14], [0, 14]]
      .every(([dx, dy]) => !occupied(cx + dx, cy + dy));
    const cy = b.y + b.height / 2;
    for (const [cx, cyy] of [[b.x - 30, cy], [b.x + b.width + 30, cy]]) {
      if (free(cx, cyy)) return [cx, cyy];
    }
    return [Math.max(12, b.x), Math.max(12, b.y)];
  }, box);
}

async function framedScreenshot(file, { fullPage = false, marks = [] } = {}) {
  const { width, height: viewportHeight } = page.viewportSize();
  const bottom = await contentBottom(fullPage);
  let height = Math.max(MIN_FIGURE_HEIGHT, Math.ceil(bottom + 28));
  if (!fullPage) height = Math.min(height, viewportHeight);
  await page.screenshot({ path: file, fullPage, clip: { x: 0, y: 0, width, height } });

  const markers = file.replace(/\.png$/, '.markers.tex');
  if (!marks.length) {
    await rm(markers, { force: true });
    return;
  }
  const scrollY = fullPage ? await page.evaluate(() => window.scrollY) : 0;
  const lines = ['% Written by capture-screenshots.mjs: the step markers on this figure (#132).'];
  for (const [step, locator] of marks) {
    const box = await locator.first().boundingBox().catch(() => null);
    if (!box) {
      console.warn(`    ! marker ${step}: its element is not on the screen`);
      continue;
    }
    const [vx, vy] = await markerSpot(box);
    const x = vx;
    const y = vy + scrollY;
    lines.push(`\\elicitmark{${step}}{${(x / width).toFixed(4)}}{${(y / height).toFixed(4)}}`);
  }
  await writeFile(markers, lines.join('\n') + '\n');
}

async function shot(name, options = {}) {
  if (!FIGURES.includes(name)) {
    throw new Error(`${name} is not one of the manual's figures`);
  }
  await pause(700);
  await framedScreenshot(path.join(OUT, `${name}.png`), options);
  captured.add(name);
  console.log('  ✓', name);
}

/** Records a figure this run could not produce, with the reason. */
function skip(name, why) {
  skipped.push({ name, why });
  console.log('  –', name, '—', why);
}

async function combo(label, value) {
  const input = host(label).locator('input');
  await input.focus();
  await pause(250);
  await input.fill(value);
  await pause(1000);
  await input.press('ArrowDown');
  await pause(250);
  await input.press('Enter');
  await pause(800);
}

/** A lane opens to reveal its section cards; a card opens to reveal its question rows. */
async function openOnBoard(step, section) {
  await page.locator(`[aria-label="${step}"]`).click();
  await pause(1800);
  if (section) {
    await page.locator(`[aria-label^="${section},"]`).click();
    await pause(1800);
  }
}

async function elementMenu(ariaLabel, item) {
  await page.locator(`vaadin-menu-bar-item[aria-label="${ariaLabel}"]:visible`).first().click();
  await pause(900);
  await page.getByText(item, { exact: true }).last().click();
  await pause(1600);
}

/** The drawer panels remember whether they are open, so toggle only when the target is hidden. */
async function revealPanel(name, probe) {
  for (let i = 0; i < 2; i++) {
    if (await page.locator(probe).isVisible().catch(() => false)) return;
    await page.getByText(name, { exact: true }).first().click();
    await pause(1200);
  }
}

/**
 * Vaadin Grid keeps every cell's content in the light DOM and slots it into a row in the shadow
 * DOM, so a row is not a DOM ancestor of its own cells: `getByRole('row').filter({hasText})` never
 * matches, and taking the first Open on the page opens whichever survey sorts first. Walk the slot
 * instead — name cell -> its slot -> the row that slot sits in -> that row's cell holding a button.
 */
async function rowButtonSlot(cellText, buttonText = null) {
  return page.evaluate(([text, label]) => {
    const contents = [...document.querySelectorAll('vaadin-grid-cell-content')];
    const cell = contents.find((c) => c.textContent.trim() === text);
    if (!cell) return null;
    const grid = document.querySelector('vaadin-grid');
    const slot = grid?.shadowRoot?.querySelector(`slot[name="${cell.getAttribute('slot')}"]`);
    const tr = slot?.closest('tr');
    if (!tr) return null;
    for (const name of [...tr.querySelectorAll('slot')].map((sl) => sl.getAttribute('name'))) {
      const content = document.querySelector(`vaadin-grid-cell-content[slot="${name}"]`);
      const buttons = [...(content?.querySelectorAll('vaadin-button') || [])];
      if (buttons.some((b) => !label || b.textContent.trim() === label)) return name;
    }
    return null;
  }, [cellText, buttonText]);
}

async function dismiss() {
  await page.keyboard.press('Escape');
  await pause(900);
}

async function goto(url) {
  await page.goto(url, { waitUntil: 'networkidle' });
  await pause(1800);
}

/**
 * Runs one figure's block. A block that cannot find its screen costs that figure and not the whole
 * run, so one changed selector leaves the other committed figures alone.
 */
async function attempt(name, fn) {
  try {
    await fn();
  } catch (e) {
    skip(name, (e.message || String(e)).split('\n')[0]);
    await dismiss().catch(() => {});
  }
}

/**
 * The figures walk one survey with the structure the manual describes. Install it from the fixture
 * when the deployment does not hold it (UC-005), so the run needs nothing authored by hand.
 */
async function ensureSurvey() {
  await goto(BASE);
  if (await page.getByRole('gridcell', { name: SURVEY }).first().count()) {
    console.log(`  · ${SURVEY} is already in this deployment`);
    return;
  }
  if (!existsSync(DEFINITION)) {
    throw new Error(`${SURVEY} is not in this deployment and the fixture ${DEFINITION} is missing`);
  }
  console.log(`  · importing ${path.basename(DEFINITION)}`);
  await goto(`${BASE}/import`);
  await page.locator('vaadin-upload input[type=file]').setInputFiles(DEFINITION);
  // The view replaces its result panel either way, so wait for it to say something.
  await page.locator('h3, .text-error').first().waitFor({ state: 'visible', timeout: 120_000 })
    .catch(() => {});
  await pause(1200);
  await goto(BASE);
  if (!(await page.getByRole('gridcell', { name: SURVEY }).first().count())) {
    await goto(`${BASE}/import`);
    const why = await page.locator('.text-error').first().innerText().catch(() => '');
    throw new Error(`importing ${path.basename(DEFINITION)} did not produce ${SURVEY}`
      + (why ? `: ${why.trim()}` : ''));
  }
  console.log(`  · imported ${SURVEY}`);
}

async function main() {
  await mkdir(OUT, { recursive: true });
  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1.25 });
  page = await context.newPage();

  // 01 — the identity provider's sign-in page (UC-001).
  await page.goto(BASE, { waitUntil: 'networkidle' });
  if (page.url().includes('/protocol/openid-connect/auth')) {
    await pause(800);
    await shot('01-sign-in');
    await page.fill('#username', USER);
    await page.fill('#password', PASS);
    await page.click('#kc-login');
    await page.waitForLoadState('networkidle');
  }
  await pause(2500);

  await ensureSurvey();

  // 02 — the Surveys list, the application's front door (UC-003).
  await shot('02-surveys-list');

  // 03 — New survey: the metadata an author supplies to start (UC-003).
  await attempt('03-new-survey', async () => {
    await page.getByRole('button', { name: 'New survey' }).click();
    await pause(1500);
    await field('Name').fill('FamilyHistory');
    await field('Title').fill('Family Health History');
    await field('Description').fill('A short family health history questionnaire.');
    await shot('03-new-survey');
    await dismiss();
  });

  // Every row carries its own Open button, so open the one in this survey's row.
  const actionsSlot = await rowButtonSlot(SURVEY);
  if (!actionsSlot) {
    throw new Error(`${SURVEY} is not listed in the Surveys grid`);
  }
  await page.locator(`vaadin-grid-cell-content[slot="${actionsSlot}"] vaadin-button`).first().click();
  await pause(2500);
  const surveyUrl = page.url();

  // 04 — the survey overview: validation, and the four things you can do to a survey (UC-007).
  await shot('04-survey-overview');

  // 05 — the designer board (UC-006): drawer toolbox on the left, lanes in the middle, preview right.
  await page.getByRole('button', { name: 'Design…' }).click();
  await pause(3000);
  const designUrl = page.url();
  await shot('05-designer-board');

  const reportingUrl = surveyUrl.replace(/\/$/, '') + '/analysis';
  const onBoard = async () => {
    await goto(designUrl);
    await openOnBoard('Step 1: About you', 'Section You');
  };

  // 06 — New step (UC-015).
  await attempt('06-new-step', async () => {
    await page.getByRole('button', { name: 'Add step' }).click();
    await pause(1500);
    await field('Name').fill('About you');
    await shot('06-new-step');
    await dismiss();
  });

  // 07 — a step's ⋮ menu: everything a drag can do, the menu can do too (NFR-012).
  await attempt('07-step-menu', async () => {
    await goto(designUrl);
    await page.locator('vaadin-menu-bar-item[aria-label="Actions for step About you"]:visible').first().click();
    await pause(1200);
    await shot('07-step-menu');
    await dismiss();
  });

  // 08 — New section (UC-015).
  await attempt('08-new-section', async () => {
    await goto(designUrl);
    await elementMenu('Actions for step About you', 'Add section');
    await field('Name').fill('You');
    await shot('08-new-section');
    await dismiss();
  });

  // 09 — a list of options, which every choice question needs (UC-014).
  await attempt('09-list-of-options', async () => {
    await goto(designUrl);
    await revealPanel('Lists', 'vaadin-button:has-text("New list…")');
    await page.getByRole('button', { name: 'New list…' }).click();
    await pause(1800);
    await field('Name').fill('Yes / No');
    for (const [i, option] of ['Yes', 'No'].entries()) {
      await page.getByRole('button', { name: 'Add option' }).click();
      await pause(500);
      await page.locator('input[placeholder="Display text"]').nth(i).fill(option);
      await pause(250);
    }
    await shot('09-list-of-options');
    await dismiss();
  });

  // 10 — New question, and 11 — the same editor once the type needs a list (UC-011).
  await attempt('10-question-editor', async () => {
    await onBoard();
    await elementMenu('Actions for question First name', 'Edit…');
    await shot('10-question-editor');
    await dismiss();
  });

  await attempt('11-choice-question', async () => {
    await onBoard();
    await elementMenu('Actions for question Has children', 'Edit…');
    await shot('11-choice-question');
    await dismiss();
  });

  // 12 — the board with a section opened: rows, glyphs and the respondent preview beside them.
  await attempt('12-section-opened', async () => {
    await onBoard();
    await shot('12-section-opened');
  });

  // 13 — a question's ⋮ menu, where rules, analysis tags and report marks start.
  await attempt('13-question-menu', async () => {
    await onBoard();
    await page.locator('vaadin-menu-bar-item[aria-label="Actions for question Has children"]:visible').first().click();
    await pause(1200);
    await shot('13-question-menu');
    await dismiss();
  });

  // 14 — New rule: upstream question, operator, value, action, downstream target (UC-019).
  await attempt('14-new-rule', async () => {
    await onBoard();
    await elementMenu('Actions for question Child count', 'Add rule…');
    await combo('Operator', 'Greater than');
    await field('Value').fill('0');
    await combo('Action', 'Repeat');
    await pause(600);
    await page.getByRole('radio', { name: 'Section', exact: true }).click();
    await pause(1000);
    await combo('Target element', 'Child');
    await shot('14-new-rule');
    await dismiss();
  });

  // 15 — the banks: every section and question once, with how many places each appears in (UC-011 A6).
  await attempt('15-question-bank', async () => {
    await goto(designUrl);
    await revealPanel('Questions', 'input[aria-label="Search questions by name, text or type"]');
    await shot('15-question-bank');
  });

  // 16 — the analysis page: tags, assignments and the schema they produce (UC-021, UC-024).
  await attempt('16-reporting', async () => {
    await goto(reportingUrl);
    await shot('16-reporting');
  });

  // 17 — assigning a tag to a question (UC-021).
  await attempt('17-assign-tag', async () => {
    await goto(reportingUrl);
    await page.getByRole('button', { name: 'Assign tag…' }).click();
    await pause(1800);
    await combo('Element', 'Child sex');
    await combo('Tag', 'children');
    await shot('17-assign-tag');
    await dismiss();
  });

  // 18 — Export: the release note and the .elicit file Admin installs (UC-008).
  await attempt('18-export', async () => {
    await goto(surveyUrl);
    await page.getByRole('button', { name: 'Export…' }).click();
    await pause(1800);
    await field('Release note').fill('First release of the family health history questionnaire.');
    await shot('18-export');
    await dismiss();
  });

  // 19 — the in-app guide the manual is the printable form of (UC-029).
  await attempt('19-guide', async () => {
    await goto(`${BASE}/guide`);
    await shot('19-guide');
  });

  // 20 — Draft with AI: the specification an agent drafts from, and the round trip (UC-042).
  await attempt('20-ai-draft', async () => {
    await goto(`${BASE}/ai-draft`);
    await shot('20-ai-draft');
  });

  // 21 — the Reports page: the report services and the marks each report shows (UC-047, UC-048).
  await attempt('21-reports', async () => {
    await goto(surveyUrl.replace(/\/$/, '') + '/reports');
    await shot('21-reports');
  });

  // 22 — the Post-survey actions page (UC-049).
  await attempt('22-post-survey-actions', async () => {
    await goto(surveyUrl.replace(/\/$/, '') + '/actions');
    await shot('22-post-survey-actions');
  });

  // 24 — the Translations page, every respondent string beside its translation (UC-043).
  await attempt('24-translations', async () => {
    await goto(surveyUrl.replace(/\/$/, '') + '/translations');
    await shot('24-translations');
  });

  // 23 — the delete confirmation, opened and cancelled: nothing is deleted (UC-041).
  await attempt('23-delete-survey', async () => {
    await goto(BASE);
    const slot = await rowButtonSlot(SURVEY, 'Delete');
    if (!slot) throw new Error(`no Delete button in the row of ${SURVEY}`);
    await page.locator(`vaadin-grid-cell-content[slot="${slot}"] vaadin-button`)
      .filter({ hasText: 'Delete' }).first().click();
    await pause(1800);
    await shot('23-delete-survey');
    await dismiss();
  });

  // 25 — the preview Survey, where an author walks the survey as a respondent (UC-048 A5).
  await attempt('25-preview', async () => {
    await goto(PREVIEW);
    await shot('25-preview');
  });

  await browser.close();

  console.log(`\n${captured.size} of ${FIGURES.length} figures written to ${OUT}`);
  const missing = FIGURES.filter((name) => !captured.has(name));
  if (missing.length) {
    console.log('\nNot captured on this run — the committed figure is unchanged:');
    for (const name of missing) {
      const reason = skipped.find((sk) => sk.name === name);
      console.log(`  ${name}${reason ? ` — ${reason.why}` : ''}`);
    }
  }
  console.log(
    '\nCheck every \\screenshot{…} caption in elicit-author-manual.tex still matches what the '
    + 'figure shows: the caption is part of the instruction (UC-039 BR-002).',
  );
  return missing.length;
}

main().then((missing) => process.exit(missing ? 1 : 0)).catch((e) => {
  console.error(e);
  process.exit(1);
});
