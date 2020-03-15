package middleware.messages;

import lombok.Data;
import middleware.VectorClocks;

@Data
public class Message<T> {
    private T content;
    private VectorClocks timestamp;
}
