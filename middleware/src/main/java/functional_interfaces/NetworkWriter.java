package functional_interfaces;

@FunctionalInterface
public interface NetworkWriter {
    void writeObject(Object object);
}
