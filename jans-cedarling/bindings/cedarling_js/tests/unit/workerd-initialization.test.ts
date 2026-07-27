import type QUnitApi from "qunit";

import { registerEngineInitializationTests } from "./engine-initialization-fixture.js";

/**
 * Registers workerd engine-initialization tests.
 *
 * The workerd engine factory is built on the shared `createEngineFactory`,
 * so the suite body is shared with the other host engines. This module
 * exists to record that the workerd host's externally visible construction
 * contract matches the canonical shared behavior.
 */
export default function registerWorkerdInitializationTests(QUnit: QUnitApi): void {
  registerEngineInitializationTests(QUnit, "workerd");
}
