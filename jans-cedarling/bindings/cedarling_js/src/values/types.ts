import type { CEDAR_EXTENSION_FUNCTIONS } from "../helpers/constants.js";

/**
 * Primitive values accepted by JSON-shaped SDK inputs.
 *
 * @example
 * ```ts
 * const primitive: JsonPrimitive = null;
 * ```
 */
export type JsonPrimitive = null | boolean | number | string;

/**
 * Readonly JSON-compatible value.
 *
 * Runtime validation additionally rejects non-finite and unsafe numbers,
 * cyclic structures, sparse arrays, accessors, symbols, and behavior-bearing
 * prototypes.
 *
 * @example
 * ```ts
 * const value: JsonValue = { enabled: true, limits: [1, 2, null] };
 * ```
 */
export type JsonValue =
  | JsonPrimitive
  | readonly JsonValue[]
  | { readonly [key: string]: JsonValue };

/**
 * Readonly JSON object used for policy stores and safe error details.
 *
 * @example
 * ```ts
 * const object: JsonObject = { source: "inline", revision: 2 };
 * ```
 */
export type JsonObject = Readonly<Record<string, JsonValue>>;

/**
 * Detached inline policy-store document passed during initialization.
 *
 * @example
 * ```ts
 * const policyStore: PolicyStoreDocument = {
 *   policy_stores: {
 *     example: { policies: {} },
 *   },
 * };
 * ```
 */
export type PolicyStoreDocument = JsonObject;

/**
 * Primitive Cedar entity-attribute value.
 *
 * Unlike JSON, Cedar entity attributes do not accept `null`.
 *
 * @example
 * ```ts
 * const value: CedarPrimitive = "engineering";
 * ```
 */
export type CedarPrimitive = boolean | number | string;

/**
 * Recursive value accepted in Cedar entity attributes.
 *
 * Plain numbers must be safe integers. Fractional and specialized values use
 * an explicit {@link CedarExtensionValue}.
 *
 * @example
 * ```ts
 * const attributes: CedarValue = {
 *   department: "engineering",
 *   score: { __extn: { fn: "decimal", arg: "1.2346" } },
 *   owner: { __entity: { type: "Jans::User", id: "alice" } },
 *   roles: ["reader", "editor"],
 * };
 * ```
 */
export type CedarValue =
  | CedarPrimitive
  | CedarExtensionValue
  | CedarEntityReference
  | readonly CedarValue[]
  | { readonly [key: string]: CedarValue };

/**
 * Readonly object containing Cedar entity attributes.
 *
 * @example
 * ```ts
 * const attributes: CedarObject = { active: true, clearance: 4 };
 * ```
 */
export type CedarObject = Readonly<Record<string, CedarValue>>;

/**
 * Cedar extension functions supported in authorization context markers.
 *
 * @example
 * ```ts
 * const fn: CedarExtensionFunction = "ip";
 * ```
 */
export type CedarExtensionFunction =
  (typeof CEDAR_EXTENSION_FUNCTIONS)[number];

/**
 * Explicit Cedar extension marker for request context.
 *
 * Plain request-context numbers must be safe integers. Use a `"decimal"`
 * marker when a fractional Cedar value is required.
 *
 * @example
 * ```ts
 * const sourceIp: CedarExtensionValue = {
 *   __extn: { fn: "ip", arg: "192.0.2.1" },
 * };
 * ```
 */
export interface CedarExtensionValue {
  /** Exact marker consumed by the Cedarling canonical-JSON boundary. */
  readonly __extn: {
    /** Supported Cedar extension function. */
    readonly fn: CedarExtensionFunction;

    /** Non-empty canonical string argument for the extension function. */
    readonly arg: string;
  };
}

/**
 * Cedar entity reference marker embedded in context or entity attributes.
 *
 * Cedar JSON represents entity references with a `{ __entity: { type, id } }`
 * wrapper. The SDK validates the marker and passes it through to the core.
 *
 * @example
 * ```ts
 * const owner: CedarEntityReference = {
 *   __entity: { type: "Jans::User", id: "alice" },
 * };
 * ```
 */
export interface CedarEntityReference {
  /** Exact marker consumed by the Cedarling canonical-JSON boundary. */
  readonly __entity: {
    /** Cedar entity type, including namespace. */
    readonly type: string;

    /** Cedar entity identifier. */
    readonly id: string;
  };
}

/**
 * Recursive value accepted in an unsigned authorization request context.
 *
 * Plain numbers must be safe integers. Fractional, IP, date-time, and duration
 * values use {@link CedarExtensionValue}; `null` is not accepted.
 *
 * @example
 * ```ts
 * const context: CedarContextValue = {
 *   attempt: 1,
 *   sourceIp: { __extn: { fn: "ip", arg: "192.0.2.1" } },
 * };
 * ```
 */
export type CedarContextValue = CedarValue;

/**
 * Readonly authorization-request context object.
 *
 * @example
 * ```ts
 * const context: CedarContextObject = {
 *   authenticated: true,
 *   requestTime: {
 *     __extn: { fn: "datetime", arg: "2026-07-23T12:00:00Z" },
 *   },
 * };
 * ```
 */
export type CedarContextObject = CedarObject;

/**
 * Cedar-compatible value stored for later injection below `context.data`.
 *
 * The SDK validates only generic Cedar representation. The application remains
 * responsible for defining the key and its exact type in the active policy
 * store schema.
 *
 * @example
 * ```ts
 * const data: ContextDataValue = {
 *   enabled: true,
 *   score: { __extn: { fn: "decimal", arg: "1.5" } },
 * };
 * ```
 */
export type ContextDataValue = CedarContextValue;
