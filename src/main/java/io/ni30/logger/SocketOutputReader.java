package io.ni30.logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static io.ni30.logger.Constants.SOCKET_KEY_VALUE_DELIMETER;
import static io.ni30.logger.Utility.isValidSocketIOKey;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class SocketOutputReader extends AbstractSocketOutputReader<String, String> {
    private final ByteBuffer readByteBuffer;
    private final Queue<Map.Entry<String, String>> outputQueue = new LinkedList<>();

    public SocketOutputReader(SocketIO socketIO, int readBufferByteCapacity) {
        super(socketIO);
        this.readByteBuffer = ByteBuffer.allocate(readBufferByteCapacity);
    }

    @Override
    public synchronized Map.Entry<String, String> readNext() throws IOException {
        return super.readNext();
    }

    protected Map.Entry<String, String> convertFromByteBuffer(ByteBuffer outputBuffer) {
        while (outputBuffer.hasRemaining()) {
            byte b = outputBuffer.get();
            if(b == '\n') {
                readByteBuffer.flip();
                StringBuilder sb = new StringBuilder();
                while (readByteBuffer.hasRemaining()) {
                    sb.append((char) readByteBuffer.get());
                }

                String[] arr = sb.toString().split(String.valueOf(SOCKET_KEY_VALUE_DELIMETER), 2);
                if (arr.length == 2) {
                    Map.Entry<String, String> out = new AbstractMap.SimpleEntry<>(arr[0], arr[1]);
                    if(isValidSocketIOKey(out.getKey())) {
                        outputQueue.add(out);
                    }
                }

                readByteBuffer.clear();
            } else {
                if(!readByteBuffer.hasRemaining()) {
                    readByteBuffer.clear();
                }
                readByteBuffer.put(b);
            }
        }

        return outputQueue.poll();
    }
}