package io.jans.as.server.dev;

import java.util.List;
import java.util.Properties;

import io.jans.model.token.TokenEntity;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;

public final class TokenTest {


    public static SqlEntryManager createSqlEntryManager() {
        SqlEntryManagerFactory sqlEntryManagerFactory = new SqlEntryManagerFactory();
        sqlEntryManagerFactory.create();
        Properties connectionProperties = new Properties();

        connectionProperties.put("sql#db.schema.name", "public");
    	connectionProperties.put("sql#connection.uri", "jdbc:postgresql://localhost:16432/jansdb");

    	connectionProperties.put("sql#auth.userName", "jans");
        connectionProperties.put("sql#auth.userPassword", "ObIwZ94SPhO8");

        SqlEntryManager sqlEntryManager = sqlEntryManagerFactory.createEntryManager(connectionProperties);
        System.out.println("Created SqlEntryManager: " + sqlEntryManager);

        return sqlEntryManager;
    }

    public static void main1(SqlEntryManager sqlEntryManager, String[] args) throws Exception {
        String baseDn = "ou=tokens,o=jans";
        Filter typeFilter = Filter.createEqualityFilter("tknTyp", "access_token");
        Filter userFilter = Filter.createEqualityFilter("usrId", "admin");
        Filter delFilter = Filter.createEqualityFilter("del", true);

        Filter filter = Filter.createANDFilter(typeFilter, userFilter, delFilter);
        List<TokenEntity> tokens = sqlEntryManager.findEntries(baseDn, TokenEntity.class, filter);

        System.out.println("tokens: " + tokens.size());
    }

    public static void main2(SqlEntryManager sqlEntryManager, String[] args) throws Exception {
        String baseDn = "ou=tokens,o=jans";

        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setDn(baseDn);
        tokenEntity.setUserId("admin");
        tokenEntity.setTokenType("access_token");

        List<TokenEntity> tokens = sqlEntryManager.findEntries(tokenEntity);

        System.out.println("tokens: " + tokens.size());
    }

    public static void main(String[] args) throws Exception {
        SqlEntryManager sqlEntryManager = createSqlEntryManager();

        main1(sqlEntryManager, args);
    	main2(sqlEntryManager, args);

    	sqlEntryManager.destroy();
    }

}
