import type { Plugin } from "vite";
export declare const DEFAULT_TRACE_DIR = "/sdcard/FIRST/traces";
export declare const LATEST_TRACE = "latest.trace.json";
export type TraceFetchMessage = {
    ok: true;
    name: string;
    pulledAt: number;
    text: string;
} | {
    ok: false;
    error: string;
};
type AdbResult = {
    code: number | string | null;
    output: string;
};
export declare function explainAdbFailure({ code, output }: AdbResult, traceDir: string): string;
/** Serves /__trace/latest: pulls the newest robot trace from the Control Hub with adb. */
export declare function traceFetchPlugin(): Plugin;
export {};
