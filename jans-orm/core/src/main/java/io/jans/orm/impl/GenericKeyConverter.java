/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import io.jans.orm.exception.KeyConversionException;
import io.jans.orm.impl.model.ParsedKey;
import io.jans.orm.util.StringHelper;

/**
 * DN to Generic key convert
 *
 * @author Yuriy Movchan Date: 05/30/2018
 */
public class GenericKeyConverter {
	
	private boolean useAllRdn;

	public GenericKeyConverter() {
		this.useAllRdn = true;
	}

	public GenericKeyConverter(boolean useAllRdn) {
		this.useAllRdn = useAllRdn;
	}

    public ParsedKey convertToKey(String dn) {
        if (StringHelper.isEmpty(dn)) {
            throw new KeyConversionException("Failed to convert empty DN to Key");
        }

        StringBuilder result = new StringBuilder();
        String[] tokens = dn.split(",");

        String orgInum = null;
        String attributeName = null;
        for (String token : tokens) {
            int pos = token.indexOf("=");
            if (pos == -1) {
                throw new KeyConversionException("Failed to convert empty DN to Key");
            }

            String name = token.substring(0, pos);
            if (attributeName == null) {
            	attributeName = name;
            }
            String value = token.substring(pos + 1, token.length());
            if (StringHelper.equalsIgnoreCase(name, "o")) {
                if (!StringHelper.equalsIgnoreCase(value, "jans")) {
                    orgInum = value;
                }
                continue;
            }

            result.insert(0, "_" + value);
            if (!useAllRdn) {
            	break;
            }
        }

        String key = result.toString();
        if (key.length() == 0) {
            key = "_";
        } else {
            key = key.substring(1);
        }

        return new ParsedKey(key, attributeName, orgInum);
    }

}
