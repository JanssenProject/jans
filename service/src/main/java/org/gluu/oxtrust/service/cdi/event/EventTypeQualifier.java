package org.gluu.oxtrust.service.cdi.event;

import javax.enterprise.util.AnnotationLiteral;

public class EventTypeQualifier extends AnnotationLiteral<EventType> implements EventType {

	private static final long serialVersionUID = 1L;

	private Events value;

	public EventTypeQualifier(Events value) {
		this.value = value;
	}

	public Events value() {
		return value;
	}
}
