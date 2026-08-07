import {
  FIELD_BEHAVIORS,
  INPUT_FIELDS,
} from "../helpers/constants.js";
import { createInputValidator } from "../helpers/validation.js";
import type { IssuerReference } from "./types.js";

const {
  exactFields,
  field,
  invalid,
  record,
  requiredString,
} = createInputValidator("invalid issuer reference");

/** Validates and detaches an exact-one issuer reference. */
export function snapshotIssuerReference(
  value: IssuerReference,
): IssuerReference {
  const reference = record(value, []);
  exactFields(reference, INPUT_FIELDS.issuerReference, []);
  const id = field(reference, "id", [], FIELD_BEHAVIORS.strictEnumerableData);
  const iss = field(reference, "iss", [], FIELD_BEHAVIORS.strictEnumerableData);
  if ((id === undefined) === (iss === undefined)) {
    invalid("conflict");
  }
  const selected = id ?? iss;
  const snapshot = requiredString(selected, [id === undefined ? "iss" : "id"]);
  return id === undefined ? { iss: snapshot } : { id: snapshot };
}
