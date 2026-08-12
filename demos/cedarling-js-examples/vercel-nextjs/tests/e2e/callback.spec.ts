import { expect, test } from '@playwright/test';

import {
  callbackFailureLog,
  validateCallbackParameters,
} from '../../libs/oidc/callback';

test('normalizes provider-controlled callback failures before logging', () => {
  const injected = 'access_denied\r\nforged log entry';
  const result = validateCallbackParameters(
    new URLSearchParams({ error: injected, state: 'expected-state' }),
    'expected-state',
  );

  expect(result).toEqual({ ok: false, reason: 'provider_error' });
  if (result.ok) throw new Error('Expected callback validation to fail');
  const logMessage = callbackFailureLog(result.reason);
  expect(logMessage).toBe('[oidc] Callback failed: provider_error');
  expect(logMessage).not.toContain(injected);
  expect(logMessage).not.toMatch(/[\r\n]/);
});

test('rejects duplicate and mixed security-sensitive callback parameters', () => {
  for (const query of [
    'code=one&code=two&state=expected-state',
    'code=one&state=expected-state&state=other',
    'error=access_denied&error=server_error&state=expected-state',
    'error=access_denied&code=one&state=expected-state',
  ]) {
    expect(
      validateCallbackParameters(new URLSearchParams(query), 'expected-state'),
    ).toEqual({ ok: false, reason: 'invalid_response' });
  }
});

test('requires one non-empty code and a matching transaction state', () => {
  expect(
    validateCallbackParameters(
      new URLSearchParams({ code: 'code', state: 'wrong-state' }),
      'expected-state',
    ),
  ).toEqual({ ok: false, reason: 'invalid_state' });
  expect(
    validateCallbackParameters(
      new URLSearchParams({ code: '', state: 'expected-state' }),
      'expected-state',
    ),
  ).toEqual({ ok: false, reason: 'invalid_response' });
  expect(
    validateCallbackParameters(
      new URLSearchParams({ code: 'code', state: 'expected-state' }),
      'expected-state',
    ),
  ).toEqual({ ok: true, code: 'code' });
});
