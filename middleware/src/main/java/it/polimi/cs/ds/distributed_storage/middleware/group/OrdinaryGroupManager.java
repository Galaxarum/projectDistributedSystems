package it.polimi.cs.ds.distributed_storage.middleware.group;

import it.polimi.cs.ds.distributed_storage.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.exceptions.ParsingException;
import it.polimi.cs.ds.distributed_storage.markers.Primitive;
import it.polimi.cs.ds.distributed_storage.middleware.messages.MessageBroker;
import it.polimi.cs.ds.distributed_storage.middleware.messages.VectorClock;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.function.Consumer;

import static it.polimi.cs.ds.distributed_storage.middleware.group.GroupCommands.*;

public class OrdinaryGroupManager<K, V> extends GroupManager<K, V> {

    public OrdinaryGroupManager(final String id,
                                final int port,
                                final Socket leaderSocket,
                                final MessageBroker<?> broker,
                                final Consumer<Map<K,V>> dataInitializer) {
        super(id, port, broker);

        try {
            //Get streams
            final ObjectOutputStream leaderOut = new ObjectOutputStream(leaderSocket.getOutputStream());
            final ObjectInputStream leaderIn = new ObjectInputStream(leaderSocket.getInputStream());

            final Socket messageSocket = new Socket(leaderSocket.getInetAddress(),leaderSocket.getPort()+NodeInfo.MESSAGES_PORT_OFFSET);
            final ObjectOutputStream mOut = new ObjectOutputStream(messageSocket.getOutputStream());
            final ObjectInputStream mIn = new ObjectInputStream(messageSocket.getInputStream());
            mOut.writeUTF(id);

            leaderOut.writeObject(JOIN);
            leaderOut.writeObject(id);

            //Save leader id for later
            final String leaderId = ( String ) leaderIn.readObject();

            //Receive list of actual replicas from the leader (the list will include the leader itself)
            final Map<String,NodeInfo> replicas = ( Map<String, NodeInfo> ) leaderIn.readObject();

            //Inform other replicas that you're joining
            replicas.forEach(this::initReplica);

            //Get data from the master replica
            leaderOut.writeObject(SYNC);
            dataInitializer.accept(( Map<K, V> ) leaderIn.readObject());

            final VectorClock initialClock  = ( VectorClock ) leaderIn.readObject();

            //Inform other replicas that you're done
            replicas.values().forEach(
                    nodeInfo -> {
                        try{
                            nodeInfo.getGroupOut().writeObject(ACK);
                        }catch ( IOException e ) {
                            throw new BrokenProtocolException("Unable to contact the replica: " + nodeInfo.getHostname());
                        }
                    }
            );

            //Save leader info
            broker.addReplica(new NodeInfo(leaderSocket,leaderOut,leaderIn,messageSocket,mOut,mIn), leaderId);

            broker.init(initialClock,replicas);

        }catch (ClassNotFoundException | ClassCastException e) {
            throw new BrokenProtocolException("Unexpected object received: ", e);
        }catch (IOException e){
            throw new BrokenProtocolException("Assumption on channel reliability failed", e);
        }

    }

    /**
     * This implementation runs the following communication protocol for each known replica:
     * <ul>
     *     <li>Inform the replica of your intention to leave the group</li>
     *     <li>Send your id to allow identification of this replica</li>
     *     <li>Wait for an ack</li>
     * </ul>
     * OPT: split send and receive for better parallelism
     */
    @Override
    public void leave() {
        //For each replica
        broker.getReplicasUnmodifiable().values().forEach(nodeInfo -> {
                    try {
                        //Load stream
                        ObjectOutputStream out = nodeInfo.getGroupOut();

                        //Inform current replica of leaving
                        out.writeObject(GroupCommands.LEAVE);
                        out.writeObject(id);

                        //Close connection
                        nodeInfo.close();
                    } catch ( IOException e ) {
                        throw new BrokenProtocolException("Assumption on channel reliability failed",e);
                    } catch ( ClassCastException e ) {
                        throw new BrokenProtocolException("Unexpected object received", e);
                    }
                });
        super.leave();
    }

    /**
     *This method will init a specific replica performing the following operations:
     * <ul>
     *     <li>Open a {@linkplain Socket} connection with the given replica, using the information holden into {@code nodeInfo}</li>
     *     <li>Send {@linkplain GroupCommands#JOINING} to the replica</li>
     *     <li>Wait for {@linkplain GroupCommands#ACK}</li>
     * </ul>
     * @param id the identifier of the replica to initialize
     * @param nodeInfo the information about the replica to initialize
     */
    //Inform other replicas of your existence and store a socket to communicate with them (They'll add you to their local list)
    private void initReplica(final String id,final NodeInfo nodeInfo) throws BrokenProtocolException {
        try {

            //Establish connection
            nodeInfo.connect();

            //Load streams
            final ObjectOutputStream newOut = nodeInfo.getGroupOut();
            final ObjectInputStream newIn = nodeInfo.getGroupIn();

            //Send "JOINING"
            newOut.writeObject(JOINING);
            newOut.writeObject(id);
            Primitive.checkEquals(ACK, newIn.readObject());
            newOut.writeObject(ACK);

            //Save the replica
            broker.addReplica(nodeInfo, id);

        } catch (IOException e) {
            throw new BrokenProtocolException("Assumption on channel reliability failed",e);
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new BrokenProtocolException("Unexpected object received: ",e);
        }
    }

    @Override
    public void parse(@NotNull final GroupCommands command,
                      final ObjectOutputStream writer,
                      final ObjectInputStream reader,
                      final Socket socket) throws ParsingException {
        try {
            final String replicaId;
            final NodeInfo replicaInfo;
            switch ( command ) {
                case JOINING:

                    //Read id
                    replicaId = ( String ) reader.readObject();

                    //Create info
                    replicaInfo = new NodeInfo(socket,writer,reader);

                    //Save info
                    broker.addReplica(replicaInfo, replicaId);

                    //Wait until you receive an ACK
                    broker.runBlocking(()->{
                        try {
                            writer.writeObject(ACK);
                            Primitive.checkEquals(ACK,reader.readObject());
                        }catch ( IOException | ClassNotFoundException e ){
                            throw new BrokenProtocolException("");
                        }
                    });
                    break;

                case LEAVE:

                    //Get id of leaving
                    replicaId = ( String ) reader.readObject();

                    //Close connection
                    broker.removeReplica(replicaId);
                    break;
                case JOIN:
                case SYNC:
                case ACK:
                default:
                    throw new ParsingException(command.toString());
            }
        }catch ( IOException | ClassNotFoundException e ){
            throw new BrokenProtocolException("Something went wrong with socket communication",e);
        }
    }
}
