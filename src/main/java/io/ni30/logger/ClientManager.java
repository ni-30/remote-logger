package io.ni30.logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import static io.ni30.logger.Constants.*;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class ClientManager {
    private static ClientManager INSTANCE;

    public static ClientManager getOrCreate(Properties properties) {
        if(INSTANCE == null) {
            synchronized (ClientManager.class) {
                if(INSTANCE == null) {
                    String groupName = properties.getProperty(GROUP_NAME, ANONYMOUS);
                    ClientFileManager clientFileManager = new ClientFileManager(groupName);
                    INSTANCE = new ClientManager(properties, clientFileManager);
                }
            }
        }

        return INSTANCE;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ClientSocketReadWrite clientSocketReadWrite;
    private final ClientFileManager clientFileManager;
    private boolean isShutdown = false;

    private RandomAccessFile inputFile;
    private FileChannel inputChannel;
    private RandomAccessFile outputFile;
    private FileChannel outputChannel;

    public ClientManager(Properties properties, ClientFileManager clientFileManager) {
        this.clientFileManager = clientFileManager;
        this.clientSocketReadWrite = ClientSocketReadWrite.open(properties);
        this.clientSocketReadWrite.setId(properties.getProperty(CLIENT_ID));

        try {
            inputFile = this.clientFileManager.getWritableServerLogFile(this.clientSocketReadWrite.getId());
            outputFile = this.clientFileManager.getReadableClientLogFile(this.clientSocketReadWrite.getId());
        } catch (IOException e) {
            if(inputFile != null) {
                try {
                    inputFile.close();
                } catch (IOException e1) {
                } finally {
                    if(outputFile != null) {
                        try {
                            outputFile.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }

            throw new RuntimeException(e);
        }

        inputChannel = inputFile.getChannel();
        outputChannel = outputFile.getChannel();
    }

    public ClientFileManager getClientFileManager() {
        return this.clientFileManager;
    }

    public void run() throws Exception {
        try {
            this.handshake();
        } catch (Exception e) {
            if(!clientSocketReadWrite.isClosed()) clientSocketReadWrite.close();

            throw e;
        }

        this.clientSocketReadWrite.configureBlocking(true);

        Future f = this.executorService.submit(() -> runLoop());
        f.get();
    }

    public String getClientId() {
        return this.clientSocketReadWrite.getId();
    }

    public boolean isShutdown() {
        return this.isShutdown;
    }

    public void shutdown() {
        try {
            this.executorService.shutdown();
            isShutdown = true;
        } finally {
            if (this.clientSocketReadWrite != null && !this.clientSocketReadWrite.isClosed()) {
                this.clientSocketReadWrite.close();
            }

            try {
                this.inputChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                this.outputChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                this.inputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                this.outputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runLoop() {
        StringBuilder socketOutPutBuilder = new StringBuilder();
        while (!Thread.interrupted() && !clientSocketReadWrite.isClosed()) {
            try {
                Map.Entry<String, String> read = this.clientSocketReadWrite.read();;

                if(read != null) {
                    this.inputChannel.write(ByteBuffer.wrap((read.getKey() + SOCKET_KEY_VALUE_DELIMETER + read.getValue()).getBytes("UTF-8")));
                }

                ByteBuffer buffer = ByteBuffer.allocate(2048);
                this.outputChannel.read(buffer);
                buffer.flip();

                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    if(b == '\n') {
                        if(socketOutPutBuilder.length() != 0) {
                            String[] arr = socketOutPutBuilder.toString().split(SOCKET_KEY_VALUE_DELIMETER, 2);
                            clientSocketReadWrite.write(arr[0], arr.length == 2 ? arr[0] : "null");
                        }
                        socketOutPutBuilder = new StringBuilder();
                    } else {
                        socketOutPutBuilder.append((char) b);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handshake() throws Exception {
        this.clientSocketReadWrite.write(CLIENT_ID, clientSocketReadWrite.getId());
        this.clientSocketReadWrite.configureBlocking(true);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean isSucess = new AtomicBoolean(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map.Entry<String, String> info = clientSocketReadWrite.read();
                    if(info != null
                            && HANDSHAKE_STATUS.equals(info.getKey())
                            && SUCCESS.equals(info.getValue())) {
                        isSucess.set(true);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            }
        }).start();

        latch.wait(3000);

        if(latch.getCount() != 0 || !isSucess.get()) {
            throw new Exception("handshake timeout/failure");
        }
    }
}
