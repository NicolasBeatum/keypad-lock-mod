package com.keypadlock.client

import com.keypadlock.client.network.NetworkHandlersClient
import net.fabricmc.api.ClientModInitializer

object KeypadLockModClient : ClientModInitializer {
    override fun onInitializeClient() {
        // NetworkCommon.register() (registro de tipos de payload) ya corre
        // desde KeypadLockMod.onInitialize(), que tambien se ejecuta del lado
        // cliente -- no volver a registrar aca.
        NetworkHandlersClient.register()
        ClientBlockEntityRenderers.register()
    }
}
