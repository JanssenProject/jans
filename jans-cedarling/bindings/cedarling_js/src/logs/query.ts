import {
  FIELD_BEHAVIORS,
  INPUT_FIELDS,
  LOG_TAG_SET,
} from "../helpers/constants.js";
import { createInputValidator } from "../helpers/validation.js";
import type {
  CedarlingLogTag,
  LogQuery,
} from "./types.js";

const {
  exactFields,
  field: readField,
  invalid,
  record,
  requiredString,
} = createInputValidator("invalid log query");

/** Validates and detaches the request-correlated retained-log query. */
export function snapshotLogQuery(
  value: LogQuery | undefined,
): LogQuery | undefined {
  if (value === undefined) {
    return undefined;
  }
  const query = record(value, []);
  exactFields(query, INPUT_FIELDS.logQuery, []);
  const id = readField(query, "id", [], FIELD_BEHAVIORS.strictEnumerableData);
  const requestId = readField(
    query,
    "requestId",
    [],
    FIELD_BEHAVIORS.strictEnumerableData,
  );
  const tag = readField(query, "tag", [], FIELD_BEHAVIORS.strictEnumerableData);
  const present = [id, requestId, tag].filter(
    (item) => item !== undefined,
  ).length;

  if (present === 0) {
    invalid("conflict");
  }
  if (id !== undefined) {
    if (present !== 1) {
      invalid("conflict");
    }
    return { id: requiredString(id, ["id"], { empty: "empty" }) };
  }
  if (
    tag !== undefined &&
    (typeof tag !== "string" || !LOG_TAG_SET.has(tag))
  ) {
    invalid("unsupported", ["tag"]);
  }

  return requestId === undefined
    ? { tag: tag as CedarlingLogTag }
    : {
        requestId: requiredString(requestId, ["requestId"], {
          empty: "empty",
        }),
        ...(tag === undefined ? {} : { tag: tag as CedarlingLogTag }),
      };
}
