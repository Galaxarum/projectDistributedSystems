package it.polimi.cs.ds.distributed_storage.middleware.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data @AllArgsConstructor @NoArgsConstructor
public class Message<T> implements Comparable<Message<T>>{
    private T content;
    private VectorClock timestamp;

    @Override
    public int compareTo(@NotNull Message<T> tMessage) {
        return timestamp.compareTo(tMessage.timestamp);
    }
}
