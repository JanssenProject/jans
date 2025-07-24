/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.PagedResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import java.util.*;


@ApplicationScoped
public class DatabaseService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    public Map<String, Map<String, AttributeType>> getTableColumnsMap() {
        return persistenceEntryManager.getTableColumnsMap();
    }
    
    private PagedResult<Map<String, Map<String, AttributeType>>> getDatabaseObjectsPagedResult(SearchRequest searchRequest) {
            logger.info("Database searchRequest:{}", searchRequest);


        PagedResult<Map<String, Map<String, AttributeType>>> pagedResult = new PagedResult<>();
            try {

                Map<String, Map<String, AttributeType>> tableInfo = getTableColumnsMap();
                logger.info("Database tableInfo:{}", tableInfo);
                
                if (tableInfo == null && tableInfo.isEmpty()) {
                    return pagedResult;
                }
                    
                int startIndex = 0;
                int limit = searchRequest.getCount();
                int totalEntries = tableInfo.size();
                int toIndex = tableInfo.size();
                
                if(searchRequest !=null) {
                    startIndex = searchRequest.getStartIndex();   
                    toIndex = (startIndex + limit <= totalEntries) ? startIndex + limit
                            : totalEntries;
                }
           
                logger.info("Final startIndex:{}, limit:{}, toIndex:{}", startIndex, limit, toIndex);

          
                pagedResult.setStart(startIndex);
                pagedResult.setEntriesCount(limit);
                pagedResult.setTotalEntriesCount(totalEntries);
               // pagedResult.setEntries(tableInfo);

            } catch (IndexOutOfBoundsException ioe) {
                
            }
        

        logger.info("Database logPagedResult:{}", pagedResult);

        return pagedResult;
    }

       
}
