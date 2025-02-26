package io.jans.lock.service.stat;

import static io.jans.as.model.util.Util.escapeLog;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

import io.jans.lock.model.StatEntry;
import io.jans.lock.model.stat.StatResponse;
import io.jans.lock.model.stat.StatResponseItem;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.agkn.hll.HLL;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
@ApplicationScoped
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
            responseItem.setMonthlyActiveClients(clientCardinality(entries));
            responseItem.setMonth(month);

            unionOpearationsMapIntoResponseItem(entries, responseItem);

            return responseItem;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    // This should not occur for newly created StatEntry (only outdated db)
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
        HLL hll = decodeUserHll(entries.get(0));

        // Union hll
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size(); i++) {
                hll.union(decodeUserHll(entries.get(i)));
            }
        }
        return hll.cardinality();
    }

    private long clientCardinality(List<StatEntry> entries) {
        HLL hll = decodeClientHll(entries.get(0));

        // Union hll
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size(); i++) {
                hll.union(decodeClientHll(entries.get(i)));
            }
        }
        return hll.cardinality();
    }

    private HLL decodeUserHll(StatEntry entry) {
        try {
            return HLL.fromBytes(Base64.getDecoder().decode(entry.getUserHllData()));
        } catch (Exception e) {
            log.error("Failed to decode user HLL data, entry dn: {}, data: {}", entry.getDn(), entry.getUserHllData());
            return statService.newUserHll();
        }
    }


    private HLL decodeClientHll(StatEntry entry) {
        try {
            return HLL.fromBytes(Base64.getDecoder().decode(entry.getClientHllData()));
        } catch (Exception e) {
            log.error("Failed to decode client HLL data, entry dn: {}, data: {}", entry.getDn(), entry.getUserHllData());
            return statService.newClientHll();
        }
    }

    private static void unionOpearationsMapIntoResponseItem(List<StatEntry> entries, StatResponseItem responseItem) {
        for (StatEntry entry : entries) {
            entry.getStat().getOperationsByType().entrySet().stream().filter(en -> en.getValue() != null).forEach(en -> {
                final Map<String, Long> operationMap = responseItem.getOperationsByType().get(en.getKey());
                if (operationMap == null) {
                    responseItem.getOperationsByType().put(en.getKey(), en.getValue());
                    return;
                }
                for (Map.Entry<String, Long> operationEntry : en.getValue().entrySet()) {
                    final Long counter = operationMap.get(operationEntry.getKey());
                    if (counter == null) {
                        operationMap.put(operationEntry.getKey(), operationEntry.getValue());
                        continue;
                    }

                    operationMap.put(operationEntry.getKey(), counter + operationEntry.getValue());
                }
            });
        }
    }

}
