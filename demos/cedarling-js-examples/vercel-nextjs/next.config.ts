import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  outputFileTracingRoot: process.cwd(),
  serverExternalPackages: [
    '@janssenproject/cedarling',
    '@janssenproject/cedarling_wasm',
  ],
  webpack(config, { nextRuntime }) {
    if (nextRuntime === 'nodejs') {
      config.externals = [
        ...(config.externals ?? []),
        '@janssenproject/cedarling',
        '@janssenproject/cedarling_wasm',
      ];
    }
    return config;
  },
};

export default nextConfig;
