package distributedStorage;

import middleware.MessagingMiddleware;
import middleware.MessagingMiddlewareImpl;

import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());
    public static final int ID_INDEX = 0;
    public static final int KNOWN_HOST_INDEX = 1;
    public static final int MIDDLEWARE_PORT_INDEX = 2;
    public static final int CLIENT_PORT_INDEX = 3;

    public static void main(String[] args) {
        final String id;
        final String knownHost;
        MessagingMiddleware messagingMiddleware;
        try {
            id = args[ID_INDEX];
            knownHost = args[KNOWN_HOST_INDEX];
        }catch (ArrayIndexOutOfBoundsException e){
            logger.severe("Id or known host not specified");
            return;
        }
        try {
            final int port = Integer.parseInt(args[MIDDLEWARE_PORT_INDEX]);
            messagingMiddleware = new MessagingMiddlewareImpl(id,port);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            messagingMiddleware = new MessagingMiddlewareImpl(id);
        }
        messagingMiddleware.join(knownHost);
    }
}
