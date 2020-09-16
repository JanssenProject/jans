package org.gluu.configapi.rest.resource;

import org.slf4j.Logger;

import javax.inject.Inject;

//@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.DATABASE + ApiConstants.LDAP)
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
public class LdapConfigurationResource extends BaseResource {
  
  @Inject
  Logger logger;
/*
  @Inject
  LdapConfigurationService ldapConfigurationService;
  
  @Inject
  ConnectionStatus connectionStatus;
  
  @GET
  @ProtectedApi( scopes = {READ_ACCESS} )
  public Response getLdapConfiguration() {
     List<GluuLdapConfiguration> ldapConfigurationList = this.ldapConfigurationService.findLdapConfigurations();
     return Response.ok(ldapConfigurationList).build();
  }  
  
  @GET
  @Path(ApiConstants.NAME_PARAM_PATH)
  @ProtectedApi( scopes = {READ_ACCESS} )
  public Response getLdapConfigurationByName(@PathParam(ApiConstants.NAME) String name) {
    GluuLdapConfiguration ldapConfiguration = findLdapConfigurationByName(name);
    return Response.ok(ldapConfiguration).build();
  }
  
  @POST
  @ProtectedApi( scopes = {WRITE_ACCESS} )
  public Response addLdapConfiguration(@Valid @NotNull  GluuLdapConfiguration ldapConfiguration) {
    this.ldapConfigurationService.save(ldapConfiguration);
    return Response.status(Response.Status.CREATED).entity(ldapConfiguration).build();  
  }
    
  @PUT
  @ProtectedApi( scopes = {WRITE_ACCESS} )
  public Response updateLdapConfiguration(@Valid @NotNull  GluuLdapConfiguration ldapConfiguration) {
    findLdapConfigurationByName(ldapConfiguration.getConfigId());
    this.ldapConfigurationService.update(ldapConfiguration);
    return Response.ok(ldapConfiguration).build();  
  }
  
  @DELETE
  @Path(ApiConstants.NAME_PARAM_PATH)
  @ProtectedApi( scopes = {WRITE_ACCESS} )
  public Response deleteLdapConfigurationByName(@PathParam(ApiConstants.NAME) String name) {
    findLdapConfigurationByName(name);
    logger.info("Delete Ldap Configuration by name " + name);
    this.ldapConfigurationService.remove(name);
    return Response.noContent().build();
  }
  
  
  @PATCH
  @Path(ApiConstants.NAME_PARAM_PATH)
  @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
  @ProtectedApi(scopes = { WRITE_ACCESS })
  public Response patchLdapConfigurationByName(@PathParam(ApiConstants.NAME) String name, @NotNull String requestString) throws Exception {
    GluuLdapConfiguration ldapConfiguration = findLdapConfigurationByName(name);
    logger.info("Patch Ldap Configuration by name " + name);
    ldapConfiguration = Jackson.applyPatch(requestString, ldapConfiguration);
    this.ldapConfigurationService.update(ldapConfiguration);
      return Response.ok(ldapConfiguration).build();
  }
  
  @POST
  @Path(ApiConstants.TEST)
  @ProtectedApi(scopes = { READ_ACCESS })
  public Response testLdapConfigurationByName(@Valid @NotNull  GluuLdapConfiguration ldapConfiguration) throws Exception {
    logger.info("\n\n\n LdapConfigurationResource:::testLdapConfigurationByName() - ldapConfiguration = "+ldapConfiguration+"\n\n\n");
    logger.info("Test Ldap Configuration " + ldapConfiguration);
    boolean status = connectionStatus.isUp(ldapConfiguration);
    logger.info("\n\n\n LdapConfigurationResource:::testLdapConfigurationByName() - status = "+status+"\n\n\n");
    return Response.ok(status).build();
  }
      
  
  private GluuLdapConfiguration findLdapConfigurationByName(String name) {
   try{
      return this.ldapConfigurationService.findLdapConfigurationByName(name);
    }
    catch(Exception ex) {
      logger.error("Could not find Ldap Configuration by name '"+ name+"'", ex);
      throw new NotFoundException(getNotFoundError("Ldap Configuration - '"+name+"'"));
      
    }
  }
  */
}
