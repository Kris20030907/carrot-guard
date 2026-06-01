package com.ktpro.carrotguard;

import org.junit.jupiter.api.Test;

final class SmokeChecksTest {
    @Test
    void gameStateSmokeCheckPasses() {
        GameStateSmokeCheck.main(new String[0]);
    }

    @Test
    void gamePanelRenderSmokeCheckPasses() {
        GamePanelRenderCheck.main(new String[0]);
    }

    @Test
    void gameProgressSmokeCheckPasses() {
        GameProgressSmokeCheck.main(new String[0]);
    }

    @Test
    void soundEffectsSmokeCheckPasses() {
        SoundEffectsSmokeCheck.main(new String[0]);
    }

    @Test
    void assetStoreSmokeCheckPasses() {
        AssetStoreSmokeCheck.main(new String[0]);
    }
}
