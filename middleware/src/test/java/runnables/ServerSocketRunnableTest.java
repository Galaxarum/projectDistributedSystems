package runnables;

import exceptions.BrokenProtocolException;
import functional_interfaces.PrimitiveParser;
import lombok.AllArgsConstructor;
import markers.Primitive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerSocketRunnableTest {
	private static final int PORT = 12345;
	private static final PrimitiveParser<TestPrimitive> PARSING_FUNCTION = (x, writer, reader, socket) -> {
		try {
			writer.writeObject(x.asInt + 1);
			TestPrimitive response = ( TestPrimitive ) reader.readObject();
			writer.writeObject(response.equals(TestPrimitive.ZERO));
		}catch ( IOException | ClassNotFoundException e ){
			throw new BrokenProtocolException("Broken protocol!");
		}
	};

	@BeforeAll
	public static void startListener() throws IOException {
		new Thread(new ServerSocketRunnable<>(new ServerSocket(PORT), PARSING_FUNCTION)).start();
	}

	@Test
	@DisplayName("A socket to localhost:PORT can read and write to the ServerSocketRunnable and the specified PrimitiveParserRunnable is created")
	public void newSocketToLocalhostAndPORT__createdAndCanReadAndWriteAndTheParsingFunctionRunsWell() throws IOException, ClassNotFoundException {
		final Socket socket = new Socket(InetAddress.getLoopbackAddress(), PORT);
		final ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
		final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
		out.writeObject(TestPrimitive.ONE);
		int answer = ( int ) in.readObject();
		assertEquals(2, answer);
		out.writeObject(TestPrimitive.ZERO);
		assertTrue(( Boolean ) in.readObject());
	}

	@AllArgsConstructor
	private enum TestPrimitive implements Primitive {
		ZERO(0),
		ONE(1),
		TWO(2),
		THREE(3);
		private int asInt;
	}
}
