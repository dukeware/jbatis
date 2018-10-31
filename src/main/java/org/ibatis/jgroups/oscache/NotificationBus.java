package org.ibatis.jgroups.oscache;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.ibatis.cglib.FastMethod;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.util.Promise;
import org.jgroups.util.Util;

/**
 * This class provides notification sending and handling capability. Producers can send notifications to all registered
 * consumers. Provides hooks to implement shared group state, which allows an application programmer to maintain a local
 * cache which is replicated by all instances. NotificationBus sits on top of a channel, however it creates its channel
 * itself, so the application programmers do not have to provide their own channel.
 *
 * @author Bela Ban
 */
public class NotificationBus implements Receiver {
    final List<Address> members = new Vector<Address>();
    Channel channel = null;
    Address local_addr = null;
    Consumer consumer = null; // only a single consumer allowed
    String bus_name = "notification_bus";
    int inst = hashCode();
    final Promise<Serializable> get_cache_promise = new Promise<Serializable>();
    final Object cache_mutex = new Object();

    protected final Log log = LogFactory.getLog(getClass());

    String props = null;

    public interface Consumer {
        void handleNotification(Serializable n);

        /** Called on the coordinator to obtains its cache */
        Serializable getCache();

        void memberJoined(Address mbr);

        void memberLeft(Address mbr);
    }

    public NotificationBus(Channel channel, String bus_name) throws Exception {
        if (bus_name != null)
            this.bus_name = bus_name;
        this.inst = System.identityHashCode(channel) + System.identityHashCode(this.bus_name);
        this.channel = channel;
        channel.setReceiver(this);
    }

    public void setConsumer(Consumer c) {
        consumer = c;
    }

    public Address getLocalAddress() {
        if (local_addr != null)
            return local_addr;
        if (channel != null)
            local_addr = channel.getAddress();
        return local_addr;
    }

    /**
     * Returns a reference to the real membership: don't modify. If you need to modify, make a copy first !
     */
    public List<Address> getMembership() {
        return members;
    }

    /**
     * Answers the Channel. Used to operate on the underlying channel directly, e.g. perform operations that are not
     * provided using only NotificationBus. Should be used sparingly.
     * 
     * @return underlying Channel
     */
    public Channel getChannel() {
        return channel;
    }

    public boolean isCoordinator() {
        Address first_mbr = null;

        synchronized (members) {
            first_mbr = !members.isEmpty() ? members.get(0) : null;
            if (first_mbr == null)
                return true;
        }
        return getLocalAddress() != null && getLocalAddress().equals(first_mbr);
    }

    public void start() throws Exception {
        channel.connect(bus_name);
    }

    public void stop() {
        if (channel != null) {
            channel.close(); // disconnects from channel and closes it
            channel = null;
        }
    }

    /** Pack the argument in a Info, serialize that one into the message buffer and send the message */
    public void sendNotification(Serializable n) {
        sendNotification(null, n);
    }

    /** Pack the argument in a Info, serialize that one into the message buffer and send the message */
    public void sendNotification(Address dest, Serializable n) {
        Message msg = null;
        byte[] data = null;
        Info info;

        try {
            if (n == null)
                return;
            info = new Info(Info.NOTIFICATION, n);
            info.inst = inst;
            data = Util.objectToByteBuffer(info);
            msg = new Message(dest, null, data);
            if (channel == null) {
                if (log.isErrorEnabled())
                    log.error("channel is null. Won't send notification");
                return;
            }
            channel.send(msg);
        } catch (Throwable ex) {
            if (log.isErrorEnabled())
                log.error("error sending notification", ex);
        }
    }

    /**
     * Determines the coordinator and asks it for its cache. If there is no coordinator (because we are first member),
     * null will be returned. Used only internally by NotificationBus.
     * 
     * @param timeout
     *            Max number of msecs until the call returns
     * @param max_tries
     *            Max number of attempts to fetch the cache from the coordinator
     */
    public Serializable getCacheFromCoordinator(long timeout, int max_tries) {
        return getCacheFromMember(null, timeout, max_tries);
    }

