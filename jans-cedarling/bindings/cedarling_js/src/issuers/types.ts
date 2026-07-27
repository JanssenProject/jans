import type { CedarlingIssuerError, Result } from "../errors/types.js";

/**
 * Exact configured-issuer reference accepted by readiness observation.
 *
 * @example
 * ```ts
 * const issuer: IssuerReference = { id: "ExampleIssuer" };
 * ```
 */
export type IssuerReference =
  | {
      /** Configured trusted-issuer identifier. */
      readonly id: string;
      /** Excluded when the reference selects an ID. */
      readonly iss?: never;
    }
  | {
      /** Exact issuer claim value. */
      readonly iss: string;
      /** Excluded when the reference selects an issuer claim. */
      readonly id?: never;
    };

/**
 * Public trusted-issuer readiness service.
 *
 * @example
 * ```ts
 * const loaded = await client.issuers.isLoaded({ id: "ExampleIssuer" });
 * ```
 */
export interface CedarlingIssuers {
  /**
   * Observes whether one configured issuer is currently loaded.
   *
   * `false` is a normal observation for unknown, pending, or failed issuers.
   */
  isLoaded(
    issuer: IssuerReference,
  ): Promise<Result<boolean, CedarlingIssuerError>>;

}
