import type QUnitApi from "qunit";

import { registerEngineInitializationTests } from "./engine-initialization-fixture.js";

/**
 * Registers Node engine-initialization tests.
 *
 * The Node engine factory is built on the shared `createEngineFactory`, so
 * the suite body is shared with the other host engines. This module exists
 * to record that the Node host's externally visible construction contract
 * matches the canonical shared behavior.
 */
export default function registerNodeInitializationTests(QUnit: QUnitApi): void {
  registerEngineInitializationTests(QUnit, "node");
}
