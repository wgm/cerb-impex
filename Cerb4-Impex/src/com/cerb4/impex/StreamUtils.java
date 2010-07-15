package com.cerb4.impex;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Utility class for working with streams and writers.
 *
 * @author <a href="mailto:jj@displayboy.com">Jan Jungnickel</a>
 */
public class StreamUtils {
    /**
     * Closes the <code>OutputStream</code>, ignoring any exceptions raised while closing. Flushes the stream before closing it.
     *
     * @param outputStream Stream to close.
     */
    public static void closeSilently(OutputStream outputStream) {
        flushSilently(outputStream);
        try {
            outputStream.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Flushes the <code>OutputStream</code>, ignoring any exceptions raised while closing.
     *
     * @param outputStream Stream to flush.
     */
    public static void flushSilently(OutputStream outputStream) {
        try {
            outputStream.flush();
        } catch (IOException ignored) {
        }
    }
}
