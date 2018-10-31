package com.ibatis.sqlmap.engine.config;

import com.ibatis.sqlmap.engine.cache.*;
import com.ibatis.sqlmap.engine.impl.*;
import com.ibatis.sqlmap.engine.scope.*;

import java.util.Properties;

public class CacheModelConfig {
    private ErrorContext errorContext;
    private CacheModel cacheModel;

    CacheModelConfig(SqlMapConfiguration config, String id, CacheController controller) {
        this.errorContext = config.getErrorContext();
        this.cacheModel = new CacheModel();
        SqlMapClientImpl client = config.getClient();
        errorContext.setActivity("building a cache model");
        errorContext.setObjectId(id + " cache model");
        errorContext.setMoreInfo("Check the cache model type.");
        cacheModel.setId(id);
        cacheModel.setResource(errorContext.getResource());
        cacheModel.setCacheController(controller);
        errorContext.setMoreInfo("Check the cache model configuration.");
        if (client.getDelegate().isCacheModelsEnabled()) {
            client.getDelegate().addCacheModel(cacheModel);
        }
        errorContext.setMoreInfo(null);
        errorContext.setObjectId(null);
    }

    public void setFlushInterval(long hours, long minutes, long seconds, long milliseconds) {
        errorContext.setMoreInfo("Check the cache model flush interval.");
        long t = 0L;
        t += milliseconds;
        t += seconds * 1000L;
        t += minutes * 60L * 1000L;
        t += hours * 60L * 60L * 1000L;
        if (t < 1L)
            throw new RuntimeException(
                "A flush interval must specify one or more of milliseconds, seconds, minutes or hours.");
        cacheModel.setFlushInterval(t);
    }

    public void addFlushTriggerStatement(String statement) {
        errorContext.setMoreInfo("Check the cache model flush on statement elements.");
        cacheModel.addFlushTriggerStatement(statement);
    }

    public void addFlushTriggerRoot(String name) {
        cacheModel.addFlushTriggerRoot(name);
    }

    public void addFlushTriggerCache(String cacheId) {
        errorContext.setMoreInfo("Check the cache model flush on cache flush.");
        cacheModel.addFlushTriggerCache(cacheId);
    }

    public void addFlushTriggerEntityClass(Class<?> clazz) {
        errorContext.setMoreInfo("Check the cache model flush on entity flush.");
        cacheModel.addFlushTriggerEntityClass(clazz);
    }

    public CacheModel getCacheModel() {
        return cacheModel;
    }

    public void setControllerProperties(Properties cacheProps) {
        cacheModel.setControllerProperties(cacheProps);
    }
}
