package io.jans.configapi.plugin.mgt.service;

import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.model.GluuStatus;
import io.jans.model.attribute.AttributeValidation;
import io.jans.as.common.model.common.User;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.core.service.ConfigUserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.mgt.model.user.UserPatchRequest;
import io.jans.configapi.plugin.mgt.util.MgtUtil;
import io.jans.configapi.util.AuthUtil;
import io.jans.configapi.service.auth.AttributeService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.model.JansAttribute;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.jans.as.model.util.Util.escapeLog;

@ApplicationScoped
@Named("userMgmtSrv")
public class UserMgmtService {

    @Inject
    private Logger logger;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AttributeService attributeService;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    AuthUtil authUtil;

    @Inject
    MgtUtil mgtUtil;

    @Inject
    ConfigUserService userService;

    private static final String BIRTH_DATE = "birthdate";

    public String getPeopleBaseDn() {
        return userService.getPeopleBaseDn();
    }

    public PagedResult<User> searchUsers(SearchRequest searchRequest) {
        if (logger.isInfoEnabled()) {
            logger.info("Search Users with searchRequest:{}, getPeopleBaseDn():{}", escapeLog(searchRequest),
                    getPeopleBaseDn());
        }

        boolean useLowercaseFilter = configurationService.isLowercaseFilter(userService.getPeopleBaseDn());
        logger.info("For searching user user useLowercaseFilter?:{}", useLowercaseFilter);

        Filter displayNameFilter, descriptionFilter, mailFilter, uidFilter, inumFilter, givenNameFilter,
                middleNameFilter, nicknameFilter, snFilter, searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                logger.info("For searching user - assertionValue:{}", assertionValue);
                assertionValue = StringHelper.toLowerCase(assertionValue);
                String[] targetArray = new String[] { assertionValue };
                logger.info("For searching user - targetArray?:{}", targetArray);

                if (useLowercaseFilter) {
                    displayNameFilter = Filter.createSubstringFilter(
                            Filter.createLowercaseFilter(AttributeConstants.DISPLAY_NAME), null, targetArray, null);
                    descriptionFilter = Filter.createSubstringFilter(
                            Filter.createLowercaseFilter(AttributeConstants.DESCRIPTION), null, targetArray, null);
                    mailFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter(AttributeConstants.MAIL),
                            null, targetArray, null);
                    givenNameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("givenName"), null,
                            targetArray, null);
                    middleNameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("middleName"), null,
                            targetArray, null);
                    nicknameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("nickname"), null,
                            targetArray, null);
                    snFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("sn"), null, targetArray,
                            null);
                    uidFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, targetArray,
                            null);
                } else {
                    displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                            null);
                    descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                            null);
                    mailFilter = Filter.createSubstringFilter(AttributeConstants.MAIL, null, targetArray, null);
                    givenNameFilter = Filter.createSubstringFilter("givenName", null, targetArray, null);
                    middleNameFilter = Filter.createSubstringFilter("middleName", null, targetArray, null);
                    nicknameFilter = Filter.createSubstringFilter("nickname", null, targetArray, null);
                    snFilter = Filter.createSubstringFilter("sn", null, targetArray, null);
                    uidFilter = Filter.createSubstringFilter("uid", null, targetArray, null);
                }

                inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, mailFilter, uidFilter,
                        givenNameFilter, middleNameFilter, nicknameFilter, snFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }
        logger.info("Users searchFilter:{}", searchFilter);
        PagedResult<User> pagedResult = persistenceEntryManager.findPagedEntries(userService.getPeopleBaseDn(),
                User.class, searchFilter, null, searchRequest.getSortBy(),
                SortOrder.getByValue(searchRequest.getSortOrder()), searchRequest.getStartIndex(),
                searchRequest.getCount(), searchRequest.getMaxCount());

        // remove inactive claims
        List<User> users = this.verifyCustomAttributes(pagedResult.getEntries());
        pagedResult.setEntries(users);
        return pagedResult;

    }

    public List<User> getUserByName(String name) {
        logger.info("Get user by name:{} ", name);
        String[] targetArray = new String[] { name };
        Filter nameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, targetArray, null);

        List<User> users = persistenceEntryManager.findEntries(userService.getPeopleBaseDn(), User.class, nameFilter);
        logger.trace("Asset by name:{} are users:{}", name, users);
        return users;
    }

    public List<User> getUserByEmail(String email) {
        logger.info("Get user by email:{} ", email);
        String[] targetArray = new String[] { email };
        Filter emailFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("mail"), null, targetArray, null);

        List<User> users = persistenceEntryManager.findEntries(userService.getPeopleBaseDn(), User.class, emailFilter);
        logger.trace("Asset by email:{} are users:{}", email, users);
        return users;
    }

    public void removeUser(User user) {
        persistenceEntryManager.removeRecursively(user.getDn(), User.class);
    }

    public User patchUser(String inum, UserPatchRequest userPatchRequest) throws JsonPatchException, IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Details to patch user  inum:{}, UserPatchRequest:{} ", escapeLog(inum),
                    escapeLog(userPatchRequest));
        }
        if (StringHelper.isEmpty(inum)) {
            return null;
        }

        User user = userService.getUserByInum(inum);
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
        ignoreCustomObjectClassesForNonLDAP(user);
        user = userService.updateUser(user);

        // remove inactive claims
        if (user != null) {
            List<User> users = new ArrayList<>();
            users.add(user);
            users = this.verifyCustomAttributes(users);
            user = users.get(0);
        }

        logger.info("User after patch user:{}", user);
        return user;

    }

    public User getUserBasedOnInum(String inum) {
        User result = null;
        try {
            result = userService.getUserByInum(inum);

            // remove inactive claims
            if (result != null) {
                List<User> users = new ArrayList<>();
                users.add(result);
                users = this.verifyCustomAttributes(users);
                result = users.get(0);
            }

        } catch (Exception ex) {
            logger.error("Failed to load user entry", ex);
        }
        return result;
    }

    private User updateCustomAttributes(User user, List<CustomObjectAttribute> customAttributes) {
        logger.info("Custom Attributes to update for - user:{} ", user);

        if (customAttributes == null || customAttributes.isEmpty()) {
            return user;
        }
        //validate custom attribute validation
        validateAttributes(customAttributes);
        
        for (CustomObjectAttribute attribute : customAttributes) {
            CustomObjectAttribute existingAttribute = userService.getCustomAttribute(user, attribute.getName());
            logger.debug("Existing CustomAttributes with existingAttribute.getName():{} ", existingAttribute.getName());

            // add
            if (existingAttribute == null) {
                boolean result = userService.addUserAttribute(user, attribute.getName(), attribute.getValues(),
                        attribute.isMultiValued());
                logger.debug("Result of adding CustomAttributes attribute.getName():{} , result:{} ", attribute.getName(), result);
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
        logger.info("Users:{} after excluding attribute:{} ", users, commaSeparatedString);

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

        logger.info("Attributes map:{} to be excluded ", map);
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

            // check if to be excluded
            if (isExcludedAttribute(excludeAttributes, attribute)) {
                logger.debug("Not checking if the attribute:{} is missing as it's in excludeAttributes:{}", attribute,
                        excludeAttributes);
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

        logger.info("Returning missingAttributes:{} ", missingAttributes);
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
            // parse date with persistenceEntryManager.decodeTime if it is null
            if (date == null) {
                date = persistenceEntryManager.decodeTime(null, optionalBithdate.get().toString());
            }
            user.getCustomAttributes().remove(new CustomObjectAttribute(BIRTH_DATE));
            user.getCustomAttributes().add(new CustomObjectAttribute(BIRTH_DATE, date));
        }
        return user;
    }

    public User ignoreCustomObjectClassesForNonLDAP(User user) {
        String persistenceType = configurationService.getPersistenceType();
        logger.debug("persistenceType: {}, isLDAP?:{}, user.getCustomObjectClasses():{}", persistenceType, isLDAP(),
                user.getCustomObjectClasses());

        if (!isLDAP()) {
            logger.info(
                    "Setting CustomObjectClasses :{} to null as its used only for LDAP and current persistenceType is {} ",
                    user.getCustomObjectClasses(), persistenceType);
            user.setCustomObjectClasses(null);
        }
        logger.debug("Final user.getCustomObjectClasses():{} ", user.getCustomObjectClasses());
        return user;
    }

    public boolean isLDAP() {
        boolean isLDAP = false;
        String persistenceType = getPersistenceType();
        logger.debug("persistenceType: {}", persistenceType);
        if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceType)) {
            isLDAP = true;
        }
        return isLDAP;
    }

    public String getPersistenceType() {
        return configurationService.getPersistenceType();
    }

    public User addUser(User user, boolean active) {
        logger.info("\n Creating user:{}, active:{}", user, active);
        user = userService.addUser(user, active);
        logger.info("New user:{}\n", user);
        // remove inactive claims
        if (user != null) {
            List<User> users = new ArrayList<>();
            users.add(user);
            users = this.verifyCustomAttributes(users);
            if (users != null && !users.isEmpty()) {
                user = users.get(0);
            }
        }
        return user;
    }

    public User updateUser(User user) {
        logger.info("\n Updating user:{}", user);
        user = userService.updateUser(user);
        logger.info("Updated user:{} \n", user);
        // remove inactive claims
        if (user != null) {
            List<User> users = new ArrayList<>();
            users.add(user);
            users = this.verifyCustomAttributes(users);
            if (users != null && !users.isEmpty()) {
                user = users.get(0);
            }
        }
        return user;
    }

    public List<User> verifyCustomAttributes(List<User> users) {
        logger.info("Verify CustomAttributes for users: {}", users);
        if (users == null || users.isEmpty()) {
            return users;
        }
        for (User user : users) {
            List<CustomObjectAttribute> customAttributes = user.getCustomAttributes();
            // remove inactive attributes
            removeInActiveCustomAttribute(customAttributes);
        }
        return users;
    }

    public List<CustomObjectAttribute> removeInActiveCustomAttribute(List<CustomObjectAttribute> customAttributes) {

        if (customAttributes == null || customAttributes.isEmpty()) {
            return customAttributes;
        }

        // remove attribute that are not active
        for (Iterator<CustomObjectAttribute> it = customAttributes.iterator(); it.hasNext();) {
            String attributeName = it.next().getName();
            logger.debug("Verify status of attributeName: {}", attributeName);
            List<JansAttribute> attList = findAttributeByName(attributeName);
            logger.debug("attributeName:{} data is attList: {}", attributeName, attList);

            if (CollectionUtils.isNotEmpty(attList)
                    && !GluuStatus.ACTIVE.getValue().equalsIgnoreCase(attList.get(0).getStatus().getValue())) {
                logger.info("\n\n*** Removing attribute as it is not active attributeName: {} , status:{} ***\n",
                        attributeName, attList.get(0).getStatus().getValue());
                it.remove();
            }
        }
        return customAttributes;
    }

    public List<JansAttribute> findAttributeByName(String name) {
        return persistenceEntryManager.findEntries(getDnForAttribute(null), JansAttribute.class,
                Filter.createEqualityFilter(AttributeConstants.JANS_ATTR_NAME, name));
    }

    private String getDnForAttribute(String inum) {
        String attributesDn = staticConfiguration.getBaseDn().getAttributes();
        if (StringHelper.isEmpty(inum)) {
            return attributesDn;
        }
        return String.format("inum=%s,%s", inum, attributesDn);
    }

    public void validateAttributes(List<CustomObjectAttribute> customAttributes) {
        if (customAttributes == null || customAttributes.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (CustomObjectAttribute customObjectAttribute : customAttributes) {
            logger.info("customObjectAttribute:{}, customObjectAttribute.getName():{}", customObjectAttribute,
                    customObjectAttribute.getName());
            JansAttribute attribute = attributeService.getAttributeByName(customObjectAttribute.getName());
            AttributeValidation validation = null;
            if (attribute != null) {
                validation = attribute.getAttributeValidation();
            }
            logger.info("customObjectAttribute.getName():{}, validation:{}", customObjectAttribute.getName(),
                    validation);
            if (validation != null) {
                String errorMsg = validateCustomAttributes(customObjectAttribute, validation);
                logger.info("customObjectAttribute.getName():{}, errorMsg:{}", customObjectAttribute.getName(),
                        errorMsg);
                if (StringUtils.isNotBlank(errorMsg)) {
                    sb.append(errorMsg);
                }
            }
        }

        if (StringUtils.isNotBlank(sb.toString())) {
            logger.error("Attribute validation failed with error msg:{} \n", sb);
            throw new InvalidAttributeException(sb.toString());
        }

    }

    private String validateCustomAttributes(CustomObjectAttribute customObjectAttribute,
            AttributeValidation attributeValidation) {
        logger.info("Validate attributeValidation:{}", attributeValidation);
        
        StringBuilder sb = new StringBuilder();
        if (customObjectAttribute == null || attributeValidation == null) {
            return sb.toString();
        }

        String attributeName = customObjectAttribute.getName();
        try {
            String attributeValue = String.valueOf(customObjectAttribute.getValue());
            if (StringUtils.isBlank(attributeValue)) {
                return sb.toString();

            }
            Integer minvalue = attributeValidation.getMinLength();
            Integer maxValue = attributeValidation.getMaxLength();
            String regexpValue = attributeValidation.getRegexp();
            logger.info(
                    "Validate attributeValue.length():{}, attributeValidation.getMinLength():{}, attributeValidation.getMaxLength():{}, attributeValidation.getRegexp():{}",
                    attributeValue.length(), attributeValidation.getMinLength(),
                    attributeValidation.getMaxLength(), attributeValidation.getRegexp());

            // minvalue Validation
            if (minvalue != null && attributeValue.length() < minvalue) {
                sb.append(",must be at least " + minvalue + " characters");
            }

            // maxValue Validation
            if (maxValue != null && attributeValue.length() > maxValue) {
                sb.append(",must be less than " + maxValue + " characters");
            }

            // regexpValue
            if (StringUtils.isNotBlank(regexpValue)) {
                Pattern pattern = Pattern.compile(regexpValue);
                Matcher matcher = pattern.matcher(attributeValue);
                if (!matcher.matches()) {
                    sb.append(",must match (" + regexpValue + ") pattern");
                }
            }
        } catch (Exception ex) {
            logger.error("Error while validating attributeName:{}", attributeName);
        }
        logger.info("Validate reuslt for attributeName:{} is sb :{} ", attributeName, sb);

        if (StringUtils.isNotBlank(sb.toString())) {
            sb.insert(0, "'" + attributeName + "' -> ");
            sb.append("  ");
        }
        return sb.toString();
    }

}
