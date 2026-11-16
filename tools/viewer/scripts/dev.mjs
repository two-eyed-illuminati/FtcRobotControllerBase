import { spawn } from "node:child_process";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const children = [
  [
    "viewer",
    process.execPath,
    [resolve(root, "node_modules/vite/bin/vite.js")],
  ],
  [
    "export",
    process.execPath,
    [resolve(import.meta.dirname, "export-watch.mjs")],
  ],
].map(([name, command, args]) => {
  console.log(`[dev] starting ${name}: ${command} ${args.join(" ")}`);
  const child = spawn(command, args, { cwd: root, stdio: "inherit" });
  child.on("exit", (code) => {
    console.log(`[dev] ${name} exited with ${code}`);
    shutdown(code ?? 1);
  });
  return child;
});

function shutdown(code) {
  for (const child of children) child.kill();
  process.exit(code);
}
process.on("SIGINT", () => shutdown(0));
process.on("SIGTERM", () => shutdown(0));
