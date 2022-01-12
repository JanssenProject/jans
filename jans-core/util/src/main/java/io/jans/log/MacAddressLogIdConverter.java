/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.log;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import io.jans.net.InetAddressUtility;

/**
 * Append to log mac address of server
 *
 * @author Yuriy Movchan
 * @version Jan 09, 2017
 */
@Plugin(name = MacAddressLogIdConverter.PLUGIN_NAME, category = "Converter")
@ConverterKeys({ "macAddr" })
public class MacAddressLogIdConverter extends LogEventPatternConverter {

    public static final String PLUGIN_NAME = "MacAddressLogIdConverter";

    protected MacAddressLogIdConverter(final String name, final String style) {
        super(name, style);
    }

    public static MacAddressLogIdConverter newInstance(final String[] options) {
        return new MacAddressLogIdConverter("macAddr", Thread.currentThread().getName());
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        String macAddress = InetAddressUtility.getMACAddressOrNull();
        toAppendTo.append(macAddress);

    }

}
