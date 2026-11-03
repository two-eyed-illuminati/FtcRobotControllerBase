export declare const AUTO_DIR = "TeamCode/src/main/java/org/firstinspires/ftc/teamcode/auto";
export declare const EXPORTS_DIR = "TeamCode/build/exports";
export declare const SELECTED_FILE = ".selected";
export declare const EXPORTING_FILE = ".exporting";
export declare const DEFAULT_AUTO = "ExampleAuto";
export type AutoStatus = {
    name: string;
    exportedAt: number | null;
    stale: boolean;
    failed: boolean;
};
export type AutosMessage = {
    autos: AutoStatus[];
    selected: string | null;
    exporting: string | null;
    locked: boolean;
};
export declare const ppFileFor: (repoRoot: string, name: string) => string;
/** Classes directly in auto/ that define `static AutoBuilder createRoutine(...)`. */
export declare function listAutos(repoRoot: string): string[];
/** The chosen auto, or the default one when nothing valid was chosen yet. */
export declare function readSelected(repoRoot: string): string | null;
export declare function writeSelected(repoRoot: string, name: string): void;
export declare function autosMessage(repoRoot: string, locked: boolean): AutosMessage;
