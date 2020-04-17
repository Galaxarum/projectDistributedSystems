package runnables;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import functional_interfaces.PrimitiveParser;
import markers.Primitive;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

final class PrimitiveParserRunnable<T extends Primitive> implements Runnable{
    /**
     * The connection to the client
     */
    private final Socket clientSocket;
    /**
     * Used to read middleware.messages from the client
     */
    private final ObjectInputStream in;
    /**
     * Used to write middleware.messages to the client
     */
    private final ObjectOutputStream out;
    private final PrimitiveParser<T> parsingFunction;
    /**
     * A logger
     */
    private static final Logger logger = Logger.getLogger(PrimitiveParserRunnable.class.getName());

    protected PrimitiveParserRunnable(Socket clientSocket, PrimitiveParser<T> primitiveParser) throws IOException {
        this.clientSocket = clientSocket;
        this.out = new ObjectOutputStream(clientSocket.getOutputStream());
        this.in = new ObjectInputStream(clientSocket.getInputStream());
        this.parsingFunction = primitiveParser;
    }



    /**
     * While {@linkplain #clientSocket} is opened,
     * executes {@linkplain #parsingFunction)} over the next command in {@linkplain #in}.
     * Catches {@link ParsingException}, {@link ClassCastException}, {@link ClassNotFoundException}, {@link IOException} logging the exception and closing the connection
     */
    @Override
    public final void run() {
        while (!clientSocket.isClosed()){
            try {
                @SuppressWarnings("unchecked")
                T command = (T) in.readObject();
                parsingFunction.parse(command, this.out, this.in, clientSocket);
            } catch ( ClassCastException | ClassNotFoundException | IOException e){
                logger.throwing(PrimitiveParserRunnable.class.getName(),"run",e);
                stop();
            } catch (ParsingException e) {
                throw new BrokenProtocolException("Parsing failed",e);
            }
        }
        stop();
    }

    /**
     * Closes {@link #in}, {@link #out}, {@link #clientSocket}.
     */
    private void stop(){
        logger.entering(PrimitiveParserRunnable.class.getName(),"stop",clientSocket);
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.finer("IOException raised while closing an IO channel. Not a problem");
        }
        logger.exiting(PrimitiveParserRunnable.class.getName(),"stop",clientSocket);
    }

}
