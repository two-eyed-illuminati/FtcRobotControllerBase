import { spawnSync } from "node:child_process";
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  statSync,
  watch,
  writeFileSync,
} from "node:fs";
import { homedir, tmpdir } from "node:os";
import {
  basename,
  delimiter,
  dirname,
  join,
  relative,
  resolve,
  sep,
} from "node:path";

const repoRoot = resolve(import.meta.dirname, "../../..");
const ppFile = resolve(
  repoRoot,
  process.env.PP_FILE || "TeamCode/build/exports/auto-path.pp",
);
const errorFile = `${ppFile}.error`;
const classpathCache = join(dirname(ppFile), ".export-classpath");
const printCpScript = resolve(import.meta.dirname, "printcp.gradle");
const autoDir = resolve(
  repoRoot,
  "TeamCode/src/main/java/org/firstinspires/ftc/teamcode/auto",
);
const mainClass = "org.firstinspires.ftc.teamcode.auto.utils.ExportAutoPath";
const sourceRoots = ["TeamCode/src", "FtcRobotController/src"].map((p) =>
  resolve(repoRoot, p),
);
const gradleDirs = [repoRoot, resolve(repoRoot, "TeamCode")];
const gradleNames = new Set([
  "build.gradle",
  "build.common.gradle",
  "build.dependencies.gradle",
  "settings.gradle",
  "gradle.properties",
]);
const sourceExtensions = [".java", ".kt", ".xml"];
const debounceMs = 150;

const env = { ...process.env };
env.ANDROID_HOME ||= join(homedir(), "Library/Android/sdk");
const classesDir = join(tmpdir(), `pedro-viewer-classes-${process.pid}`);

const COMMAND_TIMEOUT_MS = 180_000;

const log = (message) => console.log(`[export] ${message}`);

function run(command, args, label) {
  const started = performance.now();
  const result = spawnSync(command, args, {
    cwd: repoRoot,
    env,
    encoding: "utf8",
    maxBuffer: 64 * 1024 * 1024,
    timeout: COMMAND_TIMEOUT_MS,
  });
  const seconds = ((performance.now() - started) / 1000).toFixed(1);
  const timedOut = result.error?.code === "ETIMEDOUT";
  const output = timedOut
    ? `${label} did not finish within ${COMMAND_TIMEOUT_MS / 1000}s and was stopped.`
    : `${result.stdout ?? ""}${result.stderr ?? ""}`.trim();
  log(
    `${label}: ${result.status === 0 ? "ok" : `FAILED (exit ${result.status})`} in ${seconds}s`,
  );
  return { ok: result.status === 0, output };
}

function publish(tmpPp, output) {
  const tmpSim = `${tmpPp}.sim.json`;
  if (existsSync(tmpSim)) renameSync(tmpSim, `${ppFile}.sim.json`);
  else rmSync(`${ppFile}.sim.json`, { force: true });
  renameSync(tmpPp, ppFile);
  rmSync(errorFile, { force: true });
  log(`wrote ${ppFile} (${statSync(ppFile).size} bytes)`);
  for (const line of output.split("\n")) {
    if (/^(Simulated|Sim warning)/.test(line)) log(line);
  }
}

function reportFailure(output) {
  mkdirSync(dirname(errorFile), { recursive: true });
  writeFileSync(errorFile, output || "Export failed with no output.");
  console.error(
    `[export] export failed; the viewer keeps the last good path\n${output}`,
  );
}

function refreshClasspath() {
  const result = run(
    "./gradlew",
    ["-q", "-I", printCpScript, ":TeamCode:printExportCp"],
    "gradle classpath",
  );
  const line = result.output.split("\n").find((l) => l.startsWith("CP="));
  if (!result.ok || !line)
    return reportFailure(
      `Could not read the export classpath from Gradle.\n${result.output}`,
    );
  writeFileSync(classpathCache, line.slice(3));
  log(`cached classpath: ${line.slice(3).split(delimiter).length} entries`);
}

