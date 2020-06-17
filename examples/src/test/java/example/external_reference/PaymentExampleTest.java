package example.external_reference;

import com.velopayments.blockchain.sdk.RemoteBlockchain;
import example.ExamplesConfig;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;


public class PaymentExampleTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static GenericContainer<?> agentdContainer = new GenericContainer<>("velopayments/velochain-agentd:latest")
        .withNetworkAliases("agentd")
        .withExposedPorts(RemoteBlockchain.DEFAULT_AGENTD_PORT);

    @ClassRule
    public static GenericContainer<?> vaultContainer =  new MockServerContainer();

    @Before
    public void setEnv() throws Exception {
        System.setOut(new PrintStream(outContent));

        System.setProperty(ExamplesConfig.HOST_PROPERTY_NAME, agentdContainer.getContainerIpAddress());
        System.setProperty(ExamplesConfig.AGENT_PORT_PROPERTY_NAME, agentdContainer.getMappedPort(RemoteBlockchain.DEFAULT_AGENTD_PORT).toString());
        System.setProperty(ExamplesConfig.VAULT_PORT_PROPERTY_NAME, vaultContainer.getMappedPort(RemoteBlockchain.DEFAULT_VAULT_PORT).toString());
        System.setProperty(ExamplesConfig.KEYS_PROPERTY_NAME, temporaryFolder.newFolder().getPath());
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        outContent.reset();
    }

    @Test @Ignore // FIXME: Vault
    public void testExternalReferences() throws Exception {
        PaymentExample.main(new String[]{});
        assertThat(outContent.toString()).contains("CUSTOMER_EMAIL             : bob.smith@example.com\n");
        assertThat(outContent.toString()).contains("CUSTOMER_EMAIL             : null\n");
        assertThat(outContent.toString()).contains("Found image signature      : U28dqGHpwBPkFIPW8ca9R1uJ676Iww3znMilNXi573g=\n");
        assertThat(outContent.toString()).contains("Found image signature      : null\n");
    }
}
