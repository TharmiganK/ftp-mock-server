package org.example.ftp.mock.server;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates a Mock FTP Servers
 */
public class MockFtpServer {

    MockFtpServer() {
        // empty constructor
    }

    private static final Logger logger = LoggerFactory.getLogger("MockFtpServer");
    private static FakeFtpServer anonFtpServer;
    private static FakeFtpServer ftpServer;
    private static SshServer sftpServer;
    private static SftpAuthStatusHolder sftpAuthStatusHolder = new SftpAuthStatusHolder();

    public static Object initAnonymousFtpServer() throws Exception {
        int port = 21210;
        anonFtpServer = new FakeFtpServer();
        anonFtpServer.setServerControlPort(port);
        String rootFolder = "/home/in";
        String content = "File content";

        UserAccount anonymousUserAccount = new UserAccount("anonymous", "abc", rootFolder);
        anonymousUserAccount.setPasswordCheckedDuringValidation(false);
        anonFtpServer.addUserAccount(anonymousUserAccount);
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/in/test1.txt", content));
        anonFtpServer.setFileSystem(fileSystem);
        anonFtpServer.start();
        logger.info("Starting Anonymous FTP server...");

        int i = 0;
        while (!anonFtpServer.isStarted() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting anonymous mock FTP server: " + e.getMessage());
            }
        }
        logger.info("Started Anonymous Mock FTP server");
        return null;
    }

    public static Object initFtpServer() throws Exception {
        int port = 21212;
        String username = "wso2";
        String password = "wso2123";

        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(port);
        String rootFolder = "/home/in";
        String content1 = "File content";
        String content2 = "";
        for (int i = 0; i < 1000; i++) {
            content2 += "123456789";
        }

        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/in/test1.txt", content1));
        fileSystem.add(new FileEntry("/home/in/test2.txt", content1));
        fileSystem.add(new FileEntry("/home/in/test3.txt", content1));
        fileSystem.add(new FileEntry("/home/in/test4.txt", content2));
        fileSystem.add(new DirectoryEntry("/home/in/folder1"));
        fileSystem.add(new DirectoryEntry("/home/in/folder1/subfolder1"));
        fileSystem.add(new DirectoryEntry("/home/in/childDirectory"));
        fileSystem.add(new DirectoryEntry("/home/in/complexDirectory/subfolder1/subSubFolder1"));
        fileSystem.add(new DirectoryEntry("/home/in/complexDirectory/subfolder1/subSubFolder2"));
        fileSystem.add(new DirectoryEntry("/home/in/complexDirectory/subfolder2"));
        fileSystem.add(new FileEntry("/home/in/complexDirectory/subfolder1/subSubFolder1/content1.txt"));
        fileSystem.add(new FileEntry("/home/in/complexDirectory/subfolder1/subSubFolder3/content1.txt"));
        fileSystem.add(new FileEntry("/home/in/complexDirectory/subfolder1/subSubFolder3/content2.txt"));
        fileSystem.add(new FileEntry("/home/in/child_directory/content1.txt"));
        fileSystem.add(new FileEntry("/home/in/child_directory/content2.txt"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
        logger.info("Starting FTP server...");

        int i = 0;
        while (!ftpServer.isStarted() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting mock FTP server: " + e.getMessage());
            }
        }
        logger.info("Started Mock FTP server");
        return null;
    }

    public static Object initSftpServer(String resources) throws Exception {
        int port = 21213;
        String username = "wso2";
        String password = "wso2123";

        File homeFolder = new File(resources + "/datafiles");

        sftpServer = SshServer.setUpDefaultServer();
        VirtualFileSystemFactory virtualFileSystemFactory
                = new VirtualFileSystemFactory(homeFolder.getAbsoluteFile().toPath());
        sftpServer.setFileSystemFactory(virtualFileSystemFactory);
        sftpServer.setHost("localhost");
        sftpServer.setPort(port);
        sftpServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sftpServer.setCommandFactory(new ScpCommandFactory());
        try {
            sftpServer.setKeyPairProvider(getKeyPairProvider(resources));
            sftpServer.setPublickeyAuthenticator(new TwoFactorAuthorizedKeysAuthenticator(
                    new File(resources + "/authorized_keys"), sftpAuthStatusHolder));
            String finalUsername = username;
            String finalPassword = password;
            sftpServer.setPasswordAuthenticator(
                    (authUsername, authPassword, session) -> sftpAuthStatusHolder.isPublicKeyAuthenticated()
                            && finalUsername.equals(authUsername) && finalPassword.equals(authPassword));
            sftpServer.setShellFactory(new ProcessShellFactory("/bin/sh", "-i", "-l"));
            sftpServer.start();
        } catch (Exception e) {
            throw new Exception("Error while starting SFTP server: " + e.getMessage());
        }

        int i = 0;
        while (!sftpServer.isOpen() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting mock FTP server: " + e.getMessage());
            }
        }
        logger.info("Started Mock SFTP server");
        return null;
    }

    private static KeyPairProvider getKeyPairProvider(String resources) throws IOException, CertificateException,
                                                                               NoSuchAlgorithmException,
                                                                               KeyStoreException,
                                                                               UnrecoverableKeyException {
        String keystorePath = resources + "/keystore.jks";
        char[] keystorePassword = "changeit".toCharArray();
        char[] keyPassword = "changeit".toCharArray();
        KeyStore ks  = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(keystorePath), keystorePassword);

        List<KeyPair> keyPairs = new ArrayList<>();
        for (Enumeration<String> it = ks.aliases(); it.hasMoreElements(); ) {
            String alias = it.nextElement();
            Key key = ks.getKey(alias, keyPassword);
            if (key instanceof PrivateKey) {
                Certificate cert = ks.getCertificate(alias);
                PublicKey publicKey = cert.getPublicKey();
                keyPairs.add(new KeyPair(publicKey, (PrivateKey) key));
            }
        }

        return new AbstractKeyPairProvider() {
            @Override
            public Iterable<KeyPair> loadKeys() {
                return keyPairs;
            }
        };

    }
}
