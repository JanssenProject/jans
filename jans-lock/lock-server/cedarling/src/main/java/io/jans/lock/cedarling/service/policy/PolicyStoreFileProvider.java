/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.cedarling.service.policy;

/**
 * SPI for managing the policy store file on the local filesystem.
 * <p>
 * The application embedding {@code CedarlingAuthorizationService} must supply a
 * CDI bean that implements this interface.  The service calls the methods in the
 * following order during its lifecycle:
 * <ol>
 *   <li>{@link #prepare()} – called once before Cedarling is initialised; the
 *       implementation should write (or extract) the policy store file to a
 *       well-known location on disk.</li>
 *   <li>{@link #getPolicyStorePath()} – called immediately after {@code prepare()}
 *       to obtain the absolute path of the file that was placed on disk.</li>
 *   <li>{@link #cleanup()} – called once when the Cedarling service is destroyed;
 *       the implementation should remove the policy store file from disk.</li>
 * </ol>
 *
 * @author Yuriy Movchan Date: 06/26/2026
 */
public interface PolicyStoreFileProvider {

    /**
     * Prepares the policy store file on the local filesystem.
     * This method is called before Cedarling is initialised.
     *
     * @throws RuntimeException if the file cannot be prepared
     */
    void prepare();

    /**
     * Returns the absolute path of the policy store file on the local filesystem.
     * {@link #prepare()} is guaranteed to have been called before this method.
     *
     * @return absolute path to the policy store file; never {@code null}
     */
    String getPolicyStorePath();

    /**
     * Removes the policy store file from the local filesystem.
     * This method is called after the Cedarling adapter has been closed.
     *
     * @throws RuntimeException if the file cannot be removed
     */
    void cleanup();
}
