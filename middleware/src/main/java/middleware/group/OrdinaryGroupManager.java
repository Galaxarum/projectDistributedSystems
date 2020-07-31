package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import markers.Primitive;
import middleware.MessagingMiddleware;
import middleware.messages.VectorClock;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

import static middleware.group.GroupCommands.*;

class OrdinaryGroupManager<K, V> extends GroupManager<K, V> {

    private final NodeInfo leaderInfo;

    OrdinaryGroupManager(String id,
                         int port,
                         Socket leaderSocket,
                         Map<String, NodeInfo> replicas,
                         Map<K,V> data) throws IOException {
        super(id, port, replicas, data);

        //Create leader info
        leaderInfo = new NodeInfo(leaderSocket);

    }

    /**
     * This implementation runs the following communication protocol:
     * <ul>
     *     <li>Inform the leader of the fact you're joining</li>
     *     <li>Receive replica list</li>
     *     <li>Inform all the replicas of the fact you're joining</li>
     *     <li>Ask the leader for a copy of the data</li>
     *     <li>Ack the leader</li>
     *     <li>Return the received data</li>
     * </ul>
     * @implNote Since processes will never fail by assumption, we may assume that the known replica will always be the same replica (for instance the first replica created).
     * This allows us to avoid implementing some kind of distributed mutex mechanism, keeping it a local mutex managed by the leader process
     */
    @SuppressWarnings("unchecked")
    @Override
    public void join(VectorClock vectorClock) {

        try {
            //Get streams
            ObjectOutputStream leaderOut = leaderInfo.getGroupOut();
            ObjectInputStream leaderIn = leaderInfo.getGroupIn();
            leaderOut.writeObject(JOIN);
            leaderOut.writeObject(id);

            //Save leader id for later
            final String leaderId = ( String ) leaderIn.readObject();

            //Receive list of actual replicas from the leader (the list will include the leader itself)
            replicas.putAll(( Map<String, NodeInfo> ) leaderIn.readObject());

            //Inform other replicas that you're joining
            replicas.forEach(this::initReplica);

            //Get data from the master replica
            leaderOut.writeObject(SYNC);
            data.putAll( ( Map<K, V> ) leaderIn.readObject() );
            vectorClock.update(( VectorClock ) leaderIn.readObject());

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
            replicas.put(leaderId, leaderInfo);

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
        replicas.values()
                .forEach(nodeInfo -> {
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
    private void initReplica(String id,NodeInfo nodeInfo) throws BrokenProtocolException {
        try {

            //Establish connection
            nodeInfo.connect();

            //Load streams
            ObjectOutputStream newOut = nodeInfo.getGroupOut();
            ObjectInputStream newIn = nodeInfo.getGroupIn();

            //Send "JOINING"
            newOut.writeObject(JOINING);
            newOut.writeObject(GroupManager.id);
            Primitive.checkEquals(ACK, newIn.readObject());

            //Save the replica
            replicas.put(id, nodeInfo);

        } catch (IOException e) {
            throw new BrokenProtocolException("Assumption on channel reliability failed",e);
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new BrokenProtocolException("Unexpected object received: ",e);
        }
    }

    @Override
    public void parse(@NotNull GroupCommands command, ObjectOutputStream writer, ObjectInputStream reader, Socket socket) throws ParsingException {
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
                    replicas.put(replicaId, replicaInfo);

                    //Send ack to ensure you won't proceed execution
                    writer.writeObject(ACK);

                    //Wait until you recive an ACK
                    try {
                        MessagingMiddleware.operativeLock.lock();
                        Primitive.checkEquals(ACK, reader.readObject());
                    } finally {
                        MessagingMiddleware.operativeLock.unlock();
                    }
                    break;

                case LEAVE:

                    //Get id of leaving
                    replicaId = ( String ) reader.readObject();

                    //Close connection
                    replicas.get(replicaId).close();

                    //Forget the replica
                    replicas.remove(replicaId);
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
