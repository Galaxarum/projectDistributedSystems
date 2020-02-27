package middleware;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VectorClocks {

    private static VectorClocks instance;

    public static VectorClocks getInstance() {
        if(instance==null) instance = new VectorClocks();
        return instance;
    }

    /**
     * Maps ids to clocks
     */
    Map<String,Integer> clocks = new HashMap<>();
}
