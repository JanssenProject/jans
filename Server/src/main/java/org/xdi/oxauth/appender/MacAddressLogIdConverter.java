package org.xdi.oxauth.appender;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.xdi.oxauth.util.ServerUtil;

@Plugin(name = MacAddressLogIdConverter.PLUGIN_NAME, category = "Converter")
@ConverterKeys({ "macAddr" })
/**
 * Append to log mac address of server
 *
 * @author Yuriy Movchan
 * @version Jan 09, 2017
 */
public class MacAddressLogIdConverter extends LogEventPatternConverter {

	public static final String PLUGIN_NAME = "MacAddressLogIdConverter";

	protected MacAddressLogIdConverter(String name, String style) {
	         super(name, style);
	     }

	public static MacAddressLogIdConverter newInstance(String[] options) {
		return new MacAddressLogIdConverter("macAddr", Thread.currentThread().getName());
	}

	@Override
	public void format(LogEvent event, StringBuilder toAppendTo) {
		String macAddress = ServerUtil.getMACAddressOrNull();
		toAppendTo.append(macAddress);

	}

}