package com.inventorymanagementsystem.inventory_backend.maintenance

import com.inventorymanagementsystem.inventory_backend.user.UserEntity
import com.inventorymanagementsystem.inventory_backend.user.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MaintenanceTicketService(
    private val ticketRepository: MaintenanceTicketRepository,
    private val messageRepository: MaintenanceMessageRepository,
    private val userRepository: UserRepository,
) {

    fun createTicket(
        request: CreateMaintenanceTicketRequest,
        reportedByUserId: Long,
    ): MaintenanceTicketDto {
        val reportedByUser = userRepository.findById(reportedByUserId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val ticket = MaintenanceTicketEntity(
            assetId = request.assetId,
            assetName = request.assetName,
            description = request.description,
            priority = request.priority,
            maintenanceType = request.maintenanceType,
            reportedBy = reportedByUser,
            createdAt = LocalDate.now(),
        )

        val savedTicket = ticketRepository.save(ticket)
        return ticketToDto(savedTicket)
    }

    fun getTicket(ticketId: Long): MaintenanceTicketDto? {
        val ticket = ticketRepository.findById(ticketId).orElse(null) ?: return null
        return ticketToDto(ticket)
    }

    fun getAllTickets(): List<MaintenanceTicketListDto> {
        return ticketRepository.findAll().map { ticketToListDto(it) }
    }

    fun getTicketsByStatus(status: MaintenanceStatusEnum): List<MaintenanceTicketListDto> {
        return ticketRepository.findByStatus(status).map { ticketToListDto(it) }
    }

    fun getTicketsByType(maintenanceType: MaintenanceTypeEnum): List<MaintenanceTicketListDto> {
        return ticketRepository.findByMaintenanceType(maintenanceType).map { ticketToListDto(it) }
    }

    fun getTicketsByStatusAndType(
        status: MaintenanceStatusEnum,
        maintenanceType: MaintenanceTypeEnum,
    ): List<MaintenanceTicketListDto> {
        return ticketRepository.findByStatusAndMaintenanceType(status, maintenanceType)
            .map { ticketToListDto(it) }
    }

    fun getEmployeeTickets(userId: Long): List<MaintenanceTicketListDto> {
        return ticketRepository.findByReportedByUserIdOrderByCreatedAtDesc(userId)
            .map { ticketToListDto(it) }
    }

    fun updateTicketStatus(
        ticketId: Long,
        request: UpdateMaintenanceTicketStatusRequest,
    ): MaintenanceTicketDto {
        val ticket = ticketRepository.findById(ticketId)
            .orElseThrow { IllegalArgumentException("Ticket not found") }

        val resolvedAt = if (request.status == MaintenanceStatusEnum.RESOLVED) LocalDate.now() else null
        val updatedTicket = ticket.copy(
            status = request.status,
            resolvedAt = resolvedAt,
        )

        val savedTicket = ticketRepository.save(updatedTicket)
        return ticketToDto(savedTicket)
    }

    fun addMessage(
        ticketId: Long,
        request: AddMaintenanceMessageRequest,
        senderId: Long,
    ): MaintenanceTicketDto {
        val ticket = ticketRepository.findById(ticketId)
            .orElseThrow { IllegalArgumentException("Ticket not found") }
        val sender = userRepository.findById(senderId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val message = MaintenanceMessageEntity(
            ticket = ticket,
            sender = sender,
            content = request.content,
        )

        messageRepository.save(message)
        return ticketToDto(ticketRepository.findById(ticketId).get())
    }

    fun getMessages(ticketId: Long): List<MaintenanceMessageDto> {
        return messageRepository.findByTicketTicketIdOrderBySentAtAsc(ticketId)
            .map { messageToDto(it) }
    }

    private fun ticketToDto(ticket: MaintenanceTicketEntity): MaintenanceTicketDto {
        return MaintenanceTicketDto(
            ticketId = ticket.ticketId!!,
            assetId = ticket.assetId,
            assetName = ticket.assetName,
            description = ticket.description,
            priority = ticket.priority,
            status = ticket.status,
            maintenanceType = ticket.maintenanceType,
            reportedByName = ticket.reportedBy.name,
            reportedByEmail = ticket.reportedBy.email,
            createdAt = ticket.createdAt,
            resolvedAt = ticket.resolvedAt,
            messages = ticket.messages.map { messageToDto(it) },
        )
    }

    private fun ticketToListDto(ticket: MaintenanceTicketEntity): MaintenanceTicketListDto {
        return MaintenanceTicketListDto(
            ticketId = ticket.ticketId!!,
            assetName = ticket.assetName,
            priority = ticket.priority,
            status = ticket.status,
            maintenanceType = ticket.maintenanceType,
            reportedByName = ticket.reportedBy.name,
            createdAt = ticket.createdAt,
            messageCount = ticket.messages.size,
        )
    }

    private fun messageToDto(message: MaintenanceMessageEntity): MaintenanceMessageDto {
        return MaintenanceMessageDto(
            messageId = message.messageId!!,
            senderName = message.sender.name,
            senderEmail = message.sender.email,
            content = message.content,
            sentAt = message.sentAt,
        )
    }
}
