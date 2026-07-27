import { expect, test } from '@playwright/test';

test('DCR starts an authorization-code flow with S256 PKCE', async ({
  request,
}) => {
  const response = await request.get('/api/oidc/start?user=bob', {
    maxRedirects: 0,
  });

  expect(response.status()).toBe(307);
  const location = response.headers().location;
  expect(location).toBeTruthy();

  const authorizationUrl = new URL(location!);
  expect(authorizationUrl.origin).toBe('http://localhost:9090');
  expect(authorizationUrl.pathname).toBe('/auth');
  expect(authorizationUrl.searchParams.get('response_type')).toBe('code');
  expect(authorizationUrl.searchParams.get('code_challenge_method')).toBe('S256');
  expect(authorizationUrl.searchParams.get('code_challenge')).toMatch(
    /^[A-Za-z0-9_-]{43}$/,
  );
  expect(authorizationUrl.searchParams.get('redirect_uri')).toBe(
    'http://127.0.0.1:3100/api/oidc/callback',
  );
  expect(authorizationUrl.searchParams.get('scope')).toBe('openid profile role');
  expect(authorizationUrl.searchParams.get('resource')).toBe(
    'http://localhost:9090',
  );
  expect(authorizationUrl.searchParams.get('login_hint')).toBe('bob');
  expect(authorizationUrl.searchParams.get('prompt')).toBe('login');
  expect(authorizationUrl.searchParams.get('state')).not.toBe('bob');
  expect(authorizationUrl.searchParams.get('nonce')).toBeTruthy();

  const rejectedCallback = await request.get(
    '/api/oidc/callback?code=attacker-code&state=wrong-state',
    { maxRedirects: 0 },
  );
  expect(rejectedCallback.status()).toBe(303);
  const rejectedLocation = new URL(rejectedCallback.headers().location!);
  expect(rejectedLocation.origin).toBe('http://127.0.0.1:3100');
  expect(rejectedLocation.searchParams.get('oidc_error')).toBe(
    'authentication_failed',
  );
});

test('completes DCR login, uses the signed session, and logs out', async ({
  context,
  page,
}) => {
  await page.goto('/');
  await page.getByRole('radio', { name: 'Signed (Local OIDC IdP)' }).check();

  const authorizationRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return url.origin === 'http://localhost:9090' && url.pathname === '/auth';
  });
  await page.getByRole('button', { name: 'Login as Bob' }).click();
  const authorizeUrl = new URL((await authorizationRequest).url());
  expect(authorizeUrl.searchParams.get('response_type')).toBe('code');
  expect(authorizeUrl.searchParams.get('code_challenge_method')).toBe('S256');

  await expect(page).toHaveURL(/localhost:9090\/interaction\//);
  await page.locator('input[name="login"]').fill('bob');
  await page.locator('input[name="password"]').fill('password');
  await page.locator('button[type="submit"], input[type="submit"]').first().click();

  if (new URL(page.url()).pathname.startsWith('/interaction/')) {
    await page.locator('button[type="submit"], input[type="submit"]').first().click();
  }

  await expect(page).toHaveURL('http://127.0.0.1:3100/');
  await expect(page.getByText('Authenticated as Bob')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Logout Bob' })).toBeVisible();

  const cookies = await context.cookies('http://127.0.0.1:3100');
  const byName = new Map(cookies.map((cookie) => [cookie.name, cookie]));
  for (const name of [
    'taskapp_oidc_client_id',
    'taskapp_oidc_id_token',
    'taskapp_oidc_userinfo_token',
  ]) {
    expect(byName.get(name)?.httpOnly).toBe(true);
    expect(byName.get(name)?.sameSite).toBe('Lax');
  }
  expect(byName.has('taskapp_oidc_state')).toBe(false);
  expect(byName.has('taskapp_oidc_verifier')).toBe(false);
  expect(byName.has('taskapp_oidc_nonce')).toBe(false);

  const legacyAuthState = await page.evaluate(() => ({
    clientId: localStorage.getItem('registered_client_id'),
    token: localStorage.getItem('token_bob'),
  }));
  expect(legacyAuthState).toEqual({ clientId: null, token: null });

  const exerciseResponse = await page.evaluate(async () => {
    const response = await fetch('/api/cedarling/exercises', { method: 'POST' });
    return { status: response.status, body: await response.json() };
  });
  expect(exerciseResponse.status).toBe(200);
  expect(exerciseResponse.body).toMatchObject({
    authorizeUnsigned: true,
    authorizeMultiIssuer: true,
    context: {
      set: true,
      get: true,
      getEntry: true,
      entries: true,
      stats: true,
      delete: true,
      clear: true,
    },
    issuers: { byId: true, byIssuer: true },
    logs: {
      ids: true,
      find: true,
      findDecision: true,
      drain: true,
    },
    lifecycle: { close: true },
  });

  const title = `OIDC task ${Date.now()}`;
  await page.getByPlaceholder('Enter task title...').fill(title);
  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/tasks') &&
      response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Add Task' }).click();
  const createResponse = await createResponsePromise;
  expect(createResponse.status(), await createResponse.text()).toBe(201);
  const taskRow = page.getByRole('row').filter({ hasText: title });
  await expect(taskRow).toBeVisible();
  await expect(taskRow.getByRole('button', { name: 'Complete' })).toBeVisible();
  await taskRow.getByRole('button', { name: 'Delete' }).click();
  await expect(taskRow).toHaveCount(0);

  await page.getByRole('button', { name: 'Logout Bob' }).click();
  await expect(page).toHaveURL(/localhost:9090\/session\/end/);
  await page.getByRole('button', { name: 'Yes, sign me out' }).click();
  await expect(page).toHaveURL('http://127.0.0.1:3100/');
  await expect(page.getByText('No authenticated OIDC session')).toBeVisible();
});
