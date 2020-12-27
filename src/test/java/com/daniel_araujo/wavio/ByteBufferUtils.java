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
        target.put(source);
        target.flip();

        source.position(sourceP);
        source.limit(sourceL);

        return target;
    }

    /**
     * Extracts contents of a ByteBuffer into an array.
     *
     * Basically like reading from current position up to limit.
     *
     * The original buffer will keep its position intact.
     *
     * @param source
     * @return
     */
    public static byte[] toArray(ByteBuffer source) {
        int sourceP = source.position();
        byte[] arr = new byte[source.remaining()];
        source.get(arr);
        source.position(sourceP);
        return arr;
    }
}
