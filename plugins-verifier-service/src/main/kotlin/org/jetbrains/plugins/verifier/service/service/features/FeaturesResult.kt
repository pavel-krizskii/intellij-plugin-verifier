package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * The result of the feature extractor service [task] [FeatureExtractorService].
 */
data class FeaturesResult(val updateInfo: UpdateInfo,
                          val resultType: ResultType,
                          val features: List<ExtensionPointFeatures>) {
  enum class ResultType {
    NOT_FOUND,
    BAD_PLUGIN,
    EXTRACTED_ALL,
    EXTRACTED_PARTIALLY
  }
}