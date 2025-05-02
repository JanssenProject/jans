package io.jans.ca.plugin.adminui.service.webhook;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.common.util.AttributeConstants;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.model.webhook.AuiFeature;
import io.jans.ca.plugin.adminui.model.webhook.ShortCodeRequest;
import io.jans.ca.plugin.adminui.model.webhook.WebhookEntry;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.python.google.common.collect.Sets;
import org.slf4j.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import jakarta.validation.Valid;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
public class WebhookService {
    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    ConfigurationFactory configurationFactory;

    public static final String AUI_FEATURE_ID = "auiFeatureId";

    /**
     * The function retrieves all AuiFeature objects from the entryManager and returns them as a List.
     *
     * @return The method is returning a List of AuiFeature objects.
     */
    public List<AuiFeature> getAllAuiFeatures() throws ApplicationException {
        try {
            final Filter filter = Filter.createPresenceFilter(AUI_FEATURE_ID);
            return entryManager.findEntries(AppConstants.ADMIN_UI_FEATURES_DN, AuiFeature.class, filter);
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.FETCH_DATA_ERROR.getDescription());
        }
    }

    /**
     * The function retrieves a list of AuiFeatures that are associated with a specific webhookId.
     *
     * @param webhookId The parameter "webhookId" is a String that represents the ID of a webhook.
     * @throws ApplicationException
     * @return The method is returning a list of AuiFeature objects that have a webhookId matching the provided webhookId
     * parameter.
     */
    public List<AuiFeature> getAllAuiFeaturesByWebhookId(String webhookId) throws ApplicationException {
        try {
            List<AuiFeature> features = getAllAuiFeatures();
            return features.stream()
                    .filter(feature -> feature.getWebhookIdsMapped() != null)
                    .filter(feature -> feature.getWebhookIdsMapped().contains(webhookId))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.FETCH_DATA_ERROR.getDescription());
        }
    }

    /**
     * The function searches for webhook entries based on the provided search criteria and returns a paged result.
     *
     * @param searchRequest The `searchRequest` parameter is an object of type `SearchRequest`. It contains information
     *                      about the search criteria and pagination for the webhooks search.
     * @throws ApplicationException
     * @return The method is returning a PagedResult object containing a list of WebhookEntry objects.
     */
    public PagedResult<WebhookEntry> searchWebhooks(SearchRequest searchRequest) throws ApplicationException {
        try {
            Filter searchFilter = null;
            List<Filter> filters = new ArrayList<>();
            if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

                for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                    String[] targetArray = new String[]{assertionValue};
                    Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                            targetArray, null);
                    Filter webhookIdFilter = Filter.createSubstringFilter(AppConstants.INUM, null, targetArray, null);
                    Filter urlFilter = Filter.createSubstringFilter("url", null, targetArray, null);
                    filters.add(Filter.createORFilter(displayNameFilter, webhookIdFilter, urlFilter));
                }
                searchFilter = Filter.createORFilter(filters);
            }
            log.debug("Webhook searchFilter:{}", searchFilter);
            return entryManager.findPagedEntries(AppConstants.WEBHOOK_DN, WebhookEntry.class, searchFilter, null,
                    searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                    searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_SEARCH_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.WEBHOOK_SEARCH_ERROR.getDescription());
        }
    }

    /**
     * The function `getWebhookByIds` retrieves a list of `WebhookEntry` objects based on a list of webhook IDs.
     *
     * @param ids A list of webhook IDs to search for.
     * @return The method is returning a List of WebhookEntry objects.
     */
    public List<WebhookEntry> getWebhookByIds(Set<String> ids) {
        try {
            Filter searchFilter = null;
            List<Filter> filters = new ArrayList<>();
            for (String id : ids) {
                Filter filter = Filter.createSubstringFilter(AppConstants.INUM, null, new String[]{id}, null);
                filters.add(filter);
            }
            searchFilter = Filter.createORFilter(filters);
            log.debug("Webhooks searchFilter:{}", searchFilter);
            return entryManager.findEntries(AppConstants.WEBHOOK_DN, WebhookEntry.class, searchFilter);
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_SEARCH_ERROR.getDescription(), e);
            return Lists.newArrayList();
        }
    }

    public List<WebhookEntry> getWebhooksByFeatureId(String featureId) {
        try {
            Filter filter = Filter.createSubstringFilter(AUI_FEATURE_ID, null, new String[]{featureId}, null);
            List<AuiFeature> features = entryManager.findEntries(AppConstants.ADMIN_UI_FEATURES_DN, AuiFeature.class, filter);
            if (CollectionUtils.isEmpty(features)) {
                log.error(ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
            }
            AuiFeature feature = features.get(0);
            List<String> webhooksIds = feature.getWebhookIdsMapped();
            if (CollectionUtils.isEmpty(webhooksIds)) {
                log.error(ErrorResponse.NO_WEBHOOK_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.NO_WEBHOOK_FOUND.getDescription());
            }

            return getWebhookByIds(Sets.newHashSet(webhooksIds));
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_SEARCH_ERROR.getDescription(), e);
            return Lists.newArrayList();
        }
    }

    /**
     * The function `getAuiFeaturesByIds` retrieves a list of AuiFeature objects based on a list of IDs.
     *
     * @param ids A list of String values representing the IDs of AuiFeatures to be retrieved.
     * @return The method is returning a List of AuiFeature objects.
     */
    public List<AuiFeature> getAuiFeaturesByIds(Set<String> ids) {
        try {
            Filter searchFilter = null;
            List<Filter> filters = new ArrayList<>();
            for (String id : ids) {
                Filter filter = Filter.createSubstringFilter(AUI_FEATURE_ID, null, new String[]{id}, null);
                filters.add(filter);
            }
            searchFilter = Filter.createORFilter(filters);
            log.debug("Features searchFilter:{}", searchFilter);
            return entryManager.findEntries(AppConstants.ADMIN_UI_FEATURES_DN, AuiFeature.class, searchFilter);
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            return Lists.newArrayList();
        }
    }

    /**
     * The function adds a webhook entry to a database, assigns it a unique ID, and associates it with Aui features if
     * provided.
     *
     * @param webhook The parameter "webhook" is an object of type WebhookEntry. It represents the webhook entry that needs
     *                to be added.
     * @throws ApplicationException
     * @return The method is returning a WebhookEntry object.
     */
    public WebhookEntry addWebhook(@Valid WebhookEntry webhook) throws ApplicationException {
        try {
            validateWebhookEntry(webhook);
            String id = idFromName(webhook.getDisplayName() + webhook.getUrl() + webhook.getHttpMethod());
            webhook.setInum(id);
            webhook.setDn(dnOfWebhook(id, AppConstants.WEBHOOK_DN));
            entryManager.persist(webhook);

            if (webhook.getAuiFeatureIds() != null) {
                List<AuiFeature> features = getAuiFeaturesByIds(webhook.getAuiFeatureIds());
                features.stream().forEach(feature -> {
                    feature.setWebhookIdsMapped(addNonExistingElements(id, feature.getWebhookIdsMapped()));
                    entryManager.merge(feature);
                });
            }

            return webhook;
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_SAVE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.WEBHOOK_SAVE_ERROR.getDescription());
        }
    }

    private List<String> addNonExistingElements(String element, List<String> collection) {
        if (collection == null) {
            collection = Lists.newArrayList();
        }
        HashSet<String> tmpSet = Sets.newHashSet(collection);
        tmpSet.add(element);
        return Lists.newArrayList(tmpSet);

    }

    /**
     * The function removes a webhook entry and throws an exception if there is an error.
     *
     * @param webhook The parameter "webhook" is of type WebhookEntry. It represents the webhook entry that needs to be
     *                removed.
     * @throws ApplicationException
     */
    public void removeWebhook(WebhookEntry webhook) throws ApplicationException {
        try {
            if (Strings.isNullOrEmpty(webhook.getInum())) {
                log.error(ErrorResponse.WEBHOOK_ID_MISSING.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_ID_MISSING.getDescription());
            }

            List<AuiFeature> features = getAllAuiFeatures();
            //removing webhookId from auiFeatures tables..
            features.stream()
                    .filter(feature -> feature.getWebhookIdsMapped() != null)
                    .filter(feature -> feature.getWebhookIdsMapped().contains(webhook.getInum()))
                    .forEach(feature -> {
                        feature.getWebhookIdsMapped().remove(webhook.getInum());
                        entryManager.merge(feature);
                    });
            //removing webhook
            entryManager.remove(webhook);
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_DELETE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.WEBHOOK_DELETE_ERROR.getDescription());
        }
    }

    /**
     * The function updates a webhook entry, validates the entry, and performs additional operations if necessary.
     *
     * @param webhook The parameter "webhook" is an object of type WebhookEntry. It represents the webhook entry that needs
     *                to be updated.
     * @throws ApplicationException
     * @return The method is returning a WebhookEntry object.
     */
    public WebhookEntry updateWebhook(WebhookEntry webhook) throws ApplicationException {
        try {
            validateWebhookEntry(webhook);
            if (Strings.isNullOrEmpty(webhook.getInum())) {
                log.error(ErrorResponse.WEBHOOK_ID_MISSING.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_ID_MISSING.getDescription());
            }

            if (Strings.isNullOrEmpty(webhook.getDn())) {
                webhook.setDn(dnOfWebhook(webhook.getInum(), AppConstants.WEBHOOK_DN));
            }
            entryManager.merge(webhook);

            List<AuiFeature> features = getAllAuiFeatures();
            //removing webhookId from auiFeatures table from the feature records
            features.stream()
                    .filter(feature -> feature.getWebhookIdsMapped() != null)
                    .filter(feature -> feature.getWebhookIdsMapped().contains(webhook.getInum()))
                    .forEach(feature -> {
                        feature.getWebhookIdsMapped().remove(webhook.getInum());
                        entryManager.merge(feature);
                    });
            //adding webhook-id to the feature record
            if (webhook.getAuiFeatureIds() != null) {
                features = getAuiFeaturesByIds(webhook.getAuiFeatureIds());
                features.stream().forEach(feature -> {
                    feature.setWebhookIdsMapped(addNonExistingElements(webhook.getInum(), feature.getWebhookIdsMapped()));
                    entryManager.merge(feature);
                });
            }

            return webhook;
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_UPDATE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.WEBHOOK_UPDATE_ERROR.getDescription());
        }
    }

    public void validateWebhookEntry(WebhookEntry webhookEntry) throws ApplicationException {
        if (webhookEntry == null) {
            log.error(ErrorResponse.WEBHOOK_ENTRY_EMPTY.getDescription());
            throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_ENTRY_EMPTY.getDescription());
        }
        if (Strings.isNullOrEmpty(webhookEntry.getDisplayName())) {
            log.error(ErrorResponse.WEBHOOK_NAME_EMPTY.getDescription());
            throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_NAME_EMPTY.getDescription());
        }
        if (Strings.isNullOrEmpty(webhookEntry.getUrl())) {
            log.error(ErrorResponse.WEBHOOK_URL_EMPTY.getDescription());
            throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_URL_EMPTY.getDescription());
        }
        if (Strings.isNullOrEmpty(webhookEntry.getHttpMethod())) {
            log.error(ErrorResponse.WEBHOOK_HTTP_METHOD_EMPTY.getDescription());
            throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_HTTP_METHOD_EMPTY.getDescription());
        }
        if (Lists.newArrayList("POST", "PUT", "PATCH").contains(webhookEntry.getHttpMethod())) {
            if (MapUtils.isEmpty(webhookEntry.getHttpRequestBody())) {
                log.error(ErrorResponse.WEBHOOK_REQUEST_BODY_EMPTY.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_REQUEST_BODY_EMPTY.getDescription());
            }
            if (webhookEntry.getHttpHeaders().stream().noneMatch(header -> header.getKey().equals(AppConstants.CONTENT_TYPE))) {
                log.error(ErrorResponse.WEBHOOK_CONTENT_TYPE_REQUIRED.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_CONTENT_TYPE_REQUIRED.getDescription());
            }
        }
    }

    /**
     * The function triggers enabled webhooks by creating a thread pool, validating each webhook entry, and executing them
     * concurrently.
     *
     * @param webhookIds A set of webhook IDs.
     * @throws ApplicationException
     * @return The method is returning a List of Strings.
     */
    public List<GenericResponse> triggerEnabledWebhooks(Set<String> webhookIds, List<ShortCodeRequest> shortCodes) throws ApplicationException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<GenericResponse> responseList = new ArrayList<>();
        List<Callable<GenericResponse>> callables = new ArrayList<>();
        List<WebhookEntry> webhooks = getWebhookByIds(webhookIds);
        for (WebhookEntry webhook : webhooks) {
            validateWebhookEntry(webhook);
            ShortCodeRequest shortCodeObj = shortCodes.stream().filter(shortCode -> shortCode.getWebhookId().equals(webhook.getInum())).findAny().orElse(null);
            replaceShortCodeWithValues(webhook, shortCodeObj);
            if (webhook.isJansEnabled()) {
                Callable<GenericResponse> callable = new WebhookCallable(webhook, log);
                callables.add(callable);
            }
        }

        for (Callable<GenericResponse> callable : callables) {
            Future<GenericResponse> future = executor.submit(callable);
            try {
                responseList.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Webhook execution interrupted!", e);
                Thread.currentThread().interrupt();
            }
        }
        //shut down the executor service
        executor.shutdown();
        return responseList;
    }

    private void replaceShortCodeWithValues(WebhookEntry webhook, ShortCodeRequest shortCodeObj) {
        if (shortCodeObj == null) {
            return;
        }

        if (CommonUtils.hasShortCode(webhook.getUrl())) {
            webhook.setUrl(CommonUtils.replacePlaceholders(webhook.getUrl(), shortCodeObj.getShortcodeValueMap()));
        }

        if (CommonUtils.hasShortCode(webhook.getHttpRequestBody()) && Lists.newArrayList("POST", "PUT", "PATCH").contains(webhook.getHttpMethod())) {
            webhook.setHttpRequestBody(CommonUtils.replacePlaceholders(webhook.getHttpRequestBody(), shortCodeObj.getShortcodeValueMap()));
        }
    }

    private static String idFromName(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(UTF_8)).toString();
    }

    private static String dnOfWebhook(String id, String baseDn) {
        return String.format("inum=%s,%s", id, baseDn);
    }

    public int getRecordMaxCount() {
        log.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }
}
