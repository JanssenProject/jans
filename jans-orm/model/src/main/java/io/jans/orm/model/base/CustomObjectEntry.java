/**
 * 
 */
package io.jans.orm.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.CustomObjectClass;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.util.StringHelper;

/**
 * @author Sergey Manoylo
 * @version July 11, 2023
 */
@DataEntry
public class CustomObjectEntry extends BaseEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4509559705248888937L;

    @AttributesList(name = "name", value = "values", sortByName = true)
    private List<CustomObjectAttribute> customObjectAttributes = new ArrayList<>();

    @CustomObjectClass
    private String[] customObjectClasses;

    public List<CustomObjectAttribute> getCustomObjectAttributes() {
        return customObjectAttributes;
    }

    public Object getCustomObjectAttributeValue(String attributeName) {
        if (customObjectAttributes == null) {
            return null;
        }
        for (CustomObjectAttribute customObjectAttribute : customObjectAttributes) {
            if (StringHelper.equalsIgnoreCase(attributeName, customObjectAttribute.getName())) {
                return customObjectAttribute.getValue();
            }
        }
        return null;
    }

    public void setCustomObjectAttributes(List<CustomObjectAttribute> customObjectAttributes) {
        this.customObjectAttributes = customObjectAttributes;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }    
}
