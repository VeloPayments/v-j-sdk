package example.certificates;

import com.velopayments.blockchain.sdk.RemoteBlockchain;
import example.ExamplesConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CertificatesPackageTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static GenericContainer<?> agentdContainer = new GenericContainer<>("velopayments/velochain-agentd:latest")
        .withNetworkAliases("agentd")
        .withExposedPorts(RemoteBlockchain.DEFAULT_AGENTD_PORT);

    @ClassRule
    public static GenericContainer<MockServerContainer> vaultContainer = new MockServerContainer();

    @Before
    public void setEnv() throws Exception {
        System.setOut(new PrintStream(outContent));

        System.setProperty(ExamplesConfig.HOST_PROPERTY_NAME, agentdContainer.getContainerIpAddress());
        System.setProperty(ExamplesConfig.AGENT_PORT_PROPERTY_NAME, agentdContainer.getMappedPort(RemoteBlockchain.DEFAULT_AGENTD_PORT).toString());
        System.setProperty(ExamplesConfig.VAULT_PORT_PROPERTY_NAME, vaultContainer.getMappedPort(MockServerContainer.PORT).toString());
        System.setProperty(ExamplesConfig.KEYS_PROPERTY_NAME, temporaryFolder.newFolder().getPath());
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        outContent.reset();
    }

    @Test
    public void exampleRunsInSingleRepositoryWithoutError() throws Exception {
        SimpleCertificate.main(new String[]{});
        String output = outContent.toString();
        System.err.println(output);
        assertThat(output).contains("field FOO_FIELD has 2 values");
        assertThat(output).contains("field FOO_FIELD: A");
        assertThat(output).contains("field FOO_FIELD: B");
        assertThat(output).contains("field BAR_FIELD: 100000");
        outContent.reset();

        ArtifactTransactions.main(new String[]{});
        output = outContent.toString();
        System.err.println(output);

        assertThat(output).contains("Firstname: Robert, Lastname: Sanders, DOB: 1975-03-01");
        outContent.reset();

        EncryptedCertificate.main(new String[]{});
        output = outContent.toString();
        System.err.println(output);

        assertThat(output).contains("ACCOUNT_NUMBER: 4111111111111119");
        assertThat(output).contains("LUCKY_NUMBER: 42");
        assertThat(output).contains("FIRST_NAME: Sam");
        assertThat(output).contains("LAST_NAME: Sandwich");
    }
}
