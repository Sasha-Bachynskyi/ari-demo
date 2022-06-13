package ari.demo.external_media;

import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.tools.ARIException;

public class StartExternalMediaApp {
    private static final int PORT = 5555;
    private static final String IP_ADDRESS = "http://45.79.191.111:8088";
    private static final String USER = "user";
    private static final String PASSWORD = "user";
    private static final String VERSION = "";

    public static void main(String[] args) throws ARIException {
        AriVersion ariVersion;
        if (VERSION.isEmpty()) {
            ariVersion = AriVersion.IM_FEELING_LUCKY;
        } else {
            ariVersion = AriVersion.fromVersionString(VERSION);
        }
        Asterisk asterisk = new Asterisk(IP_ADDRESS, USER, PASSWORD, ariVersion);
        WebServer webServer = new WebServer(PORT, asterisk);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            asterisk.stop();
            webServer.stop();
        }));
        if (!asterisk.start()) {
            System.exit(1);
        }
        webServer.start();
    }
}
