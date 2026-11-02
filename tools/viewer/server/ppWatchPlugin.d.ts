import type { Plugin } from "vite";
export declare const DEFAULT_PP_FILE = "TeamCode/build/exports/auto-path.pp";
export declare const simFileOf: (file: string) => string;
export type PpMessage = ({
    ok: true;
    file: string;
    text: string;
    mtimeMs: number;
} | {
    ok: false;
    file: string;
    error: string;
}) & {
    exportError: string | null;
    simText: string | null;
};
export declare function resolvePpFile(repoRoot: string): string;
/** Serves the exported .pp at /__pp and pushes "pp:update" whenever it changes. */
export declare function ppWatchPlugin(repoRoot: string): Plugin;
