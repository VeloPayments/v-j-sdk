package com.velopayments.blockchain.sdk.sentinel.offsetstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.velopayments.blockchain.cert.CertificateType;
import com.velopayments.blockchain.sdk.sentinel.SentinelException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * An OffsetStore implemented by a local filesystem.
 * <p>
 * This implementation is only suitable for a simplistic sentinel architecture
 * @see OffsetStore
 */
@Slf4j
public class FileSystemOffsetStore implements OffsetStore {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Path offsetLog;

    public FileSystemOffsetStore(Path offsetLog) {
        this.offsetLog = Objects.requireNonNull(offsetLog);
    }

    /**
     * @see OffsetStore#initialize()
     */
    @Override
    public BlockOffset initialize() {
        if (!Files.exists(offsetLog)) {
            try {
                Files.createFile(offsetLog);
                return resetLog();
            } catch (IOException e) {
                throw new SentinelException("Error creating block offset log", e);
            }
        }

        try (Reader reader = Files.newBufferedReader(offsetLog)) {
            return objectMapper.readValue(reader, BlockOffset.class);
        } catch (MismatchedInputException e) {
            log.warn("{} is invalid, reinitializing", offsetLog);
            return resetLog();
        } catch (IOException e) {
            throw new SentinelException("Error initializing block offset log", e);
        }
    }

    private BlockOffset readLatest() {
        try (Reader reader = Files.newBufferedReader(offsetLog)) {
            return objectMapper.readValue(reader, BlockOffset.class);
        } catch (IOException e) {
            throw new SentinelException("Error reading block offset", e);
        }
    }

    private void logBlockOffset(BlockOffset txOffset) {
        try (Writer writer = Files.newBufferedWriter(offsetLog)) {
            writer.write(objectMapper.writeValueAsString(txOffset));
        } catch (IOException e) {
            throw new SentinelException("Error logging block offset", e);
        }
    }

    /**
     * @see OffsetStore#recordBlock(UUID, long)
     */
    @Override
    public void recordBlock(UUID blockId, long blockHeight) {
        BlockOffset txOffset = readLatest();

        // update the offset and record it
        txOffset.setBlockId(blockId);
        txOffset.setBlockHeight(blockHeight);

        logBlockOffset(txOffset);
    }

    /**
     * @see OffsetStore#isBlockProcessed(long)
     */
    @Override
    public boolean isBlockProcessed(long blockHeight) {
        BlockOffset txOffset = readLatest();
        return txOffset.getBlockHeight() >= blockHeight;
    }

    private BlockOffset resetLog() {
        BlockOffset txOffset = new BlockOffset();
        txOffset.setBlockId(CertificateType.ROOT_BLOCK);
        txOffset.setBlockHeight(0L);
        logBlockOffset(txOffset);
        return txOffset;
    }
}
