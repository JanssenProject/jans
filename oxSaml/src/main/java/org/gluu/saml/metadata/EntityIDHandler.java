/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.saml.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EntityIDHandler extends DefaultHandler {
    private List<String> entityIDs = null;
    private String currentEntityID;
    private List<String> spEntityIDs = null;
    private List<String> idpEntityIDs = null;
    private Map<String, String> organizations = null;
    private boolean waitingForName;

    public List<String> getEntityIDs() {
        return this.entityIDs;
    }

    public List<String> getSpEntityIDs() {
        return this.spEntityIDs;
    }

    public List<String> getIdpEntityIDs() {
        return this.idpEntityIDs;
    }

    public Map<String, String> getOrganizations() {
        return this.organizations;
    }

    @Override
    public void startDocument() {
        entityIDs = new ArrayList<String>();
        spEntityIDs = new ArrayList<String>();
        idpEntityIDs = new ArrayList<String>();
        organizations = new HashMap<String, String>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        waitingForName = false;
        if (qName.contains("EntityDescriptor")) {
            this.currentEntityID = attributes.getValue("entityID");
            entityIDs.add(currentEntityID);
            return;
        }
        if (qName.contains("SPSSODescriptor")) {
            spEntityIDs.add(currentEntityID);
            return;
        }
        if (qName.contains("IDPSSODescriptor")) {
            idpEntityIDs.add(currentEntityID);
            return;
        }
        if (qName.contains("OrganizationDisplayName")) {
            waitingForName = true;
            return;
        }
    }

    @Override
    public void characters(char[] arg0, int arg1, int arg2) {
        if (waitingForName) {
            organizations.put(currentEntityID, new String(arg0, arg1, arg2));
            waitingForName = false;
        }
    }
}
