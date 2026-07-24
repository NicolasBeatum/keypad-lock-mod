package com.keypadlock.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

/** Registro de los tipos de payload -- debe correr en ambos lados (cliente y servidor). */
object NetworkCommon {
    fun register() {
        PayloadTypeRegistry.clientboundPlay().register(OpenKeypadPayload.TYPE, OpenKeypadPayload.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(CloseKeypadPayload.TYPE, CloseKeypadPayload.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(SubmitPasswordPayload.TYPE, SubmitPasswordPayload.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(CancelKeypadPayload.TYPE, CancelKeypadPayload.CODEC)
    }
}
