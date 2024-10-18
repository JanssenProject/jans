package io.jans.entry;

public enum Transports {
	USB("usb"), NFC("nfc"), BLE("ble"), INTERNAL("internal"), HYBRID("hybrid");

	private final String transport;

	private Transports(String t) {
		this.transport = t;
	}

	public String getValue() {
		return transport;
	}

}
