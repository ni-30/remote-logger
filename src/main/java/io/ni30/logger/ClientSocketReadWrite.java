package io.ni30.logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import static io.ni30.logger.Constants.READ_BUFFER_CAPACITY;
import static io.ni30.logger.Utility.isValidSocketIOKey;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class ClientSocketReadWrite implements Closeable {

    public static ClientSocketReadWrite open(Properties properties) {
        int readBufferByteCapacity = Integer.parseInt(properties.getProperty(READ_BUFFER_CAPACITY, "1024"));

        SocketIO socketIO = null;
        try {
            socketIO = SocketIO.open(properties);
            return new ClientSocketReadWrite(socketIO, readBufferByteCapacity);
        } catch (Exception e) {
            if(socketIO != null && !socketIO.isClosed()) {
                socketIO.close();
            }
        }

        return null;
    }

    private final SocketIO socketIO;
    private final SocketOutputWriter socketInputWriter;
    private final SocketInputReader socketOutputReader;
    private String id;

    public ClientSocketReadWrite(SocketIO socketIO, int readBufferByteCapacity) {
        this.socketIO = socketIO;
        this.socketInputWriter = new SocketOutputWriter(socketIO);
        this.socketOutputReader = new SocketInputReader(socketIO, readBufferByteCapacity);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void configureBlocking(boolean isBlocking) throws IOException {
        this.socketIO.configureBlocking(isBlocking);
    }

    public boolean isClosed() {
        return socketIO.isClosed();
    }

    public void close() {
        if(!socketIO.isClosed()) {
            socketIO.close();
        }
    }

    public void write(String key, String value) throws IOException {
        if(!isValidSocketIOKey(key)) return;
        this.socketInputWriter.writeNext(key, value);
    }

    public Entry<String, String> read() throws IOException {
        return this.socketOutputReader.readNext();
    }
}
