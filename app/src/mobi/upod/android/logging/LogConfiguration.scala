package mobi.upod.android.logging

import android.content.Context
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import java.io.{File, InputStream}
import org.slf4j.LoggerFactory

object LogConfiguration {

  private val ProductionConfigurationPath = "config/logback-production.xml"
  private val DebugConfigurationPath = "config/logback-debug.xml"

  private def configureLogging(configuration: InputStream): Unit = {
    val logContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    logContext.reset()

    val config = new JoranConfigurator
    config.setContext(logContext)
    config.doConfigure(configuration)
  }

  def configureLogging(context: Context, enhanced: Boolean): Unit = {
    val configPath = if (enhanced) DebugConfigurationPath else ProductionConfigurationPath
    configureLogging(context.getAssets.open(configPath))
  }

  def logDir(context: Context): File =
    new File(context.getFilesDir, "logs")
}
