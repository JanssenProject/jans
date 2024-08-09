"use-strict";

import { init, authz } from "../out/cedarling.js"
import test from "node:test"
import process from "process"

test("Cedarling Tests", async t => {
	switch (process.argv[2]) {
		case "local":
			await t.test("Local cedarling initialization", async () => {
				await init({
					applicationName: "test#local",
					requireAudValidation: false,
					jwtValidation: false,
					policyStore: {
						strategy: "local",
						id: "fc2fee0253af46f3dce320484c42444ae0b24f7ec84a",
					},
					supportedSignatureAlgorithms: ["HS256", "HS384", "RS256"],
				})
			})
			break;
		case "remote":
			await t.test("Remote cedarling initialization", async () => {
				await init({
					applicationName: "test#remote",
					requireAudValidation: false,
					jwtValidation: false,
					policyStore: {
						strategy: "remote",
						url: "http://localhost:5000/policy-store/default.json",
					},
					supportedSignatureAlgorithms: ["HS256", "HS384", "RS256"],
				})
			})
			break;
		case "lock-master":
			await t.test("Lock-Master authenticated cedarling initialization", async () => {
				await init({
					applicationName: "test#lock-master",
					requireAudValidation: false,
					jwtValidation: false,
					policyStore: {
						strategy: "lock-master",
						url: "http://localhost:5000",
						policyStoreId: "fc2fee0253af46f3dce320484c42444ae0b24f7ec84a",
						enableDynamicConfiguration: false,
						ssaJwt: "eeb88261-6545-42c1-b1a8-fdb954d035dc"
					},
					supportedSignatureAlgorithms: ["HS256", "HS384", "RS256"],
				})
			})
			break;
		default:
			throw new Error(`Unknown startup strategy: ${process.argv[2]}`)
	}

	// Test authz
	await t.test("authz Tests", async t => {
		await t.test("Basic cedar decision", async () => {
			let input = {
				idToken: ""
			};

			let decision = await authz(input);
		})
	})
})
