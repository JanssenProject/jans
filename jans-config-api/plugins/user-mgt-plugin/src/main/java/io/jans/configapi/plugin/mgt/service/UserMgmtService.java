package io.jans.configapi.plugin.mgt.service;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.common.model.common.User;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.mgt.model.user.UserPatchRequest;
import io.jans.configapi.plugin.mgt.util.MgtUtil;
import io.jans.configapi.util.AuthUtil;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static io.jans.as.model.util.Util.escapeLog;

@ApplicationScoped
@Named("userMgmtSrv")
public class UserMgmtService extends io.jans.as.common.service.common.UserService {

    @Inject
    private Logger logger;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    AuthUtil authUtil;

    @Inject
    MgtUtil mgtUtil;

    private static final String BIRTH_DATE = "birthdate";

    @Override
    public List<String> getPersonCustomObjectClassList() {
        return appConfiguration.getPersonCustomObjectClassList();
    }

    @Override
    public String getPeopleBaseDn() {
        return staticConfiguration.getBaseDn().getPeople();
    }

    public PagedResult<User> searchUsers(SearchRequest searchRequest) {
        if (logger.isDebugEnabled()) {
            logger.debug("Search Users with searchRequest:{}", escapeLog(searchRequest));
        }
        Filter searchFilter = null;
        if (StringUtils.isNotEmpty(searchRequest.getFilter())) {
            String[] targetArray = new String[]{searchRequest.getFilter()};
            Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                    null);
            Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                    null);
            Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
            searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);
        }

        return persistenceEntryManager.findPagedEntries(getPeopleBaseDn(), User.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex() - 1, searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public void removeUser(User user) {
        persistenceEntryManager.removeRecursively(user.getDn(), User.class);
    }

    public User patchUser(String inum, UserPatchRequest userPatchRequest) throws JsonPatchException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Details to patch user  inum:{}, UserPatchRequest:{} ", escapeLog(inum),
                    escapeLog(userPatchRequest));
        }
        if (StringHelper.isEmpty(inum)) {
            return null;
        }

        User user = getUserByInum(inum);
        if (user == null) {
            return null;
        }

        logger.debug("User to be patched- user:{}", user);
        // apply direct patch for basic attributes
        if (StringUtils.isNotEmpty(userPatchRequest.getJsonPatchString())) {
            logger.debug("Patch basic attributes");
            user = Jackson.applyPatch(userPatchRequest.getJsonPatchString(), user);
            logger.debug("User after patching basic attributes - user:{}", user);
        }

        // patch for customAttributes
        if (userPatchRequest.getCustomAttributes() != null && !userPatchRequest.getCustomAttributes().isEmpty()) {
            updateCustomAttributes(user, userPatchRequest.getCustomAttributes());
        }

        logger.debug("User before patch user:{}", user);

        // persist user
        user = updateUser(user);
        logger.debug("User after patch user:{}", user);
        return user;

    }

    public User getUserBasedOnInum(String inum) {
        User result = null;
        try {
            result = getUserByInum(inum);
        } catch (Exception ex) {
            logger.error("Failed to load user entry", ex);
        }
        return result;
    }

    private User updateCustomAttributes(User user, List<CustomObjectAttribute> customAttributes) {
        logger.debug("Custom Attributes to update for - user:{}, customAttributes:{} ", user, customAttributes);

        if (customAttributes == null || customAttributes.isEmpty()) {
            return user;
        }

        for (CustomObjectAttribute attribute : customAttributes) {
            CustomObjectAttribute existingAttribute = getCustomAttribute(user, attribute.getName());
            logger.debug("Existing CustomAttributes with existingAttribute:{} ", existingAttribute);

            // add
            if (existingAttribute == null) {
                boolean result = addUserAttribute(user, attribute.getName(), attribute.getValues(),
                        attribute.isMultiValued());
                logger.debug("Result of adding CustomAttributes attribute:{} , result:{} ", attribute, result);
            }
            // remove attribute
            else if (attribute.getValue() == null || attribute.getValues() == null) {

                user.removeAttribute(attribute.getName());
            }
            // replace attribute
            else {
                existingAttribute.setMultiValued(attribute.isMultiValued());
                existingAttribute.setValues(attribute.getValues());
            }
            // Final attribute
            logger.debug("Finally user CustomAttributes user.getCustomAttributes:{} ", user.getCustomAttributes());

        }

        return user;
    }

    public List<User> excludeAttributes(List<User> users, String commaSeparatedString)
            throws IllegalAccessException, InvocationTargetException {
        logger.debug("Attributes:{} to be excluded from users:{} ", commaSeparatedString, users);

        if (users == null || users.isEmpty() || StringUtils.isEmpty(commaSeparatedString)) {
            return users;
        }

        for (User user : users) {
            excludeAttributes(user, commaSeparatedString);
        }
        logger.debug("Users:{} after excluding attribute:{} ", users, commaSeparatedString);

        return users;
    }

    public User excludeAttributes(User user, String commaSeparatedString)
            throws IllegalAccessException, InvocationTargetException {
        logger.debug("Attributes:{} to be excluded from user:{} ", commaSeparatedString, user);

        if (user == null || StringUtils.isEmpty(commaSeparatedString)) {
            return user;
        }

        List<String> excludedAttributes = Arrays.asList(commaSeparatedString.split(","));
        logger.debug("Attributes List:{} to be excluded ", excludedAttributes);

        List<Field> allFields = authUtil.getAllFields(user.getClass());
        logger.debug("All user fields :{} ", allFields);

        HashMap<String, String> map = new HashMap<>();
        for (String attribute : excludedAttributes) {
            logger.debug("User class allFields:{} conatins attribute:{} ? :{} ", allFields, attribute,
                    authUtil.containsField(allFields, attribute));
            if (authUtil.containsField(allFields, attribute)) {
                logger.debug("User class contains attribute:{} ! ", attribute);
                map.put(attribute, null);
            } else {
                logger.debug("Removing custom attribute:{} from user:{} ", attribute, user);
                user.removeAttribute(attribute);
            }
        }

        logger.debug("Attributes map:{} to be excluded ", map);
        if (!map.isEmpty()) {
            logger.debug("Removing simple attributes:{} from user object ", map);
            BeanUtilsBean.getInstance().getConvertUtils().register(false, false, 0);
            BeanUtils.populate(user, map);
        }

        return user;
    }

    public String getUserExclusionAttributesAsString() {
        return authUtil.getUserExclusionAttributesAsString();
    }

    public String checkMandatoryFields(User user, List<String> excludeAttributes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<String> mandatoryAttributes = authUtil.getUserMandatoryAttributes();
        logger.debug("mandatoryAttributess :{}, excludeAttributes:{} ", mandatoryAttributes, excludeAttributes);

        StringBuilder missingAttributes = new StringBuilder();

        if (mandatoryAttributes == null || mandatoryAttributes.isEmpty()) {
            return missingAttributes.toString();
        }

        List<Field> allFields = authUtil.getAllFields(user.getClass());
        logger.debug("All user fields :{} ", allFields);

        Object attributeValue = null;
        for (String attribute : mandatoryAttributes) {
            logger.debug("User class allFields:{} conatins attribute:{} ? :{} ", allFields, attribute,
                    authUtil.containsField(allFields, attribute));

            //check if to be excluded
            if (isExcludedAttribute(excludeAttributes, attribute)) {
                logger.debug("Not checking if the attribute:{} is missing as it's in excludeAttributes:{}", attribute, excludeAttributes);
                continue;
            }

            if (authUtil.containsField(allFields, attribute)) {
                logger.debug("Checking if attribute:{} is simple attribute", attribute);
                attributeValue = BeanUtils.getProperty(user, attribute);
                logger.debug("User basic attribute:{} - attributeValue:{} ", attribute, attributeValue);
            } else {
                logger.debug("Checking if attribute:{} is custom attribute", attribute);
                attributeValue = user.getAttribute(attribute);
                logger.debug("User custom attribute:{} - attributeValue:{} ", attribute, attributeValue);
            }

            if (attributeValue == null) {
                missingAttributes.append(attribute).append(",");
            }
        }
        logger.debug("Checking mandatory missingAttributes:{} ", missingAttributes);
        if (missingAttributes.length() > 0) {
            missingAttributes.replace(missingAttributes.lastIndexOf(","), missingAttributes.length(), "");
        }

        logger.debug("Returning missingAttributes:{} ", missingAttributes);
        return missingAttributes.toString();
    }

    private boolean isExcludedAttribute(List<String> excludeAttributes, String attribute) {
        logger.debug(" Is attribute:{} in excludeAttributeList:{} ", attribute, excludeAttributes);

        if (excludeAttributes == null || excludeAttributes.isEmpty()) {
            return false;
        }

        return excludeAttributes.stream().anyMatch(e -> e.equals(attribute));
    }

    public User parseBirthDateAttribute(User user) {
        if (user.getAttributeObjectValues(BIRTH_DATE) != null) {

            Optional<Object> optionalBithdate = user.getAttributeObjectValues(BIRTH_DATE).stream().findFirst();

            if (!optionalBithdate.isPresent()) {
                return user;
            }

            Date date = mgtUtil.parseStringToDateObj(optionalBithdate.get().toString());
            //parse date with persistenceEntryManager.decodeTime if it is null
            if (date == null) {
                date = persistenceEntryManager.decodeTime(null, optionalBithdate.get().toString());
            }
            user.getCustomAttributes().remove(new CustomObjectAttribute(BIRTH_DATE));
            user.getCustomAttributes().add(new CustomObjectAttribute(BIRTH_DATE, date));
        }
        return user;
    }
}
