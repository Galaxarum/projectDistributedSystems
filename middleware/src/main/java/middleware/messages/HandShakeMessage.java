package middleware.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@RequiredArgsConstructor @Getter
public class HandShakeMessage implements Serializable {
    private final boolean fromClient;
}
