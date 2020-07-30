package runnables;

import exceptions.BrokenProtocolException;
import functional_interfaces.PrimitiveParser;
import lombok.AllArgsConstructor;
import markers.Primitive;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

public class ServerSocketRunnableTest {
	private static int PORT;
	private static final PrimitiveParser<TestPrimitive> PARSING_FUNCTION = (x, writer, reader, socket) -> {
		try {
			writer.writeObject(x.asInt + 1);
			TestPrimitive response = ( TestPrimitive ) reader.readObject();
			writer.writeObject(response.equals(TestPrimitive.ZERO));
		}catch ( IOException | ClassNotFoundException e ){
			throw new BrokenProtocolException("Broken protocol!");
		}
	};

	private static Thread tested;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	@BeforeAll
	public static void startListener() throws IOException {
		final ServerSocket serverSocket = new ServerSocket(0);
		PORT = serverSocket.getLocalPort();
		tested = new Thread(new ServerSocketRunnable<>(serverSocket, PARSING_FUNCTION));
		tested.start();
	}

	@BeforeEach
	public void openConnection() throws IOException {
		socket = new Socket(InetAddress.getLoopbackAddress(), PORT);
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	}

	@AfterEach
	public void closeConnection() throws IOException {
			in.close();
			out.close();
			socket.close();
			tested.interrupt();
	}

	@Test
	@DisplayName("A socket can read and write to the ServerSocketRunnable and the specified PrimitiveParserRunnable is created")
	public void newSocket__createdAndCanReadAndWriteAndTheParsingFunctionRunsWell() throws IOException, ClassNotFoundException {
		out.writeObject(TestPrimitive.ONE);
		int answer = ( int ) in.readObject();
		assertEquals(2, answer);
		out.writeObject(TestPrimitive.ZERO);
		assertTrue(( Boolean ) in.readObject());
	}

	 @Test
	 @DisplayName("Closing the connection doesn't throw any exception client side")
	 public void socketClosed__noException(){
		assertDoesNotThrow(()->{
			out.close();
			in.close();
			socket.close();
		});
	 }

	 @AfterAll
	 public static void closeThread(){
		tested.interrupt();
	 }

	@AllArgsConstructor
	private enum TestPrimitive implements Primitive {
		ZERO(0),
		ONE(1),
		TWO(2),
		THREE(3);
		private final int asInt;
	}
}
