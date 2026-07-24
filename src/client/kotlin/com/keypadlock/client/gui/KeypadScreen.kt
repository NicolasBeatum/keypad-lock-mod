package com.keypadlock.client.gui

import com.keypadlock.client.network.NetworkHandlersClient
import com.keypadlock.network.CancelKeypadPayload
import com.keypadlock.network.KeypadMode
import com.keypadlock.network.SubmitPasswordPayload
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

/**
 * HUD del keypad, reutilizado tanto para crear password (modo CREATE) como
 * para intentar abrir (modo OPEN_CHEST / OPEN_DOOR). Los digitos se muestran
 * enmascarados dentro de un recuadro propio, y todo el teclado vive dentro
 * de un marco/panel centrado por altura/ancho de contenido real (no offsets
 * fijos a ojo) para que no se desborde con GUI Scale alto. Toda la
 * validacion real ocurre server-side -- este screen solo junta digitos y
 * los manda.
 */
class KeypadScreen(val blockPos: BlockPos, private val mode: KeypadMode) :
    Screen(Component.literal(if (mode == KeypadMode.CREATE) "Crea una contraseña" else "Introduce la contraseña")) {

    companion object {
        private const val MIN_LEN = 4
        private const val MAX_LEN = 8

        private val KEY_TO_DIGIT: Map<Int, Int> = buildMap {
            // GLFW_KEY_0..9 = 48..57, GLFW_KEY_KP_0..9 = 320..329
            for (d in 0..9) {
                put(48 + d, d)
                put(320 + d, d)
            }
        }
        private const val GLFW_KEY_BACKSPACE = 259
        private const val GLFW_KEY_ENTER = 257
        private const val GLFW_KEY_KP_ENTER = 335
        private const val GLFW_KEY_ESCAPE = 256

        private const val BTN = 22
        private const val GAP = 4
        private const val PANEL_PAD_X = 18
        private const val PANEL_PAD_TOP = 14
        private const val PANEL_PAD_BOTTOM = 12

        // Paleta gris acero, a tono con el resto del mod.
        // OJO: GuiGraphicsExtractor.text()/centeredText() NO dibuja nada si el
        // byte de alpha del color es 0 (corta temprano si ARGB.alpha(color)==0)
        // -- todo color de TEXTO de aca en adelante necesita el 0xFF de alpha
        // explicito o el texto queda invisible (ese era el bug de los asteriscos).
        private const val COLOR_DIM_BG = 0xC0101010.toInt()
        private const val COLOR_PANEL_BG = 0xF0303036.toInt()
        private const val COLOR_PANEL_BORDER = 0xFF0F0F12.toInt()
        private const val COLOR_PANEL_BORDER_HILITE = 0xFF5A5A64.toInt()
        private const val COLOR_DIGITBOX_BG = 0xFF1C1C20.toInt()
        private const val COLOR_DIGITBOX_BORDER = 0xFF0A0A0C.toInt()
        private const val COLOR_TITLE_TEXT = 0xFFFFFFFF.toInt()
        private const val COLOR_DIGIT_TEXT = 0xFFAAFFAA.toInt()
    }

    private val digits = mutableListOf<Int>()
    private lateinit var confirmButton: Button

    private var panelLeft = 0
    private var panelTop = 0
    private var panelRight = 0
    private var panelBottom = 0
    private var titleY = 0

    private var digitBoxLeft = 0
    private var digitBoxTop = 0
    private var digitBoxRight = 0
    private var digitBoxBottom = 0

    override fun init() {
        super.init()

        val gridWidth = BTN * 3 + GAP * 2
        // Los botones Confirmar/Cancelar se miden con el ancho real del texto
        // (font.width) en vez de un numero fijo -- eso es lo que se salia del
        // boton con GUI Scale alto ("el texto se sale").
        val confirmLabel = Component.literal("Confirmar")
        val cancelLabel = Component.literal("Cancelar")
        val confirmMinWidth = font.width(confirmLabel) + 12
        val cancelMinWidth = font.width(cancelLabel) + 16

        val actionsRowWidth = BTN + GAP + confirmMinWidth
        val contentWidth = maxOf(gridWidth, actionsRowWidth, cancelMinWidth)

        // Altura total real del contenido (titulo + recuadro + grilla + filas de botones)
        // para poder centrar el panel entero en pantalla sin adivinar offsets.
        val titleH = 9
        val gapAfterTitle = 8
        val digitBoxHeight = 20
        val gapAfterDigitBox = 10
        val gridHeight = BTN * 4 + GAP * 3 // 4 filas: 1-3, 4-6, 7-9, 0
        val gapAfterGrid = 6
        val actionsRowHeight = 20
        val gapAfterActions = 6
        val cancelRowHeight = 20

        val contentHeight = titleH + gapAfterTitle + digitBoxHeight + gapAfterDigitBox +
            gridHeight + gapAfterGrid + actionsRowHeight + gapAfterActions + cancelRowHeight

        val panelWidth = contentWidth + PANEL_PAD_X * 2
        val panelHeight = contentHeight + PANEL_PAD_TOP + PANEL_PAD_BOTTOM

        panelLeft = (width - panelWidth) / 2
        panelRight = panelLeft + panelWidth
        panelTop = (height - panelHeight) / 2
        panelBottom = panelTop + panelHeight

        val contentLeft = panelLeft + (panelWidth - contentWidth) / 2
        val gridLeft = contentLeft + (contentWidth - gridWidth) / 2

        titleY = panelTop + PANEL_PAD_TOP
        var y = titleY + titleH + gapAfterTitle

        digitBoxLeft = gridLeft
        digitBoxRight = gridLeft + gridWidth
        digitBoxTop = y
        digitBoxBottom = y + digitBoxHeight
        y = digitBoxBottom + gapAfterDigitBox

        val gridTop = y
        for (row in 0..2) {
            for (col in 0..2) {
                val digit = row * 3 + col + 1
                val bx = gridLeft + col * (BTN + GAP)
                val by = gridTop + row * (BTN + GAP)
                addRenderableWidget(
                    Button.builder(Component.literal(digit.toString())) { onDigit(digit) }
                        .bounds(bx, by, BTN, BTN)
                        .build()
                )
            }
        }
        val zeroRowY = gridTop + 3 * (BTN + GAP)
        addRenderableWidget(
            Button.builder(Component.literal("0")) { onDigit(0) }
                .bounds(gridLeft + (BTN + GAP), zeroRowY, BTN, BTN)
                .build()
        )
        y = zeroRowY + BTN + gapAfterGrid

        val actionsLeft = contentLeft + (contentWidth - actionsRowWidth) / 2
        addRenderableWidget(
            Button.builder(Component.literal("<-")) { onBackspace() }
                .bounds(actionsLeft, y, BTN, actionsRowHeight)
                .build()
        )
        confirmButton = addRenderableWidget(
            Button.builder(confirmLabel) { onConfirm() }
                .bounds(actionsLeft + BTN + GAP, y, confirmMinWidth, actionsRowHeight)
                .build()
        )
        confirmButton.active = false
        y += actionsRowHeight + gapAfterActions

        val cancelLeft = contentLeft + (contentWidth - cancelMinWidth) / 2
        addRenderableWidget(
            Button.builder(cancelLabel) { onCancel() }
                .bounds(cancelLeft, y, cancelMinWidth, cancelRowHeight)
                .build()
        )
    }

    private fun onDigit(d: Int) {
        if (digits.size < MAX_LEN) {
            digits.add(d)
            confirmButton.active = digits.size >= MIN_LEN
            maybeAutoSubmit()
        }
    }

    private fun onBackspace() {
        if (digits.isNotEmpty()) {
            digits.removeAt(digits.size - 1)
            confirmButton.active = digits.size >= MIN_LEN
        }
    }

    /**
     * En modo OPEN (cofre/puerta), prueba la contraseña sola apenas se llega al
     * largo minimo -- sin esperar Enter/Confirmar. Cada intento es un paquete
     * chiquito + una comparacion de hash SHA-256 ya calculado: no es un gasto
     * de rendimiento real (mucho mas barato que un solo tick de fisica). Si es
     * incorrecta no pasa nada visible (el servidor no manda mensaje para
     * intentos no explicitos), el jugador simplemente sigue tipeando.
     */
    private fun maybeAutoSubmit() {
        if (mode == KeypadMode.CREATE) return
        if (digits.size < MIN_LEN) return
        NetworkHandlersClient.sendSubmitPassword(SubmitPasswordPayload(blockPos, digits.toList(), forCreate = false, explicit = false))
    }

    private fun onConfirm() {
        if (digits.size < MIN_LEN) return
        val forCreate = mode == KeypadMode.CREATE
        NetworkHandlersClient.sendSubmitPassword(SubmitPasswordPayload(blockPos, digits.toList(), forCreate, explicit = true))
        if (forCreate) {
            // Crear contraseña es una accion de un solo paso -- una vez mandada,
            // no hay nada mas que este screen tenga que esperar.
            closeScreen()
        }
        // En modo OPEN no cerramos aca: si es correcta, el servidor abre el
        // cofre/puerta (y eso reemplaza o nos manda cerrar este screen); si es
        // incorrecta, mostramos el mensaje de error pero dejamos el HUD abierto
        // para reintentar. Llamar a onClose() no cierra nada -- es un hook que
        // el juego llama SOBRE nosotros cuando YA nos estan reemplazando, no
        // algo que nosotros disparamos para cerrarnos solos.
    }

    private fun onCancel() {
        NetworkHandlersClient.sendCancel(CancelKeypadPayload(blockPos))
        closeScreen()
    }

    private fun closeScreen() {
        Minecraft.getInstance().gui.setScreen(null)
    }

    // Un HUD de gameplay (como este) no deberia pausar el juego en singleplayer,
    // a diferencia de un menu real (pausa, inventario de otro jugador, etc).
    override fun isPauseScreen(): Boolean = false

    /** Los clicks de mouse en los Button ya suenan solos (AbstractWidget.playDownSound) -- esto cubre el teclado. */
    private fun playKeySound() {
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().soundManager)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        // Abrir el inventario (tecla E por defecto) cierra este HUD como si fuera cancelar,
        // en vez de quedar atrapado atras de un screen que no reacciona a esa tecla.
        if (Minecraft.getInstance().options.keyInventory.matches(event)) {
            playKeySound()
            onCancel()
            return true
        }

        val digit = KEY_TO_DIGIT[event.key()]
        if (digit != null) {
            onDigit(digit)
            playKeySound()
            return true
        }
        when (event.key()) {
            GLFW_KEY_BACKSPACE -> {
                onBackspace()
                playKeySound()
                return true
            }
            GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                if (confirmButton.active) playKeySound()
                onConfirm()
                return true
            }
            GLFW_KEY_ESCAPE -> {
                playKeySound()
                onCancel()
                return true
            }
        }
        return super.keyPressed(event)
    }

    override fun shouldCloseOnEsc(): Boolean = false // manejamos Escape nosotros (para mandar cancel)

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, COLOR_DIM_BG)

        // Marco del panel: borde oscuro + relleno + linea de realce interior (efecto biselado simple).
        graphics.fill(panelLeft - 2, panelTop - 2, panelRight + 2, panelBottom + 2, COLOR_PANEL_BORDER)
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, COLOR_PANEL_BG)
        graphics.horizontalLine(panelLeft, panelRight - 1, panelTop, COLOR_PANEL_BORDER_HILITE)
        graphics.verticalLine(panelLeft, panelTop, panelBottom - 1, COLOR_PANEL_BORDER_HILITE)

        val centerX = (panelLeft + panelRight) / 2
        graphics.centeredText(font, title, centerX, titleY, COLOR_TITLE_TEXT)

        // Recuadro de los digitos enmascarados.
        graphics.fill(digitBoxLeft - 1, digitBoxTop - 1, digitBoxRight + 1, digitBoxBottom + 1, COLOR_DIGITBOX_BORDER)
        graphics.fill(digitBoxLeft, digitBoxTop, digitBoxRight, digitBoxBottom, COLOR_DIGITBOX_BG)
        val masked = "*".repeat(digits.size).ifEmpty { "_" }
        graphics.centeredText(
            font,
            Component.literal(masked),
            (digitBoxLeft + digitBoxRight) / 2,
            (digitBoxTop + digitBoxBottom) / 2 - 4,
            COLOR_DIGIT_TEXT
        )

        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }
}
