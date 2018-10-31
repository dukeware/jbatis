/*-
 * Copyright 2015 Owl Group
 * All rights reserved.
 */
package com.ibatis.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.ibatis.cglib.ReflectUtil;

import com.ibatis.common.jdbc.SimpleDataSource;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.common.resources.Resources;

/**
 * StatsBase
 * <p>
 * Date: 2016-01-19
 * 
 * @author Song Sun
 * @version 1.0
 */
public final class RunStats implements Runnable {

    private static final ILog log = ILogFactory.getLog(RunStats.class);

    static RunStats INSTANCE;

    public static synchronized RunStats getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RunStats();
            Thread t = new Thread(INSTANCE, "iBATIS RunStats");
            t.setDaemon(true);
            t.start();
        }
        return INSTANCE;
    }

    private final List<Statsable> stats = new ArrayList<Statsable>();
    private final List<Touchable> touchs = new ArrayList<Touchable>();

    private File statusFile;
    private final long initialTime;
    volatile long lastFileTime;

    private RunStats() {
        initialTime = System.currentTimeMillis();
        Properties p = Resources.getIbatisIniProperties();
        log.info("iBATIS ini -> " + p);
        String rcs = p.getProperty("cglib_cache_size");
        if (rcs != null && !rcs.trim().isEmpty()) {
            ReflectUtil.setCatchSize(rcs);
        }
        String rsf = p.getProperty("run_stats_file");
        if (rsf != null && rsf.length() > 0) {
            this.statusFile = new File(rsf);
        }

        final Thread t = new Thread("iBATIS Cleaner") {
            public void run() {
                statusFile = null;
                String header = getRunStatsHeader();
                try {
                    log.debug(header);
                } catch (Throwable t) {
                }
                for (Statsable stat : stats) {
                    if (stat instanceof SimpleDataSource) {
                        try {
                            ((SimpleDataSource) stat).forceCloseAll();
                        } catch (Throwable t) {
                        }
                    }
                }
                for (Statsable stat : stats) {
                    if (!(stat instanceof SimpleDataSource)) {
                        try {
                            log.info(stat.getStatus("iBATIS ! "));
                        } catch (Throwable t) {
                        }
                    }
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(t);
        this.lastFileTime = fileStamp(statusFile);
    }

    public void addTouchable(Touchable touch) {
        if (!touchs.contains(touch)) {
            touchs.add(touch);
        }
    }

    public void addStat(Statsable stat) {
        if (!stats.contains(stat)) {
            stats.add(stat);
            log.info(stat.getClass().getSimpleName() + " " + stat.hashCode() + " running...");
            log.info(stat.getStatus("iBATIS = "));
        }
    }

    public void run() {
        int freq = 1;
        while (true) {
            try {
                Thread.sleep(30000);
                freq = freq % 600; // 5 hour
                if (checkFileTime()) {
                    for (Touchable t : touchs) {
                        try {
                            t.onTouch(statusFile);
                        } catch (Exception e) {
                        }
                    }

                    String header = getRunStatsHeader();
                    log.info(header);
                    System.out.println(header);
                    for (Statsable stat : stats) {
                        String s = stat.getStatus("iBATIS - ");
                        log.info(s);
                        System.out.println(s);
                    }
                } else if (freq == 0 && log.isDebugEnabled()) {
                    String header = getRunStatsHeader();
                    log.debug(header);
                    for (Statsable stat : stats) {
                        log.debug(stat.getStatus("iBATIS = "));
                    }
                } else if ((freq % 10 == 0) && log.isTraceEnabled()) {
                    for (Statsable stat : stats) {
                        log.trace(stat.getStatus("iBATIS - "));
                    }
                }
                freq++;
            } catch (Throwable t) {
                log.error("iBATIS RunStats error: " + t.getMessage(), t);
            }
        }

    }

    String getRunStatsHeader() {
        StringBuilder buf = new StringBuilder("iBATIS run stats from ");
        Objects.outputDate(buf, new Date(initialTime));
        buf.append(" to ");
        Objects.outputDate(buf, new Date());
        return buf.toString();
    }

    static long fileStamp(File file) {
        if (file != null && file.isFile()) {
            long t = file.lastModified() / 1000L;
            if (file.canExecute()) {
                t = -t;
            }
            return t;
        }
        return 0L;
    }

    boolean checkFileTime() {
        long t = fileStamp(this.statusFile);
        if (lastFileTime != t) {
            lastFileTime = t;
            return statusFile != null;
        }
        return false;
    }
}
