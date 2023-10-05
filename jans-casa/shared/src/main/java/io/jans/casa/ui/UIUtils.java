package io.jans.casa.ui;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.util.Clients;
import static org.zkoss.zk.ui.util.Clients.NOTIFICATION_TYPE_ERROR;
import static org.zkoss.zk.ui.util.Clients.NOTIFICATION_TYPE_WARNING;
import static org.zkoss.zk.ui.util.Clients.NOTIFICATION_TYPE_INFO;

import java.util.stream.Stream;

/**
 * Utility class to show auto-dismiss notification success/error ZK notification boxes.
 * @author jgomer
 */
public final class UIUtils {

    /**
     * Duration (in ms) used for auto-dismiss in successful messages.
     */
    public static final int FEEDBACK_DELAY_SUCC = 3000;

    /**
     * Duration (in ms) used for auto-dismiss in error messages.
     */
    public static final int FEEDBACK_DELAY_ERR = 5000;

    private UIUtils() { }

    /**
     * Shows a notification box with a generic success or error message.
     * @param success Whether to use the success or error message
     */
    public static void showMessageUI(boolean success) {
        showMessageUI(success, Labels.getLabel(success ? "general.operation_completed" : "general.error.general"));
    }

    /**
     * Shows a notification box with the supplied success or error message, in the middle of the screen.
     * @param success Whether to use the success or error message
     * @param msg Message to show inside the box
     */
    public static void showMessageUI(boolean success, String msg) {
        showMessageUI(success, msg, "middle_center");
    }

    /**
     * Shows a notification box with the supplied success or error message, in the position passed in the parameter.
     * @param success Whether to use the success or error message
     * @param msg Message to show inside the box
     * @param position A string indicating the position (for a list of possible values see "useful Java utilities" in
     *                ZK developer's reference manual)
     */
    public static void showMessageUI(boolean success, String msg, String position) {
        //Calls the showNotification javascript function supplying suitable params, note that after new UI design was
        //introduced, position is ignored...
        showMessageUI(success ? NOTIFICATION_TYPE_INFO : NOTIFICATION_TYPE_ERROR, msg);
    }

    public static void showMessageUI(String notificationType, String msg) {

        String type = Stream.of(NOTIFICATION_TYPE_ERROR, NOTIFICATION_TYPE_INFO, NOTIFICATION_TYPE_WARNING)
                .filter(t -> t.equals(notificationType)).findFirst().orElse(NOTIFICATION_TYPE_WARNING);
        Clients.response(new AuInvoke("showAlert", msg, type, type.equals(NOTIFICATION_TYPE_ERROR) ? FEEDBACK_DELAY_ERR : FEEDBACK_DELAY_SUCC));

    }

}
