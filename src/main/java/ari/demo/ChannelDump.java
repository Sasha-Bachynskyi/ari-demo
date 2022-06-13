package ari.demo;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.AriWSHelper;
import ch.loway.oss.ari4java.generated.models.*;
import ch.loway.oss.ari4java.tools.ARIException;
import ch.loway.oss.ari4java.tools.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChannelDump {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDump.class);
    public static final String ARI_APP = "app";
    private ARI ari;

    public static void main(String[] args) {
        String url = "http://45.79.191.111:8088";
        String user = "user";
        String pass = "user";
        AriVersion ver = AriVersion.IM_FEELING_LUCKY;
        new ChannelDump().start(url, user, pass, ver);
    }

    private void start(String url, String user, String pass, AriVersion ver) {
        logger.info("THE START");
        boolean connected = connect(url, user, pass, ver);
        if (connected) {
            try {
//                checkChannels();
                channelDump();
            } catch (Exception e) {
                logger.error("Error: {}", e.getMessage(), e);
            } finally {
                logger.info("ARI cleanup");
                ari.cleanup();
            }
        }
        logger.info("THE END");
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

    private void channelDump() throws ARIException, InterruptedException {
        final ExecutorService threadPool = Executors.newFixedThreadPool(10);
        ari.eventsCallback(new AriWSHelper() {
            @Override
            public void onSuccess(Message message) {
                threadPool.execute(() -> super.onSuccess(message));
            }

            @Override
            public void onFailure(RestException e) {
                logger.error("Error: {}", e.getMessage(), e);
                threadPool.shutdown();
            }

            @Override
            protected void onStasisStart(StasisStart message) {
                handleStart(message);
            }

            @Override
            protected void onPlaybackFinished(PlaybackFinished message) {
                handlePlaybackFinished(message);
            }
        });
        threadPool.awaitTermination(5, TimeUnit.MINUTES);
        ari.cleanup();
        logger.info("THE END");
        System.exit(0);
    }

    private void handleStart(StasisStart start) {
        logger.info("Stasis Start Channel: {}", start.getChannel().getName());
        ARI.sleep(300);
        try {
            ari.channels().play(start.getChannel().getId(), "sound:weasels-eaten-phonesys").execute();
//            logger.info("Thread was here: " + Thread.currentThread().getName());
//            Bridge bridge = ari.bridges().create().execute();
//            ari.bridges().addChannel(bridge.getId(), start.getChannel().getId());
//            Channel externalMediaChannel = ari.channels().externalMedia("app", "45.79.191.111:4444", "ulaw").execute();
//            ari.bridges().addChannel(bridge.getId(), externalMediaChannel.getId()).execute();
        } catch (RestException e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
    }

    private void handlePlaybackFinished(PlaybackFinished playback) {
        logger.info("PlaybackFinished: {}", playback.getPlayback().getTarget_uri());
        if (playback.getPlayback().getTarget_uri().indexOf("channel:") == 0) {
            try {
                String chanId = playback.getPlayback().getTarget_uri().split(":")[1];
                logger.info("Hangup Channel: {}", chanId);
                ARI.sleep(300);
                ari.channels().hangup(chanId).execute();
            } catch (RestException e) {
                logger.error("Error: {}", e.getMessage(), e);
            }
        } else {
            logger.error("Can't handle URI - {}", playback.getPlayback().getTarget_uri());
        }
    }

    private void checkChannels() throws RestException {
        List<Channel> channels = ari.channels().list().execute();
        if (channels.isEmpty()) {
            logger.info("No channels currently");
        } else {
            logger.info("Current channels: ");
            for (Channel channel : channels) {
                logger.info(channel.getName());
            }
        }
    }
}
