/**
 * 
 */
package org.gluu.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.service.custom.script.AbstractCustomScriptService;
import org.gluu.util.OxConstants;
import org.gluu.util.StringHelper;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationScoped
@Named
public class ScriptService extends AbstractCustomScriptService {

	@Inject
	private OrganizationService organizationService;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	private static final long serialVersionUID = -5283102477313448031L;

	public CustomScript getScriptByInum(String inum) {
		CustomScript result = null;
		try {
			result = persistenceEntryManager.find(CustomScript.class, buildDn(inum));
		} catch (Exception ex) {
		}
		return result;
	}

	public List<CustomScript> findCustomAuthScripts(String pattern, int sizeLimit) {
		String[] targetArray = new String[] { pattern };
		Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.DESCRIPTION, null, targetArray, null);
		Filter scriptTypeFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE,
				CustomScriptType.PERSON_AUTHENTICATION);
		Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.DISPLAY_NAME, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(descriptionFilter, displayNameFilter);
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class,
				Filter.createANDFilter(searchFilter, scriptTypeFilter), sizeLimit);
	}

	public List<CustomScript> findCustomAuthScripts(int sizeLimit) {
		Filter searchFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE,
				CustomScriptType.PERSON_AUTHENTICATION.getValue());
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class, searchFilter,
				sizeLimit);
	}

	public List<CustomScript> findOtherCustomScripts(String pattern, int sizeLimit) {
		String[] targetArray = new String[] { pattern };
		Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.DESCRIPTION, null, targetArray, null);
		Filter scriptTypeFilter = Filter.createNOTFilter(
				Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, CustomScriptType.PERSON_AUTHENTICATION));
		Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.DISPLAY_NAME, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(descriptionFilter, displayNameFilter);
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class,
				Filter.createANDFilter(searchFilter, scriptTypeFilter), sizeLimit);
	}

	public List<CustomScript> findScriptByType(CustomScriptType type, int sizeLimit) {
		Filter searchFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, type);
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class, searchFilter,
				sizeLimit);
	}

	public List<CustomScript> findScriptByType(CustomScriptType type) {
		Filter searchFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, type);
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class, searchFilter, null);
	}

	public List<CustomScript> findScriptByPatternAndType(String pattern, CustomScriptType type, int sizeLimit) {
		String[] targetArray = new String[] { pattern };
		Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.DESCRIPTION, null, targetArray, null);
		Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.DISPLAY_NAME, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(descriptionFilter, displayNameFilter);
		Filter typeFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, type);
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class,
				Filter.createANDFilter(searchFilter, typeFilter), sizeLimit);
	}

	public List<CustomScript> findScriptByPatternAndType(String pattern, CustomScriptType type) {
		String[] targetArray = new String[] { pattern };
		Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.DESCRIPTION, null, targetArray, null);
		Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.DISPLAY_NAME, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(descriptionFilter, displayNameFilter);
		Filter typeFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, type);
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class,
				Filter.createANDFilter(searchFilter, typeFilter), null);
	}

	public List<CustomScript> findOtherCustomScripts(int sizeLimit) {
		Filter searchFilter = Filter.createNOTFilter(
				Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, CustomScriptType.PERSON_AUTHENTICATION));
		return persistenceEntryManager.findEntries(getDnForCustomScript(null), CustomScript.class, searchFilter,
				sizeLimit);
	}

	public String getDnForCustomScript(String inum) {
		String orgDn = organizationService.getDnForOrganization(null);
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=scripts,%s", orgDn);
		}
		return String.format("inum=%s,ou=scripts,%s", inum, orgDn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.service.custom.script.AbstractCustomScriptService#baseDn()
	 */
	@Override
	public String baseDn() {
		return String.format("ou=scripts,%s", organizationService.getDnForOrganization(null));
	}

}
