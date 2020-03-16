package middleware.messages;

import java.util.HashMap;

public class VectorClocks extends HashMap<String, Integer> implements Comparable<VectorClocks>{
    public Integer add(String deviceId) {
        return super.put(deviceId, 0);
    }

    public Integer remove(String deviceId) {
        return super.remove(deviceId);
    }

    public void update(VectorClocks vector) {
        vector.forEach((k,v)->this.merge(k,v, Integer::max));
    }

    @Override
    public int compareTo(VectorClocks vectorClocks) {
        //TODO
        return 0;
    }
}
