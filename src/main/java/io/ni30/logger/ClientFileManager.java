package io.ni30.logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nitish.aryan on 13/08/17.
 */

public class ClientFileManager {
    private static final String LOGS_DIR_NAME = "logs";
    private static Date startTime;
    private static String dateDirectoryName;

    static {
        startTime = new Date();
        dateDirectoryName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private String clientLogDirectoryPath = null;
    private String serverLogDirectoryPath = null;

    public ClientFileManager(String groupName) {
        synchronized (ClientFileManager.class) {
            if(clientLogDirectoryPath == null) {
                this.clientLogDirectoryPath = LOGS_DIR_NAME + "/" + groupName + "/client/" + dateDirectoryName;
            }

            if(serverLogDirectoryPath == null) {
                this.serverLogDirectoryPath = LOGS_DIR_NAME + "/" + groupName + "/server/" + dateDirectoryName;
            }

            File file1 = new File(clientLogDirectoryPath);
            if(!file1.exists() || file1.isFile()) {
                file1.mkdirs();
            }

            File file2 = new File(serverLogDirectoryPath);
            if(!file2.exists() || file2.isFile()) {
                file2.mkdirs();
            }
        }
    }

    public static Date getStartTime() {
        return startTime;
    }

    public RandomAccessFile getWritableClientLogFile(String clientId) throws IOException {
        String filePath = clientLogDirectoryPath + "/" + clientId;
        File file = new File(filePath);
        if(!file.exists() || file.isDirectory()) {
            file.createNewFile();
        }

        return new RandomAccessFile(file, "rwd");
    }

    public RandomAccessFile getReadableClientLogFile(String clientId) throws IOException {
        String filePath = clientLogDirectoryPath + "/" + clientId;
        File file = new File(filePath);
        if(!file.exists() || file.isDirectory()) {
            file.createNewFile();
        }

        return new RandomAccessFile(file, "r");
    }

    public RandomAccessFile getWritableServerLogFile(String clientId) throws IOException {
        String filePath = serverLogDirectoryPath + "/" + clientId;
        File file = new File(filePath);
        if(!file.exists() || file.isDirectory()) {
            file.createNewFile();
        }

        return new RandomAccessFile(file, "rwd");
    }

    public RandomAccessFile getReadableServerLogFile(String clientId) throws IOException {
        String filePath = serverLogDirectoryPath + "/" + clientId;
        File file = new File(filePath);
        if(!file.exists() || file.isDirectory()) {
            file.createNewFile();
        }

        return new RandomAccessFile(file, "r");
    }
}
