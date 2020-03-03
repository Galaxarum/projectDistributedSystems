package middleware.primitives;

import exceptions.BrokenProtocolException;


public final class PrimitiveOps {

    private PrimitiveOps(){}

    public static <T extends Enum<T> & Primitive> void checkEquals(T expected, Object actual){
        if(!expected.equals(actual))
            throw new BrokenProtocolException("Expected " + expected + " but " + actual + " was received.");
    }
}
