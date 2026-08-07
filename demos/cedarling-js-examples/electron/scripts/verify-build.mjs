#!/usr/bin/env node

import { access, readFile, readdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const electronRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const outputRoot = path.join(electronRoot, "out");

async function filesBelow(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  return (
    await Promise.all(
      entries.map(async (entry) => {
        const entryPath = path.join(directory, entry.name);
        return entry.isDirectory() ? filesBelow(entryPath) : [entryPath];
      }),
    )
  ).flat();
}

function requireCondition(condition, message) {
  if (!condition) throw new Error(message);
}

const requiredFiles = [
  path.join(outputRoot, "main", "index.js"),
  path.join(outputRoot, "preload", "index.js"),
  path.join(outputRoot, "renderer", "index.html"),
];
await Promise.all(requiredFiles.map((file) => access(file)));

const rendererFiles = await filesBelow(path.join(outputRoot, "renderer"));
const relativeRendererFiles = rendererFiles.map((file) =>
  path.relative(outputRoot, file),
);
requireCondition(
  rendererFiles.some((file) => file.endsWith(".wasm")),
  "The renderer build did not emit Cedarling WASM.",
);
requireCondition(
  rendererFiles.some((file) => file.endsWith(".css")),
  "The renderer build did not emit its stylesheet.",
);

const rendererJavaScript = (
  await Promise.all(
    rendererFiles
      .filter((file) => file.endsWith(".js"))
      .map((file) => readFile(file, "utf8")),
  )
).join("\n");
requireCondition(
  rendererJavaScript.includes(".wasm"),
  "The renderer JavaScript does not reference its WASM asset.",
);

console.log("Verified Electron build outputs:");
for (const file of relativeRendererFiles.sort()) console.log(`  ${file}`);
