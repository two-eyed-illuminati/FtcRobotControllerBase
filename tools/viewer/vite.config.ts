/// <reference types="vitest/config" />
import { resolve } from "node:path";
import { defineConfig } from "vite";
import { svelte } from "@sveltejs/vite-plugin-svelte";
import { ppWatchPlugin } from "./server/ppWatchPlugin.ts";

const repoRoot = resolve(import.meta.dirname, "../..");

export default defineConfig({
  plugins: [svelte(), ppWatchPlugin(repoRoot)],
  server: { port: 5173 },
  build: { outDir: "dist" },
  base: "./",
  test: {
    include: ["test/**/*.test.ts"],
    setupFiles: ["test/support/setup.ts"],
  },
});
