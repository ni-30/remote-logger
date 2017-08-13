package io.ni30.logger;

import java.nio.ByteBuffer;

import static io.ni30.logger.Constants.SOCKET_KEY_VALUE_DELIMETER;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class SocketInputWriter extends AbstractSocketInputWriter<String, String> {

    public SocketInputWriter(SocketIO socketIO) {
        super(socketIO);
    }

    protected ByteBuffer convertToByteBuffer(String key, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(SOCKET_KEY_VALUE_DELIMETER).append(value).append('\n');

        ByteBuffer byteBuffer = ByteBuffer.allocate(sb.length());
        for(int i = 0; i < sb.length(); i++) {
            byteBuffer.put((byte) sb.charAt(i));
        }

        return byteBuffer;
    }
}