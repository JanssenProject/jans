const LOOPBACK_HOSTS = new Set(['localhost', '127.0.0.1', '[::1]']);

// Credentials and tokens travel to these URLs, so plain HTTP is only allowed on loopback.
export const isSecureOrLoopbackUrl = (url: unknown): boolean => {
  if (typeof url !== 'string' || !url) return false;
  try {
    const u = new URL(url);
    return u.protocol === 'https:' || (u.protocol === 'http:' && LOOPBACK_HOSTS.has(u.hostname));
  } catch {
    return false;
  }
};

export const assertSecureUrl = (url: unknown, label: string): void => {
  if (!isSecureOrLoopbackUrl(url)) {
    throw new Error(`${label} must use https:// (http:// is allowed only for localhost): ${String(url ?? '')}`);
  }
};

export const normalizeIssuer = (issuer: unknown): string => {
  const s = typeof issuer === 'string' ? issuer.trim() : '';
  let end = s.length;
  while (end > 0 && s[end - 1] === '/') end--;
  return s.slice(0, end);
};

export const assertSameIssuer = (actual: unknown, expected: unknown): void => {
  const a = normalizeIssuer(actual);
  if (!a || a !== normalizeIssuer(expected)) {
    throw new Error(`OpenID configuration issuer ${a} does not match expected issuer ${normalizeIssuer(expected)}`);
  }
};
