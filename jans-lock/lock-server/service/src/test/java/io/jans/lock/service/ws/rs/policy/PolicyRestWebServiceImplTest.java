package io.jans.lock.service.ws.rs.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolicyRestWebServiceImplTest {

	private static final String HASH = "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

	@Test
	void matchesQuotedWeakListedAndWildcardTags() {
		assertTrue(PolicyRestWebServiceImpl.matchesIfNoneMatch("\"" + HASH + "\"", HASH));
		assertTrue(PolicyRestWebServiceImpl.matchesIfNoneMatch("W/\"" + HASH + "\"", HASH));
		assertTrue(PolicyRestWebServiceImpl.matchesIfNoneMatch("\"other\", \"" + HASH + "\"", HASH));
		assertTrue(PolicyRestWebServiceImpl.matchesIfNoneMatch("*", HASH));
	}

	@Test
	void doesNotMatchMissingOrDifferentTags() {
		assertFalse(PolicyRestWebServiceImpl.matchesIfNoneMatch(null, HASH));
		assertFalse(PolicyRestWebServiceImpl.matchesIfNoneMatch("", HASH));
		assertFalse(PolicyRestWebServiceImpl.matchesIfNoneMatch("\"sha256:other\"", HASH));
		assertFalse(PolicyRestWebServiceImpl.matchesIfNoneMatch("\"" + HASH + "\"", null));
		assertFalse(PolicyRestWebServiceImpl.matchesIfNoneMatch("\"other," + HASH + ",suffix\"", HASH));
		assertFalse(PolicyRestWebServiceImpl.matchesIfNoneMatch("\"unterminated", HASH));
	}

}
