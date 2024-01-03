package io.jans.lock.service.external.context;

import io.jans.model.custom.script.conf.CustomScriptConfiguration;

/**
 * External lock context
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
public class ExternalLockContext {

    private CustomScriptConfiguration script;
    private boolean cancelPdpOperation;
    private boolean cancelNextScriptOperation;

    public ExternalLockContext() {
    	this.cancelNextScriptOperation = false;
    	this.cancelPdpOperation = false;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

	public boolean isCancelPdpOperation() {
		return cancelPdpOperation;
	}

	public void setCancelPdpOperation(boolean cancelPdpOperation) {
		this.cancelPdpOperation = cancelPdpOperation;
	}

	public boolean isCancelNextScriptOperation() {
		return cancelNextScriptOperation;
	}

	public void setCancelNextScriptOperation(boolean cancelNextScriptOperation) {
		this.cancelNextScriptOperation = cancelNextScriptOperation;
	}

	@Override
	public String toString() {
		return "ExternalLockContext [script=" + script + ", cancelPdpOperation=" + cancelPdpOperation
				+ ", cancelNextScriptOperation=" + cancelNextScriptOperation + "]";
	}

}
