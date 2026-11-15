import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// H5 开发时把 /api 代理到后端，前端代码里不出现后端域名
export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
