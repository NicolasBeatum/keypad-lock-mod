package com.keypadlock

import com.keypadlock.block.ModBlockEntities
import com.keypadlock.block.ModBlocks
import com.keypadlock.creative.ModCreativeTabs
import com.keypadlock.item.ModItems
import com.keypadlock.network.NetworkCommon
import com.keypadlock.network.NetworkHandlersServer
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object KeypadLockMod : ModInitializer {
    const val MOD_ID = "keypadlock"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)!!

    override fun onInitialize() {
        ModBlocks.register()
        ModBlockEntities.register()
        ModItems.register()
        ModCreativeTabs.register()
        NetworkCommon.register()
        NetworkHandlersServer.register()
        LOGGER.info("Keypad Lock inicializado (server-authoritative).")
    }
}
