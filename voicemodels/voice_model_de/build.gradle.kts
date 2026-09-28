plugins {
    alias(libs.plugins.android.asset.pack)
}

assetPack {
    packName.set("voice_model_de")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}

apply(from = "../voice-model-pack.gradle.kts")
