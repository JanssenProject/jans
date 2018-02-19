package org.gluu.jsf2.service;

import java.io.Serializable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

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
