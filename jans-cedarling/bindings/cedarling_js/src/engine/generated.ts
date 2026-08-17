import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { CedarlingEngine } from "./engine.js";
import { errorCode } from "../errors/types.js";
import {
  createSdkError,
  isSdkErrorCode,
} from "../errors/errors.js";
import {
  adaptGeneratedClient,
  adaptGeneratedResult,
  disposeUnadaptedGeneratedClient,
  type GeneratedClientBoundary,
  withGeneratedWrapper,
} from "./generated-wrapper.js";
import {
  parseGeneratedResult,
  toAuthorizationDecision,
  toGeneratedMultiIssuerRequest,
  toGeneratedRequest,
} from "./generated-authorization.js";

export { hasGeneratedModuleOutput } from "./generated-wrapper.js";

class GeneratedCedarlingEngine implements CedarlingEngine {
  readonly #generated: GeneratedClientBoundary;

  constructor(generated: GeneratedClientBoundary) {
    this.#generated = generated;
  }

  async #authorize(
    operation: "authorizeUnsigned" | "authorizeMultiIssuer",
    invoke: () => Promise<unknown>,
  ): Promise<AuthorizationDecision> {
    let generatedValue: unknown;
    try {
      generatedValue = await invoke();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : String(error);
      throw createSdkError(errorCode.authorizationFailed, operation, {
        details: { wasmMessage: message },
        rawCause: error,
      });
    }
    const result = adaptGeneratedResult(generatedValue, operation);
    if (result === undefined) {
      throw createSdkError(errorCode.generatedProtocolError, operation);
    }
    return withGeneratedWrapper(result, operation, () => {
      let serialized: unknown;
      try {
        serialized = result.jsonString();
      } catch (error: unknown) {
        if (isSdkErrorCode(error, [errorCode.generatedProtocolError])) {
          throw error;
        }
        throw createSdkError(errorCode.resultConversionFailed, operation, {
          rawCause: error,
        });
      }
      if (typeof serialized !== "string") {
        throw createSdkError(errorCode.generatedProtocolError, operation);
      }
      return toAuthorizationDecision(
        parseGeneratedResult(serialized, operation),
      );
    });
  }

  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<AuthorizationDecision> {
    return this.#authorize(
      "authorizeUnsigned",
      () => this.#generated.authorizeUnsigned(
        JSON.stringify(toGeneratedRequest(request)),
      ),
    );
  }

  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<AuthorizationDecision> {
    return this.#authorize(
      "authorizeMultiIssuer",
      () => this.#generated.authorizeMultiIssuer(
        JSON.stringify(toGeneratedMultiIssuerRequest(request)),
      ),
    );
  }

  async shutDown(): Promise<void> {
    const failures: unknown[] = [];
    try {
      await this.#generated.shutDown();
    } catch (error: unknown) {
      failures.push(error);
    }
    try {
      this.#generated.dispose();
    } catch (error: unknown) {
      failures.push(error);
    }
    if (failures.length > 0) {
      throw createSdkError(errorCode.lifecycleFailed, "shutDown", {
        rawCause: failures.length === 1 ? failures[0] : Object.freeze(failures),
      });
    }
  }
}

export function createGeneratedEngine(
  generatedValue: unknown,
): CedarlingEngine | undefined {
  const generated = adaptGeneratedClient(generatedValue);
  if (generated === undefined) {
    disposeUnadaptedGeneratedClient(generatedValue);
    return undefined;
  }
  return new GeneratedCedarlingEngine(generated);
}