    /**
     * Determines the coordinator and asks it for its cache. If there is no coordinator (because we are first member),
     * null will be returned. Used only internally by NotificationBus.
     * 
     * @param mbr
     *            The address of the member from which to fetch the state. If null, the current coordinator will be
     *            asked for the state
     * @param timeout
     *            Max number of msecs until the call returns - if timeout elapses null will be returned
     * @param max_tries
     *            Max number of attempts to fetch the cache from the coordinator (will be set to 1 if < 1)
     */
    public Serializable getCacheFromMember(Address mbr, long timeout, int max_tries) {
        Serializable cache = null;
        int num_tries = 0;
        Info info;
        Message msg;
        Address dst = mbr; // member from which to fetch the cache

        long start, stop; // +++ remove

        if (max_tries < 1)
            max_tries = 1;

        get_cache_promise.reset();
        while (num_tries <= max_tries) {
            if (mbr == null) { // mbr == null means get cache from coordinator
                dst = determineCoordinator();
                if (dst == null || dst.equals(getLocalAddress())) { // we are the first member --> empty cache
                    if (log.isInfoEnabled())
                        log.info("[" + getLocalAddress() + "] no coordinator found --> first member (cache is empty)");
                    return null;
                }
            }

            // +++ remove
            if (log.isInfoEnabled())
                log.info("[" + getLocalAddress() + "] dst=" + dst + ", timeout=" + timeout + ", max_tries=" + max_tries
                    + ", num_tries=" + num_tries);

            info = new Info(Info.GET_CACHE_REQ);
            msg = new Message(dst, null, info);
            try {
                channel.send(msg);
            } catch (Exception e) {
                log.error("failed sending message", e);
                return null;
            }

            start = System.currentTimeMillis();
            cache = get_cache_promise.getResult(timeout);
            stop = System.currentTimeMillis();
            if (cache != null) {
                if (log.isInfoEnabled())
                    log.info("got cache from " + dst + ": cache is valid (waited " + (stop - start)
                        + " msecs on get_cache_promise)");
                return cache;
            } else {
                if (log.isErrorEnabled())
                    log.error(
                        "received null cache; retrying (waited " + (stop - start) + " msecs on get_cache_promise)");
            }

            Util.sleep(500);
            ++num_tries;
        }
        if (cache == null)
            if (log.isErrorEnabled())
                log.error("[" + getLocalAddress() + "] cache is null (num_tries=" + num_tries + ')');
        return cache;
    }

    /**
     * Don't multicast this to all members, just apply it to local consumers.
     */
    public void notifyConsumer(Serializable n) {
        if (consumer != null && n != null)
            consumer.handleNotification(n);
    }

    /* -------------------------------- Interface MessageListener -------------------------------- */
    public void receive(Message msg) {
        Info info = null;
        Object obj;

        if (msg == null || msg.getLength() == 0)
            return;
        try {
            obj = msg.getObject();
            if (!(obj instanceof Info)) {
                if (log.isErrorEnabled())
                    log.error("expected an instance of Info (received " + obj.getClass().getName() + ')');
                return;
            }
            info = (Info) obj;
            switch (info.type) {
            case Info.NOTIFICATION:
                if (inst != info.inst)
                    notifyConsumer(info.data);
                break;

            case Info.GET_CACHE_REQ:
                handleCacheRequest(msg.getSrc());
                break;

            case Info.GET_CACHE_RSP:
                // +++ remove
                if (log.isDebugEnabled())
                    log.debug("[GET_CACHE_RSP] cache was received from " + msg.getSrc());
                get_cache_promise.setResult(info.data);
                break;

            default:
                if (log.isErrorEnabled())
                    log.error("type " + info.type + " unknown");
                break;
            }
        } catch (Throwable ex) {
            if (log.isErrorEnabled())
                log.error("exception=" + ex);
        }
    }

    public byte[] getState() {
        return null;
    }

    public void setState(byte[] state) {
    }

    /* ----------------------------- End of Interface MessageListener ---------------------------- */

