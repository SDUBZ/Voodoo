package voodoo.mmc

import com.sun.jna.Platform
import mu.KLogging
import voodoo.data.Recommendation
import voodoo.data.sk.FeatureProperties
import voodoo.forge.Forge
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.util.Directories
import voodoo.util.readJson
import voodoo.util.writeJson
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.*
import kotlin.system.exitProcess


object MMCUtil : KLogging() {
    private val directories = Directories.get(moduleName = "multimc")
    private val cacheHome = directories.cacheHome

    fun startInstance(name: String) {
        val workingDir = cacheHome.resolve(name).apply { mkdirs() }

        ProcessBuilder("multimc", "--launch", name)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

        logger.info("started multimc instance $name")
    }

    /**
     * Finds the MultiMC data loccation
     */
    fun findDir(): File = when {
        Platform.isWindows() -> {
            val location = "where multimc".runCommand()
            val multimcFile = File(location)
            multimcFile.parentFile ?: run {
                logger.error { multimcFile }
                logger.error("Cannot find MultiMC on PATH")
                logger.error("make sure to add the multimc install location to the PATH")
                logger.error("go to `Control Panel\\All Control Panel Items\\System`" +
                        " >> Advanced system settings" +
                        " >> Environment Variables")
                logger.info("once added restart the shell and try to execute `multimc`")
                exitProcess(1)
            }
        }
        Platform.isLinux() -> File(System.getProperty("user.home") + "/.local/share/multimc")
        else -> throw Exception("unsupported platform")
    }

    fun String.runCommandWithRedirct(workingDir: File = cacheHome) {
        logger.info("running '$this' in $workingDir")
        ProcessBuilder(*split(" ").toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .apply { logger.info { directory() } }
                .start()
//                .waitFor()
    }

    fun String.runCommand(workingDir: File = cacheHome): String {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            throw Exception("cannot execute '$this'")
        }
    }

    fun readCfg(cfgFile: File): SortedMap<String, String> =
            cfgFile.bufferedReader().useLines { lines ->
                lines.map { Pair(it.substringBefore('='), it.substringAfter('=')) }.toMap().toSortedMap()
            }

    fun writeCfg(cfgFile: File, properties: Map<String, String>) {
        cfgFile.createNewFile()
        cfgFile.writeText(
                properties.map { (key, value) -> "$key=$value" }
                        .joinToString("\n")
        )
    }

    /**
     * Prepares a MultiMC instance
     * @return Minecraft Directory
     */
    fun installEmptyPack(name: String, folder: String,
                         icon: File? = null, mcVersion: String? = null, forgeBuild: Int? = null,
                         instanceDir: File = MMCUtil.findDir().resolve("instances").resolve(folder),
                         preLaunchCommand: String? = null): File {
        instanceDir.mkdirs()

        val minecraftDir = instanceDir.resolve(".minecraft")
        minecraftDir.mkdirs()

        val iconKey = if (icon != null && icon.exists()) {
            val iconName = "icon_$folder"
//            val iconName = "icon"
            icon.copyTo(instanceDir.resolve("$iconName.png"), overwrite = true)
            iconName
        } else {
            "default"
        }

        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = if (mmcPackPath.exists()) {
            mmcPackPath.readJson()
        } else MultiMCPack()

        if (mcVersion != null) {
            if (forgeBuild != null) {
                logger.info("forge version for build $forgeBuild")
                val (_, _, _, forgeVersion) = Forge.getForgeUrl(forgeBuild.toString(), mcVersion)
                logger.info("forge version : $forgeVersion")
                mmcPack.components = listOf(
                        PackComponent(
                                uid = "net.minecraftforge",
                                version = forgeVersion,
                                important = true
                        )
                ) + mmcPack.components
            }
            mmcPack.components = listOf(
                    PackComponent(
                            uid = "net.minecraft",
                            version = mcVersion,
                            important = true
                    )
            ) + mmcPack.components
        }
        mmcPackPath.writeJson(mmcPack)

        val cfgFile = instanceDir.resolve("instance.cfg")
        val cfg = if (cfgFile.exists())
            readCfg(cfgFile)
        else
            sortedMapOf()

        cfg["InstanceType"] = "OneSix"
        cfg["id"] = name
        cfg["iconKey"] = iconKey

        if (preLaunchCommand != null) {
            cfg["OverrideCommands"] = "true"
            cfg["PreLaunchCommand"] = preLaunchCommand
        }

        writeCfg(cfgFile, cfg)

        return minecraftDir
    }

    fun selectFeatures(
            features: List<FeatureProperties>,
            previousSelection: Map<String, Boolean>,
            name: String, version: String,
            forceDisplay: Boolean,
            updating: Boolean
    ): Pair<Map<String, Boolean>, Boolean> {
        if (features.isEmpty() && !forceDisplay) {
            logger.info("no selectable features")
            return Pair(mapOf(), false)
        }

        UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
        )

        logger.debug { features }
        logger.debug { previousSelection }

        val toggleButtons = features.associateBy({
            it.name
        }, {
            JToggleButton(it.name, previousSelection[it.name] ?: it.selected)
                    .apply {
                        horizontalAlignment = SwingConstants.RIGHT
                    }
        })

        var success = false
        var reinstall = false

//        logger.info { UIManager.getDefaults()/*.filterValues { it is Icon }.map { (k, v) -> "$k = $v\n" }*/ }

