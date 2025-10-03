@AcrsRead
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/acrs"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/acrs.readonly","https://jans.io/oauth/config/acrs.write","https://jans.io/oauth/config/read-all"])
};

@AcrsUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/acrs"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/acrs.write","https://jans.io/oauth/config/write-all"])
};

@AttributeGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/attributes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/attributes.readonly","https://jans.io/oauth/config/attributes.write", "https://jans.io/oauth/config/read-all"])
};

@AttributeUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/attributes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/attributes.write","https://jans.io/oauth/config/write-all"])
};


@AttributeDELETE
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/attributes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/attributes.delete","https://jans.io/oauth/config/delete-all"])
};


@CacheGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/read-all"])
};


@CachePatch
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/write-all"])
};


@CacheRedisGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly", "https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/read-all"])
};


@CacheRedisUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/write-all"])
};


@CacheInMemoryGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/in-memory"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/read-all"])
};

@CacheInMemoryUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/in-memory"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/write-all"])
};


@CacheNativePersistenceGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/native-persistence"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/read-all"])
};


@CacheNativePersistenceUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/native-persistence"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/write-all"])
};


@MemcachedPersistenceGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/memcached"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/read-all"])
};


@MemcachedPersistenceUpdate
permit(
  principal is Jans::Workload,
action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/memcached"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/write-all"])
};


@ClientsGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/openid/clients"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/openid/clients.readonly","https://jans.io/oauth/config/openid-read","https://jans.io/oauth/config/read-all"])
};


@ClientsUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/openid/clients"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/openid/clients.write","https://jans.io/oauth/config/openid-write","https://jans.io/oauth/config/write-all"])
};

@ClientsDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/openid/clients"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/openid/clients.delete","https://jans.io/oauth/config/openid-delete","https://jans.io/oauth/config/delete-all"])
};


@ConfigGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/properties.readonly","https://jans.io/oauth/jans-auth-server/config/properties.write","https://jans.io/oauth/config/read-all"])
};


@ConfigPatch
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/properties.write","https://jans.io/oauth/config/write-all"])
};


@ConfigPersistenceGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/config/persistence"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/properties.readonly","https://jans.io/oauth/jans-auth-server/config/properties.write","https://jans.io/oauth/config/read-all"])
};


@ConfigSmtpGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.readonly","https://jans.io/oauth/config/smtp.write","https://jans.io/oauth/config/read-all"])
};


@ConfigSmtpUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.write","https://jans.io/oauth/config/write-all"])
};

@ConfigSmtpDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.delete","https://jans.io/oauth/config/delete-all"])
};


@ConfigSmtpTest
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp/test"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.readonly","https://jans.io/oauth/config/smtp.write", "https://jans.io/oauth/config/read-all"])
};


@ScriptsGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.readonly","https://jans.io/oauth/config/scripts.write","https://jans.io/oauth/config/read-all"])
};


@ScriptsUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.write","https://jans.io/oauth/config/write-all"])
};


@ScriptTypeGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/type/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.readonly","https://jans.io/oauth/config/scripts.write", "https://jans.io/oauth/config/read-all"])
};


@ScriptInumGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/inum/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.readonly","https://jans.io/oauth/config/scripts.write", "https://jans.io/oauth/config/read-all"])
};


@ScriptDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.delete", "https://jans.io/oauth/config/delete-all"])
};


@ScriptPatch
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.write", "https://jans.io/oauth/config/write-all"])
};



@Fido2Get
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/fido2-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.readonly","https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/read-all"])
};


@Fido2Put
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/fido2-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/write-all"])
};



@Fido2RegistrationGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/registration"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.readonly","https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/read-all"])
};


@Fido2RegistrationUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/registration"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/write-all"])
};


@Fido2RegistrationDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/registration"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.delete","https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/write-all"])
};

@JwksGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/jwks"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jwks.readonly","https://jans.io/oauth/config/jwks.write","https://jans.io/oauth/config/read-all"])
};

@JwksUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/jwks"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jwks.write","https://jans.io/oauth/config/write-all"])
};

@JwksDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/jwks"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jwks.delete","https://jans.io/oauth/config/delete-all"])
};


@LdapGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.readonly","https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/read-all"])
};


@LdapUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/write-all"])
};


@LdapGetByName
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.readonly","https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/read-all"])
};


@LdapUpdateByName
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/write-all"])
};


@LdapDeleteByName
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.delete","https://jans.io/oauth/config/delete-all"])
};


@LdapTest
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/test"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.readonly","https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/read-all"])
};


@LoggingGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/logging"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/logging.readonly","https://jans.io/oauth/config/logging.write","https://jans.io/oauth/config/read-all"])
};


@LoggingPut
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/logging"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/logging.write","https://jans.io/oauth/config/write-all"])
};


@ScopeGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.readonly","https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/read-all"])
};


@ScopeUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/write-all"])
};

@ScopeByInumGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.readonly", "https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/read-all"])
};


@ScopeByInumUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/write-all"])
};


@ScopeByInumDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.delete","https://jans.io/oauth/config/delete-all"])
};


@UMAresourcesGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.readonly","https://jans.io/oauth/config/uma/resources.write", "https://jans.io/oauth/config/uma-read", "https://jans.io/oauth/config/read-all"])
};


@UMAresourcesUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.write","https://jans.io/oauth/config/uma-write", "https://jans.io/oauth/config/write-all"])
};


@UMAresourcesByIdGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.readonly","https://jans.io/oauth/config/uma/resources.write", "https://jans.io/oauth/config/uma-read", "https://jans.io/oauth/config/read-all"])
};

@UMAresourcesPatchById
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.write","https://jans.io/oauth/config/uma-write", "https://jans.io/oauth/config/write-all"])
};



@UMAresourcesDeleteById
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.delete","https://jans.io/oauth/config/uma-delete", "https://jans.io/oauth/config/delete-all"])
};


@StatGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/stat"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/stats.readonly", "https://jans.io/oauth/config/read-all"])
};

@healthGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health"
);


@HealthLiveGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/live"
);


@HealthReadyGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/ready"
);

@ScimGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/scim/scim-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/scim/config.readonly", "https://jans.io/scim/config.write", "https://jans.io/oauth/config/read-all"])
};


@ScimPatch
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/scim/scim-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/scim/config.write", "https://jans.io/oauth/config/write-all"])
};


@OrgGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/organization.readonly", "https://jans.io/oauth/config/organization.write", "https://jans.io/oauth/config/read-all"])
};


@OrgPatch
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/organization.write", "https://jans.io/oauth/config/write-all"])
};


@ConfigUserGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/mgt/configuser"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/user.readonly", "https://jans.io/oauth/config/user.write", "https://jans.io/oauth/config/read-all"])
};


@ConfigUserUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/user.write", "https://jans.io/oauth/config/write-all"])
};


@ConfigUserDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/user.delete", "https://jans.io/oauth/config/delete-all"])
};


@AgamaGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.readonly", "https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/read-all"])
};


@AgamaUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/write-all"])
};


@AgamaDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.delete", "https://jans.io/oauth/config/delete-all"])
};


@AgamaDeploymentGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/ads-deployment"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.readonly", "https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/read-all"])
};


@AgamaDeploymentPost
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/ads-deployment"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/write-all"])
};


@AgamaDeploymentDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/ads-deployment"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.delete", "https://jans.io/oauth/config/delete-all"])
};


@SessionGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/session"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/session.readonly", "https://jans.io/oauth/config/read-all"])
};


@SessionDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/session"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/session.delete", "https://jans.io/oauth/config/delete-all"])
};


@AdminUIRolesGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
 };


@AdminUIRolesUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
 };

@AdminUIRolesGetByRole
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
 };


@AdminUIRolesDeleteByRole
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.delete", "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all"])
 };


@AdminUIPermissionsGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
 };


@AdminUIPermissionsUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
 };

 
@AdminUIPermissionsGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};

@AdminUIPermissionsDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.delete", "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all"])
};


@AdminUIRolePermissionsMappingGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@AdminUIRolePermissionsMappingUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
};


@AdminUIRolePermissionsMappingGetByAdminUIRole
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@AdminUIRolePermissionsMappingDeleteByAdminUIRole
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.delete", "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all"])
};


@AdminUILicenseDetailsGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/licenseDetails"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@AdminUIActivateLicensePost
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/activateLicense"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
};


@AdminUILicenseIsActiveGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/isActive"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@AdminUILicenseApiCredentialsPost
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/saveApiCredentials"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
};

@PluginGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/plugin"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/plugin.readonly", "https://jans.io/oauth/config/read-all"])
};


@ConfigApiGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/properties.readonly", "https://jans.io/oauth/config/properties.write", "https://jans.io/oauth/config/read-all"])
};


@ConfigApiPatch
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/properties.write", "https://jans.io/oauth/config/write-all"])
};


@AuthorizationsGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/clients/authorizations"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/client/authorizations.readonly", "https://jans.io/oauth/config/read-all"])
};



@AuthorizationsDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/clients/authorizations"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/client/authorizations.delete", "https://jans.io/oauth/config/delete-all"])
};


@LinkConfigGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/jans-link/link-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans-link.readonly", "https://jans.io/oauth/config/jans-link.write", "https://jans.io/oauth/config/read-all"])
};



@LinkConfigUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/jans-link/link-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans-link.write", "https://jans.io/oauth/config/write-all"])
};



@TrustRelationshipGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/trust-relationship"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml.readonly", "https://jans.io/oauth/config/saml.write", "https://jans.io/oauth/config/read-all"])
};


@TrustRelationshipUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/trust-relationship"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml.write", "https://jans.io/oauth/config/write-all"])
};


@SamlConfigGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/samlConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-config.readonly", "https://jans.io/oauth/config/saml-config.write", "https://jans.io/oauth/config/read-all"])
};


@SamlConfigUpdates
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/samlConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-config.write", "https://jans.io/oauth/config/write-all"])
};


@ScopeGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/scope"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-scope.readonly", "https://jans.io/oauth/config/saml-scope.write", "https://jans.io/oauth/config/read-all"])
};


@ScopeUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/scope"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-scope.write", "https://jans.io/oauth/config/write-all"])
};


@IdpConfigGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/config.readonly", "https://jans.io/idp/config.write", "https://jans.io/oauth/config/read-all"])
};


@IdpConfigUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/config.write", "https://jans.io/oauth/config/write-all"])
};


@RealmGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/realm"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/realm.readonly", "https://jans.io/idp/realm.write", "https://jans.io/oauth/config/read-all"])
};


@RealmUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/realm"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/realm.write", "https://jans.io/oauth/config/write-all"])
};


@IdpGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/saml.readonly", "https://jans.io/idp/saml.write", "https://jans.io/oauth/config/read-all"])
};


@IdpUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/saml.write", "https://jans.io/oauth/config/write-all"])
};


@IdpDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/saml.delete", "https://jans.io/oauth/config/write-all"])
};


@ConfigMessageGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.readonly", "https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/read-all"])
};


@ConfigMessageUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@ConfigMessageRedisGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.readonly", "https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/read-all"])
};


@ConfigMessageRedisUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@ConfigMessagePostgresGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/postgres"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.readonly", "https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/read-all"])
};


@ConfigMessagePostgresPut
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/postgres"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@ConfigMessagePostgresPatch
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/postgres"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@HealthAppVersionGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/app-version"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/app-version.readonly", "https://jans.io/oauth/config/read-all"])
};


@HealthServiceStatusGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/service-status"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/data.readonly", "https://jans.io/oauth/config/read-all"])
};


@LinkConfigGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc-link/kcLinkConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/kc-link-config.readonly", "https://jans.io/oauth/kc-link-config.write", "https://jans.io/oauth/config/read-all"])
};


@LinkConfigUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc-link/kcLinkConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/kc-link-config.write", "https://jans.io/oauth/config/write-all"])
};


@LockConfigGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/lockConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock-config.readonly", "https://jans.io/oauth/lock-config.write", "https://jans.io/oauth/config/read-all"])
};


@LockConfigUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/lockConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock-config.write", "https://jans.io/oauth/config/write-all"])
};


@JansAssetsGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-assets"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans_asset-read", "https://jans.io/oauth/config/jans_asset-write", "https://jans.io/oauth/config/read-all"])
};


@JansAssetsUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-assets"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans_asset-write", "https://jans.io/oauth/config/write-all"])
};


@JansAssetsDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-assets"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans_asset-delete", "https://jans.io/oauth/config/write-all"])
};


@AuditGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/audit.readonly", "https://jans.io/oauth/lock/audit.write", "https://jans.io/oauth/lock/read-all"])
};


@AuditPost
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/audit.write", "https://jans.io/oauth/lock/write-all"])
};



@AuditHealthGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/health"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/health.readonly", "https://jans.io/oauth/lock/health.write", "https://jans.io/oauth/lock/audit.readonly"])
};


@AuditHealthPost
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/health.write", "https://jans.io/oauth/lock/audit.write"])
};


@AuditLogGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/log"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/log.readonly", "https://jans.io/oauth/lock/log.write", "https://jans.io/oauth/lock/audit.readonly"])
};


@AuditLogPost
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/log"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/log.write", "https://jans.io/oauth/lock/audit.write"])
};


@AuditTelemetryGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/telemetry"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/telemetry.readonly", "https://jans.io/oauth/lock/telemetry.write", "https://jans.io/oauth/lock/audit.readonly"])
};


@AuditTelemetryPost
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/telemetry"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/telemetry.write", "https://jans.io/oauth/lock/audit.write"])
};


@TokenGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/token"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/token.readonly", "https://jans.io/oauth/config/token.write", "https://jans.io/oauth/config/read-all"])
};


@TokenUpdate
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/token"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/token.write", "https://jans.io/oauth/config/openid-write", "https://jans.io/oauth/config/write-all"])
};


@TokenDelete
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/token"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/token.delete", "https://jans.io/oauth/config/openid-delete", "https://jans.io/oauth/config/delete-all"])
};


@AgamaRepoGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama-repo"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama-repo.readonly", "https://jans.io/oauth/config/agama-repo.write", "https://jans.io/oauth/config/read-all"])
};


@LockStatGet
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/lockStat"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/read-all", "https://jans.io/oauth/config/read-all"])
};







 


 
 


