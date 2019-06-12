package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/06/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmptyParams implements IParams {
}
