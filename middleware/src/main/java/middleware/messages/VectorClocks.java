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

    public void incrementLocal(VectorClocks vector) {
        //TODO
    }

    @Override
    public int compareTo(VectorClocks vectorClocks) {
        /*assuming all vector clocks contain the key of all active processes (otherwise two vector clocks that don't
        contain all the same processes should be considered concurrent (probably?) even if all other key-value pairs
        indicate causality)
         */
        int res = 0;
        for (String k : vectorClocks.keySet()) {
            if (res == 0) {
                if (this.get(k) > vectorClocks.get(k))
                    res = 1;
                else if (this.get(k) < vectorClocks.get(k))
                    res = -1;
            }
            else if (res == 1) {
                if (this.get(k) < vectorClocks.get(k))
                    return 0;
            }
            else    //res == -1
                if (this.get(k) > vectorClocks.get(k))
                    return 0;
        }

        return 0;   //equal or concurrent
    }
}
