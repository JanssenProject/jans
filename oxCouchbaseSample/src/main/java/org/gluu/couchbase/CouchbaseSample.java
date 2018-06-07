package org.gluu.couchbase;

import java.util.Arrays;
import java.util.List;

import org.gluu.couchbase.model.SimpleAttribute;
import org.gluu.couchbase.model.SimpleGrant;
import org.gluu.couchbase.model.SimpleSession;
import org.gluu.couchbase.model.SimpleUser;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.model.ListViewResponse;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class CouchbaseSample {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private CouchbaseSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        CouchbaseSampleEntryManager couchbaseSampleEntryManager = new CouchbaseSampleEntryManager();

        // Create Couchbase entry manager
        CouchbaseEntryManager couchbaseEntryManager = couchbaseSampleEntryManager.createCouchbaseEntryManager();

        // Find all users which have specified object classes defined in SimpleUser
        List<SimpleUser> users = couchbaseEntryManager.findEntries("o=@!5304.5F36.0E64.E1AC!0001!179C.62D7,o=gluu", SimpleUser.class, null);
        for (SimpleUser user : users) {
            LOG.info("User with uid: '{}' with DN: '{}'", user.getUserId(), user.getDn());
        }

        if (users.size() > 0) {
            // Add attribute "streetAddress" to first user
            SimpleUser user = users.get(1);
            String[] values = new String[] { "Somewhere: " + System.currentTimeMillis(), "Somewhere2: " + System.currentTimeMillis() };
            user.getCustomAttributes().add(new CustomAttribute("streetAddress", Arrays.asList(values)));
            user.setUserId("check333");

            couchbaseEntryManager.merge(user);
        }

        Filter filter = Filter.createEqualityFilter("gluuStatus", "active");
        List<SimpleAttribute> attributes = couchbaseEntryManager.findEntries("o=gluu", SimpleAttribute.class, filter, SearchScope.SUB, null, null, 10,
                0, 0);
        for (SimpleAttribute attribute : attributes) {
            LOG.info("Attribute with displayName: " + attribute.getCustomAttributes().get(1));
        }

        List<SimpleSession> sessions = couchbaseEntryManager.findEntries("o=gluu", SimpleSession.class, filter, SearchScope.SUB, null, null, 10, 0,
                0);
        LOG.info("Found sessions: " + sessions.size());

        List<SimpleGrant> grants = couchbaseEntryManager.findEntries("o=gluu", SimpleGrant.class, null, SearchScope.SUB,
                new String[] { "oxAuthGrantId" }, null, 1, 0, 0);
        LOG.info("Found grants: " + grants.size());

        try {
            ListViewResponse<SimpleUser> listViewResponse = couchbaseEntryManager.findListViewResponse("o=gluu", SimpleUser.class, null, 0, 6, 4,
                    "uid", SortOrder.ASCENDING, new String[] { "uid", "displayName", "gluuStatus" });

            LOG.info("Found persons: " + listViewResponse.getItemsPerPage() + ", total persons: " + listViewResponse.getTotalResults());
            for (SimpleUser user : listViewResponse.getResult()) {
                System.out.println(user.getUserId());
            }
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

        try {
            ListViewResponse<SimpleUser> listViewResponse = couchbaseEntryManager.findListViewResponse("o=gluu", SimpleUser.class, null, 0, 6, 4,
                    "uid", SortOrder.DESCENDING, new String[] { "uid", "displayName", "gluuStatus" });

            LOG.info("Found persons: " + listViewResponse.getItemsPerPage() + ", total persons: " + listViewResponse.getTotalResults());
            for (SimpleUser user : listViewResponse.getResult()) {
                System.out.println(user.getUserId());
            }
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

    }

}
