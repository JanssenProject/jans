import { expect, test } from "@playwright/test";

test("DCR starts a code flow with S256 PKCE", async ({ request }) => {
  expect((await request.get("/api/tasks")).status()).toBe(401);
  const response = await request.get("/api/oidc/start?user=bob", { maxRedirects: 0 });
  expect(response.status()).toBe(307);
  const authorizationUrl = new URL(response.headers().location!);
  expect(authorizationUrl.origin).toBe("http://localhost:9090");
  expect(authorizationUrl.pathname).toBe("/auth");
  expect(authorizationUrl.searchParams.get("response_type")).toBe("code");
  expect(authorizationUrl.searchParams.get("code_challenge_method")).toBe("S256");
  expect(authorizationUrl.searchParams.get("code_challenge")).toMatch(/^[A-Za-z0-9_-]{43}$/);
  expect(authorizationUrl.searchParams.get("login_hint")).toBe("bob");
  expect(authorizationUrl.searchParams.get("state")).not.toBe("bob");

  const rejected = await request.get(
    "/api/oidc/callback?code=attacker-code&state=wrong-state",
    { maxRedirects: 0 },
  );
  expect(rejected.status()).toBe(303);
  expect(new URL(rejected.headers().location!).searchParams.get("oidc_error")).toBe("authentication_failed");
});

test("unsigned mode is explicit and ignores the obsolete authMode cookie", async ({ request }) => {
  const response = await request.get("/api/tasks", {
    headers: { Cookie: "authMode=signed-idp", "x-user-id": "bob" },
  });
  expect(response.status()).toBe(200);
  const unsafe = await request.get("/api/check-edge?action=Anything&taskId=task-1&token=secret", {
    headers: { "x-user-id": "bob" },
  });
  expect(unsafe.status()).toBe(400);
});

test("completes signed login, rejects tampering and forged ownership, then logs out", async ({ context, page }) => {
  await page.goto("/");
  const authorizationRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return url.origin === "http://localhost:9090" && url.pathname === "/auth";
  });
  await page.getByRole("button", { name: "Sign in with OIDC + PKCE" }).click();
  expect(new URL((await authorizationRequest).url()).searchParams.get("code_challenge_method")).toBe("S256");

  await expect(page).toHaveURL(/localhost:9090\/interaction\//);
  await page.locator("input[name=\"login\"]").fill("bob");
  await page.locator("input[name=\"password\"]").fill("password");
  await page.getByRole("button", { name: "Sign-in" }).click();
  await page.getByRole("button", { name: "Continue" }).click();

  await expect(page).toHaveURL("http://127.0.0.1:3100/");
  await expect(page.getByText("Signed OIDC session for Bob")).toBeVisible();
  const cookies = await context.cookies("http://127.0.0.1:3100");
  const byName = new Map(cookies.map((cookie) => [cookie.name, cookie]));
  for (const name of ["taskapp_oidc_client_id", "taskapp_oidc_id_token", "taskapp_oidc_userinfo_token"]) {
    expect(byName.get(name)?.httpOnly).toBe(true);
    expect(byName.get(name)?.sameSite).toBe("Lax");
  }
  expect(byName.has("authMode")).toBe(false);

  const forged = await page.evaluate(async () => {
    const response = await fetch("/api/tasks", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ title: "Forged", owner: "alice" }),
    });
    return response.status;
  });
  expect(forged).toBe(400);

  const userinfoCookie = byName.get("taskapp_oidc_userinfo_token")!;
  await context.addCookies([{ ...userinfoCookie, value: "tampered.token.value" }]);
  expect((await page.request.get("/api/tasks")).status()).toBe(401);
  await context.addCookies([userinfoCookie]);

  const title = `OIDC task ${Date.now()}`;
  await page.getByPlaceholder("Enter task title").fill(title);
  await page.getByRole("button", { name: "Add task" }).click();
  const taskRow = page.getByRole("row").filter({ hasText: title });
  await expect(taskRow).toBeVisible();
  await taskRow.getByRole("button", { name: "Delete" }).click();
  await expect(taskRow).toHaveCount(0);

  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page).toHaveURL(/localhost:9090\/session\/end/);
  await page.getByRole("button", { name: "Yes, sign me out" }).click();
  await expect(page).toHaveURL("http://127.0.0.1:3100/");
  await expect(page.getByText(/Using explicit unsigned identity/)).toBeVisible();
});
