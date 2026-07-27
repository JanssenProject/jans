import type QUnitApi from "qunit";

import { registerEngineInitializationTests } from "./engine-initialization-fixture.js";

/**
 * Registers Edge engine-initialization tests.
 *
 * The Vercel Edge engine factory is built on the shared `createEngineFactory`,
 * so the suite body is shared with the other host engines. This module
 * exists to record that the Edge host's externally visible construction
 * contract matches the canonical shared behavior.
 */
export default function registerEdgeInitializationTests(QUnit: QUnitApi): void {
  registerEngineInitializationTests(QUnit, "edge");
}
