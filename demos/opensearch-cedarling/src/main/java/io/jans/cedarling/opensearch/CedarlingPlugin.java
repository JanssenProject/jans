package io.jans.cedarling.opensearch;

import io.jans.cedarling.opensearch.rest.SettingsRestHandler;

import java.util.*;
import java.util.function.Supplier;

import org.opensearch.cluster.service.*;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.common.settings.*;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.env.*;
import org.opensearch.plugins.*;
import org.opensearch.plugins.SearchPipelinePlugin.Parameters;
import org.opensearch.rest.*;
import org.opensearch.search.pipeline.*;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.client.*;
import org.opensearch.watcher.ResourceWatcherService;
import org.json.*;

public class CedarlingPlugin extends Plugin implements SearchPlugin, SearchPipelinePlugin, ActionPlugin {

    public static final String NAME = "cedarling";
    public static final String SETTINGS_KEY = "plugins." + NAME + ".settings";
    public static final String LAST_UPDATED_KEY = "plugins." + NAME + ".updatedAt";

    private static volatile ClusterService cs;
    private static volatile Client localClient;

    public static ClusterService getClusterService() {
        return cs;
    }
    
    public static ClusterAdminClient getClusterAdminClient() {
        return localClient.admin().cluster();
    }
    
    @Override
    public Map<String, Processor.Factory<SearchResponseProcessor>> getResponseProcessors(Parameters parameters) {
        return Map.of(CedarlingSearchResponseProcessor.TYPE, new CedarlingSearchResponseProcessor.Factory());
    }
    
    @Override
    public List<SearchPlugin.SearchExtSpec<?>> getSearchExts() {
        
        return List.of(
            new SearchExtSpec<>(
                CedarlingSearchExtBuilder.PARAM_FIELD_NAME,
                in -> new CedarlingSearchExtBuilder(in),
                parser -> CedarlingSearchExtBuilder.parse(parser)
            )
        );

    }
    
    @Override
    public Collection<Object> createComponents(
        Client localClient,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier) {

        this.cs = clusterService;
        this.localClient = localClient;
        return super.createComponents(localClient, clusterService, threadPool, resourceWatcherService,
                scriptService, xContentRegistry, environment, nodeEnvironment, namedWriteableRegistry,
                indexNameExpressionResolver, repositoriesServiceSupplier);
    }
    
    @Override    
    public List<Setting<?>> getSettings() {
        //All settings are stored in a single bulky string property: handling complex JSON content
        //for settings in Opensearch is weird and awkward. A separate endpoint was created for config
        //management. It gives the illusion of proper JSON management. The endpoint serializes
        //everything to a string before populating settings in the cluster service
        Setting.Property[] properties = new Setting.Property[] { Setting.Property.Dynamic, Setting.Property.NodeScope }; 
        return List.of(
            Setting.simpleString(SETTINGS_KEY, properties),
            Setting.longSetting(LAST_UPDATED_KEY, 0, properties)
        );

    }
    
    @Override    
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster) {
    
        return List.of(new SettingsRestHandler());
    }
    
}
