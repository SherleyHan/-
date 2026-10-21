package jp.go.mext.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchStatusTest {

    @Test
    void waitCanGoToRun() {
        assertTrue(BatchStatus.WAIT.canTransitionTo(BatchStatus.RUN));
    }

    @Test
    void waitCanGoToAbend() {
        assertTrue(BatchStatus.WAIT.canTransitionTo(BatchStatus.ABEND));
    }

    @Test
    void runCanGoToEnd() {
        assertTrue(BatchStatus.RUN.canTransitionTo(BatchStatus.END));
    }

    @Test
    void runCanGoToAbend() {
        assertTrue(BatchStatus.RUN.canTransitionTo(BatchStatus.ABEND));
    }

    @Test
    void waitCannotJumpToEnd() {
        assertFalse(BatchStatus.WAIT.canTransitionTo(BatchStatus.END));
    }

    @Test
    void endIsTerminal() {
        assertFalse(BatchStatus.END.canTransitionTo(BatchStatus.RUN));
        assertFalse(BatchStatus.END.canTransitionTo(BatchStatus.WAIT));
        assertFalse(BatchStatus.END.canTransitionTo(BatchStatus.ABEND));
    }

    @Test
    void abendIsTerminal() {
        assertFalse(BatchStatus.ABEND.canTransitionTo(BatchStatus.RUN));
        assertFalse(BatchStatus.ABEND.canTransitionTo(BatchStatus.END));
    }
}
