package middleware.group;

import middleware.messages.VectorClocks;

import java.util.Map;


public interface GroupManager <K,V>{
    void join(Map<K,V> data, VectorClocks vectorClocks);
    void leave();
}