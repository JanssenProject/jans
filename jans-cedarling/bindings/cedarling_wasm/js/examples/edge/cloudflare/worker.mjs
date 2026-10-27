import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm/edge";
import archive from "../../.build/policy-store.cjar";
import { runCedarling } from "../../shared/run-cedarling.mjs";

export default {
  async fetch(request) {
    if (new URL(request.url).pathname !== "/authorize") {
      return new Response("Not found", { status: 404 });
    }
    try {
      const result = await runCedarling(initFromArchiveBytes, archive);
      return Response.json(result);
    } catch (error) {
      console.error("Cedarling example failed", error);
      return Response.json(
        { error: "Cedarling example failed" },
        { status: 500 },
      );
    }
  },
};
