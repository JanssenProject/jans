package org.gluu.configapi;

import com.intuit.karate.junit5.Karate;

public class KarateTestRunner {

	@Karate.Test
	Karate testFullPath() {
		//return Karate.run("classpath:karate/tags.feature").tags("@first");
		return Karate.run("src/test/resources/feature");
	}

}
