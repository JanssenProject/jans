package io.jans.as.server.service.stat;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import io.jans.as.common.model.stat.StatEntry;
import io.jans.as.server.ws.rs.stat.StatResponse;
import io.jans.as.server.ws.rs.stat.StatResponseItem;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import net.agkn.hll.HLL;
import org.slf4j.Logger;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.jans.as.model.util.Util.escapeLog;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class StatResponseService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private StatService statService;

    private final Cache<String, StatResponse> responseCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public StatResponse buildResponse(Set<String> months) {
        final String cacheKey = months.toString();
        final StatResponse cachedResponse = responseCache.getIfPresent(cacheKey);
        if (cachedResponse != null) {
            if (log.isTraceEnabled()) {
                log.trace("Get stat response from cache for: {}", escapeLog(cacheKey));
            }
            return cachedResponse;
        }

        StatResponse response = new StatResponse();
        for (String month : months) {
            final StatResponseItem responseItem = buildItem(month);
            if (responseItem != null) {
                response.getResponse().put(month, responseItem);
            }
        }

        responseCache.put(cacheKey, response);
        return response;
    }

    private StatResponseItem buildItem(String month) {
        try {
            final String escapedMonth = escapeLog(month);
            log.trace("Trying to fetch stat for month: {}", escapedMonth);

            final List<StatEntry> entries = entryManager.findEntries(statService.getBaseDn(), StatEntry.class, Filter.createEqualityFilter("jansData", month));
            if (entries == null || entries.isEmpty()) {
                log.trace("Can't find stat entries for month: {}", escapedMonth);
                return null;
            }
            log.trace("Fetched stat entries for month {} successfully", escapedMonth);

            checkNotMatchedEntries(month, entries);
            if (entries.isEmpty()) {
                log.trace("No stat entries for month: {}", escapedMonth);
                return null;
            }

            final StatResponseItem responseItem = new StatResponseItem();
            responseItem.setMonthlyActiveUsers(userCardinality(entries));
            responseItem.setMonth(month);

            unionTokenMapIntoResponseItem(entries, responseItem);

            return responseItem;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    // this should not occur for newly created StatEntry (only outdated db)
    private void checkNotMatchedEntries(String month, List<StatEntry> entries) {
        final List<StatEntry> notMatched = Lists.newArrayList();
        for (StatEntry entry : entries) {
            if (!Objects.equals(month, entry.getMonth())) {
                log.error("Not matched entry: {}", entry.getDn());
                notMatched.add(entry);
            }
        }

        entries.removeAll(notMatched);
    }


    private long userCardinality(List<StatEntry> entries) {
        HLL hll = decodeHll(entries.get(0));

        // Union hll
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size(); i++) {
                hll.union(decodeHll(entries.get(i)));
            }
        }
        return hll.cardinality();
    }

    private HLL decodeHll(StatEntry entry) {
        try {
            return HLL.fromBytes(Base64.getDecoder().decode(entry.getUserHllData()));
        } catch (Exception e) {
            log.error("Failed to decode HLL data, entry dn: {}, data: {}", entry.getDn(), entry.getUserHllData());
            return statService.newHll();
        }
    }


    private void unionTokenMapIntoResponseItem(List<StatEntry> entries, StatResponseItem responseItem) {
        for (StatEntry entry : entries) {
            entry.getStat().getTokenCountPerGrantType().entrySet().stream().filter(en -> en.getValue() != null).forEach(en -> {
                final Map<String, Long> tokenMap = responseItem.getTokenCountPerGrantType().get(en.getKey());
                if (tokenMap == null) {
                    responseItem.getTokenCountPerGrantType().put(en.getKey(), en.getValue());
                    return;
                }
                for (Map.Entry<String, Long> tokenEntry : en.getValue().entrySet()) {
                    final Long counter = tokenMap.get(tokenEntry.getKey());
                    if (counter == null) {
                        tokenMap.put(tokenEntry.getKey(), tokenEntry.getValue());
                        continue;
                    }

                    tokenMap.put(tokenEntry.getKey(), counter + tokenEntry.getValue());
                }
            });
        }
    }
}
