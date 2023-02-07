package cuteneko.tgbridge

import cuteneko.tgbridge.tgbot.TgBot
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.network.message.SignedMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException

class Bridge : ModInitializer {
    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Hello Fabric world!")
        INSTANCE = this
        CONFIG = ConfigLoader.load()
        ConfigLoader.save(CONFIG)

        try {
            LANG = ConfigLoader.getLang()
        } catch (e:FileNotFoundException) {
            LOGGER.error("lang.json not found! Read the document for more info")
            return
        }

        val bot = TgBot()
        GlobalScope.launch { bot.startPolling() }

        ServerLifecycleEvents.SERVER_STARTED.register {
            SERVER = it
            if(CONFIG.sendServerStarted) GlobalScope.launch { bot.sendMessageToTelegram(CONFIG.serverStartedMessage)}
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            GlobalScope.launch {
                bot.stop()
                if(CONFIG.sendServerStopping)  bot.sendMessageToTelegram(CONFIG.serverStoppingMessage)
            }

        }

        ServerMessageEvents.CHAT_MESSAGE.register {
                message: SignedMessage?, sender: ServerPlayerEntity?, _ ->
            if(!CONFIG.sendChatMessage) return@register
            val senderName = sender?.displayName.toPlainString()
            val msg = message?.content.toPlainString()
            GlobalScope.launch { bot.sendMessageToTelegram(msg.escapeHTML(), senderName.escapeHTML()) }
        }

        ServerMessageEvents.GAME_MESSAGE.register {
                _, message: Text?, _ ->
            if(!CONFIG.sendGameMessage) return@register
            val msg = message.toPlainString()
            GlobalScope.launch { bot.sendMessageToTelegram(msg) }
        }
    }

    companion object {
        // This logger is used to write text to the console and the log file.
        // It is considered best practice to use your mod id as the logger's name.
        // That way, it's clear which mod wrote info, warnings, and errors.
        const val MOD_ID = "tgbridge"
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

        lateinit var INSTANCE: Bridge
        lateinit var SERVER: MinecraftServer
        lateinit var CONFIG: Config
        lateinit var LANG: Map<String, String>
        fun sendMessage(text: Text?) {
            SERVER.playerManager.playerList.forEach{
                it.sendMessage(text)
            }
        }
    }
}