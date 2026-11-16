import { mkdirSync, unwatchFile, watch, watchFile } from "node:fs";
import { readFile, stat } from "node:fs/promises";
import type { IncomingMessage } from "node:http";
import { resolve } from "node:path";
import type { Plugin } from "vite";
import {
  AUTO_DIR,
  DEFAULT_AUTO,
  EXPORTS_DIR,
  autosMessage,
  listAutos,
  ppFileFor,
  readSelected,
  writeSelected,
} from "./autos.ts";

const POLL_MS = 150;
const SETTLE_MS = 80;

export const simFileOf = (file: string) => `${file}.sim.json`;

export type PpMessage = (
  | { ok: true; file: string; text: string; mtimeMs: number }
  | { ok: false; file: string; error: string }
) & { exportError: string | null; simText: string | null };

/** PP_FILE wins; otherwise the .pp of the auto chosen in the viewer. */
export function resolvePpFile(repoRoot: string): string {
  if (process.env.PP_FILE) return resolve(repoRoot, process.env.PP_FILE);
  return ppFileFor(repoRoot, readSelected(repoRoot) ?? DEFAULT_AUTO);
}

async function readBody(request: IncomingMessage): Promise<string> {
  let body = "";
  for await (const chunk of request) body += chunk;
  return body;
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
        ? "File not found. It appears when the exporter finishes (pnpm dev starts it)."
        : `Cannot read file: ${(error as Error).message}`;
    return { ok: false, file, error: reason, exportError, simText };
  }
}

/** Serves the chosen auto's .pp at /__pp and the auto list at /__autos. Pushes "pp:update" and "autos:update". */
export function ppWatchPlugin(repoRoot: string): Plugin {
  const locked = Boolean(process.env.PP_FILE);
  const exportsDir = resolve(repoRoot, EXPORTS_DIR);
  const autoDir = resolve(repoRoot, AUTO_DIR);

  return {
    name: "pp-watch",
    apply: (_config, env) => env.command === "serve" && !process.env.VITEST,
    configureServer(server) {
      let file = resolvePpFile(repoRoot);
      let watched: string[] = [];
      let settle: NodeJS.Timeout | undefined;
      let changed: NodeJS.Timeout | undefined;

      const pushPp = async () => {
        const message = await readPp(file);
        console.log(
          message.ok
            ? `[pp-watch] pushed ${file}: ${message.text.length} bytes` +
                (message.simText === null
                  ? ", no simulation"
                  : `, simulation ${message.simText.length} bytes`)
            : `[pp-watch] pushed error: ${message.error}`,
        );
        server.ws.send("pp:update", message);
      };

      const pushAutos = () => {
        const message = autosMessage(repoRoot, locked);
        console.log(
          `[pp-watch] autos: ${message.autos.map((a) => a.name).join(", ") || "none"}; ` +
            `selected ${message.selected ?? "none"}, exporting ${message.exporting ?? "nothing"}`,
        );
        server.ws.send("autos:update", message);
      };

      const watchFiles = () => {
        for (const path of watched) unwatchFile(path);
        watched = [file, `${file}.error`, simFileOf(file)];
        for (const path of watched) {
          watchFile(path, { interval: POLL_MS }, (current, previous) => {
            if (
              current.mtimeMs === previous.mtimeMs &&
              current.size === previous.size
            ) {
              return;
            }
            clearTimeout(settle);
            settle = setTimeout(pushPp, SETTLE_MS);
          });
        }
      };

      /** Points the file watchers at the chosen auto. True when the file changed. */
      const followSelection = () => {
        const next = resolvePpFile(repoRoot);
        if (next === file) return false;
        console.log(`[pp-watch] now watching ${next}`);
        file = next;
        watchFiles();
        return true;
      };

      mkdirSync(exportsDir, { recursive: true });
      console.log(
        `[pp-watch] watching ${file} every ${POLL_MS}ms; autos: ${listAutos(repoRoot).join(", ") || "none"}` +
          (locked ? " (PP_FILE set, the picker is off)" : ""),
      );
      watchFiles();

      const autosChanged = () => {
        clearTimeout(changed);
        changed = setTimeout(() => {
          const moved = followSelection();
          pushAutos();
          if (moved) void pushPp();
        }, SETTLE_MS);
      };
      const dirWatchers = [
        watch(exportsDir, autosChanged),
        watch(autoDir, { recursive: true }, autosChanged),
      ];

      server.middlewares.use("/__pp", async (_request, response) => {
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Cache-Control", "no-store");
        response.end(JSON.stringify(await readPp(file)));
      });

      server.middlewares.use("/__autos/select", async (request, response) => {
        response.setHeader("Content-Type", "application/json");
        const name = String(JSON.parse((await readBody(request)) || "{}").name);
        if (locked || !listAutos(repoRoot).includes(name)) {
          response.statusCode = 400;
          const error = locked
            ? "PP_FILE is set, so the auto cannot be changed here."
            : `${name} is not an auto in auto/.`;
          console.error(`[pp-watch] select rejected: ${error}`);
          return response.end(JSON.stringify({ ok: false, error }));
        }
        writeSelected(repoRoot, name);
        console.log(`[pp-watch] selected ${name}`);
        autosChanged();
        response.end(JSON.stringify({ ok: true }));
      });

      server.middlewares.use("/__autos", (_request, response) => {
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Cache-Control", "no-store");
        response.end(JSON.stringify(autosMessage(repoRoot, locked)));
      });

      server.httpServer?.once("close", () => {
        for (const path of watched) unwatchFile(path);
        for (const watcher of dirWatchers) watcher.close();
        clearTimeout(settle);
        clearTimeout(changed);
      });
    },
  };
}
