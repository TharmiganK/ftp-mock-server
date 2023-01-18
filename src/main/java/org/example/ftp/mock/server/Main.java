package org.example.ftp.mock.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger("Main");

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                throw new Exception("Please specify the resources directory as an argument");
            }
            MockFtpServer.initAnonymousFtpServer();
            MockFtpServer.initFtpServer();
            MockFtpServer.initSftpServer(args[0]);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            System.exit(1);
        }
    }
}
