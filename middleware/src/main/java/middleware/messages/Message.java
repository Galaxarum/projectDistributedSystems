package middleware.messages;

import lombok.Data;

@Data
public class Message<T> {
    private T content;
    private VectorClocks timestamp;
}
