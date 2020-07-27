package middleware.messages;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Message<T> implements Comparable<Message<T>>{
    private T content;
    private VectorClock timestamp;

    @Override
    public int compareTo(@NotNull Message<T> tMessage) {
        return timestamp.compareTo(tMessage.timestamp);
    }
}
