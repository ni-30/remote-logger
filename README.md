Remote Logger
==============


# Running Client
### Remote-Logger Client Properties
    readBufferCapacity=1024
    serverHost=127.0.0.1
    serverPort=8333
    socketOutputBufferCapacity=4096
    connectionTimeout=10000
    
### Create RemoteLogger.class instance
    String channelName = "dummyChannel";
    RemoteLogger logger = RemoteLogger.getLogger(channelName);

### Log using RemoteLogger created instance
    logger.log("Send this log to the server");


# Running Server
### Remote-Logger Server Properties
    serverHost=127.0.0.1
    serverPort=8333
    socketOutputBufferCapacity=4096
    readBufferCapacity=1024
    clientPoolSize=4

### Using remoteLoggerServer.properties file in jar
    java -jar remote-logger.jar server
    
### Using properties file in working directory
    java -jar remote-logger.jar server <properties file name>
