package io.ni30.logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map.Entry;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public abstract class AbstractSocketOutputReader<K, V> {
    private final SocketIO socketIO;

    public AbstractSocketOutputReader(SocketIO socketIO) {
        this.socketIO = socketIO;
    }

    public Entry<K, V> readNext() throws IOException {
        final ByteBuffer outputBuffer = this.socketIO.readOutput();
        if(outputBuffer == null) {
            return null;
        }

        return this.convertFromByteBuffer(outputBuffer);
    }

    protected abstract Entry<K, V> convertFromByteBuffer(ByteBuffer outputBuffer);
}