        val windowTitle = "Features" + if (name.isBlank()) "" else " - $name" + if (version.isBlank()) "" else " - $version"
        val dialog = object : JDialog(null as Dialog?, windowTitle, true) {
            init {
                modalityType = Dialog.ModalityType.APPLICATION_MODAL

                val panel = JPanel()
                panel.layout = GridBagLayout()

                val setter = features.mapIndexed { row, feature ->
                    val indicator = JCheckBox("", toggleButtons[feature.name]!!.isSelected)
                    panel.add(indicator,
                            GridBagConstraints().apply {
                                gridx = 0
                                gridy = row
                                weightx = 0.001
                                weighty = 0.001
                                anchor = GridBagConstraints.LINE_START
                                fill = GridBagConstraints.BOTH
                                ipady = 4
                            }
                    )

                    val toggle = toggleButtons[feature.name]!!.apply {
                        alignmentX = Component.RIGHT_ALIGNMENT
                        toolTipText = feature.name
                    }

                    fun select(selected: Boolean) {
                        toggle.isSelected = selected
                        indicator.isSelected = selected
                    }
                    toggle.addItemListener {
                        select(toggle.isSelected)
                    }
                    indicator.addItemListener {
                        select(indicator.isSelected)
                    }

                    panel.add(toggle,
                            GridBagConstraints().apply {
                                gridx = 1
                                gridy = row
                                weightx = 0.001
                                weighty = 0.001
                                anchor = GridBagConstraints.LINE_START
                                fill = GridBagConstraints.BOTH
                                ipady = 4
                            }
                    )

                    val recommendation = when (feature.recommendation) {
                        Recommendation.starred -> {
                            val orange = Color(0xFFd09b0d.toInt())
                            JLabel("★").apply {
                                foreground = orange
                                toolTipText = "Recommended"
                            }
                        }
                        Recommendation.avoid -> {
                            JLabel("⚠️").apply {
                                foreground = Color.RED
                                toolTipText = "Avoid"
                            }
                        }
                        else -> {
                            JLabel("")
                        }
                    }

                    panel.add(recommendation,
                            GridBagConstraints().apply {
                                gridx = 2
                                gridy = row
                                weightx = 0.001
                                fill = GridBagConstraints.BOTH
                                insets = Insets(0, 8, 0, 8)
                            }
                    )

                    if (!feature.description.isBlank()) {
                        val descriptionText = JLabel("<html>${feature.description}</html>")
                        panel.add(descriptionText,
                                GridBagConstraints().apply {
                                    gridx = 3
                                    gridy = row
                                    weightx = 1.0
                                    anchor = GridBagConstraints.LINE_START
                                    fill = GridBagConstraints.BOTH
                                    ipady = 8
                                    insets = Insets(0, 8, 0, 8)
                                }
                        )
                    }
                    feature.name to ::select
                }.toMap()

                add(panel, BorderLayout.CENTER)
                val buttonPane = JPanel(GridBagLayout())

                val buttonResetDefault = JButton("Reset to Default").apply {
                    addActionListener {
                        setter.forEach { (name, function) ->
                            val selected = features.find { it.name == name }!!.selected
                            function(selected)
                        }
                    }
                }
                buttonPane.add(buttonResetDefault, GridBagConstraints().apply {
                    gridx = 0
                    weightx = 1.0
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })
                val buttonResetLast = JButton("Reset to Last").apply {
                    isEnabled = previousSelection.isNotEmpty()
                    addActionListener {
                        setter.forEach { (name, function) ->
                            val selected = features.find { it.name == name }!!.selected
                            function(previousSelection[name] ?: selected)
                        }
                    }
                }
                buttonPane.add(buttonResetLast, GridBagConstraints().apply {
                    gridx = 1
                    weightx = 1.0
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })
                val buttonForceReinstall = JButton("Force Reinstall").apply {
                    isEnabled = false
                    toolTipText = "enable with checkbox"
                    addActionListener {
                        reinstall = true
                        success = true
                        dispose()
                    }
                }
                buttonPane.add(buttonForceReinstall, GridBagConstraints().apply {
                    gridx = 2
                    weightx = 1.0
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })
                val checkForceReinstall = JCheckBox().apply {
                    isEnabled = updating
                    addItemListener {
                        buttonForceReinstall.isEnabled = isSelected
                    }
                }
                buttonPane.add(checkForceReinstall, GridBagConstraints().apply {
                    gridx = 3
                    weightx = 0.1
                    anchor = GridBagConstraints.LINE_END
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })

                val okText = if (updating) "Update" else "Install"
                val button = JButton(okText).apply {
                    addActionListener {
                        isVisible = false
                        success = true
                        dispose()
                    }
                }

                buttonPane.add(button, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 1
//                    weightx = 6.0
                    gridwidth = 4
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })

                add(buttonPane, BorderLayout.SOUTH)
                defaultCloseOperation = DISPOSE_ON_CLOSE
                addWindowListener(
                        object : WindowAdapter() {
                            override fun windowClosed(e: WindowEvent) {
                                logger.info("closing dialog")
                                if (!success)
                                    exitProcess(1)
                            }
                        }
                )
                pack()
                setLocationRelativeTo(null)
            }

            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (!visible) {
                    (parent as? JFrame)?.dispose()
                }
            }

//            fun getValue(): Pair<Map<String, Boolean>, Boolean> {
//                isVisible = true
//                dispose()
//                return features.associateBy({ it.id }, { toggleButtons[it.id]!!.isSelected }) to force
//            }
        }
        logger.info("created dialog")
//        return dialog.getValue()

        dialog.isVisible = true
        dialog.dispose()
        return features.associateBy({ it.name }, { toggleButtons[it.name]!!.isSelected }) to reinstall
    }
}