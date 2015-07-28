/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.statistic;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;


/**
 * base event statistic data class
 * 
 * @author Yuriy Movchan Date: 07/28/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventData implements Serializable {

	private static final long serialVersionUID = -2520744744010853187L;

}
