package org.example.ftp.mock.server;

public class SftpAuthStatusHolder {

    private boolean publicKeyAuthenticated = false;

    public boolean isPublicKeyAuthenticated() {
        return publicKeyAuthenticated;
    }

    public void setPublicKeyAuthenticated(boolean publicKeyAuthenticated) {
        this.publicKeyAuthenticated = publicKeyAuthenticated;
    }
}
