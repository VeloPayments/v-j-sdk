package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.sentinel.criteria.ArtifactIdAndState;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;
import lombok.Data;
import lombok.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.StringReader;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


public class ConfigDefTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void invalid_NoSpaces() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Configuration keys must not contain spaces: \"key a\"");

        ConfigDef.builder()
            .key("key a", ConfigDef.Type.STRING)
            .build();
    }

    @Test
    public void mapProperties() throws Exception {
        String clazz = TestSentinel.class.getPackageName() + ".ConfigDefTest$TestSentinel";
        final String config = "sentinel.my-sentinel=" + clazz + "\n" +
            "sentinel.my-sentinel.key-a= foo \n" +
            "sentinel.my-sentinel.key_b=300000\n" +
            "sentinel.my-sentinel.key.c=300\n" +
            "sentinel.my-sentinel.key-d 1.0001\n" +
            "sentinel.my-sentinel.key-e=false\n" +
            "sentinel.my-sentinel.list=a, b,  cdefg, h\n" +
            "sentinel.my-sentinel.class=" + clazz + "\n" +
            "sentinel.my-sentinel.list=a, b,  cdefg, h\n" +
            "sentinel.my-sentinel.class=" + clazz + "\n" +
            "criteria.my-sentinel.list=a, b,  cdefg, h\n" +
            "criteria.my-sentinel.class=" + clazz + "\n" +
            "sentinel.another=" + clazz + "\n" +
            "sentinel.another.list= \n" +
            "sentinel.another.class=" + clazz + "\n";


        Properties props = new Properties();
        props.load(new StringReader(config));

        props.storeToXML(System.out, "Hi there");

        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = testSentinel.config().mapToSettings("my-sentinel", props);
        assertThat(settings).hasSize(7);
        assertThat(settings).containsOnly(
            entry("key-a", "foo"),
            entry("key_b", 300000L),
            entry("key.c", 300),
            entry("key-d", 1.0001D),
            entry("key-e", false),
            entry("list", List.of("a","b","cdefg", "h")),
            entry("class", TestSentinel.class)
        );

        // another uses default values
        settings = testSentinel.config().mapToSettings("another", props);
        assertThat(settings).hasSize(7);
        assertThat(settings).containsOnly(
            entry("key-a", "a-default"),
            entry("key_b", 1000L),
            entry("key.c", 100),
            entry("key-d", 3.005D),
            entry("key-e", true),
            entry("list", List.of()),
            entry("class", TestSentinel.class)
        );
    }

    @Test
    public void failOnRequiredSettings() throws Exception {
        expectedException.expect(SentinelConfigException.class);
        expectedException.expectMessage("Missing required configuration property: sentinel.my-sentinel.class");

        final String config = "sentinel.my-sentinel=" + TestSentinel.class + "\n" +
            "sentinel.my-sentinel.list=\n" +
            "sentinel.another-sentinel.class=java.lang.String\n";

        Properties props = new Properties();
        props.load(new StringReader(config));

        TestSentinel testSentinel = new TestSentinel();
        testSentinel.config().mapToSettings("my-sentinel", props);
    }

    @Data
    static class TestSentinel implements Sentinel {

        @Override
        public ConfigDef config() {
            return ConfigDef.builder()
                .key("key-a", ConfigDef.Type.STRING, "a-default")
                .key("key_b", ConfigDef.Type.LONG, 1000L)
                .key("key.c", ConfigDef.Type.INT, 100)
                .key("key-d", ConfigDef.Type.DOUBLE, 3.005D)
                .key("key-e", ConfigDef.Type.BOOLEAN, true)
                .key("list", ConfigDef.Type.LIST)
                .key("class", ConfigDef.Type.CLASS)
                .build();
        }

        @Override
        public Criteria start(Map<String, Object> settings, BlockchainOperations blockchain) {
            throw new UnsupportedOperationException("I'm just for testing config");
        }

        @Override
        public Optional<Criteria> notify(UUID latestBlockId, Criteria criteria) {
            throw new UnsupportedOperationException("I'm just for testing config");
        }
    }
}
