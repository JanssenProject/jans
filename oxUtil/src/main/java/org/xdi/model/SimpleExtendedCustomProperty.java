package org.xdi.model;

public class SimpleExtendedCustomProperty extends SimpleCustomProperty {

	private static final long serialVersionUID = 7413216569115979793L;

	private boolean hideValue;
	
	public SimpleExtendedCustomProperty() {
		super();
		
	}
	
	public SimpleExtendedCustomProperty(String value1, String value2) {
		super(value1, value2);
	}
	
	public SimpleExtendedCustomProperty(String value1, String value2, boolean hideValue) {
		super(value1, value2);
		this.hideValue = hideValue;
	}
	
	public SimpleExtendedCustomProperty(String p_value1, String p_value2, String p_description) {
		super(p_value1, p_value2, p_description);
    }
	
	public SimpleExtendedCustomProperty(String p_value1, String p_value2, String p_description, boolean p_hideValue) {
		super(p_value1, p_value2, p_description);
		this.hideValue = p_hideValue;
    }

	public boolean getHideValue() {
		return hideValue;
	}

	public void setHideValue(boolean hideValue) {
		this.hideValue = hideValue;
	}
	
}
