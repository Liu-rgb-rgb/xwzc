const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ channel: 'msedge' });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const bad = [];
  page.on('response', (res) => {
    if (res.status() >= 400) bad.push(res.status() + ' ' + res.url());
  });
  const errors = [];
  page.on('pageerror', (e) => errors.push('pageerror: ' + e.message));
  await page.goto('http://127.0.0.1:5173/stitch-3d', { waitUntil: 'networkidle', timeout: 90000 });
  await page.waitForTimeout(6000);
  console.log('BAD_RESPONSES', JSON.stringify(bad, null, 2));
  console.log('PAGE_ERRORS', JSON.stringify(errors));

  const ranges = await page.evaluate(() =>
    [...document.querySelectorAll('.s3d-range')].map((el) => ({
      label: el.getAttribute('aria-label'),
      value: el.value
    }))
  );
  console.log('RANGES', JSON.stringify(ranges));

  await page.evaluate(() => {
    const input = document.querySelector('input[aria-label="拆解程度"]');
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    setter.call(input, '100');
    input.dispatchEvent(new Event('input', { bubbles: true }));
  });
  await page.waitForTimeout(2000);
  await page.locator('.s3d-stage').screenshot({ path: 'D:/xwzc-verify/shot-explode100.png' });
  const explodeLabel = await page.locator('.s3d-sliders, .s3d-slider-block').first().innerText().catch(() => '');
  console.log('EXPLODE_UI', JSON.stringify(explodeLabel));

  await page.evaluate(() => {
    const input = document.querySelector('input[aria-label="拆解程度"]');
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    setter.call(input, '55');
    input.dispatchEvent(new Event('input', { bubbles: true }));
  });
  await page.waitForTimeout(1500);
  await page.screenshot({ path: 'D:/xwzc-verify/shot-full-1440.png', fullPage: false });
  await browser.close();
  console.log('done');
})().catch((e) => { console.error(e); process.exit(1); });
