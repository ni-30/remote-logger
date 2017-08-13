package io.ni30.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

/**
 * Created by nitish.aryan on 12/08/17.
 */
public class ServerApp {

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "server":
                serverMain(args);
                break;
            case "test-client":
                clientMain(args);
                break;
            default:
                break;
        }
    }

    private static void clientMain(String[] args) throws Exception {
        RemoteLogger remoteLogger;
        if(args.length < 2) {
            remoteLogger = RemoteLogger.getLogger(args[0]);
        } else {
            File file = new File(args[1]);
            InputStream is = new FileInputStream(file);

            Properties properties = new Properties();
            properties.load(is);

            remoteLogger = RemoteLogger.getLogger(properties, "testChannel");
        }

        if(remoteLogger == null) return;

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("[LOG]: ");
            String nextLog = scanner.nextLine();
            remoteLogger.log(nextLog);
        }
    }

    private static void serverMain(String[] args) throws Exception {
        InputStream is;
        if(args.length < 2) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("remoteLoggerServer.properties");
        } else {
            File file = new File(args[1]);
            is = new FileInputStream(file);
        }

        Properties properties = new Properties();
        properties.load(is);

        ServerManager manager = new ServerManager(properties);
        try {
            manager.run();
        } finally {
            manager.shutdown();
        }
    }
}
