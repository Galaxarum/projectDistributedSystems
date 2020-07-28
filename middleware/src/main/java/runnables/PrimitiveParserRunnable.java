package runnables;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import functional_interfaces.PrimitiveParser;
import markers.Primitive;
import middleware.group.NodeInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

final class PrimitiveParserRunnable<T extends Primitive> implements Runnable{

    private final NodeInfo client;

    private final PrimitiveParser<T> parsingFunction;

    protected PrimitiveParserRunnable(Socket clientSocket, PrimitiveParser<T> primitiveParser) throws IOException {
        client = new NodeInfo(clientSocket);
        this.parsingFunction = primitiveParser;
    }

    /**
     * While the client socket is opened, listens for an incoming command and passes it to the {@link #parsingFunction}
     * Catches {@link ParsingException}, {@link ClassCastException}, {@link ClassNotFoundException}, {@link IOException} closing the connection
     */
    @Override
    public final void run() {
        while (!client.getSocket().isClosed()){
            try {
                @SuppressWarnings("unchecked")
                T command = (T) client.getIn().readObject();
                parsingFunction.parse(command, client.getOut(), client.getIn(), client.getSocket());
            } catch ( ClassCastException | ClassNotFoundException | IOException e){
                client.close();
            } catch (ParsingException e) {
                throw new BrokenProtocolException("Parsing failed",e);
            }
        }
        client.close();
    }

}
