/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.mapper;


import io.jans.configapi.plugin.keycloak.idp.broker.model.Realm;
import jakarta.enterprise.context.ApplicationScoped;

import org.keycloak.representations.idm.RealmRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper
@ApplicationScoped
public interface RealmMapper {

  IdentityProviderMapper INSTANCE = Mappers.getMapper(IdentityProviderMapper.class);  
  
  @Mapping(target = "inum", source = "kcRealmRepresentation.id")
  @Mapping(target = "name", source = "kcRealmRepresentation.realm")
  Realm kcRealmRepresentationToRealm(RealmRepresentation kcRealmRepresentation);  

  @Mapping(target = "id", source = "realm.inum")
  @Mapping(target = "realm", source = "realm.name")
  RealmRepresentation realmToKCRealmRepresentation(Realm realm);
}
