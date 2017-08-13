package io.ni30.logger;

import java.io.File;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class Constants {
    public static final String
            READ_BUFFER_CAPACITY = "readBufferCapacity",
            SERVER_HOST = "serverHost",
            SERVER_PORT = "serverPort",
            SOCKET_OUTPUT_BUFFER_CAPACITY = "socketOutputBufferCapacity",
            CONNECTION_TIMEOUT = "connectionTimeout",
            CLIENT_POOL_SIZE = "clientPoolSize",
            GROUP_NAME = "groupName",
            CLIENT_ID = "clientId",
            ANONYMOUS = "Anonymous",
            HANDSHAKE_STATUS = "handshakeStatus",
            SUCCESS = "success",
            FAILED = "failed";

    public static final String SOCKET_KEY_VALUE_DELIMETER = "\\t";





    public static void main(String[] args) {
        File file = new File("hello/gamer/hj");
        if(!file.exists() || file.isFile()) {
            file.mkdirs();
        }
    }
}
