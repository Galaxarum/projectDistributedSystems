package it.polimi.cs.ds.distributed_storage.exceptions;

/**
 * This exception should be raised when a communication protocol doesn't behave as expected
 */
public class BrokenProtocolException extends IllegalArgumentException {
    public BrokenProtocolException(String s){
        super(s);
    }

    public BrokenProtocolException(String s, Throwable throwable) {
        super(s,throwable);
    }
}
