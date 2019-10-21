package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import javax.annotation.PreDestroy

/**
 * Server context aggregates all services and settings necessary to run
 * the server.
 *
 * Server context must be closed on the server shutdown to de-allocate resources.
 */
class ServerContext(
  val appVersion: String?,
  val ideRepository: IdeRepository,
  val ideFilesBank: IdeFilesBank,
  val pluginRepository: MarketplaceRepository,
  val serviceDAO: ServiceDAO,
  val ideDescriptorsCache: IdeDescriptorsCache,
  val pluginDetailsCache: PluginDetailsCache,
  val verificationResultsFilter: VerificationResultFilter
) {
  @Autowired
  private lateinit var applicationContext: ApplicationContext

  val allServices
    get() = applicationContext.getBeansOfType(BaseService::class.java).values

  @PreDestroy
  fun close() {
    allServices.forEach { it.stop() }
    serviceDAO.closeLogged()
    ideDescriptorsCache.closeLogged()
    pluginDetailsCache.closeLogged()
  }

}