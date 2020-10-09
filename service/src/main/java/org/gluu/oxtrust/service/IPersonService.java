/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.oxtrust.service;

import java.util.List;
import java.util.Map;

import org.gluu.oxtrust.model.GluuCustomAttribute;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.User;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.model.AttributeData;

public interface IPersonService {

	public abstract void addCustomObjectClass(GluuCustomPerson person);

	/**
	 * Add new person
	 * 
	 * @param person
	 *            Person
	 * @throws DuplicateEntryException
	 */
	// TODO: Review this methods. We need to check if uid is unique in outside
	// method
	public abstract void addPerson(GluuCustomPerson person) throws Exception;

	/**
	 * Add person entry
	 * 
	 * @param person
	 *            Person
	 * @throws Exception 
	 */
	public abstract void updatePerson(GluuCustomPerson person) throws Exception;

	/**
	 * Remove person with persona and contacts branches
	 * 
	 * @param person
	 *            Person
	 */
	public abstract void removePerson(GluuCustomPerson person);

	/**
	 * Search persons by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of persons
	 */
	public abstract List<GluuCustomPerson> searchPersons(String pattern, int sizeLimit);

	/**
	 * Search persons by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @return List of persons
	 */
	public abstract List<GluuCustomPerson> searchPersons(String pattern);

	/**
	 * Search persons by sample object
	 * 
	 * @param person
	 *            Person with set attributes relevant to he current search (for
	 *            example gluuAllowPublication)
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of persons
	 */
	public abstract List<GluuCustomPerson> findPersons(GluuCustomPerson person, int sizeLimit);

	/**
	 * Search persons by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @param excludedPersons
	 *            list of uids that we don't want returned by service
	 * @return List of persons
	 */
	public abstract List<GluuCustomPerson> searchPersons(String pattern, int sizeLimit,
			List<GluuCustomPerson> excludedPersons) throws Exception;

	public abstract List<GluuCustomPerson> findAllPersons(String[] returnAttributes);

	public abstract List<GluuCustomPerson> findPersonsByUids(List<String> uids, String[] returnAttributes)
			throws Exception;

	public abstract GluuCustomPerson findPersonByDn(String dn, String... returnAttributes);

	/**
	 * Check if LDAP server contains person with specified attributes
	 * 
	 * @return True if person with specified attributes exist
	 */
	public abstract boolean containsPerson(GluuCustomPerson person);

	public abstract boolean contains(String dn);

	/**
	 * Get person by DN
	 * 
	 * @param dn
	 *            Dn
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByDn(String dn);

	/**
	 * Get person by inum
	 * 
	 * @param returnClass
	 *            POJO class which EntryManager should use to return entry object
	 * @param inum
	 *            Inum
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByInum(String inum);

	/**
	 * Get person by uid
	 * 
	 * @param uid
	 *            Uid
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByUid(String uid, String... returnAttributes);

	public abstract int countPersons();

	/**
	 * Generate new inum for person
	 * 
	 * @return New inum for person
	 */
	public abstract String generateInumForNewPerson();

	/**
	 * Build DN string for person
	 * 
	 * @param inum
	 *            Inum
	 * @return DN string for specified person or DN for persons branch if inum is
	 *         null
	 * @throws Exception
	 */
	public abstract String getDnForPerson(String inum);

	/**
	 * Authenticate user
	 * 
	 * @param userName
	 *            User name
	 * @param password
	 *            User password
	 * @return
	 */
	public abstract boolean authenticate(String userName, String password);

	public abstract List<GluuCustomAttribute> getMandatoryAtributes();

	public abstract List<GluuCustomPerson> createEntities(Map<String, List<AttributeData>> entriesAttributes)
			throws Exception;

	/**
	 * Get person by email
	 * 
	 * @param email
	 *            email
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByEmail(String mail, String... returnAttributes);

	/**
	 * Get person by attribute
	 * 
	 * @param attribute
	 *            attribute
	 * @param value
	 *            value
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByAttribute(String attribute, String value) throws Exception;

	/**
	 * Get user by uid
	 * 
	 * @param uid
	 *            Uid
	 * @return User
	 */
	public abstract User getUserByUid(String uid);

	/**
	 * Get list of persons by attribute
	 * 
	 * @param attribute
	 *            attribute
	 * @param value
	 *            value
	 * @return List <Person>
	 */
	public List<GluuCustomPerson> getPersonsByAttribute(String attribute, String value) throws Exception;

	List<GluuCustomPerson> findPersonsByMailids(List<String> mailids, String[] returnAttributes) throws Exception;

	String getPersonUids(List<GluuCustomPerson> persons) throws Exception;

	String getPersonMailids(List<GluuCustomPerson> persons) throws Exception;

	List<GluuCustomPerson> getPersonsByUid(String uid, String... returnAttributes);

	List<GluuCustomPerson> getPersonsByEmail(String mail, String... returnAttributes);

}