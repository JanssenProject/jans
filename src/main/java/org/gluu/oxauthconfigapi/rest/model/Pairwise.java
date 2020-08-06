package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class Pairwise implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@NotBlank
	@Size(min=1)
	@Pattern(regexp = "persistent|algorithmic")
	private String pairwiseIdType;
	
	@NotBlank
	@Size(min=1)
	private String pairwiseCalculationKey;
	
	@NotBlank
	@Size(min=1)
	private String pairwiseCalculationSalt;
	  
	public String getPairwiseIdType() {
		return pairwiseIdType;
	}
	
	public void setPairwiseIdType(String pairwiseIdType) {
		this.pairwiseIdType = pairwiseIdType;
	}
	
	public String getPairwiseCalculationKey() {
		return pairwiseCalculationKey;
	}
	
	public void setPairwiseCalculationKey(String pairwiseCalculationKey) {
		this.pairwiseCalculationKey = pairwiseCalculationKey;
	}
	
	public String getPairwiseCalculationSalt() {
		return pairwiseCalculationSalt;
	}
	
	public void setPairwiseCalculationSalt(String pairwiseCalculationSalt) {
		this.pairwiseCalculationSalt = pairwiseCalculationSalt;
	}
	  
}
