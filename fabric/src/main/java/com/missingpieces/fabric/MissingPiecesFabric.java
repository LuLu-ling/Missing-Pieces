package com.missingpieces.fabric;

import net.fabricmc.api.ModInitializer;

import com.missingpieces.MissingPieces;

public final class MissingPiecesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MissingPieces.init();
    }
}
