package io.jans.casa.ui.model;

import io.jans.casa.core.pojo.User;

/**
 * @author jgomer
 */
public class PersonSearchMatch extends User {

    private boolean alreadyReset;
    //Used to associate with checkboxes of the grid
    private boolean checked;

    public boolean isAlreadyReset() {
        return alreadyReset;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void setAlreadyReset(boolean alreadyReset) {
        this.alreadyReset = alreadyReset;
    }

}
