import { NextResponse } from 'next/server';

const IDP_BASE_URL = 'http://localhost:9090';
const CONFIG_PATHS = {
  'policy-store': '/config/policy-store',
  'test-config': '/config/test-config',
} as const;

type ConfigType = keyof typeof CONFIG_PATHS;

async function fetchConfig(type: ConfigType): Promise<unknown> {
  const response = await fetch(`${IDP_BASE_URL}${CONFIG_PATHS[type]}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch ${type}: HTTP ${response.status}`);
  }
  return response.json() as Promise<unknown>;
}

export async function GET(request: Request) {
  const configType = new URL(request.url).searchParams.get('type') ?? 'all';

  if (configType === 'policy-store' || configType === 'test-config') {
    return NextResponse.json(await fetchConfig(configType));
  }

  if (configType !== 'all') {
    return NextResponse.json(
      { error: 'type must be policy-store, test-config, or all' },
      { status: 400 },
    );
  }

  const [policyStore, testConfig] = await Promise.all([
    fetchConfig('policy-store'),
    fetchConfig('test-config'),
  ]);

  return NextResponse.json({ policyStore, testConfig });
}
