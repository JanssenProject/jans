import { createCedarlingForEngine } from "./client/client.js";
import { createEdgeEngine } from "./engine/edge.js";

export const createCedarling = createCedarlingForEngine(createEdgeEngine);
export type * from "./index.js";
