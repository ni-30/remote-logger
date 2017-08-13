package io.ni30.logger;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public abstract class AbstractSocketOutputWriter<K, V> {
    private final SocketIO socketIO;

    public AbstractSocketOutputWriter(SocketIO socketIO) {
        this.socketIO = socketIO;
    }

    public void writeNext(K key, V value) throws IOException {
        this.socketIO.writeInput(this.convertToByteBuffer(key, value));
    }

    protected abstract ByteBuffer convertToByteBuffer(K key, V value);
}
