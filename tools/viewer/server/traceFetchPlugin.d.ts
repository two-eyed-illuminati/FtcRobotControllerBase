import type { Plugin } from "vite";
export declare const DEFAULT_TRACE_DIR = "/sdcard/FIRST/traces";
export declare const TRACE_NAME: RegExp;
export type TraceFetchMessage = {
    ok: true;
    name: string;
    pulledAt: number;
    text: string;
} | {
    ok: false;
    error: string;
};
export type TraceInfo = {
    name: string;
    bytes: number;
    savedAt: number | null;
};
export type TraceListMessage = {
    ok: true;
    traces: TraceInfo[];
} | {
    ok: false;
    error: string;
};
type AdbResult = {
    code: number | string | null;
    output: string;
};
export declare function explainAdbFailure({ code, output }: AdbResult, traceDir: string): string;
export declare function isTraceName(name: string | null): name is string;
/** `adb shell ls -l` rows to traces, newest first. The save time is the epoch ms in the file name. */
export declare function parseTraceListing(output: string): TraceInfo[];
/** Serves /__trace/list, /__trace/file?name= and /__trace/latest from the Control Hub with adb. */
export declare function traceFetchPlugin(): Plugin;
export {};
