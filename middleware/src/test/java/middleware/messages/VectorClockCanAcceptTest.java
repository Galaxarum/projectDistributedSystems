package middleware.messages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <table>
 *     <thead>
 *         <tr>
 *             <td>Accepter</td>
 *             <td>Acceptee</td>
 *             <td>Expected</td>
 *             <td>Test</td>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>x</td>
 *             <td>x+1 (1time)</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2133_canAccept_2233()}</td>
 *         </tr>
 *         <tr>
 *             <td>x</td>
 *             <td>x+1(2times)</td>
 *             <td>F</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2133_cannotAccept_2243()}</td>
 *         </tr>
 *         <tr>
 *             <td>x</td>
 *             <td>x+2</td>
 *             <td>F</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2133_cannotAccept_4133()}</td>
 *         </tr>
 *         <tr>
 *             <td>x</td>
 *             <td>x</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2133_canAccept_2133()}</td>
 *         </tr>
 *         <tr>
 *             <td>x</td>
 *             <td><x(even many times)</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2133_canAccept_1012()}</td>
 *         </tr>
 *         <tr>
 *             <td>0</td>
 *             <td>unknown</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2033_canAccept_2U33()}</td>
 *         </tr>
 *         <tr>
 *             <td>1</td>
 *             <td>unknown</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2133_canAccept_2U33()}</td>
 *         </tr>
 *         <tr>
 *             <td>2(>1)</td>
 *             <td>unknown</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2233_canAccept_2U33()}</td>
 *         </tr>
 *         <tr>
 *             <td>unknown</td>
 *             <td>0</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2U33_canAccept_0000()}</td>
 *         </tr>
 *         <tr>
 *             <td>unknown</td>
 *             <td>1</td>
 *             <td>T</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2U33_canAccept_0100()}</td>
 *         </tr>
 *         <tr>
 *             <td>unknown</td>
 *             <td>x>1</td>
 *             <td>F</td>
 *             <td>{@link VectorClockCanAcceptTest#vector2U33_cannotAccept_0200()}</td>
 *         </tr>
 *     </tbody>
 *     <caption>Summary of test cases conducted</caption>
 * </table>
 */
public class VectorClockCanAcceptTest {

	private VectorClock vectorA;

	@BeforeEach
	public void init() {
		vectorA = new VectorClock("a");
		vectorA.put("a", 2);
		vectorA.put("b", 1);
		vectorA.put("c", 3);
		vectorA.put("d", 3);
	}

	@Test
	@DisplayName("(2,1,3,3) can accept (2,2,3,3)")
	public void vector2133_canAccept_2233(){
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",2);
		toAccept.put("b",2);
		toAccept.put("c",3);
		toAccept.put("d",3);
		assertTrue(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,1,3,3) cannot accept (2,2,4,3)")
	void vector2133_cannotAccept_2243(){
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",2);
		toAccept.put("b",2);
		toAccept.put("c",4);
		toAccept.put("d",3);
		assertFalse(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,1,3,3) cannot accept (4,1,3,3)")
	void vector2133_cannotAccept_4133(){
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",4);
		toAccept.put("b",1);
		toAccept.put("c",3);
		toAccept.put("d",3);
		assertFalse(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,1,3,3) can accept (2,1,3,3)")
	void vector2133_canAccept_2133(){
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",2);
		toAccept.put("b",1);
		toAccept.put("c",3);
		toAccept.put("d",3);
		assertTrue(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,1,3,3) can accept (1,0,1,2)")
	void vector2133_canAccept_1012(){
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",1);
		toAccept.put("b",0);
		toAccept.put("c",1);
		toAccept.put("d",2);
		assertTrue(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,1,3,3) can accept (2,unknown,3,3)")
	void vector2133_canAccept_2U33(){
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",2);
		//element at b is unknown
		toAccept.put("c",3);
		toAccept.put("d",3);
		assertTrue(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,0,3,3) can accept (2,unknown,3,3)")
	void vector2033_canAccept_2U33(){
		vectorA.put("b",0);
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",2);
		//element at b is unknown
		toAccept.put("c",3);
		toAccept.put("d",3);
		assertTrue(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,2,3,3) can accept (2,unknown,3,3)")
	void vector2233_canAccept_2U33(){
		vectorA.put("b",2);
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",2);
		//element at b is unknown
		toAccept.put("c",3);
		toAccept.put("d",3);
		assertTrue(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,unknown,3,3) cannot accept (0,2,0,0)")
	void vector2U33_cannotAccept_0200(){
		vectorA.remove("b");
		assumeTrue(vectorA.get("b")==null);
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",0);
		toAccept.put("b",2);
		toAccept.put("c",0);
		toAccept.put("d",0);
		assertFalse(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,unknown,3,3) can accept (0,1,0,0)")
	void vector2U33_canAccept_0100(){
		vectorA.remove("b");
		assumeTrue(vectorA.get("b")==null);
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",0);
		toAccept.put("b",1);
		toAccept.put("c",0);
		toAccept.put("d",0);
		assertTrue(vectorA.canAccept(toAccept));
	}

	@Test
	@DisplayName("(2,unknown,3,3) can accept (0,0,0,0)")
	void vector2U33_canAccept_0000(){
		vectorA.remove("b");
		assumeTrue(vectorA.get("b")==null);
		VectorClock toAccept = new VectorClock("b");
		toAccept.put("a",0);
		toAccept.put("b",0);
		toAccept.put("c",0);
		toAccept.put("d",0);
		assertTrue(vectorA.canAccept(toAccept));
	}
}
