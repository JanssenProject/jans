package io.jans.casa.service.settings;

/**
 * Allows for reading, modifying and saving config data for a plugin in the underlying database. No low-level access
 * details involved.
 * @param <T> The type of the configuration (a class modeling the data to handle)
 */
public interface IPluginSettingsHandler<T> {

    /**
     * Persists the data to the database. More specifically, the value already set via method {@link #setSettings(Object)}.
     * @throws Exception When there was an unexpected problem saving.
     */
    void save() throws Exception;

    /**
     * Returns a deep clone copy of the configuration: If the object returned by this call is modified and {@link #save()}
     * is called, no changes will be reflected. Use {@link #setSettings(Object)} beforehand
     * @return
     */
    T getSettings();

    /**
     * Sets the configuration. This only alters the in-memory copy of the runtime configuration of the app.
     * To make it persistent, call {@link #save()}.
     * @param settings Data object.
     */
    void setSettings(T settings);

}
