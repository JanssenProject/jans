package io.jans.orm.cloud.spanner.util;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.jans.orm.util.ValueHelper;

/**
 * Helps to convert values between types
 *
 * @author Yuriy Movchan Date: 04/30/201
 */
public class SpannerValueHelper extends ValueHelper {

    private static final com.google.cloud.Date[] EMPTY_GOOGLE_DATE_ARRAY = new com.google.cloud.Date[0];
    private static final com.google.cloud.Timestamp[] EMPTY_GOOGLE_TIMESTAMP_ARRAY = new com.google.cloud.Timestamp[0];

    private static final Date[] EMPTY_DATE_ARRAY = new Date[0];

	public static com.google.cloud.Date toGoogleDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof com.google.cloud.Date) {
        	return (com.google.cloud.Date) value;
        } else if (value instanceof Date) {
        	return com.google.cloud.Date.fromJavaUtilDate((Date) value);
        }
        
        try {
            return com.google.cloud.Date.parseDate(value.toString());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static com.google.cloud.Date[] toGoogleDateArray(Object values) {
        if (values == null) {
            return null;
        }
        
        if (values instanceof List) {
        	List<?> valuesList = (List<?>) values;
            if (valuesList.size() == 0) {
                return EMPTY_GOOGLE_DATE_ARRAY;
            }

            com.google.cloud.Date[] result = new com.google.cloud.Date[valuesList.size()];
            int idx = 0;
            for (Object value : valuesList) {
            	result[idx++] = toGoogleDate(value);
    		}
            
            return result;
        } else if (values.getClass().isArray()) {
        	Object[] valuesArray = (Object []) values;
            if (valuesArray.length == 0) {
                return EMPTY_GOOGLE_DATE_ARRAY;
            }

            com.google.cloud.Date[] result = new com.google.cloud.Date[valuesArray.length];
            for (int i = 0; i < valuesArray.length; i++) {
            	result[i] = toGoogleDate(valuesArray[i]);
    		}
            
            return result;
        }

        return new com.google.cloud.Date[] { toGoogleDate(values) }; 
    }

    public static List<com.google.cloud.Date> toGoogleDateList(Object[] values) {
        if (values == null) {
            return null;
        }

        return (List<com.google.cloud.Date>) Arrays.asList(toGoogleDateArray(values));
    }

    public static Date[] toJavaDateArrayFromSpannerDateList(List<com.google.cloud.Date> dates) {
    	if (dates == null) {
    		return EMPTY_DATE_ARRAY;
    	}

    	Date[] result = new Date[dates.size()];
        int idx = 0;
        for (com.google.cloud.Date date : dates) {
        	result[idx++] = com.google.cloud.Date.toJavaUtilDate(date);
		}

		return result;
	}

	public static com.google.cloud.Timestamp toGoogleTimestamp(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof com.google.cloud.Timestamp) {
        	return (com.google.cloud.Timestamp) value;
        } else if (value instanceof Date) {
        	return com.google.cloud.Timestamp.of((Date) value);
        } else if (value instanceof Timestamp) {
        	return com.google.cloud.Timestamp.of((Timestamp) value);
        }
        
        try {
            return com.google.cloud.Timestamp.parseTimestamp(value.toString());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static com.google.cloud.Timestamp[] toGoogleTimestampArray(Object values) {
        if (values == null) {
            return null;
        }
        
        if (values instanceof List) {
        	List<?> valuesList = (List<?>) values;
            if (valuesList.size() == 0) {
                return EMPTY_GOOGLE_TIMESTAMP_ARRAY;
            }

            com.google.cloud.Timestamp[] result = new com.google.cloud.Timestamp[valuesList.size()];
            int idx = 0;
            for (Object value : valuesList) {
            	result[idx++] = toGoogleTimestamp(value);
    		}
            
            return result;
        } else if (values.getClass().isArray()) {
        	Object[] valuesArray = (Object []) values;
            if (valuesArray.length == 0) {
                return EMPTY_GOOGLE_TIMESTAMP_ARRAY;
            }

            com.google.cloud.Timestamp[] result = new com.google.cloud.Timestamp[valuesArray.length];
            for (int i = 0; i < valuesArray.length; i++) {
            	result[i] = toGoogleTimestamp(valuesArray[i]);
    		}
            
            return result;
        }

        return new com.google.cloud.Timestamp[] { toGoogleTimestamp(values) }; 
    }

    public static List<com.google.cloud.Timestamp> toGoogleTimestampList(Object[] values) {
        if (values == null) {
            return null;
        }

        return (List<com.google.cloud.Timestamp>) Arrays.asList(toGoogleTimestampArray(values));
    }

    public static Date[] toJavaDateArrayFromSpannerTimestampList(List<com.google.cloud.Timestamp> dates) {
    	if (dates == null) {
    		return EMPTY_DATE_ARRAY;
    	}

    	Date[] result = new Date[dates.size()];
        int idx = 0;
        for (com.google.cloud.Timestamp date : dates) {
        	result[idx++] = new java.util.Date(date.toSqlTimestamp().getTime());
		}

		return result;
	}

}
