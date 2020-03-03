package middleware.primitives;

import exceptions.BrokenProtocolException;

/**
 * This class contains static methods to check the correctness of communication protocols
 */
public final class PrimitiveOps {

    /**
     * Constructor kept private to prevent instantiation of the class
     */
    private PrimitiveOps(){}

    /**
     * Check if the given {@link Primitive} equals the given object, throwing {@link BrokenProtocolException} in case they differ
     * @param expected the expected {@link Primitive}
     * @param actual the object to check
     * @param <T> Must be an enum
     */
    public static <T extends Enum<T> & Primitive> void checkEquals(T expected, Object actual){
        if(!expected.equals(actual))
            throw new BrokenProtocolException("Expected " + expected + " but " + actual + " was received.");
    }
}
