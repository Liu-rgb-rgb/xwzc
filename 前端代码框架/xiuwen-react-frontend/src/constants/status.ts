export const ORDER_STATUS_TEXT: Record<string, string> = {
  WAIT_PAY: '待支付',
  WAIT_CONFIRM: '待接单',
  PRODUCING: '制作中',
  WAIT_DELIVERY: '待发货',
  DELIVERED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
};

export const PRODUCT_STATUS_TEXT: Record<string, string> = {
  ON_SALE: '上架',
  OFF_SALE: '下架',
  SOLD_OUT: '售罄',
  DRAFT: '草稿'
};

export const COURSE_STATUS_TEXT: Record<string, string> = {
  PUBLISHED: '已发布',
  DRAFT: '草稿',
  HIDDEN: '隐藏'
};
