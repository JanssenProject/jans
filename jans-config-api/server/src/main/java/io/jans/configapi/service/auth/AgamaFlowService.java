
package io.jans.configapi.service.auth;

import io.jans.agama.model.Flow;
import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.model.configuration.AgamaConfiguration;
import io.jans.configapi.util.AuthUtil;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class AgamaFlowService implements Serializable {

    private static final long serialVersionUID = 7912416439116338984L;

    private static final String AGAMA_BASE = "ou=agama,o=jans";
    public static final String AGAMA_FLOWS_BASE = "ou=flows," + AGAMA_BASE;

    @Inject
    private transient Logger logger;

    @Inject
    transient AuthUtil authUtil;

    @Inject
    transient DataUtil dataUtil;

    @Inject
    private transient PersistenceEntryManager persistenceEntryManager;

    public List<Flow> searchAgamaFlows(String pattern, int sizeLimit) {
        if (logger.isDebugEnabled()) {
            logger.debug("Search Agama Flow with pattern:{}, sizeLimit:{} ", escapeLog(pattern), escapeLog(sizeLimit));
        }

        String[] targetArray = new String[] { pattern };
        Filter searchFilter = Filter.createORFilter(
                Filter.createSubstringFilter(Flow.ATTR_NAMES.QNAME, null, targetArray, null),
                Filter.createSubstringFilter(Flow.ATTR_NAMES.META, null, targetArray, null));

        logger.debug("Agama Flows with matching searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getAgamaFlowDn(null), Flow.class, searchFilter, sizeLimit);
    }

    public List<Flow> searchAgamaFlows(String pattern, int sizeLimit, boolean enabled) {
        if (logger.isDebugEnabled()) {
            logger.debug("Search Agama Flow with pattern:{}, sizeLimit:{}, enabled:{} ", escapeLog(pattern),
                    escapeLog(sizeLimit), escapeLog(enabled));
        }

        String[] targetArray = new String[] { pattern };
        Filter searchFilter = Filter.createORFilter(
                Filter.createSubstringFilter(Flow.ATTR_NAMES.QNAME, null, targetArray, null),
                Filter.createSubstringFilter(Flow.ATTR_NAMES.META, null, targetArray, null),
                Filter.createEqualityFilter("jansEnabled", enabled));

        logger.debug("Agama Flows with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getAgamaFlowDn(null), Flow.class, searchFilter, sizeLimit);

    }

    public List<Flow> getAllAgamaFlows(int sizeLimit) {
        return persistenceEntryManager.findEntries(getAgamaFlowDn(null), Flow.class, null, sizeLimit);
    }

    public List<Flow> getAllFlows() {
        return persistenceEntryManager.findEntries(getAgamaFlowDn(null), Flow.class, null);
    }

    public Flow getFlowByName(String flowName) {
        List<Flow> flows = persistenceEntryManager.findEntries(getAgamaFlowDn(flowName), Flow.class,
                Filter.createEqualityFilter(Flow.ATTR_NAMES.QNAME, flowName), 1);
        logger.debug("Agama Flow with flowName:{} flows:{}", flowName, flows);
        if (!flows.isEmpty()) {
            return flows.get(0);
        }
        return null;
    }

    public Flow getFlowByDn(String dn) {
        try {
            return persistenceEntryManager.find(Flow.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    public void addAgamaFlow(Flow flow) {
        logger.debug("Added Agama Flow:{}", flow);
        flow.setBaseDn(getAgamaFlowDn(flow.getQname()));
        persistenceEntryManager.persist(flow);
    }

    public void updateFlow(Flow flow) {
        logger.debug("Update Agama Flow:{}", flow);
        persistenceEntryManager.merge(flow);
    }

    public void removeAgamaFlow(Flow flow) {
        logger.debug("Remove Agama Flow:{}", flow);
        persistenceEntryManager.removeRecursively(flow.getDn(), Flow.class);
    }

    public String getAgamaFlowDn(String flowName) {
        logger.debug("Agama flowName:{}", flowName);
        if (StringUtils.isBlank(flowName)) {
            return AGAMA_FLOWS_BASE;
        }
        return String.format(String.format("%s=%s,%s", Flow.ATTR_NAMES.QNAME, flowName, AGAMA_FLOWS_BASE));
    }

    public AgamaConfiguration getAgamaConfiguration() {
        return authUtil.getAgamaConfiguration();
    }

    public String validateFlowFields(Flow flow, boolean checkNonMandatoryFields)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return validateFlowFields(flow, getAgamaConfiguration().getMandatoryAttributes(),
                getAgamaConfiguration().getOptionalAttributes(), checkNonMandatoryFields);
    }

    public String validateFlowFields(Flow flow, List<String> mandatoryAttributes, List<String> optionalAttributes,
            boolean checkNonMandatoryFields)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        logger.error(
                "Validate Flow Fields flow - flow:{}, mandatoryAttributes:{} , optionalAttributes:{}, checkNonMandatoryFields:{}",
                flow, mandatoryAttributes, optionalAttributes, checkNonMandatoryFields);

        StringBuilder errorMsg = new StringBuilder();

        if (mandatoryAttributes == null || mandatoryAttributes.isEmpty()) {
            return errorMsg.toString();
        }

        // List<Field> allFields = authUtil.getAllFields(flow.getClass());
        // logger.error("All Flow fields :{} ", allFields);
        Map<String, String> objectPropertyMap = DataUtil.getFieldTypeMap(flow.getClass());
        logger.error("Flow class objectPropertyMap:{} ", objectPropertyMap);
        if (objectPropertyMap == null || objectPropertyMap.isEmpty()) {
            return errorMsg.toString();
        }

        Set<String> keys = objectPropertyMap.keySet();
        logger.error("Flow class fields:{} ", keys);

        Object attributeValue = null;
        for (String attribute : mandatoryAttributes) {
            logger.error("Flow class objectPropertyMap:{} conatins attribute:{} ? :{} ", objectPropertyMap, attribute,
                    keys.contains(attribute));

            if (keys.contains(attribute)) {
                logger.error("Checking value of attribute:{}", attribute);
                attributeValue = BeanUtils.getProperty(flow, attribute);
                logger.error("Flow attribute:{} - attributeValue:{} ", attribute, attributeValue);
            }
            logger.error("Flow attribute value attribute:{} - attributeValue:{} ", attribute, attributeValue);

            logger.error("Flow class attribute:{} datatype:{} ", attribute, objectPropertyMap.get(attribute));

            if (attributeValue == null) {
                errorMsg.append(attribute).append(",");
            }

        } // for
        logger.error("Checking mandatory errorMsg:{} ", errorMsg);

        if (errorMsg.length() > 0) {
            errorMsg.insert(0, "Required feilds missing -> ");
            errorMsg.replace(errorMsg.lastIndexOf(","), errorMsg.length(), "");
        }

        // Validate non-required fields
        if (checkNonMandatoryFields) {
            String valiateNonMandatoryFieldsMsg = validateNonMandatoryFields(flow, mandatoryAttributes,
                    optionalAttributes);
            logger.error("Checking mandatory valiateNonMandatoryFieldsMsg:{} ", valiateNonMandatoryFieldsMsg);
            if (StringUtils.isNotBlank(valiateNonMandatoryFieldsMsg)) {
                errorMsg.append("\n").append(valiateNonMandatoryFieldsMsg);
            }
        }

        logger.error("Returning missingAttributes:{} ", errorMsg);
        return errorMsg.toString();
    }

    public String validateNonMandatoryFields(Flow flow, List<String> mandatoryAttributes,
            List<String> optionalAttributes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        logger.error("Validate Flow for Non Mandatory Fields - flow:{}, mandatoryAttributes:{}, optionalAttributes:{}",
                flow, mandatoryAttributes, optionalAttributes);

        StringBuilder unwantedAttributes = new StringBuilder();

        Map<String, String> objectPropertyMap = DataUtil.getFieldTypeMap(flow.getClass());
        logger.error("Flow class objectPropertyMap:{} ", objectPropertyMap);
        if (objectPropertyMap == null || objectPropertyMap.isEmpty()) {
            return unwantedAttributes.toString();
        }

        Set<String> keys = objectPropertyMap.keySet();
        logger.error("Flow class fields:{} ", keys);

        keys.removeAll(mandatoryAttributes);
        logger.error("After removing mandatoryAttributes:{}, keys:{} ", mandatoryAttributes, keys);

        Object attributeValue = null;
        for (String key : keys) {
            // Check non-mandatory attributes should be null
            logger.error("Checking value of non-mandatory attribute:{}", key);
            attributeValue = BeanUtils.getProperty(flow, key);
            logger.error("Flow attribute key:{} - attributeValue:{} ", key, attributeValue);

            // check if the attribute is to be excluded
            logger.error("Check id flow attribute key:{} is in optionalAttributes:{} and is to be excluded:{} ", key,
                    optionalAttributes, optionalAttributes.contains(key));
            if (optionalAttributes.contains(key)) {
                logger.error("Check id flow attribute key:{} is to be excluded:{}!!! ", optionalAttributes,
                        optionalAttributes.contains(key));
                continue;
            }

            if (attributeValue != null) {
                unwantedAttributes.append(key).append(",");
                /*
                 * if (("String".equalsIgnoreCase(objectPropertyMap.get(key))) &&
                 * (StringUtils.isNotBlank((String) attributeValue))) {
                 * unwantedAttributes.append(key).append(","); } else {
                 * unwantedAttributes.append(key).append(","); }
                 */
            }
        } // for
        logger.error("Checking mandatory unwantedAttributes:{} ", unwantedAttributes);

        if (unwantedAttributes.length() > 0) {
            unwantedAttributes.insert(0, "Value of these feilds should be null -> ");
            unwantedAttributes.replace(unwantedAttributes.lastIndexOf(","), unwantedAttributes.length(), "");
        }

        logger.error("Returning unwantedAttributes:{} ", unwantedAttributes);
        return unwantedAttributes.toString();
    }

}