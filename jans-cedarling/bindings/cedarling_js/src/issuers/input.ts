import { INPUT_FIELDS } from "../helpers/constants.js";
import {
  createInputValidator,
  FIELD_BEHAVIORS,
} from "../helpers/validation.js";
import { errorCode } from "../errors/types.js";
import type { IssuerReference } from "./types.js";

/** Validates and detaches an exact-one issuer reference. */
export function snapshotIssuerReference(
  value: IssuerReference,
): IssuerReference {
  const operation = "issuers.isLoaded";
  const { exactFields, field, invalid, record, requiredString } =
    createInputValidator(operation);
  const reference = record(value, []);
  exactFields(reference, INPUT_FIELDS.issuerReference, []);
  const id = field(reference, "id", [], FIELD_BEHAVIORS.strictEnumerableData);
  const iss = field(reference, "iss", [], FIELD_BEHAVIORS.strictEnumerableData);
  if ((id === undefined) === (iss === undefined)) {
    invalid(errorCode.inputConflict);
  }
  const selected = id === undefined ? iss : id;
  const snapshot = requiredString(selected, [id === undefined ? "iss" : "id"]);
  return id === undefined ? { iss: snapshot } : { id: snapshot };
}
