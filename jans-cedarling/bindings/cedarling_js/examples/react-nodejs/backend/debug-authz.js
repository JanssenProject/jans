/**
 * Debug script — tests both unsigned and multi-issuer authorization paths,
 * logging full error details including diagnostics.
 */
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { createCedarling } from '@janssenproject/cedarling';

import { readFile } from 'node:fs/promises';

const webFetch = globalThis.fetch.bind(globalThis);
globalThis.fetch = async (input, init) => {
  let url;
  try {
    url = input instanceof URL ? input : new URL(typeof input === 'string' ? input : input.url);
  } catch {
    // Delegate non-URL inputs to the host implementation unchanged.
  }
  if (url?.protocol === 'file:') {
    return new Response(await readFile(url), {
      headers: { 'content-type': 'application/wasm' },
    });
  }
  return webFetch(input, init);
};

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const policyStorePath = path.resolve(__dirname, '../../common/policy-store.json');
const policyStore = JSON.parse(fs.readFileSync(policyStorePath, 'utf8'));

console.log('=== Policy Store Schema ===');
const schema = policyStore.policy_stores?.TaskApp?.schema;
if (schema) console.log('Schema body:\n', schema.body);

console.log('\n=== Policies ===');
const policies = policyStore.policy_stores?.TaskApp?.policies;
if (policies) {
  for (const [name, p] of Object.entries(policies)) {
    console.log(`${name}: ${p.policy_content.body}`);
  }
}

const cedarlingResult = await createCedarling({
  applicationName: 'TaskApp',
  policyStore: {
    type: 'inline',
    document: policyStore,
  },
  jwt: {
    dangerouslyDisableSignatureValidation: true,
    dangerouslyDisableStatusValidation: true,
  },
  logging: {
    type: 'console',
    level: 'trace',
  },
});

if (!cedarlingResult.ok) {
  console.error('Failed to initialize:', cedarlingResult.error);
  process.exit(1);
}

const cedarling = cedarlingResult.value;
console.log('\n✅ Cedarling initialized');

function truncateToken(token) {
  if (!token || typeof token !== 'string') return '(none)';
  if (token.length <= 20) return token;
  return token.substring(0, 12) + '...' + token.substring(token.length - 8);
}

function logResult(label, res) {
  console.log(`Result: ${JSON.stringify(res, null, 2)}`);
  if (res.ok) {
    const { decision, requestId, diagnostics } = res.value;
    console.log(`  → decision=${decision ? 'ALLOW' : 'DENY'} requestId=${requestId}`);
    if (!decision && diagnostics) {
      console.log(`  → reasons: ${JSON.stringify(diagnostics.reasons)}`);
      if (diagnostics.errors && diagnostics.errors.length > 0) {
        for (const e of diagnostics.errors) {
          console.log(`  → policyError: id="${e.policyId}" msg="${e.message}"`);
        }
      }
    }
  } else {
    console.log(`  → ERROR: code=${res.error.code} msg=${res.error.message}`);
  }
}

// Test 1: ViewTask (unsigned, no specific resource)
console.log('\n=== Test 1: ViewTask (unsigned, generic) ===');
const req1 = {
  principal: { type: 'TaskApp::User', id: 'bob' },
  action: 'TaskApp::Action::"ViewTask"',
  resource: {
    type: 'TaskApp::Task',
    id: 'new-task',
    attributes: { owner: 'bob', title: 'untitled', completed: false },
  },
  context: {},
};
console.log('Request:', JSON.stringify(req1, null, 2));
logResult('Test 1', await cedarling.authorizeUnsigned(req1));

// Test 2: ViewTask with specific resource
console.log('\n=== Test 2: ViewTask (unsigned, task-1) ===');
const req2 = {
  principal: { type: 'TaskApp::User', id: 'bob' },
  action: 'TaskApp::Action::"ViewTask"',
  resource: {
    type: 'TaskApp::Task',
    id: 'task-1',
    attributes: { owner: 'bob', title: 'Buy groceries', completed: false },
  },
  context: {},
};
console.log('Request:', JSON.stringify(req2, null, 2));
logResult('Test 2', await cedarling.authorizeUnsigned(req2));

