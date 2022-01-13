/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Helps to convert values between types
 *
 * @author Yuriy Movchan Date: 04/29/201
 */
public class ValueHelper {

    private static final Boolean[] EMPTY_BOOLEAN_ARRAY = new Boolean[0];
    private static final Long[] EMPTY_LONG_ARRAY = new Long[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final BigDecimal[] EMPTY_BIG_DECIMAL_ARRAY = new BigDecimal[0];

	protected ValueHelper() {
    }

    public static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
        	return (Boolean) value;
        }
        
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static Boolean[] toBooleanArray(Object values) {
        if (values == null) {
            return null;
        }
        
        if (values instanceof List) {
        	List<?> valuesList = (List<?>) values;
            if (valuesList.size() == 0) {
                return EMPTY_BOOLEAN_ARRAY;
            }

            Boolean[] result = new Boolean[valuesList.size()];
            int idx = 0;
            for (Object value : valuesList) {
            	result[idx++] = toBoolean(value);
    		}
            
            return result;
        } else if (values.getClass().isArray()) {
        	Object[] valuesArray = (Object []) values;
            if (valuesArray.length == 0) {
                return EMPTY_BOOLEAN_ARRAY;
            }

            Boolean[] result = new Boolean[valuesArray.length];
            for (int i = 0; i < valuesArray.length; i++) {
            	result[i] = toBoolean(valuesArray[i]);
    		}
            
            return result;
        }

        return new Boolean[] { toBoolean(values) }; 
    }

    public static List<Boolean> toBooleanList(Object[] values) {
        if (values == null) {
            return null;
        }

        return (List<Boolean>) Arrays.asList(toBooleanArray(values));
    }

    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
        	return (Long) value;
        } else if (value instanceof Integer) {
        	return ((Integer) value).longValue();
        } else if (value instanceof BigDecimal) {
        	return ((BigDecimal) value).longValue();
        }
        
        try {
            return Long.parseLong(value.toString());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static Long[] toLongArray(Object values) {
        if (values == null) {
            return null;
        }
        
        if (values instanceof List) {
        	List<?> valuesList = (List<?>) values;
            if (valuesList.size() == 0) {
                return EMPTY_LONG_ARRAY;
            }

            Long[] result = new Long[valuesList.size()];
            int idx = 0;
            for (Object value : valuesList) {
            	result[idx++] = toLong(value);
    		}
            
            return result;
        } else if (values.getClass().isArray()) {
        	Object[] valuesArray = (Object []) values;
            if (valuesArray.length == 0) {
                return EMPTY_LONG_ARRAY;
            }

            Long[] result = new Long[valuesArray.length];
            for (int i = 0; i < valuesArray.length; i++) {
            	result[i] = toLong(valuesArray[i]);
    		}
            
            return result;
        }

        return new Long[] { toLong(values) }; 
    }

    public static List<Long> toLongList(Object[] values) {
        if (values == null) {
            return null;
        }

        return (List<Long>) Arrays.asList(toLongArray(values));
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
        	return (BigDecimal) value;
        } else if (value instanceof Long) {
        	return new BigDecimal((Long) value);
        } else if (value instanceof Integer) {
        	return new BigDecimal((Integer) value);
        }
        
        try {
            return new BigDecimal(value.toString());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static BigDecimal[] toBigDecimalArray(Object values) {
        if (values == null) {
            return null;
        }
        
        if (values instanceof List) {
        	List<?> valuesList = (List<?>) values;
            if (valuesList.size() == 0) {
                return EMPTY_BIG_DECIMAL_ARRAY;
            }

            BigDecimal[] result = new BigDecimal[valuesList.size()];
            int idx = 0;
            for (Object value : valuesList) {
            	result[idx++] = toBigDecimal(value);
    		}
            
            return result;
        } else if (values.getClass().isArray()) {
        	Object[] valuesArray = (Object []) values;
            if (valuesArray.length == 0) {
                return EMPTY_BIG_DECIMAL_ARRAY;
            }

            BigDecimal[] result = new BigDecimal[valuesArray.length];
            for (int i = 0; i < valuesArray.length; i++) {
            	result[i] = toBigDecimal(valuesArray[i]);
    		}
            
            return result;
        }

        return new BigDecimal[] { toBigDecimal(values) }; 
    }

    public static List<BigDecimal> toBigDecimalList(Object[] values) {
        if (values == null) {
            return null;
        }

        return (List<BigDecimal>) Arrays.asList(toBigDecimalArray(values));
    }

    public static Long[] toJavaLongArrayFromBigDecimalList(List<BigDecimal> numbers) {
    	if (numbers == null) {
    		return EMPTY_LONG_ARRAY;
    	}

    	Long[] result = new Long[numbers.size()];
        int idx = 0;
        for (BigDecimal number : numbers) {
        	result[idx++] = number.longValue();
		}

		return result;
	}

    public static String toString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
        	return (String) value;
        }
        
        try {
            return value.toString();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static String[] toStringArray(Object values) {
        if (values == null) {
            return null;
        }
        
        if (values instanceof List) {
        	List<?> valuesList = (List<?>) values;
            if (valuesList.size() == 0) {
                return EMPTY_STRING_ARRAY;
            }

            String[] result = new String[valuesList.size()];
            int idx = 0;
            for (Object value : valuesList) {
            	result[idx++] = toString(value);
    		}
            
            return result;
        } else if (values.getClass().isArray()) {
        	Object[] valuesArray = (Object []) values;
            if (valuesArray.length == 0) {
                return EMPTY_STRING_ARRAY;
            }

            String[] result = new String[valuesArray.length];
            for (int i = 0; i < valuesArray.length; i++) {
            	result[i] = toString(valuesArray[i]);
    		}
            
            return result;
        }

        return new String[] { toString(values) }; 
    }


    public static List<String> toStringList(Object[] values) {
        if (values == null) {
            return null;
        }

        return (List<String>) Arrays.asList(toStringArray(values));
    }

}
