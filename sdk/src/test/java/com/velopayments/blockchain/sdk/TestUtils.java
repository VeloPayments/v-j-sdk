package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

import java.time.Duration;

public class TestUtils {

    public final static EntityKeys TEST_ENTITY = EntityTool.generate("Testy Testerson");
    public static final long COMMIT_FREQUENCY_MILLIS = 200L;


    public static RemoteBlockchain createTestBlockchain(GenericContainer<?> agentdContainer, GenericContainer<?> vaultContainer) throws Exception {
        return RemoteBlockchain.builder()
            .entityKeys(TEST_ENTITY)
            .agentHost(agentdContainer.getContainerIpAddress())
            .agentPort(agentdContainer.getMappedPort(RemoteBlockchain.DEFAULT_AGENTD_PORT))
            .vaultHost(vaultContainer.getContainerIpAddress())
            .vaultPort(vaultContainer.getMappedPort(MockServerContainer.PORT))
            .maxAgentConnections(1)
            .agentConnectionTimeout(Duration.ofSeconds(5))
            .build();
    }

    public static GenericContainer<?> agentdContainer() {
        return new GenericContainer<>("velopayments/velochain-agentd:latest")
            .withNetworkAliases("agentd")
            .withExposedPorts(RemoteBlockchain.DEFAULT_AGENTD_PORT);
    }

    public static GenericContainer<MockServerContainer> vaultContainer() {
        return new MockServerContainer();
    }
}
