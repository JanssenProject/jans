CREATE TABLE jansClnt_Array (
  doc_id STRING(64) NOT NULL,
  objectClass STRING(48),
  dn STRING(128),
  jansRedirectURI ARRAY<STRING(MAX)>,
) PRIMARY KEY(doc_id)
