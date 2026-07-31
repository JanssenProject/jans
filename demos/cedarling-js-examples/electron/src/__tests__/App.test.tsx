import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";

import App from "../renderer/App";

jest.mock("@janssenproject/cedarling", () => ({
  createCedarling: jest.fn(async () => ({
    ok: true,
    value: {
      authorizeUnsigned: jest.fn(async () => ({
        ok: true,
        value: { decision: true, requestId: "test", diagnostics: { reasons: [], errors: [] } },
      })),
    },
  })),
}));

const task = { id: "task-1", title: "Buy groceries", completed: false, owner: "bob" as const };

beforeEach(() => {
  Object.defineProperty(window, "electron", {
    configurable: true,
    value: {
      cedarling: {
        options: jest.fn(async () => ({ applicationName: "TaskApp", policyStoreDocument: {} })),
        signedPermission: jest.fn(async () => false),
      },
      oidc: {
        login: jest.fn(),
        logout: jest.fn(),
        session: jest.fn(async () => ({ authenticated: false })),
      },
      tasks: {
        create: jest.fn(),
        delete: jest.fn(),
        list: jest.fn(async () => [task]),
        update: jest.fn(),
      },
    },
  });
});

test("renders the desktop shell and loads tasks through the typed bridge", async () => {
  render(<App />);
  expect(screen.getByText("Electron desktop")).toBeInTheDocument();
  expect(await screen.findByText(task.title)).toBeInTheDocument();
  expect(window.electron.tasks.list).toHaveBeenCalledWith({ userId: "bob" });
});
