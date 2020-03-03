package middleware;

import lombok.AccessLevel;
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
    private VectorClocks clocks = new VectorClocks();
    
    public VectorClocks getClocks(){
   	 return clocks;
    }
    
    public void add(String deviceId, Integer timestamp) {
   	 clocks.put(deviceId, timestamp);
    }
    
    public void remove(String deviceId) {
   	 clocks.remove(deviceId);
    }
}