/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.cdi.event;

import jakarta.enterprise.util.AnnotationLiteral;

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
