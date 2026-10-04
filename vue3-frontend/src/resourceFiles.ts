import { api } from './api';

const fallbackCovers = [
  '/demo/product/postcard-gift-cover.jpg',
  '/demo/product/round-coaster-set-cover.jpg',
  '/demo/pattern/round-flower-pattern-01.jpg',
  '/demo/pattern/peony-phoenix-pattern-02.jpg',
  '/demo/pattern/lion-dance-pattern-01.jpg',
  '/demo/pattern/peony-phoenix-pattern-01.jpg',
  '/demo/pattern/lingnan-window-pattern-01.jpg',
  '/demo/product/peony-canvas-bag-mockup.png'
];

export function resourceCover(resource: any, index = 0) {
  const cover = resource?.coverImage || resource?.image || resource?.imageUrl;
  if (typeof cover === 'string' && cover.trim()) return cover;
  const resourceIndex = Number(resource?.id);
  const fallbackIndex =
    Number.isFinite(resourceIndex) && resourceIndex > 0
      ? (resourceIndex - 1) % fallbackCovers.length
      : index % fallbackCovers.length;
  return fallbackCovers[fallbackIndex];
}

function usableResourceUrl(value: unknown) {
  return typeof value === 'string' && value.trim() !== '' && !value.includes('example.com');
}

function filenameFor(resource: any, url: string) {
  const path = url.split('?')[0];
  const extension = path.match(/\.[a-z0-9]{2,5}$/i)?.[0] || '';
  const title = String(resource?.title || '创作资源').replace(/[\\/:*?"<>|]/g, '-');
  return `${title}${extension}`;
}

async function saveUrl(url: string, filename: string) {
  const response = await fetch(url);
  if (!response.ok) throw new Error('资源文件读取失败');
  const blobUrl = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);
}

export async function downloadResource(resource: any, index = 0) {
  let latest = resource;
  if (resource?.id != null) {
    try {
      latest = { ...resource, ...(await api.resources.download(resource.id)) };
    } catch {
      // 下载统计接口异常时，仍允许用户获取页面已经展示的资源。
    }
  }
  const url = usableResourceUrl(latest?.resourceUrl)
    ? String(latest.resourceUrl)
    : resourceCover(latest, index);
  await saveUrl(url, filenameFor(latest, url));
}
