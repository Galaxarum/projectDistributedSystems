package it.polimi.cs.ds.distributed_storage.server.middleware.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data @AllArgsConstructor @NoArgsConstructor
public class Message<T extends Serializable> implements Comparable<Message<T>>, Serializable {
    private T content;
    private VectorClock timestamp;

    @Override
    public int compareTo(@NotNull Message<T> tMessage) {
        return timestamp.compareTo(tMessage.timestamp);
    }
}