// Test 3: UpdateTask (unsigned, bob owns task-1 → should ALLOW)
console.log('\n=== Test 3: UpdateTask (unsigned, bob owns task-1) ===');
const req3 = {
  principal: { type: 'TaskApp::User', id: 'bob' },
  action: 'TaskApp::Action::"UpdateTask"',
  resource: {
    type: 'TaskApp::Task',
    id: 'task-1',
    attributes: { owner: 'bob', title: 'Buy groceries', completed: false },
  },
  context: { userId: 'bob' },
};
console.log('Request:', JSON.stringify(req3, null, 2));
logResult('Test 3', await cedarling.authorizeUnsigned(req3));

// Test 4: DeleteTask (unsigned, charlie does NOT own task-1 → should DENY)
console.log('\n=== Test 4: DeleteTask (unsigned, charlie != owner) ===');
const req4 = {
  principal: { type: 'TaskApp::User', id: 'charlie' },
  action: 'TaskApp::Action::"DeleteTask"',
  resource: {
    type: 'TaskApp::Task',
    id: 'task-1',
    attributes: { owner: 'bob', title: 'Buy groceries', completed: false },
  },
  context: { userId: 'charlie' },
};
console.log('Request:', JSON.stringify(req4, null, 2));
logResult('Test 4', await cedarling.authorizeUnsigned(req4));

// Test 5: Multi-issuer with a mock JWT (no signature validation)
console.log('\n=== Test 5: Multi-issuer with mock JWT ===');
const mockJwt = [
  'eyJhbGciOiJSUzI1NiIsImtpZCI6ImRldi1zaWduaW5nLWtleSIsInR5cCI6IkpXVCJ9',
  'eyJzdWIiOiJib2IiLCJyb2xlIjpbIlVzZXIiXSwianRpIjoiZGVidWctdG9rZW4tMTIzIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo5MDkwIiwiZXhwIjo0MTAyNDQ0ODAwLCJpYXQiOjE3MjA5MDAwMDB9',
  'fake_signature_for_debug',
].join('.');
console.log(`Token prefix: ${truncateToken(mockJwt)}`);

const req5 = {
  tokens: [
    {
      mapping: 'LocalMockIdP::Userinfo_token',
      payload: mockJwt,
    },
  ],
  action: 'TaskApp::Action::"ViewTask"',
  resource: {
    type: 'TaskApp::Task',
    id: 'task-1',
    attributes: { owner: 'bob', title: 'Buy groceries', completed: false },
  },
  context: {},
};
console.log('Request:', JSON.stringify(req5, null, 2));
logResult('Test 5', await cedarling.authorizeMultiIssuer(req5));

// Test 6: Multi-issuer UpdateTask (owner match via token tags)
console.log('\n=== Test 6: Multi-issuer UpdateTask (bob=owner via token sub) ===');
const req6 = {
  tokens: [
    {
      mapping: 'LocalMockIdP::Userinfo_token',
      payload: mockJwt,
    },
  ],
  action: 'TaskApp::Action::"UpdateTask"',
  resource: {
    type: 'TaskApp::Task',
    id: 'task-1',
    attributes: { owner: 'bob', title: 'Buy groceries', completed: false },
  },
  context: {},
};
console.log('Request:', JSON.stringify(req6, null, 2));
logResult('Test 6', await cedarling.authorizeMultiIssuer(req6));

// Test 7: Multi-issuer DeleteTask (charlie != owner via token sub)
console.log('\n=== Test 7: Multi-issuer DeleteTask (alice != bob owner) ===');
const aliceJwt = [
  'eyJhbGciOiJSUzI1NiIsImtpZCI6ImRldi1zaWduaW5nLWtleSIsInR5cCI6IkpXVCJ9',
  'eyJzdWIiOiJhbGljZSIsInJvbGUiOlsiQWRtaW4iXSwianRpIjoiZGVidWctdG9rZW4tNDU2IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo5MDkwIiwiZXhwIjo0MTAyNDQ0ODAwLCJpYXQiOjE3MjA5MDAwMDB9',
  'fake_signature_for_debug',
].join('.');
const req7 = {
  tokens: [
    {
      mapping: 'LocalMockIdP::Userinfo_token',
      payload: aliceJwt,
    },
  ],
  action: 'TaskApp::Action::"DeleteTask"',
  resource: {
    type: 'TaskApp::Task',
    id: 'task-1',
    attributes: { owner: 'bob', title: 'Buy groceries', completed: false },
  },
  context: {},
};
console.log('Request:', JSON.stringify(req7, null, 2));
logResult('Test 7', await cedarling.authorizeMultiIssuer(req7));

await cedarling.close();
console.log('\nDone.');
