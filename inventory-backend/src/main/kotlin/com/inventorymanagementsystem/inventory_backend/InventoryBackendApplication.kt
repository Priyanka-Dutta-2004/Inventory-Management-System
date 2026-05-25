package com.inventorymanagementsystem.inventory_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import java.net.URI

@SpringBootApplication
class InventoryBackendApplication

fun main(args: Array<String>) {
	val env = System.getenv()
	val urlCandidates = listOf(
		"JDBC_DATABASE_URL",
		"DATABASE_URL",
		"INTERNAL_DATABASE_URL",
		"EXTERNAL_DATABASE_URL"
	)
	val selected = urlCandidates.firstNotNullOfOrNull { key ->
		env[key]?.takeIf { it.isNotBlank() }?.let { key to it }
	}

	if (selected != null) {
		val (key, rawUrlInput) = selected
		val rawUrl = rawUrlInput.trim().removeSurrounding("\"").removeSurrounding("'")
		println("Using database URL from env var: $key")

		if (rawUrl.startsWith("jdbc:", ignoreCase = true)) {
			SpringApplicationBuilder(InventoryBackendApplication::class.java)
				.properties("spring.datasource.url=$rawUrl")
				.run(*args)
			return
		}

		if (rawUrl.startsWith("postgres://", ignoreCase = true) || rawUrl.startsWith("postgresql://", ignoreCase = true)) {
			val uri = URI.create(rawUrl)
			val userInfo = uri.userInfo.orEmpty()
			val username = userInfo.substringBefore(':')
			val password = userInfo.substringAfter(':', "")
			val host = uri.host
			val port = if (uri.port == -1) 5432 else uri.port
			val database = uri.path.removePrefix("/")
			val query = uri.query?.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
			val jdbcUrl = "jdbc:postgresql://$host:$port/$database$query"

			SpringApplicationBuilder(InventoryBackendApplication::class.java)
				.properties(
					"spring.datasource.url=$jdbcUrl",
					"spring.datasource.username=$username",
					"spring.datasource.password=$password"
				)
				.run(*args)
			return
		}

		println("Unsupported DB URL format in $key. Value must start with jdbc:, postgres://, or postgresql://")
	}

	val pgHost = env["PGHOST"]?.takeIf { it.isNotBlank() }
	val pgPort = env["PGPORT"]?.takeIf { it.isNotBlank() } ?: "5432"
	val pgDatabase = env["PGDATABASE"]?.takeIf { it.isNotBlank() }
	val pgUser = env["PGUSER"]?.takeIf { it.isNotBlank() }
	val pgPassword = env["PGPASSWORD"]?.takeIf { it.isNotBlank() }

	if (pgHost != null && pgDatabase != null && pgUser != null) {
		val jdbcUrl = "jdbc:postgresql://$pgHost:$pgPort/$pgDatabase"
		println("Using database settings from PGHOST/PGPORT/PGDATABASE/PGUSER env vars")
		SpringApplicationBuilder(InventoryBackendApplication::class.java)
			.properties(
				"spring.datasource.url=$jdbcUrl",
				"spring.datasource.username=$pgUser",
				"spring.datasource.password=${pgPassword.orEmpty()}"
			)
			.run(*args)
		return
	}

	println("No supported DB URL env var found. Falling back to application.properties datasource settings.")
	runApplication<InventoryBackendApplication>(*args)
}
