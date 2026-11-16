import { execFile } from "node:child_process";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { Plugin } from "vite";

export const DEFAULT_TRACE_DIR = "/sdcard/FIRST/traces";
export const LATEST_TRACE = "latest.trace.json";
const TIMEOUT_MS = 10_000;

export type TraceFetchMessage =
  | { ok: true; name: string; pulledAt: number; text: string }
  | { ok: false; error: string };

type AdbResult = { code: number | string | null; output: string };

function runAdb(adb: string, args: string[]): Promise<AdbResult> {
  return new Promise((resolve) => {
    execFile(adb, args, { timeout: TIMEOUT_MS }, (error, stdout, stderr) => {
      const output = `${stdout}${stderr}`.trim();
      if (!error) return resolve({ code: 0, output });
      const failure = error as NodeJS.ErrnoException & { killed?: boolean };
      resolve({
        code: failure.killed ? "timeout" : (failure.code ?? null),
        output: output || failure.message,
      });
    });
  });
}

export function explainAdbFailure(
  { code, output }: AdbResult,
  traceDir: string,
): string {
  if (code === "ENOENT") {
    return "adb was not found. Install Android platform-tools or set ADB to its path.";
  }
  if (code === "timeout") {
    return `adb did not answer within ${TIMEOUT_MS / 1000} s. Check the connection to the Control Hub.`;
  }
  if (/remote object .* does not exist/.test(output)) {
    return `No trace at ${traceDir}/${LATEST_TRACE}. Run an auto first.`;
  }
  if (/no devices\/emulators found|device offline/.test(output)) {
    return "No device connected. Over Wi-Fi run: adb connect 192.168.43.1:5555";
  }
  if (/more than one device/.test(output)) {
    return "More than one device is connected. Set ADB_SERIAL to choose one.";
  }
  return `adb failed (${code}): ${output}`;
}

/** Serves /__trace/latest: pulls the newest robot trace from the Control Hub with adb. */
export function traceFetchPlugin(): Plugin {
  const adb = process.env.ADB || "adb";
  const serial = process.env.ADB_SERIAL;
  const traceDir = process.env.TRACE_DIR || DEFAULT_TRACE_DIR;

  return {
    name: "trace-fetch",
    apply: (_config, env) => env.command === "serve" && !process.env.VITEST,
    configureServer(server) {
      console.log(
        `[trace-fetch] adb=${adb} serial=${serial ?? "any"} dir=${traceDir}`,
      );

      server.middlewares.use("/__trace/latest", async (_request, response) => {
        const startedAt = Date.now();
        const folder = await mkdtemp(join(tmpdir(), "viewer-trace-"));
        const local = join(folder, LATEST_TRACE);
        const args = [
          ...(serial ? ["-s", serial] : []),
          "pull",
          `${traceDir}/${LATEST_TRACE}`,
          local,
        ];

        let message: TraceFetchMessage;
        try {
          const result = await runAdb(adb, args);
          message =
            result.code === 0
              ? {
                  ok: true,
                  name: LATEST_TRACE,
                  pulledAt: Date.now(),
                  text: await readFile(local, "utf8"),
                }
              : { ok: false, error: explainAdbFailure(result, traceDir) };
          console.log(
            `[trace-fetch] ${adb} ${args.join(" ")} -> ${result.code}` +
              (message.ok ? `, ${message.text.length} bytes` : "") +
              `, ${Date.now() - startedAt} ms`,
          );
          if (!message.ok) console.error(`[trace-fetch] ${message.error}`);
        } finally {
          await rm(folder, { recursive: true, force: true });
        }

        response.statusCode = message.ok ? 200 : 502;
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Cache-Control", "no-store");
        response.end(JSON.stringify(message));
      });
    },
  };
}
