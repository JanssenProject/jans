import { createCedarlingForEngine } from "../client/client.js";
import { createNodeEngine } from "../engine/node.js";

export const createCedarling = createCedarlingForEngine(createNodeEngine);
export type * from "../index.js";
