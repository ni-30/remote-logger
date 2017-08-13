Remote Logger
==============

## Remote-Logger Client Properties
  readBufferCapacity=1024
  serverHost=127.0.0.1
  serverPort=8333
  socketOutputBufferCapacity=4096
  connectionTimeout=10000
  
  
## Remote-Logger Server Properties
  serverHost=127.0.0.1
  serverPort=8333
  socketOutputBufferCapacity=4096
  readBufferCapacity=1024
  clientPoolSize=4
  
 
## Create RemoteLogger.class instance
  String channelName = "dummyChannel";
  RemoteLogger logger = RemoteLogger.getLogger(channelName);

## Log using RemoteLogger created instance
  logger.log("Send this log to the server");
