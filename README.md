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

### Run test-client
    //  using properties file in jar
    java -jar remote-logger.jar test-client <properties file name>
    
    //  using properties file in working directory
    java -jar remote-logger.jar test-client <properties file name>


# Running Server
### Remote-Logger Server Properties
    serverHost=127.0.0.1
    serverPort=8333
    socketOutputBufferCapacity=4096
    readBufferCapacity=1024
    clientPoolSize=4
    groupName=dummy

### Start server using remoteLoggerServer.properties file in jar
    java -jar remote-logger.jar server
    
### Start server using properties file in working directory
    java -jar remote-logger.jar server <properties file name>
