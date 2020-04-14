package middleware.messages;

import org.junit.Before;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

class VectorClocksTest {
    VectorClocks vectorA = new VectorClocks();
    VectorClocks vectorB = new VectorClocks();
    VectorClocks vectorC = new VectorClocks();

    @Before
    public void init() {
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

    @org.junit.jupiter.api.Test
    void compareToGT() {
        assertEquals(1, vectorB.compareTo(vectorA));
    }

    @org.junit.jupiter.api.Test
    void compareToLT() {
        assertEquals(-1, vectorA.compareTo(vectorB));
    }

    @org.junit.jupiter.api.Test
    void compareToUncorrelated() {
        assertEquals(0, vectorB.compareTo(vectorA));
    }

    @Test
    void update() {
        VectorClocks vector = new VectorClocks();
        vector.put("a", 2);
        vector.put("b", 3);
        vector.put("c", 4);
        vector.put("d", 4);
        vectorA.update(vectorC);
        assertSame(vector, vectorA);
    }

    @Test
    void incrementLocal() {
    }
}