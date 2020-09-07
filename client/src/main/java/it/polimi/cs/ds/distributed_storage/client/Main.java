package it.polimi.cs.ds.distributed_storage.client;


import it.polimi.cs.ds.distributed_storage.DataOperations;

import java.io.*;
import java.net.Socket;

import static java.lang.Integer.parseInt;

public class Main {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		final String serverAddress=args[0];
		final int serverPort=parseInt(args[1]);
		final Socket socket=new Socket(serverAddress,serverPort);
		final ObjectOutputStream sout = new ObjectOutputStream(socket.getOutputStream());
		final ObjectInputStream sin =  new ObjectInputStream(socket.getInputStream());
		final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String str;
		do{
			System.out.println("Insert command");
			str = in.readLine();
			final String[] inArgs = str.split(" ");
			switch ( inArgs[0].toLowerCase() ){
				case "g": case "get":
					sout.writeObject(DataOperations.GET);
					sout.writeObject(inArgs[1]);
					break;
				case "d": case "delete":
					sout.writeObject(DataOperations.DELETE);
					sout.writeObject(inArgs[1]);
					break;
				case "p": case "put":
					sout.writeObject(DataOperations.PUT);
					sout.writeObject(inArgs[1]);
					sout.writeObject(inArgs[2]);
					break;
				default:
					System.out.println("Illegal command");
					continue;
			}
			System.out.println(sin.readObject());
		}while ( !("q".equalsIgnoreCase(str) || "quit".equalsIgnoreCase(str)) );
	}
}
