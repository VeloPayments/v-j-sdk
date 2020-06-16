package example.game;

import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.aggregate.Aggregate;
import com.velopayments.blockchain.sdk.vault.ExternalReference;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static example.game.GameMetadata.*;

public class GameAggregate implements Aggregate {

    public static final char P1 = 'X';
    public static final char P2 = 'O';
    public static final char EMPTY = '\u0000';

    private final UUID id;
    private UUID lastTransactionId;
    private int revision;

    private String playerOne;
    private String playerTwo;
    private ZonedDateTime started;
    private ZonedDateTime ended;
    private Short result;

    private final char[] board;
    private char nextMove;

    GameAggregate(UUID id) {
        this.id = id;
        this.board = new char[9];
        this.nextMove = P1;
        this.revision = 0;
    }

    public UUID getId() {
        return this.id;
    }


    public UUID getLastTransactionId() {
        return this.lastTransactionId;
    }

    public int getRevision() {
        return this.revision;
    }

    public String getPlayerOne() {
        return this.playerOne;
    }

    public String getPlayerTwo() {
        return this.playerTwo;
    }

    public ZonedDateTime getStarted() {
        return this.started;
    }

    public ZonedDateTime getEnded() {
        return this.ended;
    }

    public Short getResult() {
        return this.result;
    }

    @Override
    public void apply(TransactionReader reader, Stream<ExternalReference> externalReferences) {
        UUID transactionType = reader.getFirst(Field.TRANSACTION_TYPE).asUUID();
        if (STARTED_GAME_TRANSACTION_TYPE.getId().equals(transactionType)) {
            this.started = toDateTime(reader.getFirst(Field.CERTIFICATE_VALID_FROM).asLong());
            this.playerOne = reader.getFirst(PLAYER_ONE.getId()).asString();
            this.playerTwo = reader.getFirst(PLAYER_TWO.getId()).asString();

        } else if (MOVED_TRANSACTION_TYPE.getId().equals(transactionType)) {
            short pos = reader.getFirst(BOARD_POSITION.getId()).asShort();
            char player = (char) reader.getFirst(MOVE_PLAYER.getId()).asByte();
            if (player != nextMove) {
                throw new IllegalStateException(String.format("Player %c moved out of turn", player));
            }
            if (pos < 0  || pos > 8) {
                throw new IllegalStateException(String.format("Invalid board position %d", pos));
            }
            if (board[pos] != EMPTY) {
                throw new IllegalStateException(String.format("Board position %d is occupied", pos));
            }
            if (result != null) {
                throw new IllegalStateException(String.format("Player %c moved out of turn. Game is over.", player));
            }
            this.board[pos] = player;
            this.nextMove = (player == P1) ? P2 : P1;

        } else if (ENDED_GAME_TRANSACTION_TYPE.getId().equals(transactionType)) {
            this.ended = toDateTime(reader.getFirst(Field.CERTIFICATE_VALID_FROM).asLong());
            this.result = reader.getFirst(Field.NEW_ARTIFACT_STATE).asShort();

        } else {
            throw new IllegalStateException("Unknown game transaction type: " + transactionType);
        }
        this.revision = this.revision + 1;
        this.lastTransactionId = reader.getFirst(Field.CERTIFICATE_ID).asUUID();
    }

    private ZonedDateTime toDateTime(long epochMilli) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
    }

    private String getStatus() {
        if (result == null) {
            return null;
        }
        if (result == STARTED_STATE.getValue()) {
            return STARTED_STATE.getName();
        }
        if (result == DRAW_STATE.getValue()) {
            return DRAW_STATE.getName();
        }
        if (result == P1_WIN_STATE.getValue()) {
            return P1_WIN_STATE.getName();
        }
        if (result == P2_WIN_STATE.getValue()) {
            return P2_WIN_STATE.getName();
        }
        return String.format("Unknown (%d)",result);
    }

    boolean isDraw() {
        for (char move : board) {
            if (move == EMPTY) {
                return false;
            }
        }
        return true;
    }

    boolean hasWinner() {
        return checkRows() || checkCols() || checkDiagonal();
    }

    private boolean checkRows() {
        return (board[0] != EMPTY && board[0] == board[1] && board[1] == board[2])
            || (board[3] != EMPTY && board[3] == board[4] && board[4] == board[5])
            || (board[6] != EMPTY && board[6] == board[7] && board[7] == board[8]);
    }

    private boolean checkCols() {
        return (board[0] != EMPTY && board[0] == board[3] && board[3] == board[6])
            || (board[1] != EMPTY && board[1] == board[4] && board[4] == board[7])
            || (board[2] != EMPTY && board[2] == board[5] && board[5] == board[8]);
    }

    private boolean checkDiagonal() {
        return (board[0] != EMPTY && board[0] == board[4] && board[4] == board[8])
            || (board[2] != EMPTY && board[2] == board[4] && board[4] == board[6]);
    }

    @Override
    public String toString() {
        return "Game: " + getId() +
            "\nRevision: " + getRevision() + "\n" +
            " " + space(0) + " | " + space(1) + " | " + space(2) + " \n" +
            "---+---+---\n" +
            " " + space(3) + " | " + space(4) + " | " + space(5) + " \n" +
            "---+---+---\n" +
            " " + space(6) + " | " + space(7) + " | " + space(8) + " \n" +
            "Result: " + getStatus();
    }


    private char space(int i) {
        return this.board[i] == EMPTY ? ' ' : this.board[i];
    }
}
