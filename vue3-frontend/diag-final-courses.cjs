const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ channel: 'msedge' });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = [];
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('pageerror', (e) => errors.push(String(e)));

  await page.goto('http://localhost:5173/courses', { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.aca-course', { timeout: 20000 });
  await page.waitForTimeout(2200);

  const base = await page.evaluate(() => {
    const v = document.querySelector('.aca-hero-media video');
    return {
      heroSrc: v?.currentSrc || '',
      heroDuration: v?.duration,
      heroPaused: v?.paused,
      sides: document.querySelectorAll('.side-card').length,
      sideOpacities: [...document.querySelectorAll('.side-slot')].map((el) => getComputedStyle(el).opacity),
      sideRows: getComputedStyle(document.querySelector('.aca-sides')).gridTemplateRows,
      stageAspect: getComputedStyle(document.querySelector('.aca-stage')).aspectRatio,
      stageH: document.querySelector('.aca-stage')?.getBoundingClientRect().height,
      sidesH: document.querySelector('.aca-sides')?.getBoundingClientRect().height
    };
  });

  // advance carousel twice and re-check sides visible
  await page.locator('.aca-stage').scrollIntoViewIfNeeded();
  await page.click('.theater-arrow.next');
  await page.waitForTimeout(700);
  await page.click('.theater-arrow.next');
  await page.waitForTimeout(700);
  const afterSlides = await page.evaluate(() => ({
    active: [...document.querySelectorAll('.theater-slide')].findIndex((el) => el.classList.contains('active')),
    sides: document.querySelectorAll('.side-card').length,
    sideOpacities: [...document.querySelectorAll('.side-slot')].map((el) => getComputedStyle(el).opacity),
    sideTitles: [...document.querySelectorAll('.side-caption h4')].map((el) => el.textContent.trim())
  }));

  // detail back link
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  await page.waitForTimeout(500);
  await page.click('.aca-course');
  await page.waitForTimeout(1500);
  const detail = await page.evaluate(() => {
    const back = document.querySelector('.detail-back-top');
    const v = document.querySelector('video.detail-video');
    return {
      url: location.pathname,
      backText: back?.textContent?.trim(),
      backHref: back?.getAttribute('href'),
      videoSrc: v?.currentSrc || '',
      videoDuration: v?.duration
    };
  });
  await page.click('.detail-back-top');
  await page.waitForTimeout(800);
  const backUrl = page.url();

  // tablet
  await page.setViewportSize({ width: 1100, height: 900 });
  await page.goto('http://localhost:5173/courses', { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.aca-course', { timeout: 15000 });
  await page.waitForTimeout(800);
  const tablet = await page.evaluate(() => ({
    theaterCols: getComputedStyle(document.querySelector('.aca-theater-grid')).gridTemplateColumns,
    sidesCols: getComputedStyle(document.querySelector('.aca-sides')).gridTemplateColumns,
    sides: document.querySelectorAll('.side-card').length
  }));

  // mobile
  await page.setViewportSize({ width: 390, height: 844 });
  await page.waitForTimeout(500);
  const mobile = await page.evaluate(() => ({
    sidesCols: getComputedStyle(document.querySelector('.aca-sides')).gridTemplateColumns,
    gridCols: getComputedStyle(document.querySelector('.aca-grid')).gridTemplateColumns,
    scrollW: document.documentElement.scrollWidth,
    innerW: window.innerWidth
  }));

  console.log(JSON.stringify({ base, afterSlides, detail, backUrl, tablet, mobile, errors }, null, 2));
  await browser.close();
})();
