package com.keypadlock.network

import com.keypadlock.KeypadLockMod
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/** Que keypad mostrar / para que accion server-side es la respuesta. */
enum class KeypadMode {
    CREATE,       // crear password nueva (recien reforzado)
    OPEN_CHEST,   // intentar abrir un cofre reforzado
    OPEN_DOOR     // intentar abrir una puerta reforzada
}

/**
 * S2C: el servidor le pide al cliente que abra el HUD del keypad para el
 * bloque en [blockPos], en el modo [mode].
 */
data class OpenKeypadPayload(val blockPos: BlockPos, val mode: KeypadMode) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "open_keypad")
        val TYPE = CustomPacketPayload.Type<OpenKeypadPayload>(ID)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenKeypadPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenKeypadPayload::blockPos,
            ByteBufCodecs.VAR_INT.map({ KeypadMode.entries[it] }, { it.ordinal }), OpenKeypadPayload::mode,
            ::OpenKeypadPayload
        )
    }
}

/**
 * C2S: el jugador confirmo digitos en el keypad para [blockPos]. Si
 * [forCreate] es true es una creacion de password nueva, si es false es un
 * intento de apertura contra el hash ya guardado. El servidor NUNCA confia
 * en esto a ciegas -- re-valida que el bloque siga existiendo, siga
 * reforzado, y que el jugador este en rango antes de actuar.
 *
 * [explicit] distingue un intento manual (boton Confirmar / Enter) de un
 * intento automatico (el cliente prueba solo apenas se alcanza el largo
 * minimo, sin que el jugador aprete nada -- ver KeypadScreen.maybeAutoSubmit).
 * Solo se manda el mensaje de "Contraseña incorrecta" para intentos
 * explicitos, para no spamear chat mientras el jugador todavia esta tipeando.
 */
data class SubmitPasswordPayload(val blockPos: BlockPos, val digits: List<Int>, val forCreate: Boolean, val explicit: Boolean) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "submit_password")
        val TYPE = CustomPacketPayload.Type<SubmitPasswordPayload>(ID)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SubmitPasswordPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SubmitPasswordPayload::blockPos,
            ByteBufCodecs.collection(::ArrayList, ByteBufCodecs.VAR_INT, 8), SubmitPasswordPayload::digits,
            ByteBufCodecs.BOOL, SubmitPasswordPayload::forCreate,
            ByteBufCodecs.BOOL, SubmitPasswordPayload::explicit,
            ::SubmitPasswordPayload
        )
    }
}

/** C2S: el jugador cerro/cancelo el keypad sin confirmar (informativo, sin efecto de seguridad). */
data class CancelKeypadPayload(val blockPos: BlockPos) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "cancel_keypad")
        val TYPE = CustomPacketPayload.Type<CancelKeypadPayload>(ID)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, CancelKeypadPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CancelKeypadPayload::blockPos,
            ::CancelKeypadPayload
        )
    }
}

/**
 * S2C: el servidor le avisa al cliente que cierre el HUD del keypad para
 * [blockPos] porque el acceso ya se resolvio del lado servidor (ej: la
 * puerta se abrio). Para el cofre no hace falta -- abrir el menu del
 * contenedor ya reemplaza el screen solo -- pero mandarlo igual no molesta
 * y el cliente lo ignora si ya no es ese el screen activo.
 */
data class CloseKeypadPayload(val blockPos: BlockPos) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "close_keypad")
        val TYPE = CustomPacketPayload.Type<CloseKeypadPayload>(ID)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, CloseKeypadPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CloseKeypadPayload::blockPos,
            ::CloseKeypadPayload
        )
    }
}
