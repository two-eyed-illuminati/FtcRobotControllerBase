import { execFile } from "node:child_process";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { IncomingMessage } from "node:http";
import type { Plugin } from "vite";

export const DEFAULT_TRACE_DIR = "/sdcard/FIRST/traces";
export const TRACE_NAME = /^[\w-]+\.(run\.jsonl|trace\.json)$/;
const TIMEOUT_MS = 10_000;

export type TraceFetchMessage =
  | { ok: true; name: string; pulledAt: number; text: string }
  | { ok: false; error: string };

export type TraceInfo = { name: string; bytes: number; savedAt: number | null };

export type TraceListMessage =
  { ok: true; traces: TraceInfo[] } | { ok: false; error: string };

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
    return `No trace found in ${traceDir}. Run an auto or TeleOp first.`;
  }
  if (/No such file or directory/.test(output)) {
    return `No trace found in ${traceDir}. Run an auto or TeleOp first.`;
  }
  if (/no devices\/emulators found|device offline/.test(output)) {
    return "No device connected. Over Wi-Fi run: adb connect 192.168.43.1:5555";
  }
  if (/more than one device/.test(output)) {
    return "More than one device is connected. Set ADB_SERIAL to choose one.";
  }
  return `adb failed (${code}): ${output}`;
}

export function isTraceName(name: string | null): name is string {
  return name !== null && TRACE_NAME.test(name);
}

/** `adb shell ls -l` rows to traces, newest first. The save time is the epoch ms in the file name. */
export function parseTraceListing(output: string): TraceInfo[] {
  const traces: TraceInfo[] = [];
  for (const line of output.split("\n")) {
    const match =
      /^\S+\s+\d+\s+\S+\s+\S+\s+(\d+)\s+\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}\s+(\S+)\s*$/.exec(
        line.trim(),
      );
    if (!match || !TRACE_NAME.test(match[2])) continue;
    const stamp = /-(\d{10,})\.[^-]+$/.exec(match[2]);
    traces.push({
      name: match[2],
      bytes: Number(match[1]),
      savedAt: stamp ? Number(stamp[1]) : null,
    });
  }
  return traces.sort((a, b) => (b.savedAt ?? 0) - (a.savedAt ?? 0));
}

function send(
  response: import("node:http").ServerResponse,
  message: { ok: boolean },
  status: number,
) {
  response.statusCode = status;
  response.setHeader("Content-Type", "application/json");
  response.setHeader("Cache-Control", "no-store");
  response.end(JSON.stringify(message));
}

function requestedName(request: IncomingMessage) {
  return new URL(request.url ?? "", "http://viewer").searchParams.get("name");
}

/** Serves /__trace/list, /__trace/file?name= and /__trace/latest from the Control Hub with adb. */
export function traceFetchPlugin(): Plugin {
  const adb = process.env.ADB || "adb";
  const serial = process.env.ADB_SERIAL;
  const traceDir = process.env.TRACE_DIR || DEFAULT_TRACE_DIR;
  const adbArgs = (...args: string[]) => [
    ...(serial ? ["-s", serial] : []),
    ...args,
  ];

  async function list(): Promise<TraceListMessage> {
    const startedAt = Date.now();
    const args = adbArgs("shell", "ls", "-l", traceDir);
    const result = await runAdb(adb, args);
    const message: TraceListMessage =
      result.code === 0
        ? { ok: true, traces: parseTraceListing(result.output) }
        : { ok: false, error: explainAdbFailure(result, traceDir) };
    console.log(
      `[trace-fetch] ${adb} ${args.join(" ")} -> ${result.code}` +
        (message.ok ? `, ${message.traces.length} traces` : "") +
        `, ${Date.now() - startedAt} ms`,
    );
    if (!message.ok) console.error(`[trace-fetch] ${message.error}`);
    return message;
  }

  async function pull(name: string): Promise<TraceFetchMessage> {
    const startedAt = Date.now();
    const folder = await mkdtemp(join(tmpdir(), "viewer-trace-"));
    const local = join(folder, name);
    const args = adbArgs("pull", `${traceDir}/${name}`, local);
    try {
      const result = await runAdb(adb, args);
      const message: TraceFetchMessage =
        result.code === 0
          ? {
              ok: true,
              name,
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
      return message;
    } finally {
      await rm(folder, { recursive: true, force: true });
    }
  }

  return {
    name: "trace-fetch",
    apply: (_config, env) => env.command === "serve" && !process.env.VITEST,
    configureServer(server) {
      console.log(
        `[trace-fetch] adb=${adb} serial=${serial ?? "any"} dir=${traceDir}`,
      );

      server.middlewares.use("/__trace/list", async (_request, response) => {
        const message = await list();
        send(response, message, message.ok ? 200 : 502);
      });

      server.middlewares.use("/__trace/file", async (request, response) => {
        const name = requestedName(request);
        if (!isTraceName(name)) {
          console.error(`[trace-fetch] rejected trace name ${name}`);
          return send(
            response,
            {
              ok: false,
              error: `Not a trace file name: ${name}`,
            } as TraceFetchMessage,
            400,
          );
        }
        const message = await pull(name);
        send(response, message, message.ok ? 200 : 502);
      });

      server.middlewares.use("/__trace/latest", async (_request, response) => {
        const listing = await list();
        if (!listing.ok) return send(response, listing, 502);
        if (listing.traces.length === 0) {
          const empty = {
            ok: false,
            error: explainAdbFailure(
              { code: 1, output: "No such file or directory" },
              traceDir,
            ),
          };
          return send(response, empty, 502);
        }
        const message = await pull(listing.traces[0].name);
        send(response, message, message.ok ? 200 : 502);
      });
    },
  };
}
