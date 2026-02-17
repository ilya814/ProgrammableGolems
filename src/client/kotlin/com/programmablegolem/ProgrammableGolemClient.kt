package com.programmablegolem

import com.programmablegolem.network.ModNetworking
import net.fabricmc.api.ClientModInitializer

object ProgrammableGolemClient : ClientModInitializer {
    override fun onInitializeClient() {
        ModNetworking.registerClientPackets()
    }
}
