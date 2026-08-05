import { spawn } from "node:child_process";

const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";
const commands = [
  ["run", "lint"],
  ["run", "typecheck"],
  ["test", "--", "--runInBand"],
  ["run", "test:renderer-bundle"],
  ["run", "test:renderer-dev-bundle"],
  ["run", "build"],
];

async function run(arguments_) {
  await new Promise((resolve, reject) => {
    const child = spawn(npmCommand, arguments_, { stdio: "inherit" });
    child.once("error", reject);
    child.once("close", (code, signal) => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(
        new Error(
          `${npmCommand} ${arguments_.join(" ")} failed (${signal ?? code})`,
        ),
      );
    });
  });
}

for (const command of commands) {
  await run(command);
}
