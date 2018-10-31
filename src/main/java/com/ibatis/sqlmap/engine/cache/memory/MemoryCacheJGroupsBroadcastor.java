package com.ibatis.sqlmap.engine.cache.memory;

import com.ibatis.common.resources.Resources;
import org.ibatis.jgroups.oscache.NotificationBus;
import com.ibatis.sqlmap.engine.cache.memory.MemoryCache;
import com.ibatis.sqlmap.engine.cache.memory.MulticastorBase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jgroups.Address;
import org.jgroups.JChannel;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * MemoryCacheJGroupsBroadcastor
 * <p>
 * Date: 2016-02-13
 * 
 * @author Song Sun
 * @version 1.0
 */
public class MemoryCacheJGroupsBroadcastor extends MulticastorBase implements NotificationBus.Consumer {
    private final static Log log = LogFactory.getLog(MemoryCacheJGroupsBroadcastor.class);
    private static final String BUS_NAME = "MemoryCacheBus";
    private static final String CHANNEL_PROPERTIES = "memory.mcast.properties";
    private static final String MULTICAST_IP_PROPERTY = "memory.mcast.addr";
    private static final String MULTICAST_PORT_PROPERTY = "memory.mcast.port";

    /**
     * The first half of the default channel properties.
     */
    private static final String DEFAULT_CHANNEL_PROPERTIES_PRE = "UDP(mcast_addr=";

    /**
     * The second half of the default channel properties.
     */
    private static final String DEFAULT_CHANNEL_PROPERTIES_MID = ";mcast_port=";

    /**
     * The third half of the default channel properties.
     */
    private static final String DEFAULT_CHANNEL_PROPERTIES_LAST = ";):PING:MERGE2:FD_SOCK:VERIFY_SUSPECT:pbcast.NAKACK:UNICAST:pbcast.STABLE:FRAG:pbcast.GMS";

    private static final String DEFAULT_MULTICAST_IP = "231.6.7.8";
    private static final String DEFAULT_MULTICAST_PORT = "44566";
    private NotificationBus bus;
    private MemoryCache memoryCache;

    /**
     * Initializes the broadcasting listener by starting up a JGroups notification bus instance to handle incoming and
     * outgoing messages.
     */
    @Override
    public void init(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;

        Properties p = Resources.getIbatisIniProperties();

        String properties = p.getProperty(CHANNEL_PROPERTIES);
        String multicastIP = p.getProperty(MULTICAST_IP_PROPERTY);
        String multicastPort = p.getProperty(MULTICAST_PORT_PROPERTY);

        if ((properties == null) && (multicastIP == null)) {
            multicastIP = DEFAULT_MULTICAST_IP;
        }

        if (properties == null) {
            if (multicastIP == null) {
                multicastIP = DEFAULT_MULTICAST_IP;
            }

            if (multicastPort == null) {
                multicastPort = DEFAULT_MULTICAST_PORT;
            }
            properties = DEFAULT_CHANNEL_PROPERTIES_PRE + multicastIP.trim() + DEFAULT_CHANNEL_PROPERTIES_MID
                + multicastPort.trim() + DEFAULT_CHANNEL_PROPERTIES_LAST;
        } else {
            properties = properties.trim();
        }

        if (log.isInfoEnabled()) {
            log.info("Starting a new JGroups broadcasting listener with properties=" + properties);
        }

        try {
            JChannel ch = new JChannel(properties);
            setDiscardOwnMessages(ch);

            bus = new NotificationBus(ch, BUS_NAME);
            bus.start();
            bus.setConsumer(this);
            log.info("JGroups clustering support started successfully");
        } catch (Exception e) {
            throw new RuntimeException("Initialization failed: " + e);
        }
    }

    void setDiscardOwnMessages(JChannel ch) {
        Method m = null;
        try {
            // ch.setDiscardOwnMessages(true);
            m = ch.getClass().getMethod("setDiscardOwnMessages", boolean.class);
            m.invoke(ch, false);
            return;
        } catch (Exception e) {
        }
        try {
            // ch.setOpt(Channel.LOCAL=3, new Boolean(false));
            m = ch.getClass().getMethod("setOpt", int.class, Object.class);
            m.invoke(ch, 3, Boolean.FALSE);
            return;
        } catch (Exception e) {
        }

        log.warn("JGroups channel setDiscardOwnMessages() failed.");
    }

    /**
     * Shuts down the JGroups being managed by this listener. This occurs once the cache is shut down and this listener
     * is no longer in use.
     */
    public synchronized void finialize() {
        close();
    }

    /**
     * Handles incoming notification messages from JGroups. This method should never be called directly.
     */
    @Override
    public void handleNotification(Serializable info) {
        if (info instanceof String) {
            String cacheId = (String) info;
            onMulticastFlush(cacheId);
            return;
        }

        log.error("An unknown cluster notification message received (class=" + info.getClass().getName()
            + "). Notification ignored.");
    }

    /**
     * We are not using the caching, so we just return something that identifies us. This method should never be called
     * directly.
     */
    @Override
    public Serializable getCache() {
        return getClass().getSimpleName() + ": " + bus.getLocalAddress();
    }

    /**
     * A callback that is fired when a new member joins the cluster. This method should never be called directly.
     *
     * @param address
     *            The address of the member who just joined.
     */
    @Override
    public void memberJoined(Address address) {
        if (log.isDebugEnabled()) {
            log.debug("A new member at address '" + address + "' has joined the cluster");
        }
    }

    /**
     * A callback that is fired when an existing member leaves the cluster. This method should never be called directly.
     *
     * @param address
     *            The address of the member who left.
     */
    @Override
    public void memberLeft(Address address) {
        if (log.isDebugEnabled()) {
            log.debug("Member at address '" + address + "' left the cluster");
        }
    }

    @Override
    public void close() {
        if (log.isInfoEnabled()) {
            log.info("JGroups shutting down...");
        }

        // It's possible that the notification bus is null (CACHE-154)
        if (bus != null) {
            bus.stop();
            bus = null;
        } else {
            log.warn("Notification bus wasn't initialized or finialize was invoked before!");
        }

        if (log.isInfoEnabled()) {
            log.info("JGroups shutdown complete.");
        }
    }

    @Override
    public void multicastFlush(String cacheId) {
        if (bus != null) {
            bus.sendNotification(cacheId);
        }
    }

    @Override
    public void onMulticastFlush(String cacheId) {
        this.memoryCache.cache(cacheId).clear();
        if (MemoryCache.log.isDebugEnabled()) {
            MemoryCache.log.debug(cacheId + " cache flushed.");
        }
    }
}
