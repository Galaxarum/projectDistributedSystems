package middleware.networkThreads;

import exceptions.ParsingException;
import middleware.primitives.GroupCommands;
import templates.PrimitiveParser;

import java.io.IOException;
import java.net.Socket;

import static middleware.primitives.GroupCommands.ACK;

public class P2PConnection extends PrimitiveParser<GroupCommands> {

    private P2PConnection(Socket clientSocket) throws IOException {
        super(clientSocket);
    }

    protected void parseCommand(GroupCommands command) throws ParsingException {
        switch (command){
            case JOIN:
                //Register the replica
                //Write replica list to out
                break;
            case JOINING:
                //Register the replica
                writeObjectSafe(ACK);
                break;
            case SYNC:
                //Send a copy of the local data
                //Send a copy of the local vector clock

            case ACK:

            default:
                throw new ParsingException(command.toString());
        }
    }

    public static P2PConnection getInstance(Socket clientSocket) throws IOException {
        return new P2PConnection(clientSocket);
    }

}
