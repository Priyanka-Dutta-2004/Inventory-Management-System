package com.inventorymanagementsystem.inventory_backend.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `login succeeds for seeded admin user`() {
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "admin@ims.com",
                      "password": "admin123",
                      "role": "ADMIN"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andExpect(jsonPath("$.email").value("admin@ims.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
    }

    @Test
    fun `login fails for incorrect password`() {
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "admin@ims.com",
                      "password": "wrong-password",
                      "role": "ADMIN"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isUnauthorized)
    }
}
