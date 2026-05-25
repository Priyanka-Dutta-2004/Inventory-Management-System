package com.inventorymanagementsystem.inventory_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import java.net.URI

@SpringBootApplication
class InventoryBackendApplication

fun main(args: Array<String>) {
	val env = System.getenv()
	val databaseUrl = env["DATABASE_URL"]
	val jdbcDatabaseUrl = env["JDBC_DATABASE_URL"]

	if (!jdbcDatabaseUrl.isNullOrBlank()) {
		SpringApplicationBuilder(InventoryBackendApplication::class.java)
			.properties("spring.datasource.url=$jdbcDatabaseUrl")
			.run(*args)
		return
	}

	if (!databaseUrl.isNullOrBlank() && !databaseUrl.startsWith("jdbc:", ignoreCase = true)) {
		val uri = URI.create(databaseUrl)
		val userInfo = uri.userInfo.orEmpty()
		val username = userInfo.substringBefore(':')
		val password = userInfo.substringAfter(':', "")
		val host = uri.host
		val port = if (uri.port == -1) 5432 else uri.port
		val database = uri.path.removePrefix("/")
		val jdbcUrl = "jdbc:postgresql://$host:$port/$database"

		SpringApplicationBuilder(InventoryBackendApplication::class.java)
			.properties(
				"spring.datasource.url=$jdbcUrl",
				"spring.datasource.username=$username",
				"spring.datasource.password=$password"
			)
			.run(*args)
		return
	}

	runApplication<InventoryBackendApplication>(*args)
}
