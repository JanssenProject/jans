import { InputValidationError } from "../errors/errors.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
} from "../values/inspect.js";
import type { IssuerReference } from "./types.js";

/** Validates and detaches an exact-one issuer reference. */
export function snapshotIssuerReference(
  value: IssuerReference,
): IssuerReference {
  if (!isPlainDataRecord(value, false)) {
    throw new InputValidationError("type", "Expected an issuer reference.");
  }
  for (const key of Object.keys(value)) {
    if (key !== "id" && key !== "iss") {
      throw new InputValidationError(
        "unknownField",
        "Unknown issuer reference field.",
        [key],
      );
    }
  }
  const idProperty = inspectOwnProperty(value, "id");
  const issProperty = inspectOwnProperty(value, "iss");
  if (
    idProperty.kind === "accessor" ||
    issProperty.kind === "accessor" ||
    idProperty.kind === "data" && !idProperty.enumerable ||
    issProperty.kind === "data" && !issProperty.enumerable
  ) {
    throw new InputValidationError(
      "type",
      "Expected enumerable data properties.",
    );
  }
  const id = idProperty.kind === "data" ? idProperty.value : undefined;
  const iss = issProperty.kind === "data" ? issProperty.value : undefined;
  if ((id === undefined) === (iss === undefined)) {
    throw new InputValidationError(
      "conflict",
      "Expected exactly one issuer reference.",
    );
  }
  const selected = id ?? iss;
  if (typeof selected !== "string" || selected.trim().length === 0) {
    throw new InputValidationError(
      typeof selected === "string" ? "required" : "type",
      "Expected a non-empty issuer reference.",
      [id === undefined ? "iss" : "id"],
    );
  }
  return id === undefined ? { iss: selected } : { id: selected };
}
