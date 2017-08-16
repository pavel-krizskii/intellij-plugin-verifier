package com.jetbrains.pluginverifier.tasks

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.PluginDependency
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.Progress
import com.jetbrains.pluginverifier.dependencies.DefaultDependencyResolver
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.utils.IdeResourceUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams) : Task() {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiTask::class.java)
  }

  private fun getCustomizedDependencyResolver() = object : DependencyResolver {
    private val trunkResolver = DefaultDependencyResolver(parameters.trunkDescriptor.ide)
    private val releaseResolver = DefaultDependencyResolver(parameters.releaseDescriptor.ide)

    override fun resolve(dependency: PluginDependency, isModule: Boolean): DependencyResolver.Result {
      val result = trunkResolver.resolve(dependency, isModule)
      return if (result is DependencyResolver.Result.NotFound) {
        releaseResolver.resolve(dependency, isModule)
      } else {
        result
      }
    }
  }

  private fun getUpdatesToCheck(trunkVersion: IdeVersion, releaseVersion: IdeVersion): List<UpdateInfo> {
    val lastUpdatesCompatibleWithTrunk = RepositoryManager.getLastCompatibleUpdates(trunkVersion)
    val updatesCompatibleWithRelease = RepositoryManager.getLastCompatibleUpdates(releaseVersion)
    val trunkCompatiblePluginIds = lastUpdatesCompatibleWithTrunk.map { it.pluginId }.toSet()
    return lastUpdatesCompatibleWithTrunk + updatesCompatibleWithRelease.filterNot { it.pluginId in trunkCompatiblePluginIds }
  }

  override fun execute(progress: Progress): CheckTrunkApiResult {
    val trunkVersion = parameters.trunkDescriptor.ideVersion
    val releaseVersion = parameters.trunkDescriptor.ideVersion

    val updatesToCheck = getUpdatesToCheck(trunkVersion, releaseVersion)

    LOG.debug("The following updates will be checked with both #$trunkVersion and #$releaseVersion\n" +
        "The dependencies will be resolved against #$trunkVersion or against #$releaseVersion (if not found): " + updatesToCheck.joinToString())

    val dependencyResolver = getCustomizedDependencyResolver()

    val excludedPlugins = getBrokenPluginsWhichShouldBeIgnored()
    val trunkResults = runCheckIdeConfiguration(parameters.trunkDescriptor, updatesToCheck, dependencyResolver, excludedPlugins, progress)
    val releaseResults = runCheckIdeConfiguration(parameters.releaseDescriptor, updatesToCheck, dependencyResolver, excludedPlugins, progress)

    return CheckTrunkApiResult(trunkResults, releaseResults)
  }

  private fun getBrokenPluginsWhichShouldBeIgnored(): List<PluginIdAndVersion> {
    val trunkBrokenPlugins = IdeResourceUtil.getBrokenPluginsListedInBuild(parameters.trunkDescriptor.ide) ?: emptyList()
    val releaseBrokenPlugins = IdeResourceUtil.getBrokenPluginsListedInBuild(parameters.releaseDescriptor.ide) ?: emptyList()
    return (trunkBrokenPlugins + releaseBrokenPlugins).distinct()
  }

  private fun runCheckIdeConfiguration(ideDescriptor: IdeDescriptor,
                                       updatesToCheck: List<UpdateInfo>,
                                       dependencyResolver: DependencyResolver,
                                       excludedPlugins: List<PluginIdAndVersion>,
                                       progress: Progress): CheckIdeResult {
    val pluginCoordinates = updatesToCheck.map { PluginCoordinate.ByUpdateInfo(it) }
    val checkIdeParams = CheckIdeParams(ideDescriptor, parameters.jdkDescriptor, pluginCoordinates, excludedPlugins, emptyList(), Resolver.getEmptyResolver(), parameters.externalClassesPrefixes, parameters.problemsFilter, dependencyResolver)
    return CheckIdeTask(checkIdeParams).execute(progress)
  }

}