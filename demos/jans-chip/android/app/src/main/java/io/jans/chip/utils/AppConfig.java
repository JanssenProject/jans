package io.jans.chip.utils;

public enum AppConfig {
    INSTANCE;
    public static final String INTEGRITY_APP_SERVER_URL = "https://play-integrity-checker-server-2eua-3ndysngub.vercel.app";
    public static final long GOOGLE_CLOUD_PROJECT_ID = 618764598105L;
    public static final String SQLITE_DB_NAME = "chipDB";
    public static final String APP_NAME = "jans-chip";
    public static final String DEFAULT_S_NO = "1";
    public static final int ROOM_DATABASE_VERSION = 1;
    public static final String[] scopeArray = {"openid", "authorization_challenge", "profile"};
    public static final String[] defaultScopeArray = {"openid", "authorization_challenge"};
}
