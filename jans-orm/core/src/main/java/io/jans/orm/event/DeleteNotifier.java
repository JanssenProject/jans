/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.event;

public interface DeleteNotifier {

    void onBeforeRemove(String dn, String[] objectClasses);

    void onAfterRemove(String dn, String[] objectClasses);

}
