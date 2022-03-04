/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.service;

import java.io.Serializable;

import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ConversationScoped
public class ConversationService implements Serializable {

    private static final long serialVersionUID = -7432197667275722872L;

    private static final long CONVERSATION_TIMEOUT = 30 * 60 * 1000L;

    @Inject
    private Conversation conversation;

    public void initConversation() {
        if (!FacesContext.getCurrentInstance().isPostback() && conversation.isTransient()) {
            conversation.begin();
            conversation.setTimeout(CONVERSATION_TIMEOUT);
        }
    }

    public void endConversation() {
        if (!conversation.isTransient()) {
            conversation.end();
        }
    }

}
