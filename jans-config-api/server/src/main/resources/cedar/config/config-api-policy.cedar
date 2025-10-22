@id("@AcrsRead")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/acrs"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/acrs.readonly","https://jans.io/oauth/config/acrs.write","https://jans.io/oauth/config/read-all"])
};

@id("@AcrsUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/acrs"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/acrs.write","https://jans.io/oauth/config/write-all"])
};

@id("@AttributeGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/attributes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/attributes.readonly","https://jans.io/oauth/config/attributes.write", "https://jans.io/oauth/config/read-all"])
};

@id("AttributeUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/attributes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/attributes.write","https://jans.io/oauth/config/write-all"])
};

@id("@AttributeDELETE")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/attributes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/attributes.delete","https://jans.io/oauth/config/delete-all"])
};


@id("@CacheGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/read-all"])
};


@id("@CachePatch")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/write-all"])
};


@id("@CacheRedisGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly", "https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/read-all"])
};


@id("@CacheRedisUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/write-all"])
};


@id("@CacheInMemoryGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/in-memory"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/read-all"])
};

@id("@CacheInMemoryUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/in-memory"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/write-all"])
};


@id("@CacheNativePersistenceGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/native-persistence"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/read-all"])
};


@id("@CacheNativePersistenceUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/native-persistence"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/write-all"])
};


@id("@MemcachedPersistenceGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/memcached"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.readonly","https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/read-all"])
};


@id("@MemcachedPersistenceUpdate")
permit(
  principal is Jans::Workload,
action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache/memcached"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write","https://jans.io/oauth/config/write-all"])
};


@id("@ClientsGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/openid/clients"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/openid/clients.readonly","https://jans.io/oauth/config/openid-read","https://jans.io/oauth/config/read-all"])
};


@id("@ClientsUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/openid/clients"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/openid/clients.write","https://jans.io/oauth/config/openid-write","https://jans.io/oauth/config/write-all"])
};

@id("@ClientsDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/openid/clients"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/openid/clients.delete","https://jans.io/oauth/config/openid-delete","https://jans.io/oauth/config/delete-all"])
};


@id("@ConfigGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/properties.readonly","https://jans.io/oauth/jans-auth-server/config/properties.write","https://jans.io/oauth/config/read-all"])
};


@id("@ConfigPatch")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/properties.write","https://jans.io/oauth/config/write-all"])
};


@id("@ConfigPersistenceGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/config/persistence"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/properties.readonly","https://jans.io/oauth/jans-auth-server/config/properties.write","https://jans.io/oauth/config/read-all"])
};


@id("@ConfigSmtpGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.readonly","https://jans.io/oauth/config/smtp.write","https://jans.io/oauth/config/read-all"])
};


@id("@ConfigSmtpUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.write","https://jans.io/oauth/config/write-all"])
};

@id("@ConfigSmtpDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.delete","https://jans.io/oauth/config/delete-all"])
};


@id("@ConfigSmtpTest")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/smtp/test"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.readonly","https://jans.io/oauth/config/smtp.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ScriptsGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.readonly","https://jans.io/oauth/config/scripts.write","https://jans.io/oauth/config/read-all"])
};


@id("@ScriptsUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.write","https://jans.io/oauth/config/write-all"])
};


@id("@ScriptTypeGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/type/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.readonly","https://jans.io/oauth/config/scripts.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ScriptInumGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/inum/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.readonly","https://jans.io/oauth/config/scripts.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ScriptDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@ScriptPatch")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/scripts/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scripts.write", "https://jans.io/oauth/config/write-all"])
};


@id("@Fido2Get")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/fido2-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.readonly","https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/read-all"])
};


@id("@Fido2Put")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/fido2-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/write-all"])
};



@id("@Fido2RegistrationGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/registration"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.readonly","https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/read-all"])
};


@id("@Fido2RegistrationUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/registration"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/write-all"])
};


@id("@Fido2RegistrationDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/fido2/registration"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.delete","https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/write-all"])
};

@id("@JwksGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/jwks"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jwks.readonly","https://jans.io/oauth/config/jwks.write","https://jans.io/oauth/config/read-all"])
};

@id("@JwksUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/jwks"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jwks.write","https://jans.io/oauth/config/write-all"])
};

@id("@JwksDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/jwks"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jwks.delete","https://jans.io/oauth/config/delete-all"])
};


@id("@LdapGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.readonly","https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/read-all"])
};


@id("@LdapUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/write-all"])
};


@id("@LdapGetByName")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.readonly","https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/read-all"])
};


@id("@LdapUpdateByName")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/write-all"])
};


@id("@LdapDeleteByName")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.delete","https://jans.io/oauth/config/delete-all"])
};


@id("@LdapTest")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/database/ldap/test"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.readonly","https://jans.io/oauth/config/database/ldap.write","https://jans.io/oauth/config/read-all"])
};


@id("@LoggingGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/logging"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/logging.readonly","https://jans.io/oauth/config/logging.write","https://jans.io/oauth/config/read-all"])
};


@id("@LoggingPut")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/logging"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/logging.write","https://jans.io/oauth/config/write-all"])
};


