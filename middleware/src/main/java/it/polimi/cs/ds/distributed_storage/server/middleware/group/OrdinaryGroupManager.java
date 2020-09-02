package it.polimi.cs.ds.distributed_storage.server.middleware.group;

import it.polimi.cs.ds.distributed_storage.server.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.server.exceptions.ParsingException;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageBroker;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.VectorClock;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static it.polimi.cs.ds.distributed_storage.server.markers.Primitive.checkEquals;
import static it.polimi.cs.ds.distributed_storage.server.middleware.group.GroupCommands.*;

public class OrdinaryGroupManager<K extends Serializable, V extends Serializable> extends GroupManager {
    public static final Logger logger = Logger.getLogger(OrdinaryGroupManager.class.getName());
    static {
        logger.setParent(GroupManager.logger);
    }

    public OrdinaryGroupManager(final String id,
                                final int port,
                                final Socket leaderSocket,
                                final MessageBroker<?> broker,
                                final Consumer<Hashtable<K,V>> dataInitializer) {
        super(id, port, broker);

        try {
            //Get streams
            final ObjectOutputStream leaderOut = new ObjectOutputStream(leaderSocket.getOutputStream());
            final ObjectInputStream leaderIn = new ObjectInputStream(leaderSocket.getInputStream());

            leaderOut.writeObject(JOIN);
            leaderOut.writeObject(id);
            logger.info("Sent join with id "+id);

            final Socket messageSocket = new Socket(leaderSocket.getInetAddress(),leaderSocket.getPort()+NodeInfo.MESSAGES_PORT_OFFSET);
            final ObjectOutputStream mOut = new ObjectOutputStream(messageSocket.getOutputStream());
            final ObjectInputStream mIn = new ObjectInputStream(messageSocket.getInputStream());
            mOut.writeObject(id);
            mOut.flush();
            logger.info("created messaging connection with "+messageSocket.getInetAddress()+" with remote port "+messageSocket.getPort());

            //Save leader id for later
            final String leaderId = ( String ) leaderIn.readObject();
            logger.info("received id: "+leaderId);

            //Receive list of actual replicas from the leader (the list will include the leader itself)
            final Map<String,NodeInfo> replicas = ( Map<String, NodeInfo> ) leaderIn.readObject();
            logger.info("received replica list of length "+replicas.size());
            replicas.values().parallelStream().forEach(NodeInfo::init);
            broker.initReplicas(replicas);

            //Inform other replicas that you're joining
            replicas.forEach(this::initReplica);

            //Get data from the master replica
            leaderOut.writeObject(SYNC);
            logger.info("Wrote SYNCH command");
            dataInitializer.accept(( Hashtable<K, V> ) leaderIn.readObject());
            logger.info("Received initial data");

            broker.initClock(( VectorClock ) leaderIn.readObject());
            logger.info("received initial clock");

            //Inform other replicas that you're done
            replicas.values().parallelStream().forEach(
                    nodeInfo -> {
                        try{
                            nodeInfo.getGroupOut().writeObject(ACK);
                            logger.info("sent ACK to "+nodeInfo.getHostname());
                        }catch ( IOException e ) {
                            throw new BrokenProtocolException("Unable to contact the replica: " + nodeInfo.getHostname());
                        }
                    }
            );

            //Save leader info
            broker.addReplica(new NodeInfo(leaderSocket,leaderOut,leaderIn,messageSocket,mOut,mIn), leaderId);
            logger.info("setup completed");

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
            logger.info("informing "+nodeInfo.getHostname()+" of leaving");
            try {
                //Load stream
                final ObjectOutputStream out = nodeInfo.getGroupOut();
                final ObjectInputStream in = nodeInfo.getGroupIn();

                //Inform current replica of leaving
                out.writeObject(GroupCommands.LEAVE);
                out.writeObject(id);
                logger.info("Informed "+nodeInfo.getHostname()+" of leaving");

                checkEquals(ACK,in.readObject());

                //Close connection
                nodeInfo.close();
            } catch ( IOException e ) {
                throw new BrokenProtocolException("Assumption on channel reliability failed",e);
            } catch ( ClassCastException | ClassNotFoundException e ) {
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
            logger.info("connecting to "+nodeInfo.getHostname());
            nodeInfo.connect(this.port,this.id);

            //Load streams
            final ObjectOutputStream newOut = nodeInfo.getGroupOut();
            final ObjectInputStream newIn = nodeInfo.getGroupIn();

            //Save the replica
            broker.addReplica(nodeInfo, id);
            logger.info("connected to "+nodeInfo.getHostname());

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
            logger.info("parsing "+command);
            switch ( command ) {
                case JOINING:

                    //Read id
                    replicaId = ( String ) reader.readObject();

                    //Create info
                    replicaInfo = new NodeInfo(socket,writer,reader);

                    //Save info
                    broker.addReplica(replicaInfo, replicaId);
                    replicaInfo.getGroupOut().writeObject(ACK);
                    logger.info("Added replica "+replicaId+" at "+replicaInfo.getHostname());

                    //Wait until you receive an ACK
                    broker.runBlocking(()->{
                        try {
                            checkEquals(ACK,reader.readObject());
                        }catch ( IOException | ClassNotFoundException e ){
                            throw new BrokenProtocolException("");
                        }
                    });
                    logger.info("Replica "+replicaId+" joined");
                    break;

                case LEAVE:
                    //Get id of leaving
                    replicaId = ( String ) reader.readObject();
                    //Close connection
                    broker.removeReplica(replicaId);
                    logger.info("removed replica "+replicaId);
                    writer.writeObject(ACK);
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
