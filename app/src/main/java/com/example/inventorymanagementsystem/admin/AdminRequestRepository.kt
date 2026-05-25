package com.example.inventorymanagementsystem.admin

import android.content.Context
import com.example.inventorymanagementsystem.employee.EmployeeRequestRepository
import java.time.LocalDate

data class AdminRequestRecord(
    val id: Long,
    val employeeName: String,
    val employeeEmail: String,
    val category: String,
    val assetName: String,
    val justification: String,
    val priority: String,
    val neededBy: LocalDate,
    val status: String,
    val createdAt: LocalDate,
)

object AdminRequestRepository {
    fun getRequests(context: Context): List<AdminRequestRecord> {
        val submitted = EmployeeRequestRepository.getSubmittedRequests(context)
        return submitted.map { request ->
            AdminRequestRecord(
                id = request.id,
                employeeName = request.employeeName,
                employeeEmail = request.employeeEmail,
                category = request.category,
                assetName = request.assetName,
                justification = request.justification,
                priority = request.priority,
                neededBy = request.neededBy,
                status = request.status,
                createdAt = request.createdAt,
            )
        }.sortedWith(
            compareByDescending<AdminRequestRecord> { 
                it.status == "Pending admin review" 
            }
                .thenByDescending { it.createdAt }
                .thenByDescending { it.id }
        )
    }

    fun getRequest(context: Context, requestId: Long): AdminRequestRecord? {
        return getRequests(context).firstOrNull { it.id == requestId }
    }

    fun acceptRequest(
        context: Context,
        requestId: Long,
        comment: String?,
    ): Boolean {
        val request = getRequest(context, requestId) ?: return false
        
        // Update the request status to "Approved"
        EmployeeRequestRepository.updateRequestStatus(context, requestId, "Approved")
        
        // Send email notification to the employee
        if (request.employeeEmail.isNotBlank()) {
            EmailService.sendApprovalEmail(
                to = request.employeeEmail,
                employeeName = request.employeeName,
                assetName = request.assetName,
                comment = comment,
            )
        }
        
        return true
    }

    fun rejectRequest(
        context: Context,
        requestId: Long,
        comment: String?,
    ): Boolean {
        val request = getRequest(context, requestId) ?: return false
        
        // Update the request status to "Rejected"
        EmployeeRequestRepository.updateRequestStatus(context, requestId, "Rejected")
        
        // Send email notification to the employee
        if (request.employeeEmail.isNotBlank()) {
            EmailService.sendRejectionEmail(
                to = request.employeeEmail,
                employeeName = request.employeeName,
                assetName = request.assetName,
                comment = comment,
            )
        }
        
        return true
    }
}