@id("@ScopeGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.readonly","https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/read-all"])
};


@id("@ScopeUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/write-all"])
};

@id("@ScopeByInumGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.readonly", "https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/read-all"])
};


@id("@ScopeByInumUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.write","https://jans.io/oauth/config/write-all"])
};


@id("@ScopeByInumDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/scopes/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.delete","https://jans.io/oauth/config/delete-all"])
};


@id("@UMAresourcesGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.readonly","https://jans.io/oauth/config/uma/resources.write", "https://jans.io/oauth/config/uma-read", "https://jans.io/oauth/config/read-all"])
};


@id("@UMAresourcesUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.write","https://jans.io/oauth/config/uma-write", "https://jans.io/oauth/config/write-all"])
};


@id("@UMAresourcesByIdGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.readonly","https://jans.io/oauth/config/uma/resources.write", "https://jans.io/oauth/config/uma-read", "https://jans.io/oauth/config/read-all"])
};

@id("@UMAresourcesPatchById")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.write","https://jans.io/oauth/config/uma-write", "https://jans.io/oauth/config/write-all"])
};


@id("@UMAresourcesDeleteById")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/uma/resources/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/uma/resources.delete","https://jans.io/oauth/config/uma-delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@StatGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/stat"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/stats.readonly", "https://jans.io/oauth/config/read-all"])
};

@id("@healthGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health"
);


@id("@HealthLiveGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/live"
);


@id("@HealthReadyGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/ready"
);

@id("@ScimGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/scim/scim-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/scim/config.readonly", "https://jans.io/scim/config.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ScimPatch")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/scim/scim-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/scim/config.write", "https://jans.io/oauth/config/write-all"])
};


@id("@OrgGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/organization.readonly", "https://jans.io/oauth/config/organization.write", "https://jans.io/oauth/config/read-all"])
};


@id("@OrgPatch")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/organization.write", "https://jans.io/oauth/config/write-all"])
};


@id("@ConfigUserGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/mgt/configuser"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/user.readonly", "https://jans.io/oauth/config/user.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ConfigUserUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/user.write", "https://jans.io/oauth/config/write-all"])
};


@id("@ConfigUserDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/org"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/user.delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@AgamaGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.readonly", "https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/read-all"])
};


@id("@AgamaUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/write-all"])
};


@id("@AgamaDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@AgamaDeploymentGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/ads-deployment"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.readonly", "https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/read-all"])
};


@id("@AgamaDeploymentPost")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/ads-deployment"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/write-all"])
};


@id("@AgamaDeploymentDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/ads-deployment"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama.delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@SessionGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/session"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/session.readonly", "https://jans.io/oauth/config/read-all"])
};


@id("@SessionDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/session"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/session.delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@AdminUIRolesGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
 };


@id("AdminUIRolesUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
 };

@id("@AdminUIRolesGetByRole")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
 };


@id("@AdminUIRolesDeleteByRole")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRoles/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.delete", "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all"])
 };


@id("@AdminUIPermissionsGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
 };


@id("@AdminUIPermissionsUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
 };

 
@id("@AdminUIPermissionsGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};

@id("@AdminUIPermissionsDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIPermissions/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.delete", "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all"])
};


@id("@AdminUIRolePermissionsMappingGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@id("@AdminUIRolePermissionsMappingUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
};


