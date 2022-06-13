package ari.demo.external_media;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.AriWSHelper;
import ch.loway.oss.ari4java.generated.models.AsteriskInfo;
import ch.loway.oss.ari4java.generated.models.Message;
import ch.loway.oss.ari4java.generated.models.StasisEnd;
import ch.loway.oss.ari4java.generated.models.StasisStart;
import ch.loway.oss.ari4java.tools.ARIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Asterisk {
    private static final String ARI_APP_NAME = "app";
    private static final Logger logger = LoggerFactory.getLogger(Asterisk.class);
    private static final Map<String, String> lookups = new HashMap<>();
    private static final Map<String, State> states = new HashMap<>();
    private final String address;
    private final String user;
    private final String pass;
    private final AriVersion ariVersion;
    private ARI ari;
    private ExecutorService threadPool;


    public Asterisk(String ipAddress, String user, String password, AriVersion ariVersion) {
        this.address = ipAddress;
        this.user = user;
        this.pass = password;
        this.ariVersion = ariVersion;
    }

    public boolean start() {
        logger.info("Starting ARI...");
        try {
            ari = ARI.build(address, ARI_APP_NAME, user, pass, ariVersion);
            AsteriskInfo info = ari.asterisk().getInfo().execute();
            threadPool = Executors.newFixedThreadPool(5);
            ari.events().eventWebsocket(ARI_APP_NAME).execute(new Handler());
            logger.info("Connected to Asterisk {}", info.getSystem().getVersion());
            return true;
        } catch (ARIException e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
        return false;
    }

    public void stop() {
        if (ari != null) ari.cleanup();
        if (threadPool != null) threadPool.shutdown();
    }

    private void clear(String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        String stateId = null;
        synchronized (lookups) {
            if (lookups.containsKey(id)) {
                stateId = lookups.remove(id);
            }
        }
        if (stateId != null) {
            synchronized (states) {
                State state = states.get(stateId);
                if (state != null) {
                    if (id.equals(state.extChannel)) {
                        state.extChannel = null;
                    } else if (id.equals(state.mediaChannel)) {
                        state.mediaChannel = null;
                    } else if (id.equals(state.bridge)) {
                        state.bridge = null;
                    }
                    if (state.extChannel == null && state.mediaChannel == null && state.bridge == null) {
                        states.remove(stateId);
                    }
                }
            }
        }
    }

    private static class State {
        private final String id = UUID.randomUUID().toString();
//        private String from;
//        private String to;
        private String extChannel;
        private String mediaChannel;
        private String bridge;
    }

    private class Handler extends AriWSHelper {
        @Override
        public void onSuccess(Message message) {
            threadPool.execute(() -> super.onSuccess(message));
        }

        @Override
        protected void onStasisStart(StasisStart message) {
            logger.debug("onStasisStart, chan id: {}, name: {}", message.getChannel().getId(), message.getChannel().getName());
        }

        @Override
        protected void onStasisEnd(StasisEnd message) {
            logger.debug("onStasisEnd {}", message.getChannel().getId());
            clear(message.getChannel().getId());
        }
    }
}
