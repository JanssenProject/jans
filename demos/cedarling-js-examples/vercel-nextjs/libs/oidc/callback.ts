export type CallbackFailureReason =
  | 'provider_error'
  | 'invalid_response'
  | 'invalid_state'
  | 'missing_transaction'
  | 'token_exchange_failed'
  | 'userinfo_failed'
  | 'unexpected_error';

type CallbackParameters =
  | { readonly ok: true; readonly code: string }
  | { readonly ok: false; readonly reason: CallbackFailureReason };

export function validateCallbackParameters(
  searchParams: URLSearchParams,
  expectedState: string,
): CallbackParameters {
  const errors = searchParams.getAll('error');
  const codes = searchParams.getAll('code');
  const states = searchParams.getAll('state');
  if (errors.length > 1 || codes.length > 1 || states.length !== 1) {
    return { ok: false, reason: 'invalid_response' };
  }

  const returnedState = states[0];
  if (!returnedState || returnedState !== expectedState) {
    return { ok: false, reason: 'invalid_state' };
  }

  if (errors.length === 1) {
    if (!errors[0] || codes.length !== 0) {
      return { ok: false, reason: 'invalid_response' };
    }
    return { ok: false, reason: 'provider_error' };
  }

  if (codes.length !== 1 || !codes[0]) {
    return { ok: false, reason: 'invalid_response' };
  }
  return { ok: true, code: codes[0] };
}

export function callbackFailureLog(reason: CallbackFailureReason): string {
  return `[oidc] Callback failed: ${reason}`;
}
