/*
 * UC-039 BR-002: every stage of the manual carries a screenshot of the screen it describes.
 *
 * Drives a real Chromium against a running Author (the umbrella compose stack) and writes
 * the manual's figures to docs/manual/images/. See docs/manual/README.md for how to run it.
 */
import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import path from 'node:path';

const BASE = process.env.AUTHOR_URL || 'http://localhost:8084';
const USER = process.env.AUTHOR_USER || 'author';
const PASS = process.env.AUTHOR_PASSWORD || 'author';
const OUT = process.env.MANUAL_IMAGES || path.resolve(import.meta.dirname, '../images');
const SURVEY = process.env.MANUAL_SURVEY || 'Family Health History';

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

async function shot(name) {
  await pause(700);
  await page.screenshot({ path: path.join(OUT, `${name}.png`) });
  console.log('  ✓', name);
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

async function dismiss() {
  await page.keyboard.press('Escape');
  await pause(900);
}

async function goto(url) {
  await page.goto(url, { waitUntil: 'networkidle' });
  await pause(1800);
}

async function main() {
  await mkdir(OUT, { recursive: true });
  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 });
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

  // 02 — the Surveys list, the application's front door (UC-003).
  await shot('02-surveys-list');

  // 03 — New survey: the metadata an author supplies to start (UC-003).
  await page.getByRole('button', { name: 'New survey' }).click();
  await pause(1500);
  await field('Name').fill('FamilyHistory');
  await field('Title').fill('Family Health History');
  await field('Description').fill('A short family health history questionnaire.');
  await shot('03-new-survey');
  await dismiss();

  const row = page.getByRole('gridcell', { name: SURVEY }).first();
  if (await row.count()) {
    await row.click();
    await pause(800);
  }
  await page.getByRole('button', { name: 'Open' }).first().click();
  await pause(2500);
  const surveyUrl = page.url();

  // 04 — the survey overview: validation, and the four things you can do to a survey (UC-007).
  await shot('04-survey-overview');

  // 05 — the designer board (UC-006): drawer toolbox on the left, lanes in the middle, preview right.
  await page.getByRole('button', { name: 'Design…' }).click();
  await pause(3000);
  const designUrl = page.url();
  await shot('05-designer-board');

  // 06 — New step (UC-015).
  await page.getByRole('button', { name: 'Add step' }).click();
  await pause(1500);
  await field('Name').fill('About you');
  await shot('06-new-step');
  await dismiss();

  // 07 — a step's ⋮ menu: everything a drag can do, the menu can do too (NFR-012).
  await page.locator('vaadin-menu-bar-item[aria-label="Actions for step About you"]:visible').first().click();
  await pause(1200);
  await shot('07-step-menu');
  await dismiss();

  // 08 — New section (UC-015).
  await elementMenu('Actions for step About you', 'Add section');
  await field('Name').fill('You');
  await shot('08-new-section');
  await dismiss();

  // 09 — a list of options, which every choice question needs (UC-014).
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

  // 10 — New question, and 11 — the same editor once the type needs a list (UC-011).
  await goto(designUrl);
  await openOnBoard('Step 1: About you', 'Section You');
  await elementMenu('Actions for question First name', 'Edit…');
  await shot('10-question-editor');
  await dismiss();

  await goto(designUrl);
  await openOnBoard('Step 1: About you', 'Section You');
  await elementMenu('Actions for question Has children', 'Edit…');
  await shot('11-choice-question');
  await dismiss();

  // 12 — the board with a section opened: rows, glyphs and the respondent preview beside them.
  await goto(designUrl);
  await openOnBoard('Step 1: About you', 'Section You');
  await shot('12-section-opened');

  // 13 — a question's ⋮ menu, where rules and reporting tags start.
  await page.locator('vaadin-menu-bar-item[aria-label="Actions for question Has children"]:visible').first().click();
  await pause(1200);
  await shot('13-question-menu');
  await dismiss();

  // 14 — New rule: upstream question, operator, value, action, downstream target (UC-019).
  await goto(designUrl);
  await openOnBoard('Step 1: About you', 'Section You');
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

  // 15 — the banks: every section and question once, with how many places each appears in (UC-011 A6).
  await goto(designUrl);
  await revealPanel('Questions', 'input[aria-label="Search questions by name, text or type"]');
  await shot('15-question-bank');

  // 16 — the reporting page: tags, assignments and the schema they produce (UC-021, UC-024).
  await goto(surveyUrl.replace(/\/$/, '') + '/reporting');
  await shot('16-reporting');

  // 17 — assigning a tag to a question (UC-021).
  await page.getByRole('button', { name: 'Assign tag…' }).click();
  await pause(1800);
  await combo('Element', 'Child sex');
  await combo('Tag', 'children');
  await shot('17-assign-tag');
  await dismiss();

  // 18 — Export: the release note and the .elicit file Admin installs (UC-008).
  await goto(surveyUrl);
  await page.getByRole('button', { name: 'Export…' }).click();
  await pause(1800);
  await field('Release note').fill('First release of the family health history questionnaire.');
  await shot('18-export');
  await dismiss();

  // 19 — the in-app guide the manual is the printable form of (UC-029).
  await goto(`${BASE}/guide`);
  await shot('19-guide');

  await browser.close();
  console.log(`\nFigures written to ${OUT}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
