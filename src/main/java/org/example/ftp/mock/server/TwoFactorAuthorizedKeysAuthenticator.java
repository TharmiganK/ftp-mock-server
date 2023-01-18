package org.example.ftp.mock.server;

import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.io.File;
import java.security.PublicKey;

public class TwoFactorAuthorizedKeysAuthenticator extends AuthorizedKeysAuthenticator {

    private static SftpAuthStatusHolder sftpAuthStatusHolder;

    public TwoFactorAuthorizedKeysAuthenticator(File file, SftpAuthStatusHolder authStatusHolder) {
        super(file);
        sftpAuthStatusHolder = authStatusHolder;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        boolean authSuccess = super.authenticate(username, key, session);
        sftpAuthStatusHolder.setPublicKeyAuthenticated(authSuccess);
        return false;
    }
}
