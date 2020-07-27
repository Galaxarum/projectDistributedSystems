package middleware.messages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VectorClocksTest {
    private VectorClocks vectorA;
    private VectorClocks vectorB;
    private VectorClocks vectorC;
    private final List<String> keys = List.of("a","b","c","d");


    @BeforeEach
    public void init() {
        vectorA = new VectorClocks("a");
        vectorB = new VectorClocks("b");
        vectorC = new VectorClocks("c");
        vectorA.put("a", 2);
        vectorA.put("b", 1);
        vectorA.put("c", 3);
        vectorA.put("d", 3);
        vectorB.put("a", 3);
        vectorB.put("b", 3);
        vectorB.put("c", 4);
        vectorB.put("d", 4);
        vectorC.put("a", 1);
        vectorC.put("b", 3);
        vectorC.put("c", 4);
        vectorC.put("d", 4);
    }

    @Test
    @DisplayName("Helpers are coherent with tested values")
    void checkTest(){
        assertTrue(this.keys.containsAll(vectorA.keySet()));
        assertTrue(vectorA.keySet().containsAll(this.keys));
        assertTrue(this.keys.containsAll(vectorB.keySet()));
        assertTrue(vectorB.keySet().containsAll(this.keys));
        assertTrue(this.keys.containsAll(vectorC.keySet()));
        assertTrue(vectorC.keySet().containsAll(this.keys));
    }

    @Test
    @DisplayName("(2,1,3,3)<(3,3,4,4)")
    void compareToGTandLT() {
        assertEquals(1, vectorB.compareTo(vectorA));
        assertEquals(-1,vectorA.compareTo(vectorB));
    }

    @Test
    @DisplayName("(2,1,3,3)||(1,3,4,4)")
    void compareToUncorrelated() {
        assertEquals(0, vectorC.compareTo(vectorA));
    }

    @Test
    @DisplayName("(2,1,3,3) updated with (3,3,4,4) gives (3,3,4,4)")
    void updateGrater() {
        vectorA.update(vectorB);
        assertEquals(vectorB, vectorA);
    }

    @Test
    @DisplayName("(2,1,3,3) updated with (1,3,4,4) gives (2,3,4,4)")
    void updateParallel(){
        VectorClocks expected = new VectorClocks("a");
        expected.put("a",2);
        expected.put("b",3);
        expected.put("c",4);
        expected.put("d",4);
        vectorA.update(vectorC);
        assertEquals(expected,vectorA);
    }

    @Test
    void incrementLocal() {
        VectorClocks expected = new VectorClocks("a");
        expected.put("a",3);
        expected.put("b", 1);
        expected.put("c", 3);
        expected.put("d", 3);
        vectorA.incrementLocal();
        assertEquals(expected,vectorA);
    }

    @Test
    void cloneTest(){
        assertEquals(vectorA,vectorA.clone());
        assertEquals(vectorB,vectorB.clone());
        assertEquals(vectorC,vectorC.clone());
    }

    @Test
    void removeTest(){
        vectorA.remove("d");
        assertEquals(2,vectorA.get("a"));
        assertEquals(1,vectorA.get("b"));
        assertEquals(3,vectorA.get("c"));
        assertNull(vectorA.get("d"));
    }
}