package example.game;

import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.aggregate.AggregateGuard;
import com.velopayments.blockchain.sdk.aggregate.AggregateRepository;

import java.util.UUID;

public class GameAggregateGuard extends AggregateGuard<GameAggregate> {

    public GameAggregateGuard(AggregateRepository<GameAggregate> repository) {
        super(repository);
    }

    @Override
    public boolean applies(TransactionReader transactionReader) {
        UUID artifactType = transactionReader.getFirst(Field.ARTIFACT_TYPE).asUUID();
        return GameMetadata.GAME_ARTIFACT_TYPE_ID.equals(artifactType);
    }
}
