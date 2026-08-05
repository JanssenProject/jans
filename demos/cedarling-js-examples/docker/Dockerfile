# syntax=docker/dockerfile:1.7

ARG NODE_IMAGE=node:22-bookworm-slim

FROM ${NODE_IMAGE} AS cedarling-toolchain

ARG WASM_PACK_VERSION=0.14.0

ENV CARGO_HOME=/opt/cargo \
    RUSTUP_HOME=/opt/rustup \
    PATH=/opt/cargo/bin:/opt/rustup/toolchains/stable-x86_64-unknown-linux-gnu/bin:${PATH}

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        build-essential \
        ca-certificates \
        clang \
        curl \
        libprotobuf-dev \
        pkg-config \
        protobuf-compiler \
    && rm -rf /var/lib/apt/lists/*

RUN curl --proto '=https' --tlsv1.2 --silent --show-error --fail \
        https://sh.rustup.rs \
        | sh -s -- -y --profile minimal --default-toolchain stable \
    && rustup target add wasm32-unknown-unknown

RUN curl --location --silent --show-error --fail \
        "https://github.com/rustwasm/wasm-pack/releases/download/v${WASM_PACK_VERSION}/wasm-pack-v${WASM_PACK_VERSION}-x86_64-unknown-linux-musl.tar.gz" \
        --output /tmp/wasm-pack.tar.gz \
    && mkdir /tmp/wasm-pack \
    && tar --extract --gzip --file /tmp/wasm-pack.tar.gz \
        --directory /tmp/wasm-pack --strip-components=1 \
    && install --mode 0755 /tmp/wasm-pack/wasm-pack /usr/local/bin/wasm-pack \
    && rm -rf /tmp/wasm-pack /tmp/wasm-pack.tar.gz \
    && wasm-pack --version

FROM cedarling-toolchain AS cedarling-packages

ARG CEDARLING_PACKAGE_VERSION=1.0.0

WORKDIR /workspace
COPY jans-cedarling/ ./jans-cedarling/

# A clean Docker build must never consume generated artifacts from the host.
RUN test ! -e jans-cedarling/target \
    && test ! -e jans-cedarling/bindings/cedarling_wasm/pkg \
    && test ! -e jans-cedarling/bindings/cedarling_js/dist \
    && test ! -e jans-cedarling/bindings/cedarling_js/node_modules

WORKDIR /workspace/jans-cedarling/bindings/cedarling_wasm
RUN --mount=type=cache,target=/opt/cargo/registry \
    --mount=type=cache,target=/opt/cargo/git \
    --mount=type=cache,target=/workspace/jans-cedarling/target \
    wasm-pack build --release --locked --target web --scope janssenproject

WORKDIR /workspace/jans-cedarling/bindings/cedarling_js
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund
RUN node scripts/stage-release.mjs \
    --pack-destination /artifacts \
    --version "${CEDARLING_PACKAGE_VERSION}"
RUN test "$(find /artifacts -maxdepth 1 -name '*.tgz' -type f | wc -l)" -eq 2

FROM ${NODE_IMAGE} AS example-source

WORKDIR /workspace
COPY demos/cedarling-js-examples/ ./demos/cedarling-js-examples/
COPY --from=cedarling-packages /artifacts/ /artifacts/

FROM ${NODE_IMAGE} AS idp-build

WORKDIR /workspace/common
COPY demos/cedarling-js-examples/common/package.json ./
COPY demos/cedarling-js-examples/common/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --omit=dev --no-audit --no-fund
COPY demos/cedarling-js-examples/common/ ./
RUN npm test

FROM ${NODE_IMAGE} AS idp

ENV NODE_ENV=production \
    PORT=9090 \
    OIDC_ISSUER=http://localhost:9090 \
    FRONTEND_ORIGIN=http://localhost:3000

WORKDIR /app
COPY --from=idp-build --chown=node:node /workspace/common/node_modules/ ./node_modules/
COPY --from=idp-build --chown=node:node /workspace/common/package.json ./
COPY --from=idp-build --chown=node:node /workspace/common/idp.js ./
COPY --from=idp-build --chown=node:node /workspace/common/policy-store.js ./
COPY --from=idp-build --chown=node:node /workspace/common/cedarling-config.json ./
COPY --from=idp-build --chown=node:node /workspace/common/schema.cedarschema ./
COPY --from=idp-build --chown=node:node /workspace/common/policies/ ./policies/
COPY --from=idp-build --chown=node:node /workspace/common/ui/ ./ui/
USER node
EXPOSE 9090
CMD ["node", "idp.js"]

FROM example-source AS react-backend-build

WORKDIR /workspace/demos/cedarling-js-examples
RUN --mount=type=cache,target=/root/.npm \
    node scripts/install-example.mjs react-nodejs/backend \
        --package-directory /artifacts
WORKDIR /workspace/demos/cedarling-js-examples/react-nodejs/backend
RUN npm test

FROM ${NODE_IMAGE} AS react-backend

ENV NODE_ENV=production \
    PORT=8080 \
    OIDC_ISSUER=http://localhost:9090 \
    FRONTEND_ORIGIN=http://localhost:3000

WORKDIR /app
COPY --from=react-backend-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/react-nodejs/backend/node_modules/ ./node_modules/
COPY --from=react-backend-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/react-nodejs/backend/package.json ./
COPY --from=react-backend-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/react-nodejs/backend/server.js ./
COPY --from=react-backend-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/react-nodejs/backend/cedarling/ ./cedarling/
USER node
EXPOSE 8080
CMD ["node", "server.js"]

FROM example-source AS react-frontend-build

ARG VITE_BACKEND_URL=http://localhost:8080
ARG VITE_OIDC_ISSUER=http://localhost:9090

ENV VITE_BACKEND_URL=${VITE_BACKEND_URL} \
    VITE_OIDC_ISSUER=${VITE_OIDC_ISSUER}

WORKDIR /workspace/demos/cedarling-js-examples
RUN --mount=type=cache,target=/root/.npm \
    node scripts/install-example.mjs react-nodejs/frontend \
        --package-directory /artifacts
WORKDIR /workspace/demos/cedarling-js-examples/react-nodejs/frontend
RUN npm run typecheck \
    && npm test \
    && npm run build

FROM nginx:1.29-alpine AS react-frontend

COPY demos/cedarling-js-examples/react-nodejs/frontend/nginx.conf \
    /etc/nginx/nginx.conf
COPY --from=react-frontend-build \
    /workspace/demos/cedarling-js-examples/react-nodejs/frontend/dist/ \
    /usr/share/nginx/html/
USER nginx
EXPOSE 3000
ENTRYPOINT []
CMD ["nginx", "-g", "daemon off;"]

FROM example-source AS nextjs-build

ENV NEXT_TELEMETRY_DISABLED=1

WORKDIR /workspace/demos/cedarling-js-examples
RUN --mount=type=cache,target=/root/.npm \
    node scripts/install-example.mjs vercel-nextjs \
        --package-directory /artifacts
WORKDIR /workspace/demos/cedarling-js-examples/vercel-nextjs
RUN npm run build

FROM example-source AS nextjs-runtime-deps

WORKDIR /workspace/demos/cedarling-js-examples/vercel-nextjs
RUN node -e \
    "const fs=require('node:fs');const path='package.json';const manifest=JSON.parse(fs.readFileSync(path,'utf8'));delete manifest.devDependencies;if(manifest.overrides)delete manifest.overrides.postcss;fs.writeFileSync(path,JSON.stringify(manifest,null,2)+'\\n')"
WORKDIR /workspace/demos/cedarling-js-examples
RUN --mount=type=cache,target=/root/.npm \
    node scripts/install-example.mjs vercel-nextjs \
        --package-directory /artifacts \
        --omit-dev

FROM ${NODE_IMAGE} AS nextjs

ENV NODE_ENV=production \
    HOSTNAME=0.0.0.0 \
    PORT=3000 \
    APP_ORIGIN=http://localhost:3000 \
    OIDC_ISSUER=http://localhost:9090 \
    NEXT_TELEMETRY_DISABLED=1

WORKDIR /app
COPY --from=nextjs-runtime-deps --chown=node:node \
    /workspace/demos/cedarling-js-examples/vercel-nextjs/package.json ./
COPY --from=nextjs-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/vercel-nextjs/next.config.ts ./
COPY --from=nextjs-runtime-deps --chown=node:node \
    /workspace/demos/cedarling-js-examples/vercel-nextjs/node_modules/ ./node_modules/
# Next.js loads next.config.ts at startup and requires its TypeScript parser in
# the final image even though the application itself has already been built.
COPY --from=nextjs-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/vercel-nextjs/node_modules/typescript/ \
    ./node_modules/typescript/
COPY --from=nextjs-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/vercel-nextjs/.next/ ./.next/
USER node
EXPOSE 3000
CMD ["node", "node_modules/next/dist/bin/next", "start"]

FROM example-source AS hono-build

WORKDIR /workspace/demos/cedarling-js-examples
RUN --mount=type=cache,target=/root/.npm \
    node scripts/install-example.mjs hono --package-directory /artifacts
WORKDIR /workspace/demos/cedarling-js-examples/hono
RUN npm test \
    && npm run typecheck:node

FROM example-source AS hono-runtime-deps

WORKDIR /workspace/demos/cedarling-js-examples/hono
RUN node -e \
    "const fs=require('node:fs');const path='package.json';const manifest=JSON.parse(fs.readFileSync(path,'utf8'));delete manifest.devDependencies;fs.writeFileSync(path,JSON.stringify(manifest,null,2)+'\\n')"
WORKDIR /workspace/demos/cedarling-js-examples
RUN --mount=type=cache,target=/root/.npm \
    node scripts/install-example.mjs hono \
        --package-directory /artifacts \
        --omit-dev

FROM oven/bun:1.3.13-debian AS hono-bun

ENV PORT=3001 \
    OIDC_ISSUER=http://localhost:9090 \
    FRONTEND_ORIGIN=http://localhost:3000

WORKDIR /app
COPY --from=hono-runtime-deps --chown=bun:bun \
    /workspace/demos/cedarling-js-examples/hono/package.json ./
COPY --from=hono-runtime-deps --chown=bun:bun \
    /workspace/demos/cedarling-js-examples/hono/node_modules/ ./node_modules/
COPY --from=hono-build --chown=bun:bun \
    /workspace/demos/cedarling-js-examples/hono/src/app.ts ./src/app.ts
COPY --from=hono-build --chown=bun:bun \
    /workspace/demos/cedarling-js-examples/hono/src/tasks.ts ./src/tasks.ts
COPY --from=hono-build --chown=bun:bun \
    /workspace/demos/cedarling-js-examples/hono/src/entry.bun.ts ./src/entry.bun.ts
COPY --from=hono-build --chown=bun:bun \
    /workspace/demos/cedarling-js-examples/hono/src/cedarling/ ./src/cedarling/
USER bun
EXPOSE 3001
CMD ["bun", "run", "src/entry.bun.ts"]

FROM denoland/deno:debian-2.9.4 AS hono-deno-check

ENV DENO_DIR=/tmp/deno-dir

WORKDIR /app
COPY --from=hono-build --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/ ./
USER deno
RUN deno check src/entry.deno.ts

FROM denoland/deno:debian-2.9.4 AS hono-deno

ENV PORT=3001 \
    OIDC_ISSUER=http://localhost:9090 \
    FRONTEND_ORIGIN=http://localhost:3000 \
    DENO_DIR=/tmp/deno-dir

WORKDIR /app
COPY --from=hono-runtime-deps --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/package.json ./
COPY --from=hono-build --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/tsconfig.json ./
COPY --from=hono-runtime-deps --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/node_modules/ ./node_modules/
COPY --from=hono-build --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/src/app.ts ./src/app.ts
COPY --from=hono-build --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/src/tasks.ts ./src/tasks.ts
COPY --from=hono-build --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/src/entry.deno.ts ./src/entry.deno.ts
COPY --from=hono-build --chown=deno:deno \
    /workspace/demos/cedarling-js-examples/hono/src/cedarling/ ./src/cedarling/
COPY --from=hono-deno-check --chown=deno:deno /tmp/deno-dir/ /tmp/deno-dir/
USER deno
EXPOSE 3001
CMD ["run", "--allow-net", "--allow-read", "--allow-env", "--allow-ffi", "src/entry.deno.ts"]

FROM ${NODE_IMAGE} AS hono-cloudflare

ENV HOME=/tmp \
    WRANGLER_LOG=info

WORKDIR /app
COPY --from=hono-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/hono/package.json ./
COPY --from=hono-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/hono/node_modules/ ./node_modules/
COPY --from=hono-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/hono/wrangler.toml ./
COPY --from=hono-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/hono/src/app.ts ./src/app.ts
COPY --from=hono-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/hono/src/tasks.ts ./src/tasks.ts
COPY --from=hono-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/hono/src/entry.cloudflare.ts \
    ./src/entry.cloudflare.ts
COPY --from=hono-build --chown=node:node \
    /workspace/demos/cedarling-js-examples/hono/src/cedarling/ ./src/cedarling/
RUN mkdir --parents /app/.wrangler \
    && chown node:node /app/.wrangler
USER node
EXPOSE 8787
CMD ["npm", "run", "dev:cf", "--", "--ip", "0.0.0.0", "--port", "8787"]

FROM hono-cloudflare AS hono-cloudflare-build

CMD ["npm", "run", "build:cf"]

FROM example-source AS electron-build

WORKDIR /workspace/demos/cedarling-js-examples
RUN --mount=type=cache,target=/root/.npm \
    node scripts/install-example.mjs electron --package-directory /artifacts
WORKDIR /workspace/demos/cedarling-js-examples/electron
CMD ["node", "scripts/validate-docker.mjs"]
