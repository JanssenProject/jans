import react from "@vitejs/plugin-react";
import { defineConfig } from "electron-vite";

export default defineConfig({
  main: {},
  preload: {
    build: {
      externalizeDeps: false,
    },
  },
  renderer: {
    base: "./",
    // The generated WASM wrapper resolves its binary relative to import.meta.url.
    // Keep both packages out of Vite's JS-only dependency pre-bundle so that
    // relationship is preserved by the development server.
    optimizeDeps: {
      exclude: [
        "@janssenproject/cedarling",
        "@janssenproject/cedarling_wasm",
      ],
    },
    plugins: [react()],
  },
});
