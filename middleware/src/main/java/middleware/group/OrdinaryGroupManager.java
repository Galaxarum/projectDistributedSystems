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

public class OrdinaryGroupManager<K, V> extends GroupManager<K, V> {

    public OrdinaryGroupManager(String id,
                         int port,
                         Socket leaderSocket,
                         VectorClock initialClock,
                         MessagingMiddleware<K,V,?> owner) throws IOException {
        super(id, port, owner);
        final Map<String, NodeInfo> replicas = owner.getReplicas();
        final Map<K,V> data = owner.getData();

        //Create leader info
        final NodeInfo leaderInfo = new NodeInfo(leaderSocket);

        try {
            //Get streams
            final ObjectOutputStream leaderOut = leaderInfo.getGroupOut();
            final ObjectInputStream leaderIn = leaderInfo.getGroupIn();
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
            initialClock.update(( VectorClock ) leaderIn.readObject());

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
        owner.getReplicas().values()
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
            newOut.writeObject(id);
            Primitive.checkEquals(ACK, newIn.readObject());
            newOut.writeObject(ACK);

            //Save the replica
            owner.getReplicas().put(id, nodeInfo);

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
            final Map<String,NodeInfo> replicas = owner.getReplicas();
            switch ( command ) {
                case JOINING:

                    //Read id
                    replicaId = ( String ) reader.readObject();

                    //Create info
                    replicaInfo = new NodeInfo(socket,writer,reader);

                    //Save info
                    replicas.put(replicaId, replicaInfo);

                    //Wait until you receive an ACK
                    owner.runCriticalOperation(()->{
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
