package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import functional_interfaces.NetworkReader;
import functional_interfaces.NetworkWriter;
import markers.Primitive;
import middleware.MessagingMiddleware;
import middleware.messages.VectorClocks;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Logger;

import static middleware.group.GroupCommands.*;

class OrdinaryGroupManager<K, V> extends GroupManager<K, V> {

    private static final Logger logger = Logger.getLogger(OrdinaryGroupManager.class.getName());
    private static Socket leaderSocket;

    OrdinaryGroupManager(String id,
                         int port,
                         String leaderHost,
                         Map<String, NodeInfo> replicas,
                         Map<K,V> data) {
        super(id, port, replicas, data);
        try {
            OrdinaryGroupManager.leaderSocket = new Socket(leaderHost, port);
        } catch ( IOException e ) {
            throw new BrokenProtocolException("Cannot establish connection with the leader");
        }
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
    public void join(VectorClocks vectorClocks) {

        try ( ObjectOutputStream out = new ObjectOutputStream(leaderSocket.getOutputStream());
              ObjectInputStream in = new ObjectInputStream(leaderSocket.getInputStream()) ) {

            out.writeObject(JOIN);

            out.writeObject(id);
            out.flush();

            final String leaderId = ( String ) in.readObject();
            final NodeInfo leaderInfo = new NodeInfo(leaderSocket.getInetAddress().getHostName(), leaderSocket.getPort());
            leaderInfo.setSocket(leaderSocket);

            //Receive list of actual replicas from the leader (the list will include the leader itself)
            replicas.putAll(( Map<String, NodeInfo> ) in.readObject());

            replicas.forEach(this::initReplica);

            //Get data from the master replica
            out.writeObject(GroupCommands.SYNC);
            data.putAll( ( Map<K, V> ) in.readObject() );
            vectorClocks.update(( VectorClocks ) in.readObject());
            replicas.values().stream()
                    .map(NodeInfo::getSocket)
                    .forEach(socket -> {
                        try ( ObjectOutputStream r_out = new ObjectOutputStream(socket.getOutputStream()) ) {
                            r_out.writeObject(ACK);
                        } catch ( IOException e ) {
                            throw new BrokenProtocolException("Unable to contact the replica: " + socket.getInetAddress());
                        }
                    });

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
        replicas.values().stream()
                .map(NodeInfo::getSocket)
                .forEach(socket -> {
                    try ( ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()) ) {

                        //Inform current replica of leaving
                        out.writeObject(GroupCommands.LEAVE);
                        out.writeObject(id);

                        socket.close();
                    } catch ( IOException e ) {
                        throw new BrokenProtocolException("Assumption on channel reliability failed");
                    } catch ( ClassCastException e ) {
                        throw new BrokenProtocolException("Unexpected object received", e);
                    }
                });
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

            //Save the replica
            replicas.put(id, nodeInfo);

            //Send "JOINING"
            try ( ObjectOutputStream newOut = new ObjectOutputStream(replicas.get(id).getSocket().getOutputStream());
                  ObjectInputStream newIn = new ObjectInputStream(replicas.get(id).getSocket().getInputStream()) ) {
                newOut.writeObject(JOINING);
                newOut.writeObject(GroupManager.id);
                Primitive.checkEquals(ACK, newIn.readObject());
            }


        } catch (IOException e) {
            throw new BrokenProtocolException("Assumption on channel reliability failed");
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new BrokenProtocolException("Unexpected object received: ",e);
        }
    }

    @Override
    public void parse(GroupCommands command, NetworkWriter writer, NetworkReader reader, Socket socket) throws ParsingException {
        final String replicaId;
        final NodeInfo replicaInfo;
        switch ( command ) {
            case JOINING:
                //Register the replica
                replicaId = ( String ) reader.readObject();
                replicaInfo = new NodeInfo(socket.getInetAddress().getHostName(),socket.getPort());
                replicaInfo.setSocket(socket);
                replicas.put(replicaId,replicaInfo);
                //Send ack to ensure you won't proceed execution
                writer.writeObject(ACK);
                try {
                    MessagingMiddleware.operativeLock.lock();
                    Primitive.checkEquals(ACK, reader.readObject());
                } finally {
                    MessagingMiddleware.operativeLock.unlock();
                }
                break;
            case LEAVE:
                replicaId = ( String ) reader.readObject();
                try {
                    replicas.get(replicaId).getSocket().close();
                } catch ( IOException e ) {/*Ignored*/}
                replicas.remove(replicaId);
                break;
            case JOIN:
            case SYNC:
            case ACK:
            default:  //ACK should be catched in the methods expecting them
                throw new ParsingException(command.toString());
        }
    }
}
