package com.missingpieces;

import com.missingpieces.content.VanillaBlockVariants;

public final class MissingPieces {
    public static final String MOD_ID = "missing_pieces";

    private MissingPieces() {
    }

    public static void init() {
        VanillaBlockVariants.register();
    }
}
