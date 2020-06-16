package example.game;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.aggregate.AggregateRepository;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.metadata.ArtifactState;
import com.velopayments.blockchain.sdk.sentinel.ConfigDef;
import com.velopayments.blockchain.sdk.sentinel.Sentinel;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static example.game.GameMetadata.*;

public class RefereeSentinel implements Sentinel {

    private EntityKeys referee;
    private AggregateRepository<GameAggregate> repository;
    private BlockchainOperations blockchain;

    @Override
    public ConfigDef config() {
        return ConfigDef.builder()
            .key("keys", ConfigDef.Type.STRING)
            .build();
    }

    @Override
    public Criteria start(Map<String, Object> settings, BlockchainOperations blockchain) {
        EntityKeys entityKeys = EntityTool.fromJson(Path.of((String) settings.get("keys")));
        return start(entityKeys, blockchain);
    }

    // for testing
    Criteria start(EntityKeys keys, BlockchainOperations blockchain){
        this.blockchain = Objects.requireNonNull(blockchain);
        this.referee = Objects.requireNonNull(keys);
        this.repository = new GameRepository(blockchain);
        return Criteria.builder()
            .latestBlockId(blockchain.getLatestBlockId())
            .transactionType(MOVED_TRANSACTION_TYPE.getId())
            .build();
    }

    @Override
    public synchronized Optional<Criteria> notify(UUID blockId, Criteria criteria) {
        blockchain.findAllBlocksAfter(criteria.getLatestBlockId())
            .flatMap(blockReader -> blockReader.getTransactions().stream())
            .filter(transactionReader -> MOVED_TRANSACTION_TYPE.getId().equals(transactionReader.getTransactionType()))
            .forEach(this::processTransactions);

        return criteria.withBlockId(blockId);
    }
    private void processTransactions(TransactionReader reader) {
        UUID artifactId = reader.getFirst(Field.ARTIFACT_ID).asUUID();
        repository.findAggregate(artifactId).ifPresent(game -> {
            ArtifactState result = null;
            if (game.hasWinner()) {
                char player = (char) reader.getFirst(MOVE_PLAYER.getId()).asByte();
                result = (player == GameAggregate.P1) ? P1_WIN_STATE : P2_WIN_STATE;
            } else if (game.isDraw()) {
                result = DRAW_STATE;
            }
            if (result != null) {
                Certificate endGameTransaction = BlockchainUtils.transactionCertificateBuilder()
                    .transactionType(ENDED_GAME_TRANSACTION_TYPE.getId())
                    .transactionId(UUID.randomUUID())
                    .artifactType(GAME_ARTIFACT_TYPE_ID)
                    .artifactId(artifactId)
                    .previousTransactionId(reader.getFirst(Field.CERTIFICATE_ID).asUUID())
                    .previousState(reader.getFirst(Field.NEW_ARTIFACT_STATE).asInt())
                    .newState(result.getValue())
                    .withFields()
                    .sign(referee.getEntityId(), referee.getSigningKeyPair().getPrivateKey());
                try {
                    CompletableFuture<TransactionStatus> results = repository.getBlockchain().submit(endGameTransaction);
                    results.get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to record transaction ending the game", e);
                }
            }
        });
    }
}
