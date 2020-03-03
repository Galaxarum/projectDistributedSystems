package exceptions;

public class BrokenProtocolException extends IllegalArgumentException {
    public BrokenProtocolException(String s){
        super(s);
    }

    public BrokenProtocolException(String s, Throwable throwable) {
        super(s,throwable);
    }
}
