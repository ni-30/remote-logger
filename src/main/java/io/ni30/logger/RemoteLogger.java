package io.ni30.logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static io.ni30.logger.Constants.*;

/**
 * Created by nitish.aryan on 12/08/17.
 */

public class RemoteLogger implements Closeable {
    private static Map<String, RemoteLogger> loggers = new ConcurrentHashMap<>();

    public static RemoteLogger getLogger(Properties properties, String channelName) throws IOException {
        final String loggerName = properties.getProperty(GROUP_NAME, ANONYMOUS);

        final RemoteLogger prevLogger = loggers.get(loggerName);
        if (prevLogger != null && prevLogger.isRunning()) {
            return prevLogger;
        }

        String clientId = properties.getProperty(GROUP_NAME, ANONYMOUS) + "_" + UUID.randomUUID().toString();
        properties.setProperty(CLIENT_ID, clientId);

        ClientManager clientManager = ClientManager.getOrCreate(properties);
        final RemoteLogger newLogger = new RemoteLogger(channelName, clientManager);
        newLogger.run();

        RemoteLogger currLogger = loggers.compute(loggerName, (k,v) -> (v == null || v == prevLogger) ? newLogger : v);

        if(currLogger != newLogger) {
            newLogger.close();
        }

        return currLogger;
    }

    public static RemoteLogger getLogger(String channelName) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("remoteLoggerClient.properties");

        Properties properties = new Properties();
        properties.load(is);

        return getLogger(properties, channelName);
    }



    private final String channelName;
    private final ClientManager clientManager;
    private RandomAccessFile inputFile;
    private FileChannel inputChannel;
    private RandomAccessFile outputFile;
    private FileChannel outputChannel;
    private boolean isClosed = false;

    private RemoteLogger(String channelName, ClientManager clientManager) throws IOException {
        this.channelName = channelName;
        this.clientManager = clientManager;

        try {
            inputFile = this.clientManager.getClientFileManager().getReadableServerLogFile(this.clientManager.getClientId());
            outputFile = this.clientManager.getClientFileManager().getWritableClientLogFile(this.clientManager.getClientId());
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

    private boolean isRunning() {
        return this.clientManager.isShutdown();
    }

    private void run() {
        try {
            this.clientManager.run();
        } catch (Exception e) {
            this.close();
            e.printStackTrace();
        }
    }

    public void log(String data) {
        String newLine = this.channelName + SOCKET_KEY_VALUE_DELIMETER + data + '\n';
        try {
            this.outputChannel.write(ByteBuffer.wrap(newLine.getBytes("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        if(isClosed) return;

        isClosed = true;

        try {
            this.clientManager.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
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
