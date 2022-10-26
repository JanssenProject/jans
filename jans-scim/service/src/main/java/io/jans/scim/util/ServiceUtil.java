/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.scim.model.GluuCustomPerson;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.service.GroupService;
import io.jans.scim.service.PersonService;

/**
 * User: Dejan Maric
 */
@ApplicationScoped
public class ServiceUtil implements Serializable {

	private static final long serialVersionUID = -2842459224631032594L;

	@Inject
	private PersonService personService;

	@Inject
	private GroupService groupService;

	private static final SecureRandom random = new SecureRandom();

	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Delete a Group from a Person
	 *
	 * @return void
	 * @throws Exception
	 */
	public void deleteGroupFromPerson(GluuGroup group, String dn) throws Exception {
		List<String> persons = group.getMembers();
		for (String onePerson : persons) {
			GluuCustomPerson gluuPerson = personService.getPersonByDn(onePerson);
			List<String> memberOflist = gluuPerson.getMemberOf();

			List<String> tempMemberOf = new ArrayList<>();
			for (String aMemberOf : memberOflist) {
				tempMemberOf.add(aMemberOf);
			}

			for (String oneMemberOf : tempMemberOf) {
				if (oneMemberOf.equalsIgnoreCase(dn)) {
					tempMemberOf.remove(oneMemberOf);
					break;
				}
			}
			List<String> cleanMemberOf = new ArrayList<>();

			for (String aMemberOf : tempMemberOf) {
				cleanMemberOf.add(aMemberOf);
			}

			gluuPerson.setMemberOf(cleanMemberOf);
			personService.updatePerson(gluuPerson);

		}

	}

	public String iterableToString(Iterable<?> list) {
		if (list == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Object item : list) {
			sb.append(item);
			sb.append(",");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * Adds a group to a person's memberOf
	 *
	 * @return void
	 * @throws Exception
	 */
	public void personMembersAdder(GluuGroup gluuGroup, String dn) throws Exception {
		List<String> members = gluuGroup.getMembers();
		for (String member : members) {
			GluuCustomPerson gluuPerson = personService.getPersonByDn(member);
			List<String> groups = gluuPerson.getMemberOf();
			if (!isMemberOfExist(groups, dn)) {
				List<String> cleanGroups = new ArrayList<String>();
				cleanGroups.add(dn);
				for (String aGroup : groups) {
					cleanGroups.add(aGroup);
				}
				gluuPerson.setMemberOf(cleanGroups);
				personService.updatePerson(gluuPerson);
			}

		}

	}

	/**
	 * checks if the memeberOf attribute already contains a given group
	 *
	 * @return boolean
	 */
	private boolean isMemberOfExist(List<String> groups, String dn) {
		for (String group : groups) {
			if (group.equalsIgnoreCase(dn)) {
				return true;
			}

		}
		return false;
	}

	/**
	 * Adds a person to a group
	 *
	 * @return void
	 * @throws Exception
	 */
	public void groupMembersAdder(GluuCustomPerson gluuPerson, String dn) throws Exception {
		List<String> groups = gluuPerson.getMemberOf();
		for (String group : groups) {
			GluuGroup oneGroup = groupService.getGroupByDn(group);
			List<String> groupMembers = oneGroup.getMembers();
			if ((groupMembers != null && !groupMembers.isEmpty()) && !isMemberExist(groupMembers, dn)) {
				List<String> cleanGroupMembers = new ArrayList<String>();
				cleanGroupMembers.add(dn);
				for (String personDN : groupMembers) {
					cleanGroupMembers.add(personDN);
				}
				oneGroup.setMembers(cleanGroupMembers);
				groupService.updateGroup(oneGroup);
			}
		}
	}

	/**
	 * checks if the member already exist in a group
	 *
	 * @return boolean
	 */
	private boolean isMemberExist(List<String> groupMembers, String dn) {
		for (String member : groupMembers) {
			if (member.equalsIgnoreCase(dn)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Save file with random name with provided base directory and extension.
	 * 
	 * @param array
	 *            binary content of file.
	 * @param baseDir
	 *            Write to directory.
	 * @param extension
	 *            Filename extension.
	 * @return Return full path
	 * @throws IOException
	 */
	public static String saveRandomFile(byte[] array, String baseDir, String extension) throws IOException {
		final String filepath = baseDir + File.separator + Math.abs(random.nextLong()) + "." + extension;
		final File dir = new File(baseDir);
		if (!dir.exists())
			dir.mkdirs();
		else if (!dir.isDirectory())
			throw new IllegalArgumentException("parameter baseDir should be directory. The value: " + baseDir);

		try(InputStream in = new ByteArrayInputStream(array);FileOutputStream out = new FileOutputStream(filepath)) {
			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
		} 
		return filepath;
	}

	public static ObjectMapper getObjectMapper() {
		return mapper;
	}

	/**
	 * Read all bytes from the supplied input stream. Closes the input stream.
	 *
	 * @param is
	 *            Input stream
	 * @return All bytes
	 * @throws IOException
	 *             If an I/O problem occurs
	 */
	public static byte[] readFully(InputStream is) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			byte[] buffer = new byte[2048];
			int read = 0;
			while ((read = is.read(buffer)) != -1) {
				baos.write(buffer, 0, read);
			}
			return baos.toByteArray();
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
}
