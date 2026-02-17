package com.programmablegolem

import com.programmablegolem.gui.GolemComputerScreen
import com.programmablegolem.network.OpenScreenPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object ProgrammableGolemClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenScreenPayload.TYPE) { payload, context ->
            context.client().execute {
                context.client().setScreen(
                    GolemComputerScreen(payload.computerPos, payload.isConnected, payload.isDownloading, payload.progress)
                )
            }
        }
    }
}
