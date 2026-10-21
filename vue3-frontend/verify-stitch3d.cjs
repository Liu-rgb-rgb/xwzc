const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge' });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text());
  });
  page.on('pageerror', (error) => errors.push('pageerror: ' + error.message));

  await page.goto('http://127.0.0.1:5173/stitch-3d', { waitUntil: 'networkidle', timeout: 90000 });
  await page.waitForTimeout(7000);

  const info = await page.evaluate(() => ({
    parts: document.querySelectorAll('.s3d-part').length,
    counts: [...document.querySelectorAll('.s3d-part-meta')].map((el) => el.textContent.trim()),
    status: document.querySelector('.s3d-status span')?.textContent,
    callouts: [...document.querySelectorAll('.s3d-callout')].map((el) => ({
      text: el.textContent.trim(),
      visibility: getComputedStyle(el).visibility,
      left: el.style.left,
      top: el.style.top
    })),
    canvas: (() => {
      const canvas = document.querySelector('.s3d-canvas canvas');
      return canvas ? { width: canvas.width, height: canvas.height } : null;
    })()
  }));
  console.log('INFO', JSON.stringify(info, null, 2));

  await page.screenshot({ path: 'D:/xwzc-verify/shot-new-page.png' });
  await page.locator('.s3d-stage').screenshot({ path: 'D:/xwzc-verify/shot-new-stage.png' });
  await page.locator('.s3d-left').screenshot({ path: 'D:/xwzc-verify/shot-new-left.png' });

  await page.getByRole('button', { name: '绣制回放' }).click();
  await page.waitForTimeout(2600);
  await page.locator('.s3d-stage').screenshot({ path: 'D:/xwzc-verify/shot-new-replay.png' });
  console.log('REPLAY_STATUS', await page.locator('.s3d-status span').textContent());

  await page.getByRole('button', { name: '丝线质感' }).click();
  await page.waitForTimeout(2200);
  await page.locator('.s3d-stage').screenshot({ path: 'D:/xwzc-verify/shot-new-gloss.png' });

  await page.getByRole('button', { name: '结构分层' }).click();
  await page.waitForTimeout(1600);
  await page.locator('.s3d-part').nth(1).click();
  await page.waitForTimeout(1400);
  await page.locator('.s3d-stage').screenshot({ path: 'D:/xwzc-verify/shot-new-selected.png' });

  const eye = page.locator('.s3d-part').nth(3).locator('.s3d-part-eye');
  await eye.click();
  await page.waitForTimeout(800);
  const offOpacity = await page.evaluate(() => {
    const rows = [...document.querySelectorAll('.s3d-part')];
    return rows.map((row) => row.className);
  });
  console.log('ROW_CLASSES_AFTER_EYE', JSON.stringify(offOpacity));

  console.log('ERRORS', JSON.stringify(errors.slice(0, 12)));
  await browser.close();
})().catch((error) => {
  console.error('SCRIPT_FAILED', error);
  process.exit(1);
});
