#!/usr/bin/env node

import { spawn } from "node:child_process";
import { constants } from "node:fs";
import { access, mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

import {
  ELECTRON_IDP_ISSUER,
  composeArguments,
  nativeElectronEnvironment,
  signalExitCode,
  validatePackageArtifacts,
  waitForDiscovery,
} from "./docker-workflow.mjs";

const electronRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const examplesRoot = path.resolve(electronRoot, "..");
const composeFile = path.join(examplesRoot, "compose.yaml");
const installScript = path.join(examplesRoot, "scripts", "install-example.mjs");
const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";

function commandLabel(command, arguments_) {
  return [command, ...arguments_].join(" ");
}

async function runCommand(
  command,
  arguments_,
  {
    allowFailure = false,
    capture = false,
    cwd = examplesRoot,
    environment = process.env,
    forwardSignals = true,
  } = {},
) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, arguments_, {
      cwd,
      env: environment,
      stdio: capture ? ["inherit", "pipe", "inherit"] : "inherit",
    });
    let stdout = "";
    child.stdout?.setEncoding("utf8");
    child.stdout?.on("data", (chunk) => {
      stdout += chunk;
    });

    const signalHandlers = new Map();
    if (forwardSignals) {
      for (const signal of ["SIGINT", "SIGTERM"]) {
        const handler = () => child.kill(signal);
        signalHandlers.set(signal, handler);
        process.once(signal, handler);
      }
    }
    const removeSignalHandlers = () => {
      for (const [signal, handler] of signalHandlers) {
        process.removeListener(signal, handler);
      }
    };

    child.once("error", (error) => {
      removeSignalHandlers();
      reject(error);
    });
    child.once("close", (code, signal) => {
      removeSignalHandlers();
      const result = { code, signal, stdout: stdout.trim() };
      if (code === 0 || allowFailure) {
        resolve(result);
        return;
      }
      const error = new Error(
        `${commandLabel(command, arguments_)} failed (${signal ?? code})`,
      );
      error.exitCode = code ?? signalExitCode(signal);
      reject(error);
    });
  });
}

function compose(...arguments_) {
  return runCommand(
    "docker",
    composeArguments(examplesRoot, composeFile, ...arguments_),
  );
}

async function cleanupContainers() {
  await runCommand(
    "docker",
    composeArguments(
      examplesRoot,
      composeFile,
      "rm",
      "--force",
      "--stop",
      "idp-electron",
      "electron-packages",
    ),
    { allowFailure: true },
  );
}

async function electronBinaryInstalled() {
  try {
    const moduleRoot = path.join(electronRoot, "node_modules", "electron");
    const relativeBinary = (await readFile(path.join(moduleRoot, "path.txt"), "utf8")).trim();
    if (!relativeBinary) return false;
    await access(
      path.join(moduleRoot, "dist", relativeBinary),
      process.platform === "win32" ? constants.F_OK : constants.X_OK,
    );
    return true;
  } catch {
    return false;
  }
}

async function ensureElectronBinary() {
  if (await electronBinaryInstalled()) return;
  console.log("Electron binary is missing; rebuilding its native installation...");
  await runCommand(
    npmCommand,
    ["rebuild", "electron", "--foreground-scripts", "--no-audit", "--no-fund"],
    { cwd: electronRoot },
  );
  if (!(await electronBinaryInstalled())) {
    throw new Error("Electron did not install its native executable correctly.");
  }
}

async function main() {
  const artifactDirectory = await mkdtemp(
    path.join(tmpdir(), "cedarling-electron-packages-"),
  );
  let exitCode;
  let operationError;
  let cleanupError;
  try {
    console.log("Building Cedarling packages and the external Electron IdP...");
    await compose("build", "electron-packages", "idp-electron");

    await compose("create", "electron-packages");
    const container = await runCommand(
      "docker",
      composeArguments(
        examplesRoot,
        composeFile,
        "ps",
        "--all",
        "--quiet",
        "electron-packages",
      ),
      { capture: true },
    );
    if (!container.stdout) {
      throw new Error("Compose did not create the Cedarling package exporter.");
    }
    await runCommand(
      "docker",
      ["cp", `${container.stdout}:/artifacts/.`, artifactDirectory],
    );
    const artifacts = await validatePackageArtifacts(artifactDirectory);
    await compose("rm", "--force", "electron-packages");
    console.log(`Installing Cedarling ${artifacts.version} into Electron...`);
    await runCommand(
      process.execPath,
      [
        installScript,
        "electron",
        "--package-directory",
        artifactDirectory,
      ],
    );
    await ensureElectronBinary();

    console.log("Starting the external Electron IdP...");
    await compose("up", "--detach", "idp-electron");
    await waitForDiscovery();
    console.log(`OIDC discovery: ${ELECTRON_IDP_ISSUER}/.well-known/openid-configuration`);
    console.log("Building and launching the native Electron TaskApp...");
    const result = await runCommand(npmCommand, ["start"], {
      allowFailure: true,
      cwd: electronRoot,
      environment: nativeElectronEnvironment(),
    });
    exitCode = result.code ?? signalExitCode(result.signal);
  } catch (error) {
    operationError = error;
  } finally {
    console.log("Stopping the external Electron IdP and removing temporary artifacts...");
    try {
      await cleanupContainers();
    } catch (error) {
      cleanupError = error;
    }
    try {
      await rm(artifactDirectory, {
        force: true,
        recursive: true,
        maxRetries: 3,
        retryDelay: 50,
      });
    } catch (error) {
      cleanupError ??= error;
    }
    if (cleanupError) {
      console.error(
        `Cleanup also failed: ${
          cleanupError instanceof Error ? cleanupError.message : cleanupError
        }`,
      );
    }
  }
  if (operationError) throw operationError;
  if (cleanupError) throw cleanupError;
  return exitCode;
}

try {
  process.exitCode = await main();
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode =
    error && typeof error === "object" && "exitCode" in error
      ? Number(error.exitCode)
      : 1;
}
