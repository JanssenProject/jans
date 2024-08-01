import { init } from "../out/cedarling.js"

let config = {
	applicationName: "test#docs",
	requireAudValidation: false,
	jwtValidation: false,
	policyStore: {
		strategy: "remote",
		url: "http://localhost:5000/policy-store/default.json",
	},
	supportedSignatureAlgorithms: ["HS256", "HS384", "RS256"],
}

await init(config);
