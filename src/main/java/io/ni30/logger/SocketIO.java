package io.ni30.logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class SocketIO implements Closeable {

    public static SocketIO open(Properties properties) throws Exception {
        String serverHost = properties.getProperty(Constants.SERVER_HOST, "127.0.0.1");
        int serverPort = Integer.parseInt(properties.getProperty(Constants.SERVER_PORT, "8333"));
        int maxOutputBufferSize = Integer.parseInt(properties.getProperty(Constants.SOCKET_OUTPUT_BUFFER_CAPACITY, "4096"));

        final long waitTime = Long.parseLong(properties.getProperty(Constants.CONNECTION_TIMEOUT, "10000")); // in ms
        final CountDownLatch latch = new CountDownLatch(1);
        final IOException[] connectionException = new IOException[1];

        final SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);

        new Thread(() -> {
            SocketAddress socketAddress = new InetSocketAddress(serverHost, serverPort);
            try {
                socketChannel.connect(socketAddress);
                latch.countDown();
            } catch (IOException e) {
                connectionException[0] = e;
            }
        }).start();

        try {
            latch.await(waitTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            socketChannel.close();
            throw new RuntimeException(e);
        }

        if(latch.getCount() != 0) {
            try {
                if (connectionException[0] == null) {
                    throw new Exception("connection timeout");
                } else {
                    throw connectionException[0];
                }
            } finally {
                socketChannel.close();
            }
        }

        SocketIO socketIO = new SocketIO(socketChannel, maxOutputBufferSize);
        return socketIO;
    }

    private final SocketChannel socketChannel;
    private final int outputBufferByteCapacity;

    public SocketIO(SocketChannel socketChannel, int outputBufferByteCapacity) throws Exception {
        this.socketChannel = socketChannel;
        this.outputBufferByteCapacity = outputBufferByteCapacity;
        this.socketChannel.configureBlocking(false);
    }

    public void configureBlocking(boolean isBlocking) throws IOException {
        this.socketChannel.configureBlocking(isBlocking);
    }

    public void writeInput(ByteBuffer inputBuffer) throws IOException {
        if(inputBuffer == null || !inputBuffer.hasRemaining()) return;
        this.socketChannel.write(inputBuffer);
    }

    public ByteBuffer readOutput() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(outputBufferByteCapacity);
        this.socketChannel.read(byteBuffer);
        byteBuffer.flip();

        if(!byteBuffer.hasRemaining()) {
            return null;
        }

        return byteBuffer;
    }

    public void close() {
        if(this.socketChannel.isOpen()) {
            try {
                this.socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isClosed() {
        final boolean isClosed = !this.socketChannel.isConnected();
        if(isClosed && this.socketChannel.isOpen()) {
            try {
                this.socketChannel.close();
            } catch (IOException ignore) {
            }
        }

        return isClosed;
    }
}
