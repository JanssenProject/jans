package io.jans.scim.service.antlr.scimFilter;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.scim.model.scim2.AttributeDefinition.Type;
import io.jans.model.GluuAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.StringHelper;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.extensions.ExtensionField;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.service.antlr.scimFilter.antlr4.ScimFilterBaseListener;
import io.jans.scim.service.antlr.scimFilter.antlr4.ScimFilterParser;
import io.jans.scim.service.antlr.scimFilter.enums.CompValueType;
import io.jans.scim.service.antlr.scimFilter.enums.ScimOperator;
import io.jans.scim.service.antlr.scimFilter.util.FilterUtil;
import io.jans.scim.service.scim2.ExtensionService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.Pair;

import javax.lang.model.type.NullType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

/**
 * Created by jgomer on 2017-12-09.
 */
public class FilterListener extends ScimFilterBaseListener {

    private Logger log = LogManager.getLogger(getClass());
    private Deque<Filter> filter;
    private Class<? extends BaseScimResource> resourceClass;
    private String error;
    private SubFilterGenerator subFilterGenerator;
    private ExtensionService extService;
    private Map<String, GluuAttribute> attributesMap;
    private boolean ldapBackend;
	
    public FilterListener(Class<? extends BaseScimResource> resourceClass, Map<String, GluuAttribute> attributesMap, boolean ldapBackend) {
        filter = new ArrayDeque<>();
        extService = CdiUtil.bean(ExtensionService.class);
        this.resourceClass = resourceClass;
        this.attributesMap = attributesMap;
        this.ldapBackend = ldapBackend;

        subFilterGenerator =  new SubFilterGenerator(ldapBackend);
    }

    @Override
    public void enterAttrexp(ScimFilterParser.AttrexpContext ctx) {

        if (StringUtils.isEmpty(error)) {
            log.trace("enterAttrexp.");

            String path = ctx.attrpath().getText();
            ScimFilterParser.CompvalueContext compValueCtx = ctx.compvalue();
            boolean isPrRule = compValueCtx == null && ctx.getChild(1).getText().equals("pr");

            Type attrType = null;
            Attribute attrAnnot = IntrospectUtil.getFieldAnnotation(path, resourceClass, Attribute.class);
            String ldapAttribute = null;
            boolean isNested = false;
            Boolean multiValued = false;

            if (attrAnnot == null) {
                ExtensionField field = extService.getFieldOfExtendedAttribute(resourceClass, path);

                if (field == null) {
                    error = String.format("Attribute path '%s' is not recognized in %s", path, resourceClass.getSimpleName());
                } else {
                    attrType = field.getAttributeDefinitionType();
                    multiValued = field.isMultiValued();
                    ldapAttribute = path.substring(path.lastIndexOf(":") + 1);
                }
            } else {
                attrType = attrAnnot.type();
                Pair<String, Boolean> pair = FilterUtil.getLdapAttributeOfResourceAttribute(path, resourceClass);
                ldapAttribute = pair.getFirst();
                isNested = pair.getSecond();
                multiValued = computeMultivaluedForCoreAttribute(path, attrAnnot, ldapAttribute);
            }

            if (error != null) {
                //Intentionally left empty
            } else if (attrType == null) {
                error = String.format("Could not determine type of attribute path '%s' in %s", path, resourceClass.getSimpleName());
            } else if (ldapAttribute == null) {
                error = String.format("Could not determine LDAP attribute for path '%s' in %s", path, resourceClass.getSimpleName());
            } else {
                String subattr = isNested ? path.substring(path.lastIndexOf(".") + 1) : null;
                CompValueType type;
                ScimOperator operator;

                if (isPrRule) {
                    type = CompValueType.NULL;
                    operator = ScimOperator.NOT_EQUAL;
                } else {
                    type = FilterUtil.getCompValueType(compValueCtx);
                    operator = ScimOperator.getByValue(ctx.compareop().getText());
                }

                error = FilterUtil.checkFilterConsistency(path, attrType, type, operator);
                if (error == null) {
                    Pair<Filter, String> subf = subFilterGenerator
                            .build(subattr, ldapAttribute, isPrRule ? null : compValueCtx.getText(), attrType, type, operator, multiValued);
                    Filter subFilth = subf.getFirst();
                    error = subf.getSecond();

                    if (subFilth == null) {
                        if (error == null) {
                            error = String.format("Operator '%s' is not supported for attribute %s", operator.getValue(), path);
                        }
                    } else {
                        filter.push(subFilth);
                    }
                }
            }
        }
    }

    @Override
    public void exitAndFilter(ScimFilterParser.AndFilterContext ctx) {
        filter.push(Filter.createANDFilter(filter.poll(), filter.poll()));
    }

    @Override
    public void exitNegatedFilter(ScimFilterParser.NegatedFilterContext ctx) {
        if (ctx.getText().startsWith("not(")) {
            filter.push(Filter.createNOTFilter(filter.poll()));
        }
    }

    @Override
    public void exitOrFilter(ScimFilterParser.OrFilterContext ctx) {
        filter.push(Filter.createORFilter(filter.poll(), filter.poll()));
    }

    public String getError() {
        return error;
    }

    public Filter getFilter() {
        if (StringUtils.isEmpty(error)) {
            Filter f = filter.poll();
            log.info("LDAP filter expression computed was {}", Optional.ofNullable(f).map(Filter::toString).orElse(null));
            return f;
        }
        return null;
    }
    
    /**
     * Tries to determine a "convenient" value for multivalued with the aim of building a filter. In some cases the 
     * Attribute annotation (specifically multiValueClass) is not enough to determine this value. For instance:
     * - The terminal part of address.streetAddress entails usage of singled valued (see Address class), however a more
     *   convenient value would be multiValued=true because the parent (address) is multi valued 
     * - When multivalued seems the right fit, there can be existing data (in the case of couchbase database) expressed 
     *   as a singe value, for instance "mail": "mymail@example.com". In cases like this, a more convenient value would be 
     *   null where the filter produced tries to account both cases
     * - While using null seems to be better overall choice regardless of attribute, the generated WHERE clause may 
     *   become complex  
     * @param path Path to the scim attribute in dot notation, eg. "name.familyName"
     * @param attrAnnot References the Attribute annotation over the Java bean field that represents the terminal 
     *                  portion of the path (familyName in the example)
     * @param dbAttribute Physical attribute name mapped to the path passed
     * @return True/False/null as more convenient
     */
 	private Boolean computeMultivaluedForCoreAttribute(String path, Attribute attrAnnot, String dbAttribute) {
		
 		Boolean multiValued;
		// Determine attribute multivalued from ou=attributes
		String dbAttributeLower = StringHelper.toLowerCase(dbAttribute);
		GluuAttribute gluuAttribute = attributesMap.get(dbAttributeLower);
		if (gluuAttribute != null) {
			multiValued = (gluuAttribute.getOxMultiValuedAttribute() != null) && gluuAttribute.getOxMultiValuedAttribute();
			return multiValued;

		}
 		
 		if (!ldapBackend && (dbAttribute.equals("mail") || dbAttribute.equals("jansPPID"))) {
 			multiValued = null;
 		} else {
 			multiValued = !attrAnnot.multiValueClass().equals(NullType.class);
			
			if (!multiValued) {
				int i = path.indexOf(".");
			
				if (i > 0) {
					//path exists and is annotated with @Attribute
					attrAnnot = IntrospectUtil.getFieldAnnotation(path.substring(0, i), resourceClass, Attribute.class);
					multiValued = !attrAnnot.multiValueClass().equals(NullType.class);
				}
			}
		}
		return multiValued;
 
	}

}
