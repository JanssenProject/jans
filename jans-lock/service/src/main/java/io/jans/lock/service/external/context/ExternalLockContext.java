package io.jans.lock.service.external.context;

import io.jans.model.custom.script.conf.CustomScriptConfiguration;

/**
 * External lock context
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
public class ExternalLockContext {

    private CustomScriptConfiguration script;
    private boolean cancelOperation;

    public ExternalLockContext() {
    	this.cancelOperation = false;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

	public boolean isCancelOperation() {
		return cancelOperation;
	}

	public void setCancelOperation(boolean cancelOperation) {
		this.cancelOperation = cancelOperation;
	}

	@Override
	public String toString() {
		return "ExternalLockContext [script=" + script + ", cancelOperation=" + cancelOperation + "]";
	}

}
