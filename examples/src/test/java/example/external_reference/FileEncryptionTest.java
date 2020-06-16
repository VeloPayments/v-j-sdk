package example.external_reference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class FileEncryptionTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Before
    public void setEnv() throws Exception {
        System.setOut(new PrintStream(outContent));

        System.setProperty("example.path", temporaryFolder.newFolder().getPath());
        System.setProperty("example.keys", temporaryFolder.newFolder().getPath());
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        outContent.reset();
    }

    @Test
    public void exampleRunsWithoutError() throws Exception {
        FileEncryptionExample.main(new String[]{});
        assertThat(outContent.toString()).contains("Alice's decrypted document: " + FileEncryptionExample.DOCUMENT);
    }
}
