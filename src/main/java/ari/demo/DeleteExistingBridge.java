package ari.demo;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.models.AsteriskInfo;
import ch.loway.oss.ari4java.generated.models.Bridge;
import ch.loway.oss.ari4java.tools.ARIException;
import ch.loway.oss.ari4java.tools.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteExistingBridge {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDump.class);
    public static final String ARI_APP = "app";
    private ARI ari;

    public static void main(String[] args) {
        String url = "http://45.79.191.111:8088";
        String user = "user";
        String pass = "user";
        AriVersion ver = AriVersion.IM_FEELING_LUCKY;
        new DeleteExistingBridge().start(url, user, pass, ver);
    }

    private void start(String url, String user, String pass, AriVersion ver) {
        logger.info("THE START");
        boolean connected = connect(url, user, pass, ver);
        if (connected) {
            try {
                deleteBridges();
            } catch (Exception e) {
                logger.error("Error: {}", e.getMessage(), e);
            } finally {
                logger.info("ARI cleanup");
                ari.cleanup();
            }
        }
        logger.info("THE END");
    }

    private void deleteBridges() throws RestException {
        for (Bridge bridge : ari.bridges().list().execute()) {
            logger.info("The bridge was deleted: " + bridge.getId());
            ari.bridges().destroy(bridge.getId()).execute();
        }
    }

    private boolean connect(String url, String user, String pass, AriVersion ver) {
        try {
            ari = ARI.build(url, ARI_APP, user, pass, ver);
            logger.info("ARI Version: {}", ari.getVersion().version());
            AsteriskInfo info = ari.asterisk().getInfo().execute();
            logger.info("AsteriskInfo up since {}", info.getStatus().getStartup_time());
            return true;
        } catch (ARIException e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
        return false;
    }
}
