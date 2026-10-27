package com.financetracker.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The sidebar icon font must ship inside the jar, or the nav buttons stay text-only. */
class FontResourceTest {

    @Test
    void iconFontShipsAsResource() {
        assertNotNull(getClass().getResourceAsStream("/fonts/fa-solid-900.ttf"));
    }
}
