package example.game;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.SentinelContainer;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import example.ExamplesConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.velopayments.blockchain.sdk.metadata.MetadataHelper.initMetadata;

public class GameExample {

    public static void main(String[] args) throws Exception {
        ExamplesConfig config = ExamplesConfig.getInstance();

        EntityKeys connectionKeys = EntityTool.fromJson(config.getServiceEntityKeysConfigFile());
        RemoteBlockchain blockchain = RemoteBlockchain.builder()
            .entityKeys(connectionKeys)                   // (1)
            .agentHost(config.getHost())                  // (2)
            .agentPort(config.getAgentPort())             // (3)
            .vaultPort(config.getVaultPort())
            .maxAgentConnections(1)
            .build();

        // Simulate sentinels running elsewhere by using a different thread with it's own connection
        Thread thread = sentinelContainerThread(config);

        //FIXME: use keys for p1 and p2
        final EntityKeys aliceKeys = EntityTool.fromJson(config.getEntityKeysConfigFile("alice", true));

        try (blockchain) {
            blockchain.start();

            // configure guards
            GameRepository repository = new GameRepository(blockchain);
            blockchain.register(new GameAggregateGuard(repository));

            // init game metadata, so it can be viewed by Blockchain Explorer
            initMetadata(blockchain, aliceKeys, GameMetadata.create());

            // start sentinel container after metadata init
            thread.start();

            GameAggregate game;
            if (args.length == 0) {
                UUID gameId = UUID.randomUUID();
                System.out.println("Starting game");
                submitTransaction(startGame(aliceKeys, gameId), repository);
                game = repository.findAggregate(gameId).orElseThrow(() -> new IllegalStateException("Game not found"));
            } else {
                UUID gameId = UUID.fromString(args[0]);
                Optional<GameAggregate> maybeGame = repository.findAggregate(gameId);
                if (maybeGame.isPresent()) {
                    game = maybeGame.get();
                    if (args.length > 1) {
                        // this is a move
                        Certificate transaction = move(aliceKeys, gameId, args[1].toUpperCase(), game.getLastTransactionId());
                        submitTransaction(transaction, repository);
                        game = repository.findAggregate(gameId).orElseThrow(() -> new IllegalStateException("Game not found"));
                    }
                } else {
                    submitTransaction(startGame(aliceKeys, gameId), repository);
                    game = repository.findAggregate(gameId).orElseThrow(() -> new IllegalStateException("Game not found"));
                }
            }

            System.out.println();
            System.out.println(game);
        } finally {
            thread.interrupt();
        }
    }

    /**
     * Simulate a separate process by running SentinelContainer in a thread write a configuration file for the SentinelContainer
     */
    private static Thread sentinelContainerThread(ExamplesConfig config) throws Exception {
        ExamplesConfig.getInstance().getEntityKeysConfigFile("referee", true);
        Path configPath = Files.createTempFile("referee",".properties");
        Files.write(configPath, ("agent.host=" + config.getHost() + "\n" +
            "agent.port=" + config.getAgentPort() + "\n" +
            "sentinel.referee=" + RefereeSentinel.class.getName() +  "\n" +
            "sentinel.referee.keys=" + ExamplesConfig.getInstance().getEntityKeys() + "/referee.keyconfig\n").getBytes());
        Path connectionKeys = ExamplesConfig.getInstance().getServiceEntityKeysConfigFile();
        return new Thread(() -> {
            try {
                SentinelContainer.main(new String[]{ configPath.toString(), connectionKeys.toString() });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static Certificate startGame(EntityKeys keys, UUID gameId) {
        UUID transactionId = UUID.randomUUID();
        return BlockchainUtils.transactionCertificateBuilder()
                .transactionType(GameMetadata.STARTED_GAME_TRANSACTION_TYPE.getId())
                .transactionId(transactionId)
                .artifactType(GameMetadata.GAME_ARTIFACT_TYPE_ID)
                .artifactId(gameId)
                .previousTransactionId(BlockchainUtils.INITIAL_TRANSACTION_UUID)
                .withFields()
                .addString(GameMetadata.PLAYER_ONE.getId(), "bubba.snitman@example.com")
                .addString(GameMetadata.PLAYER_TWO.getId(), "sam.sandwich@example.com")
                .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());
    }

    private static Certificate move(EntityKeys keys, UUID gameId, String moveAndPosition, UUID previousTxn) {
        char player = moveAndPosition.charAt(0);
        short position = Short.parseShort(moveAndPosition.substring(1));
        UUID transactionId = UUID.randomUUID();
        return BlockchainUtils.transactionCertificateBuilder()
            .transactionType(GameMetadata.MOVED_TRANSACTION_TYPE.getId())
            .transactionId(transactionId)
            .artifactType(GameMetadata.GAME_ARTIFACT_TYPE_ID)
            .artifactId(gameId)
            .previousTransactionId(previousTxn)
            .withFields()
            .addByte(GameMetadata.MOVE_PLAYER.getId(), (byte) player)
            .addShort(GameMetadata.BOARD_POSITION.getId(), position)
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());
    }

    private static void submitTransaction(Certificate transaction, GameRepository repository) throws Exception {
        TransactionStatus transactionStatus = repository.getBlockchain().submit(transaction).get(2, TimeUnit.SECONDS);
        if (transactionStatus != TransactionStatus.SUCCEEDED) {
            throw new Exception("Transaction did not succeed");
        }
    }
}
