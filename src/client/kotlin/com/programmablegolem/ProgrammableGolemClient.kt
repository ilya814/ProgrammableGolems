package com.programmablegolem

import com.programmablegolem.gui.GolemComputerScreen
import com.programmablegolem.network.OpenScreenPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object ProgrammableGolemClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenScreenPayload.TYPE) { payload, context ->
            val pos = payload.blockPos
            val uuid = payload.golemUUID
            val downloading = payload.isDownloading
            val progress = payload.downloadProgress
            
            context.client().execute {
                context.client().setScreen(
                    GolemComputerScreen(pos, uuid, downloading, progress)
                )
            }
        }
    }
}
