package com.daniel_araujo.wavio;

import java.nio.ByteBuffer;

public abstract class ByteBufferUtils {
    /**
     * Creates a copy of a ByteBuffer starting from its current positions to its limit.
     * <p>
     * The original buffer will keep its position intact.
     *
     * @param source
     * @return
     */
    public static ByteBuffer deepCopy(ByteBuffer source) {
        int sourceP = source.position();
        int sourceL = source.limit();

        ByteBuffer target = ByteBuffer.allocate(source.remaining());
        target.order(source.order());
        target.put(source);
        target.flip();

        source.position(sourceP);
        source.limit(sourceL);

        return target;
    }

    /**
     * Extracts contents of a ByteBuffer into an array.
     * <p>
     * Basically like reading from current position up to limit.
     *
     * @param source
     * @return
     */
    public static byte[] getArray(ByteBuffer source) {
        int sourceP = source.position();
        byte[] arr = new byte[source.remaining()];
        source.get(arr);
        return arr;
    }
}
