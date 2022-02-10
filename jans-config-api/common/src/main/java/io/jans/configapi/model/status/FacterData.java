package io.jans.configapi.model.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "memoryfree", "swapfree", "hostname", "ipaddress", "uptime", "free_disk_space", "load_average" })
public class FacterData {

	@JsonProperty("memoryfree")
	private String memoryfree;
	
	@JsonProperty("swapfree")
	private String swapfree;
	
	@JsonProperty("hostname")
	private String hostname;
	
	@JsonProperty("ipaddress")
	private String ipaddress;
	
	@JsonProperty("uptime")
	private String uptime;
	
	@JsonProperty("free_disk_space")
	private String freeDiskSpace;
	
	@JsonProperty("load_average")
	private String loadAverage;

	
	@JsonProperty("memoryfree")
	public String getMemoryfree() {
		return memoryfree;
	}

	@JsonProperty("memoryfree")
	public void setMemoryfree(String memoryfree) {
		this.memoryfree = memoryfree;
	}

	@JsonProperty("swapfree")
	public String getSwapfree() {
		return swapfree;
	}

	@JsonProperty("swapfree")
	public void setSwapfree(String swapfree) {
		this.swapfree = swapfree;
	}

	@JsonProperty("hostname")
	public String getHostname() {
		return hostname;
	}

	@JsonProperty("hostname")
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@JsonProperty("ipaddress")
	public String getIpaddress() {
		return ipaddress;
	}

	@JsonProperty("ipaddress")
	public void setIpaddress(String ipaddress) {
		this.ipaddress = ipaddress;
	}

	@JsonProperty("uptime")
	public String getUptime() {
		return uptime;
	}

	@JsonProperty("uptime")
	public void setUptime(String uptime) {
		this.uptime = uptime;
	}

	@JsonProperty("free_disk_space")
	public String getFreeDiskSpace() {
		return freeDiskSpace;
	}

	@JsonProperty("free_disk_space")
	public void setFreeDiskSpace(String freeDiskSpace) {
		this.freeDiskSpace = freeDiskSpace;
	}

	@JsonProperty("load_average")
	public String getLoadAverage() {
		return loadAverage;
	}

	@JsonProperty("load_average")
	public void setLoadAverage(String loadAverage) {
		this.loadAverage = loadAverage;
	}

    @Override
    public String toString() {
        return "FacterData [memoryfree=" + memoryfree + ", swapfree=" + swapfree + ", hostname=" + hostname
                + ", ipaddress=" + ipaddress + ", uptime=" + uptime + ", freeDiskSpace=" + freeDiskSpace
                + ", loadAverage=" + loadAverage + "]";
    }
	
}
