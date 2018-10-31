package org.ibatis.jgroups.oscache;

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.Config;
import com.opensymphony.oscache.base.FinalizationException;
import com.opensymphony.oscache.base.InitializationException;
import com.opensymphony.oscache.plugins.clustersupport.AbstractBroadcastingListener;
import com.opensymphony.oscache.plugins.clustersupport.ClusterNotification;

import org.jgroups.Address;
import org.jgroups.JChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * <p>
 * A concrete implementation of the {@link AbstractBroadcastingListener} based on the JavaGroups library. This Class
 * uses JavaGroups to broadcast cache flush messages across a cluster.
 * </p>
 *
 * <p>
 * One of the following properties should be configured in <code>oscache.properties</code> for this listener:
 * <ul>
 * <li><b>cache.cluster.multicast.ip</b> - The multicast IP that JavaGroups should use for broadcasting</li>
 * <li><b>cache.cluster.properties</b> - The JavaGroups channel properties to use. Allows for precise control over the
 * behaviour of JavaGroups</li>
 * </ul>
 * Please refer to the clustering documentation for further details on the configuration of this listener.
 * </p>
 */
public class JavaGroupsBroadcastingListener extends AbstractBroadcastingListener implements NotificationBus.Consumer {
    private final static Logger log = LoggerFactory.getLogger(JavaGroupsBroadcastingListener.class);
    private static final String BUS_NAME = "OSCacheBus";
    private static final String CHANNEL_PROPERTIES = "cache.cluster.properties";
    private static final String MULTICAST_IP_PROPERTY = "cache.cluster.multicast.ip";

    /**
     * The first half of the default channel properties. They default channel properties are:
     * 
     * <pre>
     * UDP(mcast_addr=*.*.*.*;mcast_port=45566;ip_ttl=32;\
     * mcast_send_buf_size=150000;mcast_recv_buf_size=80000):\
     * PING(timeout=2000;num_initial_members=3):\
     * MERGE2(min_interval=5000;max_interval=10000):\
     * FD_SOCK:VERIFY_SUSPECT(timeout=1500):\
     * pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):\
     * UNICAST(timeout=300,600,1200,2400):\
     * pbcast.STABLE(desired_avg_gossip=20000):\
     * FRAG(frag_size=8096;down_thread=false;up_thread=false):\
     * pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)
     * </pre>
     *
     * Where <code>*.*.*.*</code> is the specified multicast IP, which defaults to <code>231.12.21.132</code>.
     */
    private static final String DEFAULT_CHANNEL_PROPERTIES_PRE = "UDP(mcast_addr=";

    /**
     * The second half of the default channel properties. They default channel properties are:
     * 
     * <pre>
     * UDP(mcast_addr=*.*.*.*;mcast_port=45566;ip_ttl=32;\
     * mcast_send_buf_size=150000;mcast_recv_buf_size=80000):\
     * PING(timeout=2000;num_initial_members=3):\
     * MERGE2(min_interval=5000;max_interval=10000):\
     * FD_SOCK:VERIFY_SUSPECT(timeout=1500):\
     * pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):\
     * UNICAST(timeout=300,600,1200,2400):\
     * pbcast.STABLE(desired_avg_gossip=20000):\
     * FRAG(frag_size=8096;down_thread=false;up_thread=false):\
     * pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)
     * </pre>
     *
     * Where <code>*.*.*.*</code> is the specified multicast IP, which defaults to <code>231.12.21.132</code>.
     */
    private static final String DEFAULT_CHANNEL_PROPERTIES_POST = ";mcast_port=45566;ip_ttl=32;mcast_send_buf_size=150000;mcast_recv_buf_size=80000):"
        + "PING(timeout=2000;num_initial_members=3):MERGE2(min_interval=5000;max_interval=10000):FD_SOCK:VERIFY_SUSPECT(timeout=1500):"
        + "pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):UNICAST(timeout=300,600,1200,2400):pbcast.STABLE(desired_avg_gossip=20000):"
        + "FRAG(frag_size=8096;down_thread=false;up_thread=false):pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)";
    private static final String DEFAULT_MULTICAST_IP = "231.12.21.132";
    private NotificationBus bus;

    /**
     * Initializes the broadcasting listener by starting up a JavaGroups notification bus instance to handle incoming
     * and outgoing messages.
     *
     * @param config
     *            An OSCache configuration object.
     * @throws com.opensymphony.oscache.base.InitializationException
     *             If this listener has already been initialized.
     */
    public synchronized void initialize(Cache cache, Config config) throws InitializationException {
        super.initialize(cache, config);

        String properties = config.getProperty(CHANNEL_PROPERTIES);
        String multicastIP = config.getProperty(MULTICAST_IP_PROPERTY);

        if ((properties == null) && (multicastIP == null)) {
            multicastIP = DEFAULT_MULTICAST_IP;
        }

        if (properties == null) {
            properties = DEFAULT_CHANNEL_PROPERTIES_PRE + multicastIP.trim() + DEFAULT_CHANNEL_PROPERTIES_POST;
        } else {
            properties = properties.trim();
        }

        if (log.isInfoEnabled()) {
            log.info("Starting a new JavaGroups broadcasting listener with properties=" + properties);
        }

        try {
            JChannel ch = new JChannel(properties);
            setDiscardOwnMessages(ch);

            bus = new NotificationBus(ch, BUS_NAME);
            bus.start();
            bus.setConsumer(this);
            log.info("JavaGroups clustering support started successfully");
        } catch (Exception e) {
            throw new InitializationException("Initialization failed: " + e);
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

        log.warn("JavaGroups channel setDiscardOwnMessages() failed.");
    }

    /**
     * Shuts down the JavaGroups being managed by this listener. This occurs once the cache is shut down and this
     * listener is no longer in use.
     *
     * @throws com.opensymphony.oscache.base.FinalizationException
     */
    public synchronized void finialize() throws FinalizationException {
        if (log.isInfoEnabled()) {
            log.info("JavaGroups shutting down...");
        }

        // It's possible that the notification bus is null (CACHE-154)
        if (bus != null) {
            bus.stop();
            bus = null;
        } else {
            log.warn("Notification bus wasn't initialized or finialize was invoked before!");
        }

        if (log.isInfoEnabled()) {
            log.info("JavaGroups shutdown complete.");
        }
    }

    /**
     * Uses JavaGroups to broadcast the supplied notification message across the cluster.
     *
     * @param message
     *            The cluster nofication message to broadcast.
     */
    protected void sendNotification(ClusterNotification message) {
        bus.sendNotification(message);
    }

    /**
     * Handles incoming notification messages from JavaGroups. This method should never be called directly.
     *
     * @param serializable
     *            The incoming message object. This must be a {@link ClusterNotification}.
     */
    public void handleNotification(Serializable serializable) {
        if (!(serializable instanceof ClusterNotification)) {
            log.error("An unknown cluster notification message received (class=" + serializable.getClass().getName()
                + "). Notification ignored.");

            return;
        }

        handleClusterNotification((ClusterNotification) serializable);
    }

    /**
     * We are not using the caching, so we just return something that identifies us. This method should never be called
     * directly.
     */
    public Serializable getCache() {
        return getClass().getSimpleName() + ": " + bus.getLocalAddress();
    }

    /**
     * A callback that is fired when a new member joins the cluster. This method should never be called directly.
     *
     * @param address
     *            The address of the member who just joined.
     */
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
    public void memberLeft(Address address) {
        if (log.isDebugEnabled()) {
            log.debug("Member at address '" + address + "' left the cluster");
        }
    }
}
