import { unwatchFile, watchFile } from "node:fs";
import { readFile, stat } from "node:fs/promises";
import { resolve } from "node:path";
import type { Plugin } from "vite";

export const DEFAULT_PP_FILE = "TeamCode/build/exports/auto-path.pp";
const POLL_MS = 150;
const SETTLE_MS = 80;

export const simFileOf = (file: string) => `${file}.sim.json`;

export type PpMessage = (
  | { ok: true; file: string; text: string; mtimeMs: number }
  | { ok: false; file: string; error: string }
) & { exportError: string | null; simText: string | null };

export function resolvePpFile(repoRoot: string): string {
  return resolve(repoRoot, process.env.PP_FILE || DEFAULT_PP_FILE);
}

async function readExportError(file: string): Promise<string | null> {
  try {
    return (await readFile(`${file}.error`, "utf8")).trim() || null;
  } catch {
    return null;
  }
}

async function readSim(file: string): Promise<string | null> {
  try {
    return await readFile(simFileOf(file), "utf8");
  } catch {
    return null;
  }
}

async function readPp(file: string): Promise<PpMessage> {
  const [exportError, simText] = await Promise.all([
    readExportError(file),
    readSim(file),
  ]);
  try {
    const [text, info] = await Promise.all([
      readFile(file, "utf8"),
      stat(file),
    ]);
    return {
      ok: true,
      file,
      text,
      mtimeMs: info.mtimeMs,
      exportError,
      simText,
    };
  } catch (error) {
    const code = (error as NodeJS.ErrnoException).code;
    const reason =
      code === "ENOENT"
        ? "File not found. Run the exporter (pnpm dev starts it)."
        : `Cannot read file: ${(error as Error).message}`;
    return { ok: false, file, error: reason, exportError, simText };
  }
}

/** Serves the exported .pp at /__pp and pushes "pp:update" whenever it changes. */
export function ppWatchPlugin(repoRoot: string): Plugin {
  const file = resolvePpFile(repoRoot);

  return {
    name: "pp-watch",
    apply: (_config, env) => env.command === "serve" && !process.env.VITEST,
    configureServer(server) {
      console.log(`[pp-watch] watching ${file} every ${POLL_MS}ms`);

      server.middlewares.use("/__pp", async (_request, response) => {
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Cache-Control", "no-store");
        response.end(JSON.stringify(await readPp(file)));
      });

      let settle: NodeJS.Timeout | undefined;
      const push = async () => {
        const message = await readPp(file);
        console.log(
          message.ok
            ? `[pp-watch] pushed ${message.text.length} bytes` +
                (message.simText === null
                  ? ", no simulation"
                  : `, simulation ${message.simText.length} bytes`)
            : `[pp-watch] pushed error: ${message.error}`,
        );
        server.ws.send("pp:update", message);
      };

      const watched = [file, `${file}.error`, simFileOf(file)];
      for (const path of watched) {
        watchFile(path, { interval: POLL_MS }, (current, previous) => {
          if (
            current.mtimeMs === previous.mtimeMs &&
            current.size === previous.size
          ) {
            return;
          }
          clearTimeout(settle);
          settle = setTimeout(push, SETTLE_MS);
        });
      }
      server.httpServer?.once("close", () => {
        for (const path of watched) unwatchFile(path);
        clearTimeout(settle);
      });
    },
  };
}
