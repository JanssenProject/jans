import type { CEDAR_EXTENSION_FUNCTIONS } from "../helpers/constants.js";

/**
 * Readonly JSON-compatible value.
 *
 * Runtime validation additionally rejects non-finite and unsafe numbers,
 * cyclic structures, sparse arrays, accessors, symbols, and behavior-bearing
 * prototypes.
 */
export type JsonValue =
  | null
  | boolean
  | number
  | string
  | readonly JsonValue[]
  | { readonly [key: string]: JsonValue };

export type JsonObject = Readonly<Record<string, JsonValue>>;

/**
 * Recursive value accepted in Cedar entity attributes.
 *
 * Plain numbers must be safe integers. Fractional and specialized values use
 * an explicit {@link CedarExtensionValue}.
 */
export type CedarValue =
  | boolean
  | number
  | string
  | CedarExtensionValue
  | CedarEntityReference
  | readonly CedarValue[]
  | { readonly [key: string]: CedarValue };

export type CedarObject = Readonly<Record<string, CedarValue>>;

export type CedarExtensionFunction =
  (typeof CEDAR_EXTENSION_FUNCTIONS)[number];

/**
 * Explicit Cedar extension marker for request context.
 *
 * Plain request-context numbers must be safe integers. Use a `"decimal"`
 * marker when a fractional Cedar value is required. The argument must be a
 * non-empty canonical string.
 */
export interface CedarExtensionValue {
  readonly __extn: {
    readonly fn: CedarExtensionFunction;
    readonly arg: string;
  };
}

/**
 * Cedar entity reference marker embedded in context or entity attributes.
 *
 * Cedar JSON represents entity references with a `{ __entity: { type, id } }`
 * wrapper. The SDK validates the marker and passes it through to the core.
 */
export interface CedarEntityReference {
  readonly __entity: {
    readonly type: string;
    readonly id: string;
  };
}

/**
 * Cedar-compatible value stored for later injection below `context.data`.
 *
 * The SDK validates only generic Cedar representation. The application remains
 * responsible for defining the key and its exact type in the active policy
 * store schema.
 */
export type ContextDataValue = CedarValue;
