package io.jans.orm.search.filter;

import io.jans.orm.util.StringHelper;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Z
 */
public class FileProcessorTest {

    @Test
    public void simpleFilter_whenRun_shouldProduceFilter() {
        final Filter filter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), StringHelper.toLowerCase("1"));
        FilterProcessor filterProcessor = new FilterProcessor();
        assertNotNull(filterProcessor.excludeFilter(filter, FilterProcessor.OBJECT_CLASS_EQUALITY_FILTER, FilterProcessor.OBJECT_CLASS_PRESENCE_FILTER));
    }
}
