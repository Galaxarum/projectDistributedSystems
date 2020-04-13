package middleware.messages;

import java.util.HashMap;

public class VectorClocks extends HashMap<String, Integer> implements Comparable<VectorClocks> {

    private final String localKey;

    public VectorClocks(String localId) {
        this.localKey = localId;
        add(localId);
    }

    public Integer add(String deviceId) {
        return super.put(deviceId, 0);
    }

    public Integer remove(String deviceId) {
        return super.remove(deviceId);
    }

    public void update(VectorClocks vector) {
        vector.forEach((k, v) -> this.merge(k, v, Integer::max));
    }

    public void incrementLocal() {
        this.merge(localKey, 1, Integer::sum);
    }

    @Override
    public int compareTo(VectorClocks vectorClocks) {
        int res = 0;
        for ( String k : vectorClocks.keySet() ) {
            /*nodes existing in one and not in the other are ignored (by the following if, if the "extra" node is in vectorClocks,
             *just not considered by the for loop if the extra node is in this
             */
            if ( this.containsKey(k) ) {
                if ( res == 0 ) {
                    if ( this.get(k) > vectorClocks.get(k) )
                        res = 1;
                    else if ( this.get(k) < vectorClocks.get(k) )
                        res = -1;
                } else if ( res == 1 ) {
                    if ( this.get(k) < vectorClocks.get(k) )
                        return 0;
                } else    //res == -1
                    if ( this.get(k) > vectorClocks.get(k) )
                        return 0;
            }
        }

        return 0;   //equal or concurrent
    }
}
