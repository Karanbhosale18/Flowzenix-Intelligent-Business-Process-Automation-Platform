package com.example.authapp.controller;

import com.example.authapp.dto.InvoiceExtractionDTO;
import com.example.authapp.dto.InvoiceResponseDTO;
import com.example.authapp.entity.User;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.InvoiceRepository;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.service.InvoiceProcessingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Controller for Invoice Intelligence:
 * file validation, OCR/text extraction, duplicate invoice checking,
 * arithmetic consistency checking, and persistent document attachment.
 */
@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("isAuthenticated()")
public class InvoiceController {

    private final InvoiceProcessingService invoiceProcessingService;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    public InvoiceController(InvoiceProcessingService invoiceProcessingService,
                             InvoiceRepository invoiceRepository,
                             UserRepository userRepository) {
        this.invoiceProcessingService = invoiceProcessingService;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Extracts text and fields from an invoice document (PDF, PNG, JPEG),
     * validating file size, type, calculations, and duplicate numbers.
     * Does NOT commit to storage until reviewed.
     */
    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    public ResponseEntity<InvoiceExtractionDTO> extractInvoice(@RequestParam("file") MultipartFile file) {
        InvoiceExtractionDTO extraction = invoiceProcessingService.extractInvoiceData(file);
        return ResponseEntity.ok(extraction);
    }

    /**
     * Saves the verified invoice document and links it to a request if provided.
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<InvoiceResponseDTO> uploadAndSaveInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "requestId", required = false) Long requestId,
            @RequestParam(value = "vendorName", required = false) String vendorName,
            @RequestParam(value = "invoiceNumber", required = false) String invoiceNumber,
            @RequestParam(value = "invoiceDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
            @RequestParam(value = "subtotal", required = false) BigDecimal subtotal,
            @RequestParam(value = "taxAmount", required = false) BigDecimal taxAmount,
            @RequestParam(value = "totalAmount", required = false) BigDecimal totalAmount,
            @RequestParam(value = "currency", required = false) String currency,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new WorkflowException("User not found"));

        // Extract base data from file
        InvoiceExtractionDTO extraction = invoiceProcessingService.extractInvoiceData(file);

        // Apply any manual user overrides/corrections
        if (vendorName != null && !vendorName.isBlank()) extraction.setVendorName(vendorName.trim());
        if (invoiceNumber != null && !invoiceNumber.isBlank()) extraction.setInvoiceNumber(invoiceNumber.trim());
        if (invoiceDate != null) extraction.setInvoiceDate(invoiceDate);
        if (subtotal != null) extraction.setSubtotal(subtotal);
        if (taxAmount != null) extraction.setTaxAmount(taxAmount);
        if (totalAmount != null) extraction.setTotalAmount(totalAmount);
        if (currency != null && !currency.isBlank()) extraction.setCurrency(currency.trim());

        // Re-validate calculations
        invoiceProcessingService.validateInvoiceCalculations(extraction);

        InvoiceResponseDTO response = invoiceProcessingService.saveInvoice(file, extraction, requestId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDTO> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceProcessingService.getInvoice(id));
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(@RequestParam("invoiceNumber") String invoiceNumber) {
        boolean duplicate = invoiceRepository.existsByInvoiceNumberIgnoreCase(invoiceNumber.trim());
        return ResponseEntity.ok(Map.of("duplicate", duplicate));
    }
}
