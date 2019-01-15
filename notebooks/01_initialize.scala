// Databricks notebook source
// Retrieve storage credentials
val storageAccount = dbutils.preview.secret.get("storage_scope", "storage_account")
val storageKey = dbutils.preview.secret.get("storage_scope", "storage_key")

// Set mount path
val storageMountPath = "/mnt/blob"

// Unmount if existing
dbutils.fs.mounts().map{ mp => 
  if (mp.mountPoint == storageMountPath) {
    dbutils.fs.unmount(storageMountPath)
  }
}

// Refresh mounts
dbutils.fs.refreshMounts()

// COMMAND ----------

// Mount
dbutils.fs.mount(
  source = "wasbs://databricks@" + storageAccount + ".blob.core.windows.net",
  mountPoint = storageMountPath,
  extraConfigs = Map("fs.azure.account.key." + storageAccount + ".blob.core.windows.net" -> storageKey))

// Refresh mounts
dbutils.fs.refreshMounts()

// Fix derby permissions
dbutils.fs.put("/databricks/init/fix-derby-permissions.sh", s"""
#!/bin/bash
cat <<EOF > ${System.getProperty("user.home")}/.java.policy
grant {
     permission org.apache.derby.security.SystemPermission "engine", "usederbyinternals";
};
EOF
""", true)


// COMMAND ----------

// ===============================
// Setting up SQL Schema
// ===============================

// COMMAND ----------

import java.sql.DriverManager

val serverName = dbutils.preview.secret.get("storage_scope", "sql_server_name") + ".database.windows.net"
val database =dbutils.preview.secret.get("storage_scope", "sql_server_database")
val writeuser = dbutils.preview.secret.get("storage_scope", "sql_admin_login")
val writepwd = dbutils.preview.secret.get("storage_scope", "sql_admin_password")
val tableName = dbutils.preview.secret.get("storage_scope", "DBENV_SQL_TABLE_NAME")
val jdbcPortString = dbutils.preview.secret.get("storage_scope", "DBENV_SQL_JDBC_PORT")

def isAllDigits(x: String) = x forall Character.isDigit

var jdbcPort = 1433
if(jdbcPortString != null && !jdbcPortString.isEmpty() && isAllDigits(jdbcPortString)) {
  jdbcPort = jdbcPortString.toInt
}

val driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
val jdbc_url = s"jdbc:sqlserver://${serverName}:${jdbcPort};database=${database};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
  
Class.forName(driver)
val connection = DriverManager.getConnection(jdbc_url, writeuser, writepwd)
val statement = connection.createStatement

val ensureStatement = s"""
if not exists (select * from sysobjects where name='${tableName}' and xtype='U')
    create table ${tableName} (
        UniqueId nvarchar(37),
        TweetTime datetime,
        Content text,
        Language nvarchar(10),
        Topic nvarchar(255)
    )
"""

statement.execute(ensureStatement)
connection.close

// COMMAND ----------


