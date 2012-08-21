/*
 * DatabaseConfiguration.scala
 * 
 * Overview Project
 * Created by Jonas Karlsson, June 2012
 */

package database

import com.avaje.ebean.config.GlobalProperties

/**
 * Sets up properties needed to configure the database.
 * Reads datasource.default.url in Play's format http://user:password@host/database
 * and converts to jdbc:postgresql://host/database, with user and password in 
 * separate configuration variables.
 */
class DatabaseConfiguration(
		databaseDriverProperty: String = "datasource.default.databaseDriver",
		databaseUrlProperty: String = "datasource.default.databaseUrl",
		usernameProperty: String = "datasource.default.username",
		passwordProperty: String = "datasource.default.password",
		defaultDatabaseUrlProperty: String = "datasource.default.url") {
   
  val databaseDriver = "org.postgresql.Driver"
  val (databaseUrl, username, password) = readSettings()
    
  def readSettings() : (String, String, String) = {
	val databaseSetting = sys.props.get(defaultDatabaseUrlProperty)

	databaseSetting match {
      case Some(databaseInfo) => {
        val urlPattern = """[^:]+://([^:]+):([^@]+)@([^/]+)/(.+)""".r

        val urlPattern(user, password, host, database) = databaseInfo
        ("jdbc:postgresql://"+host+"/"+database, user, password)
      }
      case None => {
        (GlobalProperties.get(databaseUrlProperty, null),
         GlobalProperties.get(usernameProperty, null),
         GlobalProperties.get(passwordProperty, null))
      }
	}
  }
}