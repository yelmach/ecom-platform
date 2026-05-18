package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidatedFileTest {

    @Test
    void equalsAndHashCode_ShouldUseByteArrayContent() {
        ValidatedFile first = new ValidatedFile(new byte[] { 1, 2, 3 }, "image/png", ".png");
        ValidatedFile second = new ValidatedFile(new byte[] { 1, 2, 3 }, "image/png", ".png");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void bytes_ShouldDefensivelyCopyArray() {
        byte[] originalBytes = new byte[] { 1, 2, 3 };
        ValidatedFile validatedFile = new ValidatedFile(originalBytes, "image/png", ".png");

        originalBytes[0] = 9;
        byte[] returnedBytes = validatedFile.bytes();
        returnedBytes[1] = 8;

        assertArrayEquals(new byte[] { 1, 2, 3 }, validatedFile.bytes());
        assertNotSame(returnedBytes, validatedFile.bytes());
    }

    @Test
    void toString_ShouldIncludeByteArrayContent() {
        ValidatedFile validatedFile = new ValidatedFile(new byte[] { 1, 2 }, "image/png", ".png");

        assertTrue(validatedFile.toString().contains("bytes=[1, 2]"));
    }
}
