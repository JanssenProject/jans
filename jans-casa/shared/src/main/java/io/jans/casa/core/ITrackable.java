package io.jans.casa.core;

/**
 * An interface that can be implemented by plugins in order to exhibit information about plugin usage.
 */
public interface ITrackable {

    /**
     * Computes the number of active users in the period of time [start, end)
     * @param start A timestamp (relative to UNIX epoch)
     * @param end A timestamp (relative to UNIX epoch)
     * @return An integer value. If the computation cannot be specifically performed for the period of time specified,
     * it will return the usage exhibited at the moment of the invocation of this method.
     */
    default int getActiveUsers(long start, long end) {
        return -1;
    }

}
