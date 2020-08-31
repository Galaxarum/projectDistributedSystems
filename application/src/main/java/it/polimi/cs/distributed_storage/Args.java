package it.polimi.cs.distributed_storage;

import it.polimi.cs.distributed_storage.database.DatabaseManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import it.polimi.cs.ds.distributed_storage.middleware.MessagingMiddleware;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Args {
	public static final int ID_INDEX = 0;
	public static final int LEADER_HOST_INDEX = 1;
	public static final int LEADER_PORT_INDEX = 2;
	public static final int MIDDLEWARE_PORT_INDEX = 3;
	public static final int CLIENT_PORT_INDEX = 4;
	public static final int STORAGE_PATH_INDEX = 5;
	private static final int SIZE = 6;
	/**
	 * The value that will be assigned to an illegal or missing port specification
	 */
	public static final String DEFAULT_STRING = "d";
	/**
	 * The string to use in the position {@value LEADER_HOST_INDEX} when running the very first replica of a group
	 */
	public static final String FIRST_REPLICA_DISCRIMINATOR = "first";
	public static final int DEFAULT_CLIENT_PORT = 12350;

	public static final String ARGS_DIGEST = "To start the application provide the following arguments in the following order"+System.lineSeparator()+
			"Id_of_this_node, " +
			"Address_of_the_leader_replica(use \""+FIRST_REPLICA_DISCRIMINATOR+"\" when starting the first replica), " +
			"[Port_of_the_leader_replica | " + DEFAULT_STRING + "], "+
			"[Port_for_communication_with_other_replicas | " + DEFAULT_STRING +"], " +
			"[Port_for_communication_with_clients | " + DEFAULT_STRING +"], " +
			"[Path_to_file_used_for_data_persistence | " + DEFAULT_STRING +"]";

	public static Map<Integer,String> parse(String[] args){
		final Map<Integer,String> result = new HashMap<>(SIZE);
		try{
			final boolean isLeader = args[LEADER_HOST_INDEX].equals(FIRST_REPLICA_DISCRIMINATOR);
			result.put(ID_INDEX,args[ID_INDEX]);
			result.put(LEADER_HOST_INDEX,isLeader ?
					null :
					args[LEADER_HOST_INDEX]);
			result.put(LEADER_PORT_INDEX, isValidArg(args,LEADER_PORT_INDEX)?
					args[LEADER_PORT_INDEX]:
					String.valueOf(MessagingMiddleware.DEFAULT_STARTING_PORT));
			result.put(MIDDLEWARE_PORT_INDEX, isValidArg(args,MIDDLEWARE_PORT_INDEX) ?
					args[MIDDLEWARE_PORT_INDEX]:
					String.valueOf(MessagingMiddleware.DEFAULT_STARTING_PORT));
			result.put(CLIENT_PORT_INDEX,isValidArg(args,CLIENT_PORT_INDEX) ?
					args[CLIENT_PORT_INDEX] :
					String.valueOf(DEFAULT_CLIENT_PORT));
			result.put(STORAGE_PATH_INDEX,isValidArg(args,STORAGE_PATH_INDEX) ?
					args[STORAGE_PATH_INDEX] :
					DatabaseManager.DEFAULT_PATH);
			return result;
		}catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
			IllegalArgumentException e1 = new IllegalArgumentException(e);
			System.out.println(ARGS_DIGEST);
			throw e1;
		}
	}

	private static boolean isValidArg(@NotNull String[] args, int index){
		return args.length>index && !args[index].equals(DEFAULT_STRING);
	}
}
