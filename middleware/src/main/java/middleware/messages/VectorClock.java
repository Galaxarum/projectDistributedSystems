package middleware.messages;

import java.util.HashMap;

public class VectorClock extends HashMap<String, Integer> implements Comparable<VectorClock> {

    private final String localKey;

    public VectorClock(String localId) {
        this.localKey = localId;
        add(localId);
    }

    public Integer add(String deviceId) {
        return super.put(deviceId, 0);
    }

    public Integer remove(String deviceId) {
        return super.remove(deviceId);
    }

    public void update(VectorClock vector) {
        vector.forEach((k,v)->this.merge(k,v, Integer::max));
    }

    public void incrementLocal() {
        this.merge(localKey, 1, Integer::sum);
    }

    /**
     *TODO: reason if missing values can be ignored. Explain here why in case
     * @param ts
     * @return 0 if this is concurrent with ts, a value <0 if this[i]<ts[i] forall i, a value >0 if this[i]>ts[i] forall i
     */
    @Override
    public int compareTo(VectorClock ts) {
        /*
         *thisMinusTs>0 => it cannot be that this<ts
         *thisMinusTs<0 => it cannot be that this>ts
         */
        int thisMinusTs = 0;
        for ( String k : ts.keySet() ) {
            final int this_i = this.get(k);
            final int ts_i =  ts.get(k);
            if ( this.containsKey(k) ) {
                if ( thisMinusTs == 0 ) thisMinusTs = Integer.compare(this_i, ts_i);
                else if ( ( thisMinusTs >0 && this_i < ts_i ) || (thisMinusTs<0 && this_i>ts_i)) return 0;
            }
        }

        return thisMinusTs;   //equal or concurrent
    }

    /**
     * <ol>
     *     <li>A clock having ts[i]>this[i]+1 for any i must be rejected</li>
     *     <li>A clock having ts[i]=this[i]+1 for more than one 1 must be rejected</li>
     *     <li>If this doesn't have a clock that ts has for some i, this[i] is considered 0</li>
     *     <li>If the other doesn't have a clock for some i, ts[i] is considered 0 and so any value of this[i] is accepted=>this case is skipped</li>
     * </ol>
     */
    public boolean canAccept(VectorClock ts){
        boolean incrementFound = false;
        for ( Entry<String, Integer> entry : ts.entrySet()) {
            final int increasedBy1 = this.get(entry.getKey())+1;
            final int ts_i = entry.getValue();
            if( ts_i == increasedBy1 )
                if ( incrementFound ) return false;
                else incrementFound = true;
            else if( ts_i > increasedBy1 )
                return false;
        }
        return true;
    }

    public VectorClock clone(){
        VectorClock result = new VectorClock(localKey);
        result.putAll(this);
        return result;
    }
}
