import { init } from "../out/cedarling.js"

let config = {
	applicationName: "test#docs",
	requireAudValidation: false,
	jwtValidation: false,
	policyStore: {
		strategy: "local",
		id: "fc2fee0253af46f3dce320484c42444ae0b24f7ec84a",
	},
	supportedSignatureAlgorithms: ["HS256", "HS384", "RS256"],
}

await init(config);
