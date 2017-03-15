/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.util.repository;

import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.xdi.util.LDAPConstants;

/**
 * Utility to simplify work with tree oriented repositories
 * 
 * @author Yuriy Movchan Date: 11.03.2010
 */
public class RepositoryUtility {

	private static final Random random = new Random();

	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}

	public static String generateTreeFolderPath(int countLevels, int countFolderPerLevel, String fileName) {
		int remainLevels = countLevels;
		StringBuilder result = new StringBuilder();
		while (remainLevels > 0) {
			result.append(getRandomFolder(countFolderPerLevel)).append('/');
			remainLevels--;

		}

		return result.append(fileName).toString();
	}

	private static String getRandomFolder(int countFolderPerLevel) {
		return String.valueOf((int) (random.nextFloat() * countFolderPerLevel));
	}

	public static String getFileNameExtension(String fileName) {
		int idx = fileName.lastIndexOf(LDAPConstants.DOT);

		if (idx == -1) {
			return "";
		}

		return fileName.substring(idx);
	}

	public static String getFileName(String fileName) {
		return FilenameUtils.getName(fileName);
	}

}
