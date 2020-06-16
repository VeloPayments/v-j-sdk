package example.game;

import com.velopayments.blockchain.sdk.metadata.*;

import java.util.UUID;

public final class GameMetadata {

    private GameMetadata() {
        throw new UnsupportedOperationException("utility class cannot be instantiated");
    }

    // Artifact
    public static final String GAME_TYPE_NAME = "GAME";
    public static final UUID GAME_ARTIFACT_TYPE_ID = UUID.fromString("b3860783-612a-4551-8e98-46dcbdef68b1");

    // Fields
    public static final FieldMetadata PLAYER_ONE = new FieldMetadata(0x0414, "PLAYER_ONE", FieldType.String, false, false, 110);
    public static final FieldMetadata PLAYER_TWO = new FieldMetadata(0x0415, "PLAYER_TWO", FieldType.String, false, false, 120);
    public static final FieldMetadata BOARD_POSITION     = new FieldMetadata(0x0416, "BOARD_POSITION",       FieldType.Short, false, false, 120);
    public static final FieldMetadata MOVE_PLAYER        = new FieldMetadata(0x0417, "MOVE_PLAYER",FieldType.Byte, false, false, 120);

    // Transactions
    public static final TransactionType STARTED_GAME_TRANSACTION_TYPE = new TransactionType(UUID.fromString("1a189d57-dbeb-4ac5-a62d-91410c0d6eb6"), "STARTED");
    public static final TransactionType MOVED_TRANSACTION_TYPE = new TransactionType(UUID.fromString("5ef0c305-b868-4bb4-8405-5cfdf3375967"), "MOVED");
    public static final TransactionType ENDED_GAME_TRANSACTION_TYPE = new TransactionType(UUID.fromString("fe3e2de5-57e1-4209-8e0c-fe691da3dc35"), "ENDED");

    // Artifact states
    public static final ArtifactState STARTED_STATE = new ArtifactState(100, "STARTED");
    public static final ArtifactState P1_WIN_STATE = new ArtifactState(101, "P1_WIN");
    public static final ArtifactState P2_WIN_STATE = new ArtifactState(102, "P2_WIN");
    public static final ArtifactState DRAW_STATE = new ArtifactState(103, "DRAW");

    public static ArtifactTypeMetadataBuilder create() {
        return ArtifactTypeMetadataBuilder.extractMetadata(GAME_ARTIFACT_TYPE_ID, GAME_TYPE_NAME, GameMetadata.class);
    }
}
