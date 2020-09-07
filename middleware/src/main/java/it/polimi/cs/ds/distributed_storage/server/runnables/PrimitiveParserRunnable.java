package it.polimi.cs.ds.distributed_storage.server.runnables;

import it.polimi.cs.ds.distributed_storage.Primitive;
import it.polimi.cs.ds.distributed_storage.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.exceptions.ParsingException;
import it.polimi.cs.ds.distributed_storage.server.functional_interfaces.PrimitiveParser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

final class PrimitiveParserRunnable<T extends Primitive> implements Runnable{

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    private final PrimitiveParser<T> parsingFunction;

    protected PrimitiveParserRunnable(Socket clientSocket, PrimitiveParser<T> primitiveParser) throws IOException {
        socket = clientSocket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        this.parsingFunction = primitiveParser;
    }

    /**
     * While the client socket is opened, listens for an incoming command and passes it to the {@link #parsingFunction}
     * Catches {@link ParsingException}, {@link ClassCastException}, {@link ClassNotFoundException}, {@link IOException} closing the connection
     */
    @Override
    public final void run() {
        while (!socket.isClosed()){
            try {
                @SuppressWarnings("unchecked")
                T command = (T) in.readObject();
                parsingFunction.parse(command, out, in, socket);
            } catch ( ClassCastException | ClassNotFoundException | IOException e){
                close();
            } catch (ParsingException e) {
                throw new BrokenProtocolException("Parsing failed",e);
            }
        }
        close();
    }

    private void close(){
        try{
            out.close();
            in.close();
            socket.close();
        } catch ( IOException ignored ) {        }
    }

}
