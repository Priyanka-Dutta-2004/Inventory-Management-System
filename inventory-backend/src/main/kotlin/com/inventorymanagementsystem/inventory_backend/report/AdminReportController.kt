package com.inventorymanagementsystem.inventory_backend.report

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/reports")
class AdminReportController(
    private val adminReportService: AdminReportService,
) {
    @GetMapping("/summary")
    fun getSummary(): AdminReportSummaryResponse {
        return adminReportService.getSummary()
    }

    @GetMapping("/preview")
    fun previewReport(
        @RequestParam(required = false) reportType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
        @RequestParam(required = false) department: String?,
    ): AdminReportPreviewResponse {
        return adminReportService.previewReport(reportType, fromDate, toDate, department)
    }
}
