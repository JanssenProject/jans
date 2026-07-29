package io.jans.shibboleth.trust.persistence;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;

import java.util.Properties;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Shares a single {@link SqlEntryManager} across every SQL integration test in the module, mirroring how an
 * application holds one manager per component. Building a manager opens a connection pool and scans the
 * database metadata, so a per-class manager pays that cost once per test class; this pays it once per JVM.
 *
 * <p>Register with {@code @ExtendWith(SqlEntryManagerExtension.class)} and declare a {@link SqlEntryManager}
 * parameter on {@code @BeforeAll} (or any test method) to have the shared instance injected. The manager is
 * built lazily on first use and gated on {@code -Dtrust.it.sql.uri}: without it the extended class is skipped.
 * It lives as a {@link ExtensionContext.Store.CloseableResource} in the root store, so JUnit destroys it
 * exactly once, after the last extended test class finishes.
 */
public final class SqlEntryManagerExtension implements BeforeAllCallback, ParameterResolver {

    private static final String KEY = "shared-sql-entry-manager";

    @Override
    public void beforeAll(ExtensionContext context) {

        assumeTrue(System.getProperty("trust.it.sql.uri") != null,
            "SQL integration tests skipped: set -Dtrust.it.sql.uri (+ .schema/.user/.password) to run them");

        context.getRoot().getStore(GLOBAL).getOrComputeIfAbsent(KEY, key -> new Holder(build()), Holder.class);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {

        return parameterContext.getParameter().getType() == SqlEntryManager.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {

        return extensionContext.getRoot().getStore(GLOBAL).get(KEY, Holder.class).entryManager;
    }

    private static SqlEntryManager build() {

        Properties properties = new Properties();
        properties.put("sql#connection.uri", System.getProperty("trust.it.sql.uri"));
        properties.put("sql#db.schema.name", System.getProperty("trust.it.sql.schema", "public"));
        properties.put("sql#auth.userName", System.getProperty("trust.it.sql.user", "jans"));
        properties.put("sql#auth.userPassword", System.getProperty("trust.it.sql.password", ""));
        properties.put("sql#connection.driver-property.serverTimezone", "UTC");
        properties.put("sql#connection.pool.max-total", "10");
        properties.put("sql#connection.pool.max-idle", "10");
        properties.put("sql#connection.pool.create-max-wait-time-millis", "20000");
        properties.put("sql#connection.pool.max-wait-time-millis", "20000");
        properties.put("sql#connection.pool.min-evictable-idle-time-millis", "1800000");
        properties.put("sql#password.encryption.method", "SSHA-256");

        SqlEntryManagerFactory factory = new SqlEntryManagerFactory();
        factory.create();

        return factory.createEntryManager(properties);
    }

    private static final class Holder implements ExtensionContext.Store.CloseableResource {

        private final SqlEntryManager entryManager;

        private Holder(SqlEntryManager entryManager) {

            this.entryManager = entryManager;
        }

        @Override
        public void close() {

            entryManager.destroy();
        }
    }
}
