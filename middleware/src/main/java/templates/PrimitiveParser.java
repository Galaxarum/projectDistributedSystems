package templates;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import functional_interfaces.ParsingFunction;
import middleware.primitives.Primitive;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

class PrimitiveParser<T extends Primitive> implements Runnable{
    /**
     * The connection to the client
     */
    private final Socket clientSocket;
    /**
     * Used to read messages from the client
     */
    private final ObjectInputStream in;
    /**
     * Used to write messages to the client
     */
    private final ObjectOutputStream out;
    private final ParsingFunction<T> parsingFunction;
    /**
     * A logger
     */
    private static final Logger logger = Logger.getLogger(PrimitiveParser.class.getName());

    protected PrimitiveParser(Socket clientSocket, ParsingFunction<T> parsingFunction) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new ObjectInputStream(clientSocket.getInputStream());
        this.out = new ObjectOutputStream(clientSocket.getOutputStream());
        this.parsingFunction = parsingFunction;
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
                parsingFunction.parse(command,this::writeObjectSafe,this::readObjectSafe);
            } catch (ClassCastException | ClassNotFoundException e){
                logger.warning("Unable to deserialize an object received by the following stream: "+in.toString()+". The connection will be interrupted");
                stop();
            }catch (IOException e){
                logger.fine("IOException happened occurred communicating to the following address: "+clientSocket.getInetAddress().getHostAddress()+". The connection will be interrupted");
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
    protected final void stop(){
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.finer("IOException raised while closing an IO channel. Not a problem");
        }
        logger.finest("Stopped listening to the client at the address: "+clientSocket.getInetAddress());
    }

    /**
     * Writes the given object to {@linkplain #out}, closing the connection in case of {@link IOException}
     * @param object the Object to be written
     */
    protected void writeObjectSafe(Object object){
        try{
            out.writeObject(object);
            out.flush();
        } catch (IOException e) {
            throw new BrokenProtocolException("IOException happened when writing on the following stream: "+out.toString()+". The connection will be interrupted");
        }
    }

    protected Object readObjectSafe(){
        try {
            return in.readObject();
        }catch (IOException | ClassNotFoundException e){
            throw new BrokenProtocolException("Unable to read incoming data from the stream: "+in.toString());
        }
    }

}
