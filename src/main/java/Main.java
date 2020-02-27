import interfaces.DataStoreInterface;
import interfaces.GroupCommunicationInterface;
import network.ServerSocketListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Logger;

public class Main {

    public static final int DEFAULT_PORT = 12345;
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        final int port = initPort(args);

        try{
            new Thread(new ServerSocketListener(new ServerSocket(port))).start();
        } catch (IOException e) {
            logger.severe("Unable to open ServerSocket");
            e.printStackTrace();
            return;
        }

        /*TODO: contact another replica
          TODO: get aligned with the contacted replica
         */

    }

    private static int initPort(String[] args){
            try{
                return Integer.parseInt(args[1]);
            }catch (NumberFormatException | ArrayIndexOutOfBoundsException e){
                return DEFAULT_PORT;
            }
    }
}
