/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.jans.cacherefresh.model.config.CacheRefreshConfiguration;
import io.jans.util.ArrayHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;

/**
 * Helper service to work with snapshots
 * 
 * @author Yuriy Movchan Date: 06.09.2011
 */
@ApplicationScoped
@Named("cacheRefreshSnapshotFileService")
public class CacheRefreshSnapshotFileService {

	@Inject
	private Logger log;

	private static final String SNAPSHOT_FILE_NAME_PATTERN = "inum-snapshot-%s.txt";
	private static final String PROBLEM_LIST_FILE_NAME = "problem-inum-list.txt";
	private static final String SNAPSHOT_FILE_NAME_DATE_PATTERN = "yyyy-MM-dd-HH-mm";

	public boolean prepareSnapshotsFolder(CacheRefreshConfiguration cacheRefreshConfiguration) {
		String snapshotFolder = cacheRefreshConfiguration.getSnapshotFolder();

		try {
			File dir = new File(snapshotFolder);
			if (!dir.exists()) {
				FileUtils.forceMkdir(dir);
			}
		} catch (IOException ex) {
			log.error("Failed to create snapshot folder '{}'", snapshotFolder, ex);
			return false;
		}

		return true;
	}

	public boolean createSnapshot(CacheRefreshConfiguration cacheRefreshConfiguration, Map<String, Integer> inumWithEntryHashCodeMap) {
		if (!prepareSnapshotsFolder(cacheRefreshConfiguration)) {
			return false;
		}
		DateFormat fileNameDateFormat = new SimpleDateFormat(SNAPSHOT_FILE_NAME_DATE_PATTERN);
		String snapshotFileName = String.format(SNAPSHOT_FILE_NAME_PATTERN, fileNameDateFormat.format(new Date()));
		File file = new File(cacheRefreshConfiguration.getSnapshotFolder() + File.separator + snapshotFileName);
		try(BufferedWriter bos = new BufferedWriter(new FileWriter(file))) {
			for (Entry<String, Integer> entry : inumWithEntryHashCodeMap.entrySet()) {
				bos.write(String.format("%s:%d\n", entry.getKey(), entry.getValue()));
			}
			bos.flush();
		} catch (IOException ex) {
			log.error("Failed to create snapshot file '{}'", file.getAbsolutePath(), ex);
			return false;
		} 
		return true;
	}

	public Map<String, Integer> readSnapshot(CacheRefreshConfiguration cacheRefreshConfiguration, String snapshotFileName) {
		if (!prepareSnapshotsFolder(cacheRefreshConfiguration)) {
			return null;
		}

		File file = new File(cacheRefreshConfiguration.getSnapshotFolder() + File.separator + snapshotFileName);
		if (!file.exists()) {
			return null;
		}
		Map<String, Integer> result = new HashMap<String, Integer>();
		try(BufferedReader bis = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] lineValues = line.split(":");
				if (lineValues.length != 2) {
					log.error("Failed to parse line: {}", line);
					return null;
				}

				try {
					result.put(lineValues[0], Integer.valueOf(lineValues[1]));
				} catch (RuntimeException ex) {
					log.error("Failed to parse '{}' to integer", lineValues[1], ex);
					return null;
				}
			}
		} catch (IOException ex) {
			log.error("Failed to load snapshot file '{}'", file.getAbsolutePath(), ex);
			return null;
		} 
		return result;
	}

	public Map<String, Integer> readLastSnapshot(CacheRefreshConfiguration cacheRefreshConfiguration) {
		if (!prepareSnapshotsFolder(cacheRefreshConfiguration)) {
			return null;
		}

		String[] snapshots = getSnapshotsList(cacheRefreshConfiguration);
		if (ArrayHelper.isEmpty(snapshots)) {
			return null;
		}

		return readSnapshot(cacheRefreshConfiguration, snapshots[snapshots.length - 1]);
	}

	private String[] getSnapshotsList(CacheRefreshConfiguration cacheRefreshConfiguration) {
		File file = new File(cacheRefreshConfiguration.getSnapshotFolder());
		String[] files = file.list(new WildcardFileFilter(String.format(SNAPSHOT_FILE_NAME_PATTERN, "*")));
		Arrays.sort(files);

		return files;
	}

	public boolean retainSnapshots(CacheRefreshConfiguration cacheRefreshConfiguration, int count) {
		if (!prepareSnapshotsFolder(cacheRefreshConfiguration)) {
			return false;
		}

		String[] snapshots = getSnapshotsList(cacheRefreshConfiguration);
		if (ArrayHelper.isEmpty(snapshots)) {
			return true;
		}

		for (int i = 0; i < snapshots.length - count; i++) {
			File file = new File(cacheRefreshConfiguration.getSnapshotFolder() + File.separator + snapshots[i]);
			if (!file.delete()) {
				log.error("Failed to remove snaphost file '{}'", file.getAbsolutePath());
			}
		}

		return true;
	}

	public List<String> readProblemList(CacheRefreshConfiguration cacheRefreshConfiguration) {
		if (!prepareSnapshotsFolder(cacheRefreshConfiguration)) {
			return null;
		}
		File file = new File(cacheRefreshConfiguration.getSnapshotFolder() + File.separator + PROBLEM_LIST_FILE_NAME);
		if (!file.exists()) {
			return null;
		}
		List<String> result = new ArrayList<String>();
		try (BufferedReader bis = new BufferedReader(new FileReader(file))){
			String line;
			while ((line = bis.readLine()) != null) {
				result.add(line);
			}
		} catch (IOException ex) {
			log.error("Failed to load problem list from file '{}'", file.getAbsolutePath(), ex);
			return null;
		} 
		return result;
	}

	public boolean writeProblemList(CacheRefreshConfiguration cacheRefreshConfiguration, Set<String> changedInums) {
		if (!prepareSnapshotsFolder(cacheRefreshConfiguration)) {
			return false;
		}
		File file = new File(cacheRefreshConfiguration.getSnapshotFolder() + File.separator + PROBLEM_LIST_FILE_NAME);
		try (BufferedWriter bos = new BufferedWriter(new FileWriter(file))){
			for (String changedInum : changedInums) {
				bos.write(String.format("%s\n", changedInum));
			}
			bos.flush();
		} catch (IOException ex) {
			log.error("Failed to write problem list to file '{}'", file.getAbsolutePath(), ex);
			return false;
		} 
		return true;
	}

}