function fullGradleExport() {
  mkdirSync(dirname(ppFile), { recursive: true });
  const tmpPp = `${ppFile}.tmp`;
  const result = run(
    "./gradlew",
    [":TeamCode:exportAutoPath", `-PppOutput=${tmpPp}`, "-q"],
    "gradle exportAutoPath",
  );
  if (!result.ok) return reportFailure(result.output);
  publish(tmpPp, result.output);
  rmSync(classesDir, { recursive: true, force: true });
  refreshClasspath();
}

function classpathStaleReason() {
  if (!existsSync(classpathCache)) return "no cached classpath";
  const cachedAt = statSync(classpathCache).mtimeMs;
  for (const dir of gradleDirs) {
    for (const name of gradleNames) {
      const file = join(dir, name);
      if (existsSync(file) && statSync(file).mtimeMs > cachedAt)
        return `${relative(repoRoot, file)} is newer than the cached classpath`;
    }
  }
  const missing = readFileSync(classpathCache, "utf8")
    .split(delimiter)
    .find((entry) => !existsSync(entry));
  return missing ? `classpath entry is missing: ${missing}` : null;
}

function autoSources() {
  return readdirSync(autoDir, { recursive: true })
    .filter((name) => name.endsWith(".java"))
    .map((name) => join(autoDir, name))
    .sort();
}

function fastExport() {
  const classpath = readFileSync(classpathCache, "utf8");
  mkdirSync(classesDir, { recursive: true });
  const sources = autoSources();
  const compiled = run(
    "javac",
    [
      "-nowarn",
      "-proc:none",
      "-cp",
      `${classesDir}${delimiter}${classpath}`,
      "-d",
      classesDir,
      ...sources,
    ],
    `javac ${sources.length} auto files`,
  );
  if (!compiled.ok) return reportFailure(compiled.output);
  const tmpPp = `${ppFile}.tmp`;
  const exported = run(
    "java",
    ["-cp", `${classesDir}${delimiter}${classpath}`, mainClass, tmpPp],
    "java ExportAutoPath",
  );
  if (!exported.ok) return reportFailure(exported.output);
  publish(tmpPp, exported.output);
}

function exportFor(changed) {
  const outsideAuto = [...changed].filter(
    (file) => !file.startsWith(autoDir + sep) || !file.endsWith(".java"),
  );
  const stale = classpathStaleReason();
  if (outsideAuto.length || stale) {
    const reason = outsideAuto.length
      ? `changed outside auto/: ${outsideAuto.map((f) => relative(repoRoot, f)).join(", ")}`
      : stale;
    log(`full gradle export (${reason})`);
    return fullGradleExport();
  }
  log(
    `fast export (changed: ${[...changed].map((f) => relative(repoRoot, f)).join(", ")})`,
  );
  fastExport();
}

let pending = new Set();
let timer;
let running = false;

function schedule(file) {
  pending.add(file);
  clearTimeout(timer);
  timer = setTimeout(drain, debounceMs);
}

function drain() {
  if (running) return;
  while (pending.size) {
    const batch = pending;
    pending = new Set();
    running = true;
    try {
      exportFor(batch);
    } catch (error) {
      reportFailure(String(error?.stack ?? error));
    }
    running = false;
  }
}

log(`repo root: ${repoRoot}`);
log(`output: ${ppFile}`);
log(
  `watching: ${sourceRoots.map((p) => relative(repoRoot, p)).join(", ")} and gradle files`,
);
log(`ANDROID_HOME=${env.ANDROID_HOME}`);

for (const root of sourceRoots) {
  watch(root, { recursive: true }, (_event, name) => {
    if (name && sourceExtensions.some((ext) => name.endsWith(ext)))
      schedule(resolve(root, name));
  });
}
for (const dir of gradleDirs) {
  watch(dir, (_event, name) => {
    if (name && gradleNames.has(basename(name))) schedule(join(dir, name));
  });
}

log(
  "initial full gradle export (builds the classes the fast path links against)",
);
fullGradleExport();
