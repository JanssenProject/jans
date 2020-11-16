/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

//import io.jans.scim.ldap.service.IGroupService;
//import io.jans.scim.model.GluuGroup;
import org.apache.commons.lang.StringUtils;
import io.jans.model.attribute.AttributeDataType;
import io.jans.scim.model.scim.ScimCustomPerson;
//import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.service.AttributeService;
import io.jans.scim.service.antlr.scimFilter.ScimFilterParserService;
//import io.jans.scim.service.scim2.Scim2GroupService;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.scim.util.ServiceUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named
@Path("/scim")
public class ScimResourcesUpdatedWebService extends BaseScimWebService {

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private ScimFilterParserService scimFilterParserService;

    @Inject
    private AttributeService attributeService;

    private boolean ldapBackend;

    private Map<String, AttributeDataType> attributeDataTypes;
/*
    @Inject
    private UserWebService userWebService;
    @Inject
    private IGroupService groupService;

    @Inject
    private Scim2GroupService scim2GroupService;

    @Inject
    private GroupWebService groupWebService;
*/

    @Path("UpdatedUsers")
    @GET
    @Produces(MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT)
    @ProtectedApi
    public Response usersChangedAfter(@QueryParam("timeStamp") String isoDate,
                                      @QueryParam("start") int start,
                                      @QueryParam("pageSize") int itemsPerPage) {

        Response response;
        log.debug("Executing web service method. usersChangedAfter");

        try {
            if (start < 0 || itemsPerPage <=0) {
                return getErrorResponse(Response.Status.BAD_REQUEST, "No suitable value for 'start' or 'pageSize' params");
            }

            String date = ldapBackend ? DateUtil.ISOToGeneralizedStringDate(isoDate) : DateUtil.gluuCouchbaseISODate(isoDate);
            if (date == null) {
                response = getErrorResponse(Response.Status.BAD_REQUEST, "Unparsable date: " + isoDate);
            } else {
                log.info("Searching users updated or created after {} (starting at index {} - at most {} results)", date, start, itemsPerPage);
                Filter filter = Filter.createORFilter(
                        Filter.createGreaterOrEqualFilter("jansCreationTimestamp", date),
                        Filter.createGreaterOrEqualFilter("updatedAt", date));
                log.trace("Using filter {}", filter.toString());

                List<ScimCustomPerson> list = entryManager.findPagedEntries(personService.getDnForPerson(null), ScimCustomPerson.class,
                        filter, null,  "uid", SortOrder.ASCENDING, start, itemsPerPage, getMaxCount()).getEntries();

                response = Response.ok(getUserResultsAsJson(list)).build();
            }
        } catch (Exception e1) {
            log.error("Failure at usersChangedAfter method", e1);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e1.getMessage());
        }
        return response;

    }

    private String getUserResultsAsJson(List<ScimCustomPerson> list) throws Exception {

        List<Map<String, List<Object>>> resources = new ArrayList<>();
        long fresher = 0;

        for (ScimCustomPerson person : list) {
            long updatedAt = Optional.ofNullable(person.getUpdatedAt()).map(Date::getTime).orElse(0L);
            if (fresher < updatedAt) {
                fresher = updatedAt;
            }

            Map<String, List<Object>> map = new TreeMap<>();
            person.getTypedCustomAttributes().forEach(attr -> map.put(attr.getName(), new ArrayList<>(attr.getValues())));
            map.putAll(getNonCustomAttributes(person));

            //Do a best effort to supply output in proper data types
            for (String key : map.keySet()) {
                List<Object> values = map.get(key);
                for (int i = 0; i < values.size(); i++) {

                    Object rawValue = values.get(i);
                    String value = rawValue.toString();
                    Object finalValue = null;

                    AttributeDataType dataType = Optional.ofNullable(attributeDataTypes.get(key)).orElse(AttributeDataType.STRING);
                    switch (dataType) {
                        case DATE:
                            finalValue = getStringDateFrom(value);
                            break;
                        case BOOLEAN:
                            if (ldapBackend) {
                                value = value.toLowerCase();
                            }
                            if (value.equals(Boolean.TRUE.toString()) || value.equals(Boolean.FALSE.toString())) {
                                finalValue = Boolean.valueOf(value);
                            }
                            break;
                        case NUMERIC:
                            try {
                                finalValue = new Integer(value);
                            } catch (Exception e) {
                                log.warn("{} is not a numeric value!", value);
                            }
                            break;
                    }

                    if (finalValue == null) {
                        if (rawValue.getClass().equals(Date.class)) {
                            Instant instant = Instant.ofEpochMilli(Date.class.cast(rawValue).getTime());
                            finalValue = DateTimeFormatter.ISO_INSTANT.format(instant);
                        } else {
                            finalValue = getStringDateFrom(value);
                            finalValue = finalValue == null ? value : finalValue;
                        }
                    }
                    values.set(i, finalValue);
                }
            }

            resources.add(map);
        }
        return getResultsAsJson(resources, fresher);

    }

    private Map<String, List<Object>> getNonCustomAttributes(ScimCustomPerson person) {

        Map<String, List<Object>> map = new HashMap<>();
        Field[] fields = ScimCustomPerson.class.getDeclaredFields();

        for (Field field : fields) {
            try {
                AttributeName annotation = field.getAnnotation(AttributeName.class);
                if (annotation != null) {

                    String fieldName = field.getName();
                    String attribute = StringUtils.isEmpty(annotation.name()) ? fieldName : annotation.name();
                    Method getter = IntrospectUtil.getGetter(fieldName, ScimCustomPerson.class);

                    if (getter != null) {
                        Object value = getter.invoke(person);
                        if (value != null) {
                            map.put(attribute, new ArrayList<>(Collections.singletonList(value)));
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return map;

    }

    private String getResultsAsJson(List<?> resources, long fresher) throws Exception {

        int total = resources.size();
        log.info("Found {} matching entries", total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        if (fresher > 0) {
            result.put("latestUpdateAt", DateUtil.millisToISOString(fresher));
        }
        result.put("results", resources);

        return ServiceUtil.getObjectMapper().writeValueAsString(result);

    }

    private String getStringDateFrom(String str) {
        return ldapBackend ? DateUtil.generalizedToISOStringDate(str) : DateUtil.gluuCouchbaseISODate(str);
    }

    @PostConstruct
    private void init() {
        ldapBackend = scimFilterParserService.isLdapBackend();
        attributeDataTypes = new HashMap<>();
        attributeService.getAllAttributes().forEach(ga -> attributeDataTypes.put(ga.getName(), ga.getDataType()));
    }

/*
    //Groups endpoint not necessary, but if needed, we need to guarantee first that excludeMetaLastMod is refreshed
    //whenever the group is updated in GUI or via SCIM (or cust script)
    //@Path("UpdatedGroups")
    //@GET
    @Produces(MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT)
    @ProtectedApi
    public Response groupsChangedAfter(@QueryParam("timeStamp") String isoDate) {

        Response response;
        log.debug("Executing web service method. groupsChangedAfter");

        try {
            String date = ZonedDateTime.parse(isoDate).format(DateTimeFormatter.ISO_INSTANT);
            //In database, excludeMetaLastMod is just a string (not date)

            Filter filter = Filter.createORFilter(
                    Filter.createNOTFilter(Filter.createPresenceFilter("jansMetaLastMod")),
                    Filter.createGreaterOrEqualFilter("jansMetaLastMod", date));
            List<GluuGroup> list = entryManager.findEntries(groupService.getDnForGroup(null), GluuGroup.class, filter);
            response = Response.ok(getGroupResultsAsJson(list)).build();

        } catch (DateTimeParseException e) {
            response = getErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e1) {
            log.error("Failure at groupsChangedAfter method", e1);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e1.getMessage());
        }
        return response;

    }

    private String getGroupResultsAsJson(List<GluuGroup> list) throws Exception {

        List<BaseScimResource> resources = new ArrayList<>();
        long fresher = 0;

        for (GluuGroup group : list) {
            GroupResource jsScimGrp = new GroupResource();
            scim2GroupService.transferAttributesToGroupResource(group, jsScimGrp, userWebService.getEndpointUrl(), groupWebService.getEndpointUrl());
            resources.add(jsScimGrp);

            String modified = group.getAttribute("jansMetaLastMod");
            try {
                if (modified != null) {
                    long updatedAt = ZonedDateTime.parse(modified).toInstant().toEpochMilli();
                    if (fresher < updatedAt) {
                        fresher = updatedAt;
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing supposed ISO date {}", modified);
            }
        }
        return  getResultsAsJson(resources, fresher);

    }
    */

}
