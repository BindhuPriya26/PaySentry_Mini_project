package com.paysentry.backend.service;

import org.junit.jupiter.api.Test;

import com.paysentry.backend.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrScanServiceTests {

    @Test
    void acceptsAnImageAtThePixelLimit() {
        assertDoesNotThrow(() -> QrScanService.validateImageDimensions(3000, 4000));
    }

    @Test
    void rejectsAnImageAboveThePixelLimit() {
        assertThrows(
            BadRequestException.class,
            () -> QrScanService.validateImageDimensions(12001, 1000)
        );
    }

    @Test
    void rejectsInvalidImageDimensions() {
        assertThrows(BadRequestException.class, () -> QrScanService.validateImageDimensions(0, 500));
    }
}
