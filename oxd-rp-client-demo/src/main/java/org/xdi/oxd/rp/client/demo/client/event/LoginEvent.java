package org.xdi.oxd.rp.client.demo.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.xdi.oxd.rp.client.demo.shared.TokenDetails;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class LoginEvent extends GwtEvent<LoginEvent.Handler> {

    /**
     * Handler of event.
     */
    public static interface Handler extends EventHandler {

        /**
         * Handles state based on event object.
         *
         * @param p_event event object
         */
        void update(LoginEvent p_event);
    }

    /**
     * Type of event
     */
    public static final Type<Handler> TYPE = new Type<Handler>();

    private TokenDetails tokenDetails = null;

    public LoginEvent(TokenDetails tokenDetails) {
        this.tokenDetails = tokenDetails;
    }


    public TokenDetails getTokenDetails() {
        return tokenDetails;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    /**
     * Dispatches handling to handler object.
     *
     * @param handler handler object
     */
    @Override
    protected void dispatch(Handler handler) {
        handler.update(this);
    }
}
