const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ channel: 'msedge' });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = [];
  page.on('pageerror', (e) => errors.push('pageerror: ' + e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  await page.goto('http://127.0.0.1:5173/stitch-3d', { waitUntil: 'networkidle', timeout: 90000 });
  await page.waitForTimeout(6000);

  await page.getByRole('button', { name: '绣盘' }).click();
  await page.waitForTimeout(2500);
  await page.locator('.s3d-stage').screenshot({ path: 'D:/xwzc-verify/shot-hoop.png' });

  await page.evaluate(() => {
    const input = document.querySelector('input[aria-label="丝线张力模拟"]');
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    setter.call(input, '35');
    input.dispatchEvent(new Event('input', { bubbles: true }));
  });
  await page.waitForTimeout(1200);
  await page.locator('.s3d-stage').screenshot({ path: 'D:/xwzc-verify/shot-tension35.png' });

  await page.getByRole('button', { name: '复位' }).click();
  await page.waitForTimeout(1200);

  await page.getByRole('button', { name: '画布' }).click();
  await page.waitForTimeout(2000);

  const state = await page.evaluate(() => ({
    parts: document.querySelectorAll('.s3d-part').length,
    activeMode: document.querySelector('.s3d-mode-tab.active, .s3d-demo-tab.active')?.textContent?.trim() || '',
    tension: document.querySelector('input[aria-label="丝线张力模拟"]')?.value,
    status: document.querySelector('.s3d-status span')?.textContent
  }));
  console.log('STATE', JSON.stringify(state));
  console.log('ERRORS', JSON.stringify(errors.slice(0, 10)));
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
