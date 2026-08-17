import type { Result } from "../errors/types.js";

/** Exact configured-issuer reference accepted by readiness observation. */
export type IssuerReference =
  | {
      readonly id: string;
      readonly iss?: never;
    }
  | {
      readonly iss: string;
      readonly id?: never;
    };

/**
 * Observes configured issuer readiness; `false` is a normal result for an
 * unknown, pending, or failed issuer.
 */
export interface CedarlingIssuers {
  isLoaded(
    issuer: IssuerReference,
  ): Promise<Result<boolean>>;
}