    /* ------------------------------- Interface MembershipListener ------------------------------ */

    public synchronized void viewAccepted(View new_view) {
        List<Address> joined_mbrs, left_mbrs, tmp;
        Address tmp_mbr;

        if (new_view == null)
            return;
        tmp = getMembers(new_view);

        synchronized (members) {
            // get new members
            joined_mbrs = new Vector<Address>();
            for (int i = 0; i < tmp.size(); i++) {
                tmp_mbr = tmp.get(i);
                if (!members.contains(tmp_mbr))
                    joined_mbrs.add(tmp_mbr);
            }

            // get members that left
            left_mbrs = new Vector<Address>();
            for (int i = 0; i < members.size(); i++) {
                tmp_mbr = members.get(i);
                if (!tmp.contains(tmp_mbr))
                    left_mbrs.add(tmp_mbr);
            }

            // adjust our own membership
            members.clear();
            members.addAll(tmp);
        }

        if (consumer != null) {
            if (!joined_mbrs.isEmpty())
                for (int i = 0; i < joined_mbrs.size(); i++)
                    consumer.memberJoined(joined_mbrs.get(i));
            if (!left_mbrs.isEmpty())
                for (int i = 0; i < left_mbrs.size(); i++)
                    consumer.memberLeft(left_mbrs.get(i));
        }
    }

    public void suspect(Address suspected_mbr) {
    }

    public void block() {
    }

    /* ----------------------------- End of Interface MembershipListener ------------------------- */

    /* ------------------------------------- Private Methods ------------------------------------- */

    Address determineCoordinator() {
        List<Address> v = channel != null ? getMembers(channel.getView()) : null;
        return v != null ? (Address) v.get(0) : null;
    }

    FastMethod getMembers = null;
    
    @SuppressWarnings("unchecked")
    List<Address> getMembers(View view) {
        try {
            // view.getMembers();
            if (getMembers == null) {
                Method m = view.getClass().getMethod("getMembers");
                getMembers = FastMethod.create(m);
            }
            return (List<Address>) getMembers.invoke(view);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    void handleCacheRequest(Address sender) throws Exception {
        Serializable cache = null;
        Message msg;
        Info info;

        if (sender == null) {
            // +++ remove
            //
            if (log.isErrorEnabled())
                log.error("sender is null");
            return;
        }

        synchronized (cache_mutex) {
            cache = getCache(); // get the cache from the consumer
            info = new Info(Info.GET_CACHE_RSP, cache);
            msg = new Message(sender, null, info);
            if (log.isInfoEnabled())
                log.info("[" + getLocalAddress() + "] returning cache to " + sender);
            channel.send(msg);
        }
    }

    public Serializable getCache() {
        return consumer != null ? consumer.getCache() : null;
    }

    /* --------------------------------- End of Private Methods ---------------------------------- */

    private static class Info implements Serializable {
        public final static int NOTIFICATION = 1;
        public final static int GET_CACHE_REQ = 2;
        public final static int GET_CACHE_RSP = 3;

        int type = 0;
        int inst;
        Serializable data = null; 
        // if type == NOTIFICATION data is notification, if type == GET_CACHE_RSP, data is cache
        private static final long serialVersionUID = -1;

        public Info(int type) {
            this.type = type;
        }

        public Info(int type, Serializable data) {
            this.type = type;
            this.data = data;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("type= ");
            if (type == NOTIFICATION)
                sb.append("NOTIFICATION");
            else if (type == GET_CACHE_REQ)
                sb.append("GET_CACHE_REQ");
            else if (type == GET_CACHE_RSP)
                sb.append("GET_CACHE_RSP");
            else
                sb.append("<unknown>");
            if (data != null) {
                if (type == NOTIFICATION)
                    sb.append(", notification=" + data);
                else if (type == GET_CACHE_RSP)
                    sb.append(", cache=" + data);
            }
            return sb.toString();
        }
    }

    public void getState(OutputStream arg0) throws Exception {
    }

    public void setState(InputStream arg0) throws Exception {
    }

    public void unblock() {
    }

}
