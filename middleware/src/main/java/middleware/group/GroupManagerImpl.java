package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import lombok.Getter;
import markers.Primitive;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static middleware.group.GroupCommands.*;


public class GroupManagerImpl<K,V> implements GroupManager<K,V>{

    private final String MY_ID;
    private final Socket leaderSocket;
    //TODO: this must be shared with messaging middleware impl
    @Getter
    private Map<String,Socket> socketMap = new HashMap<>();
    //TODO: this must be shared with messaging middleware impl
    // OPT: this is mandatory only for the leader replica
    @Getter
    private Map<String,NodeInfo> replicas = new HashMap<>();

    public GroupManagerImpl(String id, int port, String leaderHost) {
        try {
            this.MY_ID = id;
            this.leaderSocket = new Socket(leaderHost, port);
            new Thread(new ServerSocketRunnable<GroupCommands>(0,(command, writer, reader) -> {
                //TODO
                switch (command){
                    case JOIN:
                        //Register the replica
                        //Write replica list to out
                        break;
                    case JOINING:
                        //Register the replica
                        writer.writeObject(ACK);
                        break;
                    case SYNC:
                        //Send a copy of the local data
                        //Send a copy of the local vector clock

                    case ACK:

                    default:
                        throw new ParsingException(command.toString());
                }
            })).start();
        }catch (IOException e){
            throw new BrokenProtocolException("Impossible to initialize the connection",e);
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
     * @return A copy of the application data
     * @implNote Since processes will never fail by assumption, we may assume that the known replica will always be the same replica (for instance the first replica created).
     * This allows us to avoid implementing some kind of distributed mutex mechanism, keeping it a local mutex managed by the leader process
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<K,V> join() {

        try(ObjectOutputStream out = new ObjectOutputStream(leaderSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(leaderSocket.getInputStream())) {

            Map<K, V> data;

            out.writeObject(JOIN);
            out.writeObject(MY_ID);

            //Receive list of actual replicas from the leader (the list will include the leader itself)
            replicas = (Map<String, NodeInfo>) in.readObject();

            replicas.forEach(this::initReplica);

            //Get data from the master replica
            out.writeObject(GroupCommands.SYNC);
            data = (Map<K, V>) in.readObject();
            out.writeObject(GroupCommands.ACK);
            return data;

        }catch (ClassNotFoundException | ClassCastException e) {
            throw new BrokenProtocolException("Unexpected object received", e);
        }catch (IOException e){
            throw new BrokenProtocolException("Assumption on channel reliability failed");
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
        socketMap.forEach((id,socket)->{
            try(ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())){

                //Inform current replica of leaving
                out.writeObject(GroupCommands.LEAVE);
                out.writeObject(MY_ID);

                //Expect Ack
                final GroupCommands ack = (GroupCommands) in.readObject();
                Primitive.checkEquals(GroupCommands.ACK,ack);

            } catch (IOException e) {
                throw new BrokenProtocolException("Assumption on channel reliability failed");
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new BrokenProtocolException("Unexpected object received",e);
            }
        });
    }

    /**
     *This method will init a specific replica performing the following operations:
     * <ul>
     *     <li>Open a {@linkplain Socket} connection with the given replica, using the information holden into {@code nodeInfo}</li>
     *     <li>Send {@linkplain GroupCommands#JOINING} to the replica</li>
     *     <li>Wait for {@linkplain GroupCommands#ACK}</li>
     *     <li>Store the tuple {@code <id, socket>} into {@linkplain #socketMap}</li>
     * </ul>
     * @param id the identifier of the replica to initialize
     * @param nodeInfo the information about the replica to initialize
     */
    //Inform other replicas of your existence and store a socket to communicate with them (They'll add you to their local list)
    //OPT: Split sending JOINING and receiving ACK for better parallelism
    private void initReplica(String id,NodeInfo nodeInfo){
        try {

            //Create a socket for the replica
            Socket newSocket = new Socket(nodeInfo.getHostname(), nodeInfo.getPort());

            //Send "JOINING" and expect "ACK"
            try (ObjectOutputStream newOut = new ObjectOutputStream(newSocket.getOutputStream());
                 ObjectInputStream newIn = new ObjectInputStream(newSocket.getInputStream())) {
                newOut.writeObject(JOINING);
                final GroupCommands ack = (GroupCommands) newIn.readObject();
                Primitive.checkEquals(GroupCommands.ACK,ack);
            }

            //Save the replica
            socketMap.put(id, newSocket);

        } catch (IOException e) {
            throw new BrokenProtocolException("Assumption on channel reliability failed");
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new BrokenProtocolException("Unexpected object received",e);
        }
    }
}
