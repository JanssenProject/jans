/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Base metric data class
 *
 * @author Yuriy Movchan Date: 07/28/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricData implements Serializable {

    private static final long serialVersionUID = -2520744744010853187L;

}
