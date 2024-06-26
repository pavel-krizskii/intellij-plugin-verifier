package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.InvalidUntilBuild
import com.jetbrains.plugin.structure.intellij.problems.InvalidUntilBuildWithJustBranch
import com.jetbrains.plugin.structure.intellij.problems.InvalidUntilBuildWithMagicNumber
import com.jetbrains.plugin.structure.intellij.problems.NonexistentReleaseInUntilBuild
import com.jetbrains.plugin.structure.intellij.problems.ProductCodePrefixInBuild
import com.jetbrains.plugin.structure.intellij.problems.SuspiciousUntilBuild
import com.jetbrains.plugin.structure.intellij.verifiers.PluginUntilBuildVerifier.ValidationResult.INVALID
import com.jetbrains.plugin.structure.intellij.verifiers.PluginUntilBuildVerifier.ValidationResult.VALID
import com.jetbrains.plugin.structure.intellij.version.IdeVersion


private const val BUILD_NUMBER = "__BUILD_NUMBER__"
private const val SNAPSHOT = "SNAPSHOT"

private const val SUSPICIOUS_BASELINE_LOWER_BOUND = 281
private const val FIRST_YEARLY_BASED_RELEASE_NUMBER_BASELINE = 162
private const val FIRST_YEARLY_BASED_RELEASE_NUMBER_YEAR = 2016

class PluginUntilBuildVerifier {
  fun verify(plugin: PluginBean,
             descriptorPath: String,
             problemRegistrar: ProblemRegistrar) = with(problemRegistrar) {
    val untilBuild = plugin.ideaVersion?.untilBuild ?: return
    if (isJustASingleComponent(untilBuild)) {
      if (isSpecialSingleComponent(untilBuild)) {
        return
      }
      if (verifyMagicNumber(untilBuild, descriptorPath, problemRegistrar) == INVALID) {
        return
      }
      registerProblem(InvalidUntilBuildWithJustBranch(descriptorPath, untilBuild))
      verifySingleComponentUntilBuild(untilBuild, descriptorPath, problemRegistrar)
      return
    }

    val untilBuildParsed = IdeVersion.createIdeVersionIfValid(untilBuild)
    if (untilBuildParsed == null) {
      registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
    } else {
      verifyBaseLineVersion(untilBuildParsed, untilBuild, descriptorPath, problemRegistrar)
      if (untilBuildParsed.productCode.isNotEmpty()) {
        registerProblem(ProductCodePrefixInBuild(descriptorPath))
      }
    }
  }

  private fun verifyBaseLineVersion(untilBuild: IdeVersion,
                                    untilBuildValue: String,
                                    descriptorPath: String,
                                    problemRegistrar: ProblemRegistrar) = with(problemRegistrar) {
    verifyBaseline(untilBuild.baselineVersion, untilBuildValue, untilBuild, descriptorPath, problemRegistrar)
  }

  private fun verifyBaseline(baseline: Int,
                             untilBuildValue: String,
                             untilBuild: IdeVersion?,
                             descriptorPath: String,
                             problemRegistrar: ProblemRegistrar) = with(problemRegistrar) {
    if (baseline >= FIRST_YEARLY_BASED_RELEASE_NUMBER_YEAR) {
      registerProblem(InvalidUntilBuild(descriptorPath, untilBuildValue, untilBuild))
    } else if (baseline >= 999) {
      registerProblem(InvalidUntilBuildWithMagicNumber(descriptorPath, untilBuildValue, baseline.toString()))
    } else if (baseline >= SUSPICIOUS_BASELINE_LOWER_BOUND) {
      registerProblem(SuspiciousUntilBuild(untilBuildValue))
    } else {
      verifyInThreeReleasesPerYear(untilBuildValue, baselineVersion = baseline, problemRegistrar)
    }
  }

  private fun verifyMagicNumber(untilBuildValue: String,
                                descriptorPath: String,
                                problemRegistrar: ProblemRegistrar): ValidationResult = with(problemRegistrar) {
    val untilBuild = untilBuildValue.toIntOrNull() ?: return ValidationResult.NOT_APPLICABLE
    return if (untilBuild >= 999) {
      registerProblem(InvalidUntilBuildWithMagicNumber(descriptorPath, untilBuildValue, untilBuildValue))
      INVALID
    } else {
      VALID
    }
  }

  private enum class ValidationResult {
    VALID, INVALID, NOT_APPLICABLE
  }

  private fun verifySingleComponentUntilBuild(untilBuild: String,
                                              descriptorPath: String,
                                              problemRegistrar: ProblemRegistrar) {
    try {
      val untilBuildNumber = untilBuild.toInt()
      verifyBaseline(untilBuildNumber, untilBuild, untilBuild = null, descriptorPath, problemRegistrar)
    } catch (e: NumberFormatException) {
      problemRegistrar.registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
    }
  }

  private fun verifyInThreeReleasesPerYear(untilBuildValue: String,
                                           baselineVersion: Int,
                                           problemRegistrar: ProblemRegistrar) = with(problemRegistrar) {
    if (baselineVersion >= FIRST_YEARLY_BASED_RELEASE_NUMBER_BASELINE) {
      val lastDigit = baselineVersion % 10
      val releaseVersion = baselineVersion / 10
      if (lastDigit !in 1..3) {
        registerProblem(NonexistentReleaseInUntilBuild(untilBuildValue, "20$releaseVersion.$lastDigit"))
      }
    }
  }

  private fun isSpecialSingleComponent(untilBuild: String): Boolean {
    return BUILD_NUMBER == untilBuild || SNAPSHOT == untilBuild
  }

  private fun isJustASingleComponent(untilBuild: String): Boolean {
    if (!untilBuild.contains('.') && untilBuild.isNotBlank()) {
      return true
    }
    val components = untilBuild.split('.').size
    return components == 1
  }
}