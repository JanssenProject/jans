package org.xdi.oxauth.service.job;

import java.lang.annotation.Annotation;

public final class AsyncEvent {

	private final Object targetEvent;

	private final Annotation[] qualifiers;

	public AsyncEvent(Object targetEvent, Annotation... qualifiers) {
		super();
		
		if(targetEvent == null)
			throw new IllegalArgumentException("Target event must not be null");
		
		this.targetEvent = targetEvent;
		this.qualifiers = qualifiers;
	}

	public Object getTargetEvent() {
		return targetEvent;
	}

	public Annotation[] getQualifiers() {
		return qualifiers;
	}
	
}
