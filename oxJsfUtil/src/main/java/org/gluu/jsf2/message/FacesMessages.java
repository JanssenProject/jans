package org.gluu.jsf2.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.xdi.service.el.ExpressionEvaluator;

/**
 * @author Yuriy Movchan
 * @version 05/06/2017
 */
@RequestScoped
public class FacesMessages implements Serializable {

    private static final long serialVersionUID = -6408439483194578659L;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private ExpressionEvaluator expressionEvaluator;
    
    private HashMap<String, FacesMessage> messages;

    @PostConstruct
    private void init() {
        this.messages = new HashMap<String, FacesMessage>();
    }

    public void add(Severity severity, String message) {
        add(null, severity, message);
    }

    public void add(String clientId, Severity severity, String message) {
        if (facesContext == null) {
            return;
        }

        String evaluatedMessage = evalAsString(message);
        FacesMessage facesMessage = new FacesMessage(severity, evaluatedMessage, evaluatedMessage);
        facesContext.addMessage(clientId, facesMessage);
        
        messages.put(clientId, facesMessage);
        setKeepMessages();
    }

    public void add(Severity severity, String message, Object ... params) {
        String fomrattedMessage = String.format(message, params);

        add(severity, fomrattedMessage);
        setKeepMessages();
    }

    public void setKeepMessages() {
        if (externalContext == null) {
            return;
        }

        externalContext.getFlash().setKeepMessages(true);
    }

    public void clear() {
        messages.clear();

        if (facesContext == null) {
            return;
        }

        Iterator<FacesMessage> messages = facesContext.getMessages();
        while (messages.hasNext()) {
            messages.next();
            messages.remove();
        }
    }

    public String evalAsString(String expression) {
        if (facesContext == null) {
            return expression;
        }

        ExpressionFactory expressionFactory = facesContext.getApplication().getExpressionFactory();
        ELContext elContext = facesContext.getELContext();
        ValueExpression valueExpression = expressionFactory.createValueExpression(elContext, expression, String.class);
        String result = (String) valueExpression.getValue(elContext);

        return result;
    }

    public String evalResourceAsString(String resource) {
        // Get resource message
        String resourceMessage = evalAsString(resource);

        // Evaluate resource message
        String message = evalAsString(resourceMessage);

        return message;
    }

    public String evalAsString(String expression, Map<String, Object> parameters) {
        return expressionEvaluator.evaluateValueExpression(expression, String.class, parameters);
    }

    public HashMap<String, FacesMessage> getMessages() {
        return messages;
    }

}
