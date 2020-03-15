package middleware.group;

import java.util.Map;


public interface GroupManager <K,V>{
    Map<K,V> join() ;
    void leave();

}