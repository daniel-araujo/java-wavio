package com.daniel_araujo.wavio;

import java.util.Arrays;

public abstract class ArrayUtils {
    /**
     * Creates a new array with the contents of the given arrays.
     *
     * @param arrays
     * @return
     */
    public static byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++) {
            totalLength += arrays[i].length;
        }

        byte[] result = new byte[totalLength];

        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }

        return result;
    }
}
