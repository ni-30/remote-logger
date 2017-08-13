package io.ni30.logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import static io.ni30.logger.Constants.*;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class ServerManager {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String serverHost;
    private final int serverPort;
    private final int outputBufferByteCapacity;
    private final int readBufferByteCapacity;
    private final int clientPoolSize;
    private ServerSocketChannel serverSocketChannel;
    private final AtomicInteger totalOpenClientSocket = new AtomicInteger(0);
    private final Object waitObject = new Object();
    private final ClientSocketHandler clientSocketHandler;

    public ServerManager(Properties properties) {
        this.serverHost = properties.getProperty(SERVER_HOST, "127.0.0.1");
        this.serverPort = Integer.parseInt(properties.getProperty(SERVER_PORT, "8333"));
        this.outputBufferByteCapacity = Integer.parseInt(properties.getProperty(SOCKET_OUTPUT_BUFFER_CAPACITY, "4096"));
        this.readBufferByteCapacity = Integer.parseInt(properties.getProperty(READ_BUFFER_CAPACITY, "1024"));
        this.clientPoolSize = Integer.parseInt(properties.getProperty(CLIENT_POOL_SIZE, (2 * Runtime.getRuntime().availableProcessors())+""));

        this.clientSocketHandler = new ClientSocketHandler(this.clientPoolSize, totalOpenClientSocket);
    }

    public void run() throws Exception {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(true);
        this.serverSocketChannel.bind(new InetSocketAddress(serverHost, serverPort));

        Future f = executorService.submit(() -> runAcceptor());
        f.get();
    }

    public void shutdown() throws IOException {
        if(!this.executorService.isShutdown()) {
            executorService.shutdown();
        }
        clientSocketHandler.shutdown();
        if(serverSocketChannel != null && serverSocketChannel.isOpen()) {
            serverSocketChannel.close();
        }
    }

    private void runAcceptor() {
        System.out.println("[RemoteLoggerServer] started client acceptor loop");

        while (!Thread.interrupted() && serverSocketChannel.isOpen()) {
            SocketChannel socketChannel = null;
            try {
                socketChannel = serverSocketChannel.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                socketChannel.finishConnect();
                SocketIO socketIO = new SocketIO(socketChannel, outputBufferByteCapacity);
                ClientSocketReadWrite clientSocketReadWrite = new ClientSocketReadWrite(socketIO, readBufferByteCapacity);

                this.clientSocketHandler.handle(clientSocketReadWrite);

                if(this.totalOpenClientSocket.get() > clientPoolSize) {
                    this.waitForClientsToClose();
                }
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        System.out.println("[RemoteLoggerServer] stopped client acceptor loop");
    }

    private void waitForClientsToClose() {
        while (this.serverSocketChannel.isOpen() && this.totalOpenClientSocket.get() > this.clientPoolSize) {
            try {
                this.waitObject.wait(15000);
            } catch (InterruptedException ignore) {
            }
        }
    }
}
