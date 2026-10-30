package hu.mik.prog2.converter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchResultTest {

    @Test
    void testGenericBatchResult() {
        BatchResult<String> batch = new BatchResult<>();
        assertEquals(0, batch.getTotalCount());
        assertTrue(batch.isAllSuccessful());

        batch.addSuccess("Item 1");
        batch.addSuccess("Item 2");
        batch.addFailure("Item 3", "Disk full");

        assertEquals(3, batch.getTotalCount());
        assertEquals(2, batch.getSuccessCount());
        assertEquals(1, batch.getFailureCount());
        assertFalse(batch.isAllSuccessful());
        assertEquals("Item 3", batch.getFailedItems().get(0));
        assertEquals("Disk full", batch.getFailureMessages().get(0));
    }
}
