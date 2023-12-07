package io.jans.casa.plugins.bioid.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public class BioIDCredential implements Comparable<BioIDCredential> {

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String bcid;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String trait;

	public String getBcid() {
		return bcid;
	}

	public void setBcid(String bcid) {
		this.bcid = bcid;
	}

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public long getAddedOn() {
		return addedOn;
	}

	private long addedOn;

	public BioIDCredential() {

	}

	public String getTrait() {
		return trait;
	}

	public void setTrait(String trait) {
		this.trait = trait;
	}

	public void setAddedOn(long addedOn) {
		this.addedOn = addedOn;
	}

	@Override
	public int compareTo(BioIDCredential o) {
		long date1 = getAddedOn();
		long date2 = o.getAddedOn();
		return (date1 < date2) ? -1 : (date1 > date2 ? 1 : 0);
	}

}
