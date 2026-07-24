package com.keypadlock.client.network

import com.keypadlock.client.gui.KeypadScreen
import com.keypadlock.network.CancelKeypadPayload
import com.keypadlock.network.CloseKeypadPayload
import com.keypadlock.network.OpenKeypadPayload
import com.keypadlock.network.SubmitPasswordPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft

object NetworkHandlersClient {
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenKeypadPayload.TYPE) { payload, context ->
            context.client().execute {
                Minecraft.getInstance().gui.setScreen(KeypadScreen(payload.blockPos, payload.mode))
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(CloseKeypadPayload.TYPE) { payload, context ->
            context.client().execute {
                val gui = Minecraft.getInstance().gui
                val current = gui.screen()
                if (current is KeypadScreen && current.blockPos == payload.blockPos) {
                    gui.setScreen(null)
                }
            }
        }
    }

    fun sendSubmitPassword(payload: SubmitPasswordPayload) {
        ClientPlayNetworking.send(payload)
    }

    fun sendCancel(payload: CancelKeypadPayload) {
        ClientPlayNetworking.send(payload)
    }
}
