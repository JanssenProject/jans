package io.jans.agama.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.agama.model.serialize.Type;
import io.jans.util.Pair;

import jakarta.ws.rs.core.HttpHeaders;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineConfig {

    private boolean enabled;

    private String rootDir = Path.of(System.getProperty("server.base"), "agama").toString();
    private String templatesPath = "/ftl";
    private String scriptsPath = "/scripts";
    
    private Type serializerType = Type.KRYO;
    
    private int maxItemsLoggedInCollections = 3;
    
    //transpiled code hash verification. Boolean preferred over boolean because it helps to keep the property "hidden"
    private Boolean disableTCHV;

    private String pageMismatchErrorPage = "mismatch.ftl";
    private String interruptionErrorPage = "timeout.ftl";
    private String crashErrorPage = "crash.ftl";
    private String finishedFlowPage = "finished.ftl";
    
    //relative to https://.../jans-auth
    private String bridgeScriptPage = "agama.xhtml";
    
    private Map<String, String> defaultResponseHeaders = Stream.of(
                new Pair<>(HttpHeaders.CACHE_CONTROL, "max-age=0, no-store")
        ).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

    public String getJsonErrorPage(String page) {
        return "json_"+ page;
    }
    
    @JsonIgnore
    public String getJsonFinishedFlowPage() {
        return "json_"+ finishedFlowPage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public String getTemplatesPath() {
        return templatesPath;
    }

    public void setTemplatesPath(String templatesPath) {
        this.templatesPath = templatesPath;
    }

    public String getScriptsPath() {
        return scriptsPath;
    }

    public void setScriptsPath(String scriptsPath) {
        this.scriptsPath = scriptsPath;
    }

    public Type getSerializerType() {
        return serializerType;
    }

    public void setSerializerType(Type serializerType) {
        this.serializerType = serializerType;
    }

    public String getInterruptionErrorPage() {
        return interruptionErrorPage;
    }

    public void setInterruptionErrorPage(String interruptionErrorPage) {
        this.interruptionErrorPage = interruptionErrorPage;
    }

    public int getMaxItemsLoggedInCollections() {
        return maxItemsLoggedInCollections;
    }

    public void setMaxItemsLoggedInCollections(int maxItemsLoggedInCollections) {
        this.maxItemsLoggedInCollections = maxItemsLoggedInCollections;
    }

    public Boolean getDisableTCHV() {
        return disableTCHV;
    }

    public void setDisableTCHV(Boolean disableTCHV) {
        this.disableTCHV = disableTCHV;
    }

    public String getCrashErrorPage() {
        return crashErrorPage;
    }

    public void setCrashErrorPage(String crashErrorPage) {
        this.crashErrorPage = crashErrorPage;
    }

    public String getPageMismatchErrorPage() {
        return pageMismatchErrorPage;
    }

    public void setPageMismatchErrorPage(String pageMismatchErrorPage) {
        this.pageMismatchErrorPage = pageMismatchErrorPage;
    }

    public String getFinishedFlowPage() {
        return finishedFlowPage;
    }

    public void setFinishedFlowPage(String finishedFlowPage) {
        this.finishedFlowPage = finishedFlowPage;
    }

    public String getBridgeScriptPage() {
        return bridgeScriptPage;
    }

    public void setBridgeScriptPage(String bridgeScriptPage) {
        this.bridgeScriptPage = bridgeScriptPage;
    }

    public Map<String, String> getDefaultResponseHeaders() {
        return defaultResponseHeaders;
    }

    public void setDefaultResponseHeaders(Map<String, String> defaultResponseHeaders) {
        this.defaultResponseHeaders = defaultResponseHeaders;
    }

}
