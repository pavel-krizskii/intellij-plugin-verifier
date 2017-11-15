package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.io.File

class DeprecatedUsagesParamsBuilder(val pluginRepository: PluginRepository,
                                    val pluginDetailsProvider: PluginDetailsProvider) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): DeprecatedUsagesParams {
    val deprecatedOpts = DeprecatedUsagesOpts()
    val unparsedArgs = Args.parse(deprecatedOpts, freeArgs.toTypedArray(), false)
    if (unparsedArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify path to IDE which deprecated API usages are to be found. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val idePath = File(unparsedArgs[0])
    if (!idePath.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: " + idePath)
    }
    val ideDescriptor = OptionsParser.createIdeDescriptor(idePath, opts)
    val jdkDescriptor = OptionsParser.getJdkDir(opts)
    /**
     * If the release IDE version is specified, get the compatible plugins' versions based on it.
     * Otherwise, use the version of the verified IDE.
     */
    val ideVersionForCompatiblePlugins = deprecatedOpts.releaseIdeVersion?.let { IdeVersion.createIdeVersion(it) } ?: ideDescriptor.ideVersion
    val updatesToCheck = requestUpdatesToCheck(opts, ideVersionForCompatiblePlugins)
    val pluginCoordinates = updatesToCheck.map { PluginCoordinate.ByUpdateInfo(it, pluginRepository) }
    val ideDependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsProvider)
    return DeprecatedUsagesParams(ideDescriptor, JdkDescriptor(jdkDescriptor), pluginCoordinates, ideDependencyFinder, ideVersionForCompatiblePlugins)
  }

  private fun requestUpdatesToCheck(allOpts: CmdOpts, ideVersionForCompatiblePlugins: IdeVersion): List<UpdateInfo> {
    val (checkAllBuilds, checkLastBuilds) = OptionsParser.parsePluginsToCheck(allOpts)
    return OptionsParser.requestUpdatesToCheckByIds(checkAllBuilds, checkLastBuilds, ideVersionForCompatiblePlugins, pluginRepository)
  }

  class DeprecatedUsagesOpts {
    @set:Argument("release-ide-version", alias = "riv", description = "The version of the release IDE for which compatible plugins must be " +
        "downloaded and checked against the specified IDE. This is needed when the specified IDE is a trunk-built IDE for which " +
        "there might not be compatible updates")
    var releaseIdeVersion: String? = null
  }

}