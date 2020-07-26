package com.serendipity.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
@ConditionalOnExpression("'${serendipity.redisson.mode}'=='single' or '${serendipity.redisson.mode}'=='cluster' or '${serendipity.redisson.mode}'=='sentinel'")
@ConditionalOnClass(Redisson.class)
@EnableConfigurationProperties(RedissonProperties.class)
@Configuration
public class RedissonAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "serendipity.redisson.mode",havingValue = "signle")
    RedissonClient redissonSingle(RedissonProperties redissonProperties) {
        Config config = new Config();
        String prefix = "redis://";
        if (redissonProperties.isSsl()) {
            prefix = "rediss://";
        }
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(prefix + redissonProperties.getHost() + ":" + redissonProperties.getPort())
                .setConnectTimeout(redissonProperties.getTimeout())
                .setConnectionPoolSize(redissonProperties.getPool().getMaxIdle())
                .setConnectionMinimumIdleSize(redissonProperties.getPool().getMinIdle())
                .setConnectTimeout(redissonProperties.getTimeout())
                .setDatabase(redissonProperties.getDatabase());
        if (redissonProperties.getPassword() != null){
            singleServerConfig.setPassword(redissonProperties.getPassword());
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(name = "serendipity.redisson.mode",havingValue = "sentinel")
    RedissonClient redissonSentinel(RedissonProperties redissonProperties) {
        Config config = new Config();
        String prefix = "redis://";
        if (redissonProperties.isSsl()) {
            prefix = "rediss://";
        }
        List<String> nodes = redissonProperties.getSentinel().getNodes();
        List<String> newNodes = new ArrayList<>(nodes.size());
        nodes.forEach((index)->newNodes.add(index.startsWith("redis://") ? index : "redis://" + index));
        config.useSentinelServers()
                .addSentinelAddress(newNodes.toArray(new String[0]))
                .setMasterConnectionPoolSize(redissonProperties.getPool().getMinIdle())
                .setSlaveConnectionPoolSize(redissonProperties.getPool().getMinIdle())
                .setClientName("Sentinel")
                .setReadMode(ReadMode.SLAVE)
                .setMasterName(redissonProperties.getSentinel().getMaster())
                .setDatabase(redissonProperties.getDatabase())
                .setTimeout(redissonProperties.getTimeout());

        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(name = "serendipity.redisson.mode", havingValue = "cluster")
    public RedissonClient redissonCluster(RedissonProperties redissonProperties) {
        Config config = new Config();
        List<String> nodes = redissonProperties.getCluster().getNodes();
        ArrayList<String> addresses = new ArrayList<>(nodes.size());
        nodes.forEach((index)->addresses.add(index.startsWith("redis://") ? index : "redis://" + index));
        ClusterServersConfig clusterServersConfig = config.useClusterServers().addNodeAddress(addresses.toArray(new String[0]))
                .setClientName("Cluster")
                .setConnectTimeout(redissonProperties.getTimeout())
                .setRetryInterval(redissonProperties.getCluster().getMaxRedirects())
                .setIdleConnectionTimeout(redissonProperties.getTimeout())
                .setScanInterval(redissonProperties.getCluster().getMaxRedirects());
        if(redissonProperties.getPassword() != null) {
            clusterServersConfig.setPassword(redissonProperties.getPassword()) ;
        }
        return Redisson.create(config);

    }
}
