package io.ni30.logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.ni30.logger.Constants.*;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class ClientSocketHandler {

    private final ExecutorService executorService;
    private final AtomicInteger totalOpenClientSocket;
    private final Set<ClientSocketReadWrite> clientSocketReadWrites = Collections.newSetFromMap(new ConcurrentHashMap<ClientSocketReadWrite, Boolean>());
    private final String dir;

    public ClientSocketHandler(int clientPoolSize, AtomicInteger totalOpenClientSocket, String dir) {
        this.dir = dir;
        this.executorService = Executors.newFixedThreadPool(clientPoolSize * 2);
        this.totalOpenClientSocket = totalOpenClientSocket;
    }

    public void shutdown() {
        if(!this.executorService.isShutdown()) {
            this.executorService.shutdown();
        }
    }

    public void handle(ClientSocketReadWrite clientSocketReadWrite) throws Exception {
        try {
            this.handshake(clientSocketReadWrite);
        } catch(Exception e) {
            if(!clientSocketReadWrite.isClosed()) clientSocketReadWrite.close();
            throw e;
        }

        clientSocketReadWrite.configureBlocking(true);

        final ClientFileManager clientFileManager = new ClientFileManager(clientSocketReadWrite.getId().split("_")[0], dir);

        final AtomicBoolean isWriterRunning = new AtomicBoolean(true);
        final AtomicBoolean isReaderRunning = new AtomicBoolean(true);

        this.executorService.submit(()-> runWriter(clientSocketReadWrite, clientFileManager, isWriterRunning, isReaderRunning));
        this.executorService.submit(()-> runReader(clientSocketReadWrite, clientFileManager, isWriterRunning, isReaderRunning));

        System.out.println("[RemoteLoggerServer] connected new client - " + clientSocketReadWrite.getId());
    }

    private void runWriter(ClientSocketReadWrite clientSocketReadWrite, ClientFileManager clientFileManager, AtomicBoolean isWriterRunning, AtomicBoolean isReaderRunning) {
        RandomAccessFile clientFile = null;
        FileChannel clientChannel = null;

        try {
            this.clientSocketReadWrites.add(clientSocketReadWrite);
            this.totalOpenClientSocket.set(this.clientSocketReadWrites.size());

            try {
                clientFile = clientFileManager.getReadableServerLogFile(clientSocketReadWrite.getId());
                clientChannel = clientFile.getChannel();

                StringBuilder sb = new StringBuilder();
                while (!Thread.interrupted() && !clientSocketReadWrite.isClosed() && isReaderRunning.get()) {
                    ByteBuffer buffer = ByteBuffer.allocate(512);
                    clientChannel.read(buffer);
                    buffer.flip();

                    while (buffer.hasRemaining()) {
                        char c = (char) buffer.get();
                        if (c == '\n') {
                            if (sb.length() != 0) {
                                String[] arr = sb.toString().split(String.valueOf(SOCKET_KEY_VALUE_DELIMETER), 2);
                                clientSocketReadWrite.write(arr[0], arr.length == 2 ? arr[1] : "null");
                            }
                            sb = new StringBuilder();
                        } else {
                            sb.append(c);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (!clientSocketReadWrite.isClosed()) {
                    try {
                        clientSocketReadWrite.close();
                    } catch (Exception ignore) {
                    }
                }

                if (clientFile != null) {
                    try {
                        clientFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (clientChannel != null) {
                    try {
                        clientChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                this.clientSocketReadWrites.remove(clientSocketReadWrite);
                this.totalOpenClientSocket.set(this.clientSocketReadWrites.size());
            }
        } finally {
            isWriterRunning.compareAndSet(true, false);
        }
    }

    private void runReader(ClientSocketReadWrite clientSocketReadWrite, ClientFileManager clientFileManager, AtomicBoolean isWriterRunning, AtomicBoolean isReaderRunning) {
        RandomAccessFile clientFile = null;
        FileChannel clientChannel = null;

        try {
            this.clientSocketReadWrites.add(clientSocketReadWrite);
            this.totalOpenClientSocket.set(this.clientSocketReadWrites.size());

            try {
                clientFile = clientFileManager.getWritableClientLogFile(clientSocketReadWrite.getId());
                clientChannel = clientFile.getChannel();

                while (!Thread.interrupted() && !clientSocketReadWrite.isClosed() && isWriterRunning.get()) {
                    Map.Entry<String, String> out = clientSocketReadWrite.read();

                    if (out == null) {
                        continue;
                    }

                    StringBuilder sb = new StringBuilder()
                            .append(out.getKey())
                            .append(SOCKET_KEY_VALUE_DELIMETER)
                            .append(out.getValue())
                            .append("\n");

                    ByteBuffer buffer = ByteBuffer.wrap(sb.toString().getBytes("UTF-8"));
                    clientChannel.write(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (clientFile != null) {
                    try {
                        clientFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (clientChannel != null) {
                            try {
                                clientChannel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if (!clientSocketReadWrite.isClosed() && !clientSocketReadWrite.isClosed()) {
                    try {
                        clientSocketReadWrite.close();
                    } catch (Exception ignore) {
                    }
                }

                this.clientSocketReadWrites.remove(clientSocketReadWrite);
                this.totalOpenClientSocket.set(this.clientSocketReadWrites.size());
            }
        } finally {
            isReaderRunning.compareAndSet(true, false);
        }
    }

    private void handshake(ClientSocketReadWrite clientSocketReadWrite) throws Exception {
        clientSocketReadWrite.configureBlocking(false);

        final long startedAt = System.currentTimeMillis();
        Map.Entry<String, String> info = clientSocketReadWrite.read();
        while (info == null && System.currentTimeMillis() - startedAt < 3000) {
            info = clientSocketReadWrite.read();
        }

        if(info == null) {
            throw new Exception("handshake timeout");
        }

        if(CLIENT_ID.equals(info.getKey())
                && info.getValue() != null
                && info.getValue().split("_", 2).length == 2) {
            clientSocketReadWrite.setId(info.getValue());
            clientSocketReadWrite.write(HANDSHAKE_STATUS, SUCCESS);
        } else {
            clientSocketReadWrite.write(HANDSHAKE_STATUS, FAILED);
        }
    }
}
