/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.statistic;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * base event statistic data class
 * 
 * @author Yuriy Movchan Date: 07/28/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NumericEventData extends EventData {

	private static final long serialVersionUID = -2322501012136295255L;

	private int value;

	public NumericEventData() {
	}

	public NumericEventData(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

}
