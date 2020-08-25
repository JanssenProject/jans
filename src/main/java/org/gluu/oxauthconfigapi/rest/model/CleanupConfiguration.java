package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;


public class CleanupConfiguration implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Min(value = 1)
	@Max(value = 2147483647)
	private int cleanServiceInterval;
	
	@Min(value = 1)
	@Max(value = 2147483647)
    private int cleanServiceBatchChunkSize;
    private List<String> cleanServiceBaseDns;
    
    
	public int getCleanServiceInterval() {
		return cleanServiceInterval;
	}
	
	public void setCleanServiceInterval(int cleanServiceInterval) {
		this.cleanServiceInterval = cleanServiceInterval;
	}
	
	public int getCleanServiceBatchChunkSize() {
		return cleanServiceBatchChunkSize;
	}
	
	public void setCleanServiceBatchChunkSize(int cleanServiceBatchChunkSize) {
		this.cleanServiceBatchChunkSize = cleanServiceBatchChunkSize;
	}
	
	public List<String> getCleanServiceBaseDns() {
		return cleanServiceBaseDns;
	}
	
	public void setCleanServiceBaseDns(List<String> cleanServiceBaseDns) {
		this.cleanServiceBaseDns = cleanServiceBaseDns;
	}
	
}
