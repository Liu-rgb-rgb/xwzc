import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
export default defineConfig({
  plugins: [vue()],
  build: {
    minify: false,
    cssMinify: false
  },
  server: {
    proxy: {
      '/api': { target: 'http://127.0.0.1:8081', changeOrigin: true },
      // 纹样图存在 OSS，桶上未开放 CORS，WebGL 贴图会被拦。
      // 开发期由 dev server 同源转发，3D 舞台才能把纹样直接贴到布面。
      '/oss': {
        target: 'https://xwzc.oss-cn-beijing.aliyuncs.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/oss/, '')
      }
    }
  }
});
