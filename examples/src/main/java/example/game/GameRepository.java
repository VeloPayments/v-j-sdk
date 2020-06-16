package example.game;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.aggregate.AggregateRepository;

import java.util.UUID;

public class GameRepository implements AggregateRepository<GameAggregate> {

    private final BlockchainOperations blockchain;

    public GameRepository(BlockchainOperations blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public UUID getArtifactType() {
        return GameMetadata.GAME_ARTIFACT_TYPE_ID;
    }

    @Override
    public GameAggregate supply(UUID artifactId) {
        return new GameAggregate(artifactId);
    }

    @Override
    public BlockchainOperations getBlockchain() {
        return blockchain;
    }
}
