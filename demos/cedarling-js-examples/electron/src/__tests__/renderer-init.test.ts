const createCedarling = jest.fn(async () => ({
  ok: true,
  value: { shutDown: jest.fn(async () => ({ ok: true })) },
}));

jest.mock("@janssenproject/cedarling", () => ({ createCedarling }));
const loadCedarlingOptions = jest.fn();
jest.mock("../main/cedarling/config", () => ({ loadCedarlingOptions }));

import {
  getCedarling,
  shutDownCedarling,
} from "../main/cedarling/init";
import {
  getRendererCedarling,
  shutDownRendererCedarling,
} from "../renderer/src/cedarling/init";

test("renderer keeps the external issuer policy and disables unsupported WASM retries", async () => {
  const policyStoreDocument = {
    policy_stores: {
      TaskApp: {
        trusted_issuers: {
          ExternalIdP: {
            openid_configuration_endpoint:
              "https://idp.example/.well-known/openid-configuration",
          },
        },
      },
    },
  };
  Object.defineProperty(window, "electron", {
    configurable: true,
    value: {
      cedarling: {
        options: jest.fn(async () => ({
          applicationName: "TaskApp",
          policyStoreDocument,
        })),
      },
    },
  });

  await getRendererCedarling();

  expect(createCedarling).toHaveBeenCalledWith({
    applicationName: "TaskApp",
    http: { maxRetries: 0 },
    jwt: { allowedAlgorithms: ["RS256"] },
    policyStore: { type: "inline", document: policyStoreDocument },
  });
  await shutDownRendererCedarling();
});

test("main also disables the unsupported Electron WASM retry timer", async () => {
  const policyStoreDocument = { policy_stores: { TaskApp: {} } };
  loadCedarlingOptions.mockResolvedValue({
    applicationName: "TaskApp",
    policyStoreDocument,
  });

  await getCedarling();

  expect(createCedarling).toHaveBeenLastCalledWith({
    applicationName: "TaskApp",
    http: { maxRetries: 0 },
    jwt: { allowedAlgorithms: ["RS256"] },
    policyStore: { type: "inline", document: policyStoreDocument },
  });
  await shutDownCedarling();
});
