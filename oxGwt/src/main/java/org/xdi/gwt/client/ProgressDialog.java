/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.gwt.client;

import com.sencha.gxt.widget.core.client.AutoProgressBar;
import com.sencha.gxt.widget.core.client.Dialog;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/05/2013
 */

public class ProgressDialog extends Dialog {

    private final AutoProgressBar m_progressBar = new AutoProgressBar();

    public ProgressDialog(String p_title) {
        setHeadingText(p_title);
        setHideOnButtonClick(true);
        setModal(true);
        setWidget(m_progressBar);
    }

    public AutoProgressBar getProgressBar() {
        return m_progressBar;
    }

    @Override
    public void show() {
        super.show();
        m_progressBar.auto();
    }
}