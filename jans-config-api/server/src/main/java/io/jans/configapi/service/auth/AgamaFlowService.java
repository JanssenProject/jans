
package io.jans.configapi.service.auth;

import io.jans.agama.model.Flow;
import io.jans.agama.model.FlowMetadata;
import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.model.configuration.AgamaConfiguration;
import io.jans.configapi.util.AuthUtil;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.reflect.property.Getter;
import io.jans.orm.search.filter.Filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.Serializable;
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
            logger.error("Flow attribute value attribute:{} - attributeValue:{} - datatype:{} ", attribute,
                    attributeValue, objectPropertyMap.get(attribute));

            if (attributeValue == null) {
                errorMsg.append(attribute).append(",");
            }

        } // for
        logger.error("Checking mandatory errorMsg:{} ", errorMsg);

        if (errorMsg.length() > 0) {
            errorMsg.insert(0, "Required feilds missing -> (");
            errorMsg.replace(errorMsg.lastIndexOf(","), errorMsg.length(), "");
            errorMsg.append("). ");
        }

        // Validate non-required fields
        if (checkNonMandatoryFields) {
            String valiateNonMandatoryFieldsMsg = validateNonMandatoryFields(flow, mandatoryAttributes,
                    optionalAttributes);
            logger.error("Checking mandatory valiateNonMandatoryFieldsMsg:{} ", valiateNonMandatoryFieldsMsg);
            if (StringUtils.isNotBlank(valiateNonMandatoryFieldsMsg)) {
                errorMsg.append(valiateNonMandatoryFieldsMsg);
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

        // remove mandatoryAttributes as they are to be excluded
        keys.removeAll(mandatoryAttributes);
        logger.error("After removing mandatoryAttributes:{}, keys:{} ", mandatoryAttributes, keys);

        // remove optionalAttributes as they are to be excluded
        keys.removeAll(optionalAttributes);
        logger.error("After removing optionalAttributes:{}, keys:{} ", optionalAttributes, keys);

        Object attributeValue = null;
        String attributeClass = null;
        for (String key : keys) {

            // Check non-mandatory attributes should be null
            logger.error("Checking value of non-mandatory attribute:{}", key);
            Getter getter = DataUtil.getGetterMethod(Flow.class, key);
            Class<?> dataType = getter.getReturnType();
            attributeValue = getter.get(flow);
            logger.error("Flow attribute key:{}, getter:{}, dataType:{}, attributeValue:{}", key, getter, dataType,
                    attributeValue);

            if (attributeValue != null) {
                attributeClass = attributeValue.getClass().toString();
                logger.error("Flow attribute key:{} - attributeValue:{}, attributeClass:{}, datatype:{}", key,
                        attributeValue, attributeClass, objectPropertyMap.get(key));
                // ignore if empty

                if ("java.lang.String".equalsIgnoreCase(dataType.getName())
                        && StringUtils.isBlank(String.class.cast(attributeValue))) {
                    logger.error("validateNonMandatoryFields - 1");
                    continue;
                } else if ("java.lang.Integer".equalsIgnoreCase(dataType.getName())
                        && (Integer.class.cast(attributeValue) <= 0)) {
                    logger.error("validateNonMandatoryFields - 2");
                    continue;
                } else if ("int".equalsIgnoreCase(dataType.getName()) && (Integer.class.cast(attributeValue) <= 0)) {
                    logger.error("validateNonMandatoryFields - 3");
                    continue;
                } else if ("io.jans.agama.model.FlowMetadata".equalsIgnoreCase(dataType.getName())
                        && !isFlowMetadataPresent(FlowMetadata.class.cast(attributeValue))) {
                    logger.error("validateNonMandatoryFields - 4");
                    continue;
                }

                // report as value should be null
                unwantedAttributes.append(key).append(",");
            }
        } // for
        logger.error("Checking mandatory unwantedAttributes:{} ", unwantedAttributes);

        if (unwantedAttributes.length() > 0) {
            unwantedAttributes.insert(0, "Value of these feilds should be null -> (");
            unwantedAttributes.replace(unwantedAttributes.lastIndexOf(","), unwantedAttributes.length(), "");
            unwantedAttributes.append(").");
        }

        logger.error("Returning unwantedAttributes:{} ", unwantedAttributes);
        return unwantedAttributes.toString();
    }

    private boolean isFlowMetadataPresent(FlowMetadata flowMetadata) {
        logger.error("Validate FlowMetadata flowMetadata:{} ", flowMetadata);

        if (flowMetadata == null) {
            return false;
        }

        else if (StringUtils.isNotBlank(flowMetadata.getFuncName())
                || StringUtils.isNotBlank(flowMetadata.getDisplayName())
                || StringUtils.isNotBlank(flowMetadata.getAuthor())
                || StringUtils.isNotBlank(flowMetadata.getDescription()) || flowMetadata.getInputs() != null
                || (flowMetadata.getTimeout() != null && flowMetadata.getTimeout() > 0)
                || flowMetadata.getProperties() != null || flowMetadata.getTimestamp() > 0) {
            logger.error("FlowMetadata is not null !!!");
            return true;
        }

        return false;
    }

}