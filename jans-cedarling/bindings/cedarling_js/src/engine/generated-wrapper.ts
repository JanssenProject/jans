import { errorCode, type CedarlingOperation } from "../errors/types.js";
import { createSdkError } from "../errors/errors.js";

export interface GeneratedClientBoundary {
  authorizeUnsigned(request: string): Promise<unknown>;
  authorizeMultiIssuer(request: string): Promise<unknown>;
  shutDown(): Promise<unknown>;
  dispose(): void;
}

export interface GeneratedResultBoundary {
  jsonString(): unknown;
  dispose(): void;
}

function isObjectLike(value: unknown): value is object {
  return (
    (typeof value === "object" && value !== null) ||
    typeof value === "function"
  );
}

export function hasGeneratedModuleOutput(value: unknown): boolean {
  if (!isObjectLike(value)) return false;
  try {
    return Reflect.has(value, "memory");
  } catch {
    return false;
  }
}

function readMethod(
  value: object,
  name: PropertyKey,
): ((...arguments_: unknown[]) => unknown) | undefined {
  try {
    const method = Reflect.get(value, name) as unknown;
    return typeof method === "function"
      ? method as (...arguments_: unknown[]) => unknown
      : undefined;
  } catch {
    return undefined;
  }
}

export function withGeneratedWrapper<T>(
  wrapper: { dispose(): void },
  operation: CedarlingOperation,
  convert: () => T,
): T {
  try {
    return convert();
  } finally {
    try {
      wrapper.dispose();
    } catch (error: unknown) {
      throw createSdkError(errorCode.generatedProtocolError, operation, {
        rawCause: error,
      });
    }
  }
}

export function adaptGeneratedClient(
  value: unknown,
): GeneratedClientBoundary | undefined {
  if (!isObjectLike(value)) return undefined;
  const authorizeUnsigned = readMethod(value, "authorize_unsigned");
  const authorizeMultiIssuer = readMethod(value, "authorize_multi_issuer");
  const shutDown = readMethod(value, "shut_down");
  const dispose = readMethod(value, "free");
  if (
    authorizeUnsigned === undefined ||
    authorizeMultiIssuer === undefined ||
    shutDown === undefined ||
    dispose === undefined
  ) {
    return undefined;
  }
  return {
    async authorizeUnsigned(request: string): Promise<unknown> {
      return authorizeUnsigned.call(value, request);
    },
    async authorizeMultiIssuer(request: string): Promise<unknown> {
      return authorizeMultiIssuer.call(value, request);
    },
    async shutDown(): Promise<unknown> {
      return shutDown.call(value);
    },
    dispose(): void {
      dispose.call(value);
    },
  };
}

export function disposeUnadaptedGeneratedClient(value: unknown): void {
  if (!isObjectLike(value)) return;
  const dispose = readMethod(value, "free");
  if (dispose === undefined) return;
  try {
    dispose.call(value);
  } catch {
    // The incompatible generated protocol remains the reported failure.
  }
}

export function adaptGeneratedResult(
  value: unknown,
  operation: "authorizeUnsigned" | "authorizeMultiIssuer",
): GeneratedResultBoundary | undefined {
  if (!isObjectLike(value)) return undefined;
  const jsonString = readMethod(value, "json_string");
  const dispose = readMethod(value, "free");
  if (dispose === undefined) return undefined;
  return {
    jsonString(): unknown {
      if (jsonString === undefined) {
        throw createSdkError(errorCode.generatedProtocolError, operation);
      }
      return jsonString.call(value);
    },
    dispose(): void {
      dispose.call(value);
    },
  };
}
