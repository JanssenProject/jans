package io.jans.casa.plugins.strongauthn.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by jgomer on 2018-04-18.
 */
public class TrustedDeviceComparator implements Comparator<TrustedDevice> {

    private boolean sortedOrigins;

    public TrustedDeviceComparator(boolean sortedOrigins) {
        this.sortedOrigins = sortedOrigins;
    }

    public int compare(TrustedDevice t1, TrustedDevice t2) {

        if (t1 == null && t2 == null) {
            return 0;
        }
        if (t1 == null) {
            return -1;
        }
        if (t2 == null) {
            return 1;
        }

        List<TrustedOrigin> o1 = t1.getOrigins();
        List<TrustedOrigin> o2 = t2.getOrigins();

        o1 = o1 == null ? Collections.emptyList() : o1;
        o2 = o2 == null ? Collections.emptyList() : o2;

        Long l1, l2;

        if (sortedOrigins) {
            l1 = o1.stream().findFirst().map(TrustedOrigin::getTimestamp).orElse(0L);
            l2 = o2.stream().findFirst().map(TrustedOrigin::getTimestamp).orElse(0L);
        } else {
            l1 = o1.stream().mapToLong(TrustedOrigin::getTimestamp).max().orElse(0);
            l2 = o2.stream().mapToLong(TrustedOrigin::getTimestamp).max().orElse(0);
        }
        return l1.compareTo(l2);

    }

}
