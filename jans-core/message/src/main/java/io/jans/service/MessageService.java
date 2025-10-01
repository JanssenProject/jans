/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service;

import io.jans.service.message.provider.MessageProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides operations with messages
 *
 * @author Yuriy Movchan Date: 2023/12/03
 */
@ApplicationScoped
public class MessageService extends BaseMessageService {

	@Inject
	private MessageProvider messageProvider;

	@Override
	protected MessageProvider getMessageProvider() {
		return messageProvider;
	}

}
