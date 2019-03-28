/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.metric;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Base metric data class
 *
 * @author Yuriy Movchan Date: 07/28/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricData implements Serializable {

    private static final long serialVersionUID = -2520744744010853187L;

}
