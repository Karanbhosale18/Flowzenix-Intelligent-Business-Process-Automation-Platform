package com.example.authapp.service;

import com.example.authapp.dto.InvoiceExtractionDTO;
import com.example.authapp.dto.InvoiceResponseDTO;
import com.example.authapp.entity.Invoice;
import com.example.authapp.entity.Request;
import com.example.authapp.entity.User;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.InvoiceRepository;
import com.example.authapp.repository.RequestRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoiceProcessingService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceProcessingService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    private final InvoiceRepository invoiceRepository;
    private final RequestRepository requestRepository;

    @Value("${app.storage.upload-dir:./uploads/invoices}")
    private String uploadDir;

    public InvoiceProcessingService(InvoiceRepository invoiceRepository, RequestRepository requestRepository) {
        this.invoiceRepository = invoiceRepository;
        this.requestRepository = requestRepository;
    }

    /**
     * Validates file, extracts text via PDFBox/OCR heuristics, parses key fields,
     * checks duplicate invoice numbers and mathematical total consistency.
     */
    public InvoiceExtractionDTO extractInvoiceData(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "invoice";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        long size = file.getSize();

        InvoiceExtractionDTO dto = new InvoiceExtractionDTO();
        dto.setFileName(originalFilename);
        dto.setFileSize(size);
        dto.setContentType(contentType);

        String extractedText = "";
        try {
            if ("application/pdf".equalsIgnoreCase(contentType) || originalFilename.toLowerCase().endsWith(".pdf")) {
                try {
                    extractedText = extractTextFromPdf(file.getBytes());
                } catch (Exception pdfEx) {
                    extractedText = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } else {
                extractedText = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Text extraction encountered an issue: {}", e.getMessage());
            extractedText = "";
        }

        dto.setExtractedRawText(extractedText.length() > 500 ? extractedText.substring(0, 500) + "..." : extractedText);

        parseFieldsFromText(extractedText, dto);

        // Consistency verification
        validateInvoiceCalculations(dto);

        // Duplicate verification
        if (dto.getInvoiceNumber() != null && !dto.getInvoiceNumber().isBlank()) {
            boolean exists = invoiceRepository.existsByInvoiceNumberIgnoreCase(dto.getInvoiceNumber().trim());
            if (exists) {
                dto.setDuplicate(true);
                dto.getWarnings().add("Duplicate invoice detected: Invoice number '" + dto.getInvoiceNumber() + "' already exists in the system.");
            }
        }

        return dto;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new WorkflowException("Invoice document file cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new WorkflowException("File size exceeds 10MB limit. Current size: " + (file.getSize() / (1024 * 1024)) + "MB.");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        boolean validContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        boolean validExtension = filename.endsWith(".pdf") || filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg");

        if (!validContentType && !validExtension) {
            throw new WorkflowException("Invalid file type. Only PDF and image files (PNG, JPEG) are permitted.");
        }
    }

    private String extractTextFromPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public void parseFieldsFromText(String text, InvoiceExtractionDTO dto) {
        if (text == null || text.isBlank()) {
            dto.getWarnings().add("No readable text could be extracted from the document. Manual entry required.");
            return;
        }

        // 1. Invoice Number pattern
        Pattern invNumPattern = Pattern.compile("(?i)(?:invoice\\s*(?:#|no|number|num)?[:.\\s]+)([A-Z0-9-_/]+)");
        Matcher invNumMatcher = invNumPattern.matcher(text);
        if (invNumMatcher.find()) {
            dto.setInvoiceNumber(invNumMatcher.group(1).trim());
        }

        // 2. Invoice Date pattern (supports YYYY-MM-DD, MM/DD/YYYY, DD/MM/YYYY)
        Pattern datePattern = Pattern.compile("(?i)(?:date|dated|invoice\\s*date)[:.\\s]+(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4})");
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1).trim().replace('.', '-').replace('/', '-');
            try {
                if (dateStr.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                    dto.setInvoiceDate(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-M-d")));
                } else if (dateStr.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
                    dto.setInvoiceDate(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M-d-yyyy")));
                }
            } catch (Exception ignored) {}
        }

        // 3. Vendor pattern (same-line matching only)
        Pattern vendorPattern = Pattern.compile("(?i)(?:vendor|supplier|from|billed\\s*by|company)[:. \t]+([A-Za-z0-9&., \t-]{2,60})");
        Matcher vendorMatcher = vendorPattern.matcher(text);
        if (vendorMatcher.find()) {
            dto.setVendorName(vendorMatcher.group(1).trim());
        } else {
            // First non-empty line fallback
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                String clean = line.trim();
                if (clean.length() > 2 && !clean.toLowerCase().contains("invoice") && !clean.matches(".*\\d{3}.*")) {
                    dto.setVendorName(clean);
                    break;
                }
            }
        }

        // 4. Subtotal
        Pattern subtotalPattern = Pattern.compile("(?i)(?:subtotal|sub-total|sub\\s*total)[:.\\s]+\\$?\\s*([\\d,]+\\.?\\d*)");
        Matcher subtotalMatcher = subtotalPattern.matcher(text);
        if (subtotalMatcher.find()) {
            dto.setSubtotal(parseBigDecimal(subtotalMatcher.group(1)));
        }

        // 5. Tax
        Pattern taxPattern = Pattern.compile("(?i)(?:tax|vat|gst|sales\\s*tax)[:.\\s]+\\$?\\s*([\\d,]+\\.?\\d*)");
        Matcher taxMatcher = taxPattern.matcher(text);
        if (taxMatcher.find()) {
            dto.setTaxAmount(parseBigDecimal(taxMatcher.group(1)));
        }

        // 6. Total (word boundary ensures it does not match inside 'subtotal')
        Pattern totalPattern = Pattern.compile("(?i)\\b(?:total\\s*amount|total\\s*due|balance\\s*due|grand\\s*total|total)[:. \\t]+\\$?\\s*([\\d,]+\\.?\\d*)");
        Matcher totalMatcher = totalPattern.matcher(text);
        if (totalMatcher.find()) {
            dto.setTotalAmount(parseBigDecimal(totalMatcher.group(1)));
        }
    }

    private BigDecimal parseBigDecimal(String raw) {
        if (raw == null) return null;
        try {
            String clean = raw.replace(",", "").trim();
            return new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    public void validateInvoiceCalculations(InvoiceExtractionDTO dto) {
        if (dto.getSubtotal() != null && dto.getTaxAmount() != null && dto.getTotalAmount() != null) {
            BigDecimal expectedTotal = dto.getSubtotal().add(dto.getTaxAmount());
            if (expectedTotal.compareTo(dto.getTotalAmount()) != 0) {
                dto.setTotalConsistent(false);
                dto.getWarnings().add(String.format(
                        "Inconsistent totals: Subtotal ($%s) + Tax ($%s) = $%s, which differs from document Total ($%s).",
                        dto.getSubtotal(), dto.getTaxAmount(), expectedTotal, dto.getTotalAmount()
                ));
            }
        }
    }

    /**
     * Persists uploaded file to persistent storage directory and saves Invoice record.
     */
    @Transactional
    public InvoiceResponseDTO saveInvoice(MultipartFile file, InvoiceExtractionDTO extraction, Long requestId, User user) throws IOException {
        validateFile(file);

        Path storageDirectory = Paths.get(uploadDir);
        if (!Files.exists(storageDirectory)) {
            Files.createDirectories(storageDirectory);
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "invoice.pdf";
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String storedFilename = UUID.randomUUID() + "_" + sanitizedFilename;
        Path targetPath = storageDirectory.resolve(storedFilename);

        Files.copy(file.getInputStream(), targetPath);

        Invoice invoice = new Invoice();
        invoice.setFileName(originalFilename);
        invoice.setStoredFilePath(targetPath.toAbsolutePath().toString());
        invoice.setFileSize(file.getSize());
        invoice.setContentType(file.getContentType());
        invoice.setVendorName(extraction.getVendorName());
        invoice.setInvoiceNumber(extraction.getInvoiceNumber());
        invoice.setInvoiceDate(extraction.getInvoiceDate());
        invoice.setSubtotal(extraction.getSubtotal());
        invoice.setTaxAmount(extraction.getTaxAmount());
        invoice.setTotalAmount(extraction.getTotalAmount());
        invoice.setCurrency(extraction.getCurrency() != null ? extraction.getCurrency() : "USD");
        invoice.setCreatedBy(user);

        if (!extraction.isTotalConsistent()) {
            invoice.setValidationStatus("INCONSISTENT_TOTAL");
        } else if (extraction.isDuplicate()) {
            invoice.setValidationStatus("DUPLICATE_FLAGGED");
        } else {
            invoice.setValidationStatus("VALID");
        }

        if (extraction.getWarnings() != null && !extraction.getWarnings().isEmpty()) {
            invoice.setValidationNotes(String.join("; ", extraction.getWarnings()));
        }

        if (requestId != null) {
            Request request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new WorkflowException("Request " + requestId + " not found."));
            invoice.setRequest(request);
        }

        Invoice saved = invoiceRepository.save(invoice);
        return toResponseDTO(saved);
    }

    public InvoiceResponseDTO getInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Invoice " + id + " not found."));
        return toResponseDTO(invoice);
    }

    private InvoiceResponseDTO toResponseDTO(Invoice invoice) {
        InvoiceResponseDTO dto = new InvoiceResponseDTO();
        dto.setId(invoice.getId());
        dto.setRequestId(invoice.getRequest() != null ? invoice.getRequest().getId() : null);
        dto.setFileName(invoice.getFileName());
        dto.setFileSize(invoice.getFileSize());
        dto.setContentType(invoice.getContentType());
        dto.setVendorName(invoice.getVendorName());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setCurrency(invoice.getCurrency());
        dto.setValidationStatus(invoice.getValidationStatus());
        dto.setValidationNotes(invoice.getValidationNotes());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setCreatedByUsername(invoice.getCreatedBy() != null ? invoice.getCreatedBy().getUsername() : null);
        return dto;
    }
}
