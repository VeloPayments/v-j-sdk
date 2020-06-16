package example.game;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GameTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setEnv() throws Exception {
        System.setProperty("example.path", temporaryFolder.newFolder().getPath());
        System.setProperty("example.keys", temporaryFolder.newFolder().getPath());
    }

    @Ignore // WIP
    @Test
    public void startGame() throws Exception {

        String gameId = "e6e7e1ea-bf74-4a25-8f62-331cb5b7807b";

        GameExample.main(new String[]{ gameId });

        GameExample.main(new String[]{ gameId, "X4" });

        GameExample.main(new String[]{ gameId, "O2" });

        GameExample.main(new String[]{ gameId, "X0" });

        GameExample.main(new String[]{ gameId, "O5" });

        GameExample.main(new String[]{ gameId, "X8" });

        GameExample.main(new String[]{ gameId });
    }
}
