package com.example.inventorymanagementsystem.employee

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class MaintenanceType {
    MANUAL, SCHEDULED
}

data class MaintenanceMessage(
    val id: Long,
    val sender: String,
    val senderEmail: String,
    val content: String,
    val timestamp: LocalDateTime,
)

data class MaintenanceTicket(
    val id: Long,
    val employeeName: String,
    val employeeEmail: String,
    val assetId: String,
    val assetName: String,
    val description: String,
    val priority: String,
    val status: String,
    val createdAt: LocalDate,
    val maintenanceType: MaintenanceType = MaintenanceType.MANUAL,
    val messages: List<MaintenanceMessage> = emptyList(),
)

object EmployeeMaintenanceRepository {
    private const val preferencesName = "employee_maintenance"
    private const val keyTickets = "maintenance_tickets"
    private const val keyMessages = "maintenance_messages"
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun saveTicket(
        context: Context,
        session: EmployeeSession?,
        assetId: String,
        assetName: String,
        description: String,
        priority: String,
        maintenanceType: MaintenanceType = MaintenanceType.MANUAL,
    ): MaintenanceTicket {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val tickets = getTickets(context).toMutableList()
        val nextId = (tickets.maxOfOrNull { it.id } ?: 0L) + 1L
        
        val record = MaintenanceTicket(
            id = nextId,
            employeeName = session?.name.orEmpty().ifBlank { "Employee" },
            employeeEmail = session?.email.orEmpty(),
            assetId = assetId,
            assetName = assetName,
            description = description,
            priority = priority,
            status = "Open",
            createdAt = LocalDate.now(),
            maintenanceType = maintenanceType,
            messages = emptyList(),
        )
        tickets += record
        
        preferences.edit()
            .putString(keyTickets, JSONArray(tickets.map(::ticketToJson)).toString())
            .apply()
            
        return record
    }

    fun getTickets(context: Context): List<MaintenanceTicket> {
        val storedValue = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(keyTickets, null)
            ?: return emptyList()
            
        return runCatching {
            val jsonArray = JSONArray(storedValue)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    add(jsonToTicket(jsonArray.getJSONObject(index), context))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun getTicket(context: Context, ticketId: Long): MaintenanceTicket? {
        return getTickets(context).firstOrNull { it.id == ticketId }
    }

    fun getTicketsByType(context: Context, type: MaintenanceType): List<MaintenanceTicket> {
        return getTickets(context).filter { it.maintenanceType == type }
    }

    fun updateTicketStatus(
        context: Context,
        ticketId: Long,
        status: String,
    ): Boolean {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val tickets = getTickets(context).toMutableList()
        val index = tickets.indexOfFirst { it.id == ticketId }
        if (index == -1) return false

        tickets[index] = tickets[index].copy(status = status)
        preferences.edit()
            .putString(keyTickets, JSONArray(tickets.map(::ticketToJson)).toString())
            .apply()
        return true
    }

    fun addMessage(
        context: Context,
        ticketId: Long,
        sender: String,
        senderEmail: String,
        content: String,
    ): Boolean {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val tickets = getTickets(context).toMutableList()
        val index = tickets.indexOfFirst { it.id == ticketId }
        if (index == -1) return false

        val ticket = tickets[index]
        val newMessage = MaintenanceMessage(
            id = (ticket.messages.maxOfOrNull { it.id } ?: 0L) + 1L,
            sender = sender,
            senderEmail = senderEmail,
            content = content,
            timestamp = LocalDateTime.now(),
        )
        
        tickets[index] = ticket.copy(messages = ticket.messages + newMessage)
        preferences.edit()
            .putString(keyTickets, JSONArray(tickets.map(::ticketToJson)).toString())
            .apply()
        return true
    }

    fun getTicketMessages(context: Context, ticketId: Long): List<MaintenanceMessage> {
        return getTicket(context, ticketId)?.messages ?: emptyList()
    }

    private fun ticketToJson(ticket: MaintenanceTicket): JSONObject {
        return JSONObject()
            .put("id", ticket.id)
            .put("employeeName", ticket.employeeName)
            .put("employeeEmail", ticket.employeeEmail)
            .put("assetId", ticket.assetId)
            .put("assetName", ticket.assetName)
            .put("description", ticket.description)
            .put("priority", ticket.priority)
            .put("status", ticket.status)
            .put("createdAt", ticket.createdAt.format(dateFormatter))
            .put("maintenanceType", ticket.maintenanceType.name)
            .put("messages", JSONArray(ticket.messages.map(::messageToJson)))
    }

    private fun jsonToTicket(json: JSONObject, context: Context): MaintenanceTicket {
        val messagesArray = json.optJSONArray("messages") ?: JSONArray()
        val messages = mutableListOf<MaintenanceMessage>()
        for (i in 0 until messagesArray.length()) {
            messages.add(jsonToMessage(messagesArray.getJSONObject(i)))
        }
        
        return MaintenanceTicket(
            id = json.optLong("id"),
            employeeName = json.optString("employeeName"),
            employeeEmail = json.optString("employeeEmail"),
            assetId = json.optString("assetId"),
            assetName = json.optString("assetName"),
            description = json.optString("description"),
            priority = json.optString("priority"),
            status = json.optString("status"),
            createdAt = LocalDate.parse(json.getString("createdAt"), dateFormatter),
            maintenanceType = try {
                MaintenanceType.valueOf(json.optString("maintenanceType", "MANUAL"))
            } catch (e: Exception) {
                MaintenanceType.MANUAL
            },
            messages = messages,
        )
    }

    private fun messageToJson(message: MaintenanceMessage): JSONObject {
        return JSONObject()
            .put("id", message.id)
            .put("sender", message.sender)
            .put("senderEmail", message.senderEmail)
            .put("content", message.content)
            .put("timestamp", message.timestamp.format(dateTimeFormatter))
    }

    private fun jsonToMessage(json: JSONObject): MaintenanceMessage {
        return MaintenanceMessage(
            id = json.optLong("id"),
            sender = json.optString("sender"),
            senderEmail = json.optString("senderEmail"),
            content = json.optString("content"),
            timestamp = LocalDateTime.parse(json.getString("timestamp"), dateTimeFormatter),
        )
    }
}
