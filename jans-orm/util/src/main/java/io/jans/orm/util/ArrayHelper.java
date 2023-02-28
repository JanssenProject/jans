/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.util;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Utility methods to help work with arrays
 *
 * @author Yuriy Movchan Date: 10.21.2010
 * @see org.apache.commons.lang.ArrayUtils
 */
public final class ArrayHelper {

    private ArrayHelper() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayMerge(T[]... arrays) {
        // Determine required size of new array
        int count = 0;
        for (T[] array : arrays) {
            count += array.length;
        }

        if (count == 0) {
            return (T[]) Array.newInstance(arrays.getClass().getComponentType().getComponentType(), 0);
        }

        // create new array of required class
        T[] mergedArray = (T[]) Array.newInstance(arrays[0][0].getClass(), count);

        // Merge each array into new array
        int start = 0;
        for (T[] array : arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }

        return (T[]) mergedArray;
    }

    public static <T> boolean isEmpty(T[] objects) {
        return (objects == null) || (objects.length == 0);
    }

    public static <T> boolean isNotEmpty(T[] objects) {
        return (objects != null) && (objects.length > 0);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayClone(T[] array) {
        if (array == null) {
            return array;
        }
        if (array.length == 0) {
            return (T[]) Array.newInstance(array.getClass().getComponentType(), 0);
        }

        T[] clonedArray = (T[]) Array.newInstance(array[0].getClass(), array.length);
        System.arraycopy(array, 0, clonedArray, 0, array.length);

        return clonedArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayClone(T[] array, Class<?> componentType) {
        T[] clonedArray = (T[]) Array.newInstance(componentType, array.length);
        System.arraycopy(array, 0, clonedArray, 0, array.length);

        return clonedArray;
    }

    public static <T> T[] sortAndClone(T[] array) {
        if (array == null) {
            return array;
        }

        T[] clonedArray = arrayClone(array);
        Arrays.sort(clonedArray);

        return clonedArray;
    }

    public static <T> boolean equalsIgnoreOrder(T[] values1, T[] values2) {
        T[] valuesSorted1 = sortAndClone(values1);
        T[] valuesSorted2 = sortAndClone(values2);

        return Arrays.equals(valuesSorted1, valuesSorted2);
    }

    public static String[] addItemToStringArray(String[] array, String item) {
        String[] array2 = new String[] {item};
        if (array == null) {
            return array2;
        }

        return arrayMerge(array, array2);
    }

    public static String toString(Object[] array) {
        return StringHelper.toString(array);
    }

}
