import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import test from "node:test";

import { build } from "esbuild";

const root = dirname(fileURLToPath(import.meta.url));
const runtimeSource = join(root, "../src/runtime.ts");
let importVersion = 0;

async function loadRuntime(generated) {
  const temporary = await mkdtemp(join(tmpdir(), "cedarling-runtime-test-"));
  const output = join(temporary, "runtime.mjs");
  globalThis.__cedarlingGenerated = generated;
  try {
    await build({
      entryPoints: [runtimeSource],
      outfile: output,
      bundle: true,
      format: "esm",
      platform: "node",
      target: "node22",
      plugins: [{
        name: "generated-glue-fixture",
        setup(esbuild) {
          esbuild.onResolve(
            { filter: /^cedarling:generated-glue$/ },
            () => ({ path: "generated-glue", namespace: "fixture" }),
          );
          esbuild.onLoad(
            { filter: /.*/, namespace: "fixture" },
            () => ({
              contents: [
                "const generated = globalThis.__cedarlingGenerated;",
                "export default (...args) => generated.initWasm(...args);",
                "export const init = (...args) => generated.init(...args);",
                "export const initFromArchiveBytes = (...args) =>",
                "  generated.initFromArchiveBytes(...args);",
                "export const initSync = (...args) => generated.initSync(...args);",
              ].join("\n"),
              loader: "js",
            }),
          );
        },
      }],
    });
    const runtime = await import(
      pathToFileURL(output).href + "?" + String(importVersion += 1),
    );
    return {
      createRuntime: runtime.createRuntime,
      async dispose() {
        await rm(temporary, { force: true, recursive: true });
      },
    };
  } finally {
    delete globalThis.__cedarlingGenerated;
  }
}

function generatedClient(events, label) {
  return {
    async authorizeUnsigned() {
      events.push(label + ":authorize");
      return { decision: true, json_string: () => '{"decision":true}', free() {} };
    },
    async shutDown() {
      events.push(label + ":shutDown");
    },
    free() {
      events.push(label + ":free");
    },
  };
}

test("automatic initialization returns generated clients unchanged", async (t) => {
  const module = {};
  const events = [];
  const ordinary = generatedClient(events, "ordinary");
  const archived = generatedClient(events, "archive");
  const initialized = [];
  const received = [];
  const runtimeModule = await loadRuntime({
    initWasm() {
      throw new Error("automatic initialization must use initSync");
    },
    initSync(input) {
      initialized.push(input);
      return { memory: {} };
    },
    async init(properties) {
      received.push(["init", properties]);
      return ordinary;
    },
    async initFromArchiveBytes(properties, archive) {
      received.push(["archive", properties, archive]);
      return archived;
    },
  });
  t.after(() => runtimeModule.dispose());

  let loads = 0;
  const runtime = runtimeModule.createRuntime(async () => {
    loads += 1;
    return module;
  });
  const properties = Object.freeze({ CEDARLING_APPLICATION_NAME: "runtime-test" });
  const archive = new Uint8Array([1, 2, 3]);
  const [actualOrdinary, actualArchived] = await Promise.all([
    runtime.init(properties),
    runtime.initFromArchiveBytes(properties, archive),
  ]);

  assert.equal(loads, 1);
  assert.deepEqual(initialized, [{ module }]);
  assert.deepEqual(received, [
    ["init", properties],
    ["archive", properties, archive],
  ]);
  assert.strictEqual(actualOrdinary, ordinary);
  assert.strictEqual(actualArchived, archived);
  assert.equal(typeof actualOrdinary.free, "function");
  assert.equal(typeof actualOrdinary.shutDown, "function");
  const result = await actualOrdinary.authorizeUnsigned("{}");
  assert.equal(typeof result.free, "function");
  assert.deepEqual(events, ["ordinary:authorize"]);
  await actualOrdinary.shutDown();
  actualOrdinary.free();
  assert.deepEqual(events, ["ordinary:authorize", "ordinary:shutDown", "ordinary:free"]);
});

test("legacy initWasm delegates caller input and shares initialization", async (t) => {
  const events = [];
  const client = generatedClient(events, "legacy");
  const initialization = { memory: {} };
  const inputs = [];
  const runtimeModule = await loadRuntime({
    async initWasm(input) {
      inputs.push(input);
      return initialization;
    },
    initSync() {
      throw new Error("legacy asynchronous initialization must not call initSync");
    },
    async init() {
      return client;
    },
    async initFromArchiveBytes() {
      throw new Error("unused");
    },
  });
  t.after(() => runtimeModule.dispose());

  let loads = 0;
  const runtime = runtimeModule.createRuntime(async () => {
    loads += 1;
    return {};
  }, async (input) => {
    inputs.push(input);
    return initialization;
  });
  const input = new Uint8Array([1, 2, 3]);
  assert.strictEqual(await runtime.initWasm(input), initialization);
  assert.strictEqual(await runtime.init({}), client);
  assert.deepEqual(inputs, [input]);
  assert.equal(loads, 0);
});

test("legacy initSync shares initialization with automatic init", async (t) => {
  const client = generatedClient([], "sync");
  const initialization = { memory: {} };
  const inputs = [];
  const runtimeModule = await loadRuntime({
    initWasm() {
      throw new Error("automatic initialization must reuse initSync state");
    },
    initSync(input) {
      inputs.push(input);
      return initialization;
    },
    async init() {
      return client;
    },
    async initFromArchiveBytes() {
      throw new Error("unused");
    },
  });
  t.after(() => runtimeModule.dispose());

  let loads = 0;
  const runtime = runtimeModule.createRuntime(async () => {
    loads += 1;
    return {};
  });
  const input = new Uint8Array([4, 5, 6]);
  assert.strictEqual(runtime.initSync(input), initialization);
  assert.strictEqual(await runtime.init({}), client);
  assert.deepEqual(inputs, [input]);
  assert.equal(loads, 0);
});

test("a failed automatic initialization can retry", async (t) => {
  const failure = new Error("WASM load failed");
  const runtimeModule = await loadRuntime({
    initWasm() {
      throw new Error("unused");
    },
    initSync() {
      return { memory: {} };
    },
    async init() {
      return generatedClient([], "retry");
    },
    async initFromArchiveBytes() {
      throw new Error("unused");
    },
  });
  t.after(() => runtimeModule.dispose());

  let loads = 0;
  const runtime = runtimeModule.createRuntime(async () => {
    loads += 1;
    if (loads === 1) throw failure;
    return {};
  });

  await assert.rejects(runtime.init({}), failure);
  await runtime.init({});
  assert.equal(loads, 2);
});
