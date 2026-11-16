import type { Plugin } from "vite";
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
/** PP_FILE wins; otherwise the .pp of the auto chosen in the viewer. */
export declare function resolvePpFile(repoRoot: string): string;
/** Serves the chosen auto's .pp at /__pp and the auto list at /__autos. Pushes "pp:update" and "autos:update". */
export declare function ppWatchPlugin(repoRoot: string): Plugin;
