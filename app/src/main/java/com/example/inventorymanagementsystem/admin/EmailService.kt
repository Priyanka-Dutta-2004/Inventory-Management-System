package com.example.inventorymanagementsystem.admin

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {
    private const val TAG = "EmailService"
    
    // TODO: Replace with your actual email configuration
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private val SMTP_USERNAME = com.example.inventorymanagementsystem.BuildConfig.SMTP_USERNAME
    private val SMTP_PASSWORD = com.example.inventorymanagementsystem.BuildConfig.SMTP_PASSWORD
    private val FROM_EMAIL = com.example.inventorymanagementsystem.BuildConfig.SMTP_USERNAME
    
    fun sendApprovalEmail(
        to: String,
        employeeName: String,
        assetName: String,
        comment: String?,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val subject = "Your Asset Request Has Been Approved"
                val body = buildString {
                    append("Dear $employeeName,\n\n")
                    append("Good news! Your request for \"$assetName\" has been approved.\n\n")
                    if (!comment.isNullOrBlank()) {
                        append("Admin Comment: $comment\n\n")
                    }
                    append("You will receive further information about asset pickup/delivery shortly.\n\n")
                    append("Best regards,\n")
                    append("Inventory Management Team")
                }
                
                sendEmail(to, subject, body)
                Log.d(TAG, "Approval email sent successfully to $to")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send approval email", e)
            }
        }
    }
    
    fun sendRejectionEmail(
        to: String,
        employeeName: String,
        assetName: String,
        comment: String?,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val subject = "Your Asset Request Has Been Rejected"
                val body = buildString {
                    append("Dear $employeeName,\n\n")
                    append("We regret to inform you that your request for \"$assetName\" has been rejected.\n\n")
                    if (!comment.isNullOrBlank()) {
                        append("Reason: $comment\n\n")
                    }
                    append("If you have any questions or would like to discuss this further, please contact the admin team.\n\n")
                    append("Best regards,\n")
                    append("Inventory Management Team")
                }
                
                sendEmail(to, subject, body)
                Log.d(TAG, "Rejection email sent successfully to $to")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send rejection email", e)
            }
        }
    }
    
    private fun sendEmail(to: String, subject: String, body: String) {
        val props = Properties().apply {
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }
        
        val auth = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD)
            }
        }
        
        val session = Session.getInstance(props, auth)
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(FROM_EMAIL))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject)
            setText(body)
        }
        
        Transport.send(message)
    }
}
