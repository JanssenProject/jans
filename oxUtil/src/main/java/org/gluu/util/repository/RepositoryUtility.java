/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.repository;

import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility to simplify work with tree oriented repositories
 *
 * @author Yuriy Movchan Date: 11.03.2010
 */
public final class RepositoryUtility {

    private RepositoryUtility() { }

    private static final char DOT = '.';

    private static final Random RANDOM = new Random();

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
        return String.valueOf((int) (RANDOM.nextFloat() * countFolderPerLevel));
    }

    public static String getFileNameExtension(String fileName) {
        int idx = fileName.lastIndexOf(DOT);

        if (idx == -1) {
            return "";
        }

        return fileName.substring(idx);
    }

    public static String getFileName(String fileName) {
        return FilenameUtils.getName(fileName);
    }

}
