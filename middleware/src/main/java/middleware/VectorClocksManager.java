package middleware;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VectorClocksManager {

    private static VectorClocksManager instance;

    public static VectorClocksManager getInstance() {
        if(instance==null) instance = new VectorClocksManager();
        return instance;
    }

    /**
     * Maps ids to clocks
     */
    @Getter
    private VectorClocks clocks = new VectorClocks();
    
    public void add(String deviceId, Integer timestamp) {
   	 clocks.put(deviceId, timestamp);
    }
    
    public void remove(String deviceId) {
   	 clocks.remove(deviceId);
    }
}