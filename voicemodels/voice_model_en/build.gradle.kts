plugins {
    alias(libs.plugins.android.asset.pack)
}

assetPack {
    packName.set("voice_model_en")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}

apply(from = "../voice-model-pack.gradle.kts")
