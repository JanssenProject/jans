/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.config;

import static io.jans.scim.model.scim2.Constants.MAX_BULK_OPERATIONS;
import static io.jans.scim.model.scim2.Constants.MAX_BULK_PAYLOAD_SIZE;

import java.util.Collection;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Schema;
import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.provider.config.*;

/**
 * This class represents a ServiceProviderConfig SCIM resource. It's key for the implementation of the
 * <code>/ServiceProviderConfig</code> endpoint. For more about this resource type see RFC 7643, section 5
 */
/*
 * Created by jgomer on 2017-09-23.
 */
@Schema(id="urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig", name="ServiceProviderConfig", description = "SCIM 2.0 Service Provider Config Resource")
public class ServiceProviderConfig extends BaseScimResource {

    @Attribute(description = "An HTTP-addressable URL pointing to the service provider's human-consumable help documentation.",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String documentationUri = "https://gluu.org/docs/ce/user-management/scim2/";

    @Attribute(description = "A complex type that specifies PATCH configuration options.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
	private PatchConfig patch = new PatchConfig(true);

    @Attribute(description = "A complex type that specifies bulk configuration options. See Section 3.7 of RFC7644",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
    private BulkConfig bulk = new BulkConfig(true, MAX_BULK_OPERATIONS, MAX_BULK_PAYLOAD_SIZE);

    @Attribute(description = "A complex type that specifies FILTER options.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
	private FilterConfig filter = new FilterConfig(true);

    @Attribute(description = "A complex type that specifies configuration options related to changing a password.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
    private ChangePasswordConfig changePassword = new ChangePasswordConfig(true);

    @Attribute(description = "A complex type that specifies Sort configuration options.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
	private SortConfig sort = new SortConfig(true);

    @Attribute(description = "A complex type that specifies ETag configuration options.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
	private ETagConfig etag = new ETagConfig(false);

    @Attribute(description = "A multi-valued complex type that specifies supported authentication scheme properties. " +
            "To enable seamless discovery of configurations, the service provider SHOULD, with the appropriate " +
            "security considerations, make the authenticationSchemes attribute publicly accessible without prior authentication.",
            isRequired = true,
            multiValueClass = AuthenticationScheme.class,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
	private Collection<AuthenticationScheme> authenticationSchemes;

	public String getDocumentationUri() {
		return documentationUri;
	}

	public void setDocumentationUri(String documentationUri) {
		this.documentationUri = documentationUri;
	}

	public PatchConfig getPatch() {
		return patch;
	}

	public void setPatch(PatchConfig patch) {
		this.patch = patch;
	}

	public FilterConfig getFilter() {
		return filter;
	}

	public void setFilter(FilterConfig filter) {
		this.filter = filter;
	}

	public BulkConfig getBulk() {
		return bulk;
	}

	public void setBulk(BulkConfig bulk) {
		this.bulk = bulk;
	}

	public SortConfig getSort() {
		return sort;
	}

	public void setSort(SortConfig sort) {
		this.sort = sort;
	}

	public ChangePasswordConfig getChangePassword() {
		return changePassword;
	}

	public void setChangePassword(ChangePasswordConfig changePassword) {
		this.changePassword = changePassword;
	}

	public ETagConfig getEtag() {
		return etag;
	}

	public void setEtag(ETagConfig etag) {
		this.etag = etag;
	}

	public void setAuthenticationSchemes(Collection<AuthenticationScheme> authenticationSchemes) {
		this.authenticationSchemes = authenticationSchemes;
	}

	public Collection<AuthenticationScheme> getAuthenticationSchemes() {
		return authenticationSchemes;
	}

}
