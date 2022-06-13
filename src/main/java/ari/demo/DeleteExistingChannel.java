package ari.demo;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.models.AsteriskInfo;
import ch.loway.oss.ari4java.generated.models.Channel;
import ch.loway.oss.ari4java.tools.ARIException;
import ch.loway.oss.ari4java.tools.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DeleteExistingChannel {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDump.class);
    public static final String ARI_APP = "app";
    private ARI ari;

    public static void main(String[] args) {
        String url = "http://45.79.191.111:8088";
        String user = "user";
        String pass = "user";
        AriVersion ver = AriVersion.IM_FEELING_LUCKY;
        new DeleteExistingChannel().start(url, user, pass, ver);
    }

    private void start(String url, String user, String pass, AriVersion ver) {
        logger.info("THE START");
        boolean connected = connect(url, user, pass, ver);
        if (connected) {
            try {
                deleteChannels();
            } catch (Exception e) {
                logger.error("Error: {}", e.getMessage(), e);
            } finally {
                logger.info("ARI cleanup");
                ari.cleanup();
            }
        }
        logger.info("THE END");
    }

    private void deleteChannels() throws RestException {
        List<Channel> list = ari.channels().list().execute();
        for (Channel channel : list) {
            logger.info("The channel was deleted: " + channel.getId());
            ari.channels().hangup(channel.getId()).execute();
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