@id("@AdminUIRolePermissionsMappingGetByAdminUIRole")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@id("@AdminUIRolePermissionsMappingDeleteByAdminUIRole")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/adminUIRolePermissionsMapping/*"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.delete", "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all"])
};


@id("@AdminUILicenseDetailsGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/licenseDetails"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@id("@AdminUIActivateLicensePost")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/activateLicense"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
};


@id("@AdminUILicenseIsActiveGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/isActive"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};


@id("@AdminUILicenseApiCredentialsPost")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/admin-ui/license/saveApiCredentials"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/license.write", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
};

@id("@PluginGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/plugin"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/plugin.readonly", "https://jans.io/oauth/config/read-all"])
};


@id("@ConfigApiGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/properties.readonly", "https://jans.io/oauth/config/properties.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ConfigApiPatch")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/properties.write", "https://jans.io/oauth/config/write-all"])
};


@id("@AuthorizationsGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/clients/authorizations"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/client/authorizations.readonly", "https://jans.io/oauth/config/read-all"])
};


@id("@AuthorizationsDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/clients/authorizations"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/client/authorizations.delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@LinkConfigGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/jans-link/link-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans-link.readonly", "https://jans.io/oauth/config/jans-link.write", "https://jans.io/oauth/config/read-all"])
};


@id("@LinkConfigUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/jans-link/link-config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans-link.write", "https://jans.io/oauth/config/write-all"])
};


@id("@TrustRelationshipGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/trust-relationship"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml.readonly", "https://jans.io/oauth/config/saml.write", "https://jans.io/oauth/config/read-all"])
};


@id("@TrustRelationshipUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/trust-relationship"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml.write", "https://jans.io/oauth/config/write-all"])
};


@id("@SamlConfigGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/samlConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-config.readonly", "https://jans.io/oauth/config/saml-config.write", "https://jans.io/oauth/config/read-all"])
};


@id("@SamlConfigUpdates")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/samlConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-config.write", "https://jans.io/oauth/config/write-all"])
};


@id("@ScopeGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/scope"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-scope.readonly", "https://jans.io/oauth/config/saml-scope.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ScopeUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/scope"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/saml-scope.write", "https://jans.io/oauth/config/write-all"])
};


@id("@IdpConfigGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/config.readonly", "https://jans.io/idp/config.write", "https://jans.io/oauth/config/read-all"])
};


@id("@IdpConfigUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp/config"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/config.write", "https://jans.io/oauth/config/write-all"])
};


@id("@RealmGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/realm"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/realm.readonly", "https://jans.io/idp/realm.write", "https://jans.io/oauth/config/read-all"])
};


@id("@RealmUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/realm"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/realm.write", "https://jans.io/oauth/config/write-all"])
};


@id("@IdpGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/saml.readonly", "https://jans.io/idp/saml.write", "https://jans.io/oauth/config/read-all"])
};


@id("@IdpUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH", Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/saml.write", "https://jans.io/oauth/config/write-all"])
};


@id("@IdpDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc/saml/idp"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/idp/saml.delete", "https://jans.io/oauth/config/write-all"])
};


@id("@ConfigMessageGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.readonly", "https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ConfigMessageUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@id("@ConfigMessageRedisGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.readonly", "https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ConfigMessageRedisUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/redis"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@id("@ConfigMessagePostgresGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/postgres"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.readonly", "https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/read-all"])
};


@id("@ConfigMessagePostgresPut")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/postgres"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@id("@ConfigMessagePostgresPatch")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/message/postgres"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/message.write", "https://jans.io/oauth/config/write-all"])
};


@id("@HealthAppVersionGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/app-version"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/app-version.readonly", "https://jans.io/oauth/config/read-all"])
};


@id("@HealthServiceStatusGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/health/service-status"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/data.readonly", "https://jans.io/oauth/config/read-all"])
};


@id("@LinkConfigGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc-link/kcLinkConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/kc-link-config.readonly", "https://jans.io/oauth/kc-link-config.write", "https://jans.io/oauth/config/read-all"])
};


@id("@LinkConfigUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/kc-link/kcLinkConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/kc-link-config.write", "https://jans.io/oauth/config/write-all"])
};


@id("@LockConfigGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/lockConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock-config.readonly", "https://jans.io/oauth/lock-config.write", "https://jans.io/oauth/config/read-all"])
};


@id("@LockConfigUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/lockConfig"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock-config.write", "https://jans.io/oauth/config/write-all"])
};


@id("@JansAssetsGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-assets"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans_asset-read", "https://jans.io/oauth/config/jans_asset-write", "https://jans.io/oauth/config/read-all"])
};


@id("@JansAssetsUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-assets"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans_asset-write", "https://jans.io/oauth/config/write-all"])
};


@id("@JansAssetsDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-assets"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/jans_asset-delete", "https://jans.io/oauth/config/write-all"])
};


@id("@AuditGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/audit.readonly", "https://jans.io/oauth/lock/audit.write", "https://jans.io/oauth/lock/read-all"])
};


@id("@AuditPost")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/audit.write", "https://jans.io/oauth/lock/write-all"])
};


@id("@AuditHealthGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/health"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/health.readonly", "https://jans.io/oauth/lock/health.write", "https://jans.io/oauth/lock/audit.readonly"])
};


@id("@AuditHealthPost")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/health.write", "https://jans.io/oauth/lock/audit.write"])
};


@id("@AuditLogGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/log"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/log.readonly", "https://jans.io/oauth/lock/log.write", "https://jans.io/oauth/lock/audit.readonly"])
};


@id("@AuditLogPost")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/log"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/log.write", "https://jans.io/oauth/lock/audit.write"])
};


@id("@AuditTelemetryGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/telemetry"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/telemetry.readonly", "https://jans.io/oauth/lock/telemetry.write", "https://jans.io/oauth/lock/audit.readonly"])
};


@id("@AuditTelemetryPost")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/audit/telemetry"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/telemetry.write", "https://jans.io/oauth/lock/audit.write"])
};


@id("@TokenGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/token"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/token.readonly", "https://jans.io/oauth/config/token.write", "https://jans.io/oauth/config/read-all"])
};


@id("@TokenUpdate")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/token"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/token.write", "https://jans.io/oauth/config/openid-write", "https://jans.io/oauth/config/write-all"])
};


@id("@TokenDelete")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/token"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/token.delete", "https://jans.io/oauth/config/openid-delete", "https://jans.io/oauth/config/delete-all"])
};


@id("@AgamaRepoGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/agama-repo"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/config/agama-repo.readonly", "https://jans.io/oauth/config/agama-repo.write", "https://jans.io/oauth/config/read-all"])
};


@id("@LockStatGet")
permit(
  principal is Jans::Workload,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/lock/lockStat"
)
when {
  principal has access_token.scope &&
  principal.access_token.scope.containsAny(["https://jans.io/oauth/lock/read-all", "https://jans.io/oauth/config/read-all"])
};







 


 
 


