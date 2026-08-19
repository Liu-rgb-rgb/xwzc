export const patternImages = [
  '/demo/pattern/peony-phoenix-pattern-01.jpg',
  '/demo/pattern/peony-phoenix-pattern-02.jpg',
  '/demo/pattern/round-flower-pattern-01.jpg',
  '/demo/pattern/lingnan-window-pattern-01.jpg',
  '/demo/pattern/lion-dance-pattern-01.jpg'
];
export const patterns = [
  '牡丹呈祥',
  '凤舞花朝',
  '如意团花',
  '岭南花窗',
  '醒狮纳福',
  '锦簇花枝',
  '云起丹霞',
  '金蕊宝相'
].map((title, i) => ({
  id: i + 1,
  title,
  meta: ['广绣经典', '岭南瑞鸟', '传统团纹', '建筑纹样'][i % 4],
  image: patternImages[i % 5]
}));
export const products = [
  ['广绣牡丹帆布袋', 68, 'peony-canvas-bag-cover.jpg'],
  ['凤凰瑞彩帆布袋', 88, 'phoenix-tote-cover.jpg'],
  ['岭南真丝长巾', 198, 'lingnan-silk-scarf-cover.jpg'],
  ['木棉花影笔记本', 39, 'lingnan-notebook-cover.jpg'],
  ['醒狮如意抱枕', 168, 'lion-pillow-cover.jpg'],
  ['牡丹团花杯垫', 49, 'round-coaster-set-cover.jpg'],
  ['广绣明信片礼盒', 28, 'postcard-gift-cover.jpg'],
  ['木棉流光丝巾', 158, 'kapok-long-scarf-cover.jpg']
].map(([title, price, file], i) => ({
  id: i + 1,
  title,
  price,
  image: '/demo/product/' + file,
  desc: '传统广绣纹样的现代生活表达'
}));
export const courses = ['广绣历史与文化', '经典纹样解析', '针法基础入门', '创作实践与应用'].map(
  (title, i) => ({
    id: i + 1,
    title,
    desc: [
      '从岭南丝路读懂千年广绣',
      '读懂牡丹凤凰纹样寓意',
      '从穿针引线到基础铺针',
      '完成自己的广绣作品'
    ][i],
    lessons: [18, 32, 46, 26][i],
    image:
      i < 2
        ? patternImages[i]
        : '/demo/product/' + ['round-coaster-set-cover.jpg', 'peony-canvas-bag-cover.jpg'][i - 2]
  })
);
