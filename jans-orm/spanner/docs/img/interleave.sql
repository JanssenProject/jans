CREATE TABLE jansClnt_Interleave (
  doc_id STRING(64) NOT NULL,
  objectClass STRING(48),
  dn STRING(128),
) PRIMARY KEY(doc_id);

CREATE TABLE jansClnt_Interleave_jansRedirectURI (
  doc_id STRING(64) NOT NULL,
  dict_doc_id INT64 NOT NULL,
  jansRedirectURI STRING(MAX),
) PRIMARY KEY(doc_id, dict_doc_id),
  INTERLEAVE IN PARENT jansClnt_Interleave ON DELETE CASCADE;

CREATE INDEX jansRedirectURI_idx ON jansClnt_Interleave_jansRedirectURI(jansRedirectURI)
