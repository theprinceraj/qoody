// Claude Code PostToolUse hook: auto-format and lint a web file right after an agent edits it.
// Feedback goes to stderr with exit code 2 so the agent sees remaining lint errors immediately.
import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";
import path from "node:path";

const raw = readFileSync(0, "utf8").replace(/^﻿/, "");
const input = JSON.parse(raw || "{}");
const file = input.tool_input?.file_path;
if (!file) process.exit(0);

const root = path.resolve(process.env.CLAUDE_PROJECT_DIR ?? process.cwd());
const absolute = path.resolve(file);
const rel = path.relative(root, absolute).split(path.sep).join("/");

const inWeb = rel.startsWith("web/src/") || rel === "web/vite.config.ts";
const supported = /\.(tsx?|jsx?|json|css)$/.test(rel);
if (!inWeb || !supported || rel.endsWith("routeTree.gen.ts")) process.exit(0);

const webDir = path.join(root, "web");
const biomeBin = path.join(webDir, "node_modules", "@biomejs", "biome", "bin", "biome");

const result = spawnSync(
	process.execPath,
	[
		biomeBin,
		"check",
		"--write",
		"--no-errors-on-unmatched",
		"--files-ignore-unknown=true",
		absolute,
	],
	{ cwd: webDir, encoding: "utf8" },
);

if (result.status !== 0) {
	process.stderr.write(`Biome found problems in ${rel}:\n${result.stdout}${result.stderr}`);
	process.exit(2);
}
