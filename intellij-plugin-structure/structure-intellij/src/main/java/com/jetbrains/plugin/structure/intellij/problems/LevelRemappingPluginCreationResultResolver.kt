package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import kotlin.reflect.KClass

class LevelRemappingPluginCreationResultResolver(private val delegatedResolver: PluginCreationResultResolver,
                                                 additionalLevelRemapping: Map<KClass<*>, RemappedLevel> = emptyMap()
  ) : PluginCreationResultResolver {

  private val remappedLevel: Map<KClass<*>, RemappedLevel> = additionalLevelRemapping

  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
    return when (val pluginCreationResult = delegatedResolver.resolve(plugin, problems)) {
      is PluginCreationSuccess -> remapSuccess(pluginCreationResult)
      is PluginCreationFail -> remapFailure(plugin, pluginCreationResult)
    }
  }

  private fun remapSuccess(pluginCreationResult: PluginCreationSuccess<IdePlugin>): PluginCreationResult<IdePlugin> {
    return with(pluginCreationResult) {
      copy(warnings = remapWarnings(warnings), unacceptableWarnings = remapUnacceptableWarnings(unacceptableWarnings))
    }
  }

  private fun remapFailure(plugin: IdePlugin, pluginCreationResult: PluginCreationFail<IdePlugin>): PluginCreationResult<IdePlugin> {
    return with(pluginCreationResult) {
      val remappedErrorsAndWarnings = remapErrorsAndWarnings(errorsAndWarnings)
      if (remappedErrorsAndWarnings.hasNoErrors()) {
        return PluginCreationSuccess(plugin, remappedErrorsAndWarnings)
      } else {
        copy(errorsAndWarnings = remapErrorsAndWarnings(this.errorsAndWarnings))
      }
    }
  }

  private fun remapWarnings(warnings: List<PluginProblem>): List<PluginProblem> {
    return warnings.mapNotNull(::remapPluginProblemLevel)
  }

  private fun remapUnacceptableWarnings(unacceptableWarnings: List<PluginProblem>): List<PluginProblem> {
    return unacceptableWarnings.mapNotNull(::remapPluginProblemLevel)
  }

  private fun remapErrorsAndWarnings(errorsAndWarnings: List<PluginProblem>): List<PluginProblem> {
    return errorsAndWarnings.mapNotNull(::remapPluginProblemLevel)
  }

  private fun remapPluginProblemLevel(pluginProblem: PluginProblem): PluginProblem?{
    return when (val remappedLevel = remappedLevel[pluginProblem::class]) {
      is StandardLevel -> ReclassifiedPluginProblem(remappedLevel.originalLevel, pluginProblem)
      is IgnoredLevel -> null
      null -> pluginProblem
    }
  }

  override fun classify(plugin: IdePlugin, problems: List<PluginProblem>): List<PluginProblem> {
    return problems.mapNotNull {
      remapPluginProblemLevel(it)
    }
  }

  private fun List<PluginProblem>.hasNoErrors(): Boolean = none {
    it.level == PluginProblem.Level.ERROR
  }
}