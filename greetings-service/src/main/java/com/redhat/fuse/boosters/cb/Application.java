package com.redhat.fuse.boosters.cb;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class Application {

    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean(name = "cacheManager")
    @Primary
    public EmbeddedCacheManager cacheManager() {
        GlobalConfiguration globalConfigurationBuilder = new GlobalConfigurationBuilder()
                .transport()
                .transport(new JGroupsTransport())
                .build();
        DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfigurationBuilder);
        cacheManager.defineConfiguration("nameCache", new ConfigurationBuilder()
                .clustering()
                .cacheMode(CacheMode.REPL_SYNC)
                .build()
        );
        return cacheManager;
    }
}