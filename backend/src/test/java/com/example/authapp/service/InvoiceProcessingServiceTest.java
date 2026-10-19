package com.example.authapp.service;

import com.example.authapp.dto.InvoiceExtractionDTO;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.InvoiceRepository;
import com.example.authapp.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceProcessingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private InvoiceProcessingService invoiceService;

    @Test
    @DisplayName("rejects empty invoice document")
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> invoiceService.extractInvoiceData(file))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("rejects invalid file type")
    void rejectsInvalidFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "malicious.exe", "application/x-msdownload", "fake content".getBytes());
        assertThatThrownBy(() -> invoiceService.extractInvoiceData(file))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Only PDF and image files");
    }

    @Test
    @DisplayName("rejects files exceeding size limit")
    void rejectsOversizedFile() {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);
        assertThatThrownBy(() -> invoiceService.extractInvoiceData(file))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("exceeds 10MB limit");
    }

    @Test
    @DisplayName("parses invoice text fields accurately")
    void parsesInvoiceTextFields() {
        String sampleText = """
                Acme Cloud Services Inc.
                Vendor: Acme Cloud Services Inc.
                Invoice #: INV-2026-9876
                Date: 2026-04-15
                Subtotal: $1,000.00
                Tax: $100.00
                Total Amount: $1,100.00
                """;

        InvoiceExtractionDTO dto = new InvoiceExtractionDTO();
        invoiceService.parseFieldsFromText(sampleText, dto);

        assertThat(dto.getVendorName()).isEqualTo("Acme Cloud Services Inc.");
        assertThat(dto.getInvoiceNumber()).isEqualTo("INV-2026-9876");
        assertThat(dto.getInvoiceDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(dto.getSubtotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(dto.getTaxAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(dto.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1100.00"));

        invoiceService.validateInvoiceCalculations(dto);
        assertThat(dto.isTotalConsistent()).isTrue();
        assertThat(dto.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("flags arithmetic inconsistency when subtotal + tax does not equal total")
    void flagsInconsistentTotals() {
        InvoiceExtractionDTO dto = new InvoiceExtractionDTO();
        dto.setSubtotal(new BigDecimal("500.00"));
        dto.setTaxAmount(new BigDecimal("50.00"));
        dto.setTotalAmount(new BigDecimal("600.00")); // Mismatch: 500 + 50 != 600

        invoiceService.validateInvoiceCalculations(dto);

        assertThat(dto.isTotalConsistent()).isFalse();
        assertThat(dto.getWarnings()).anyMatch(w -> w.contains("Inconsistent totals"));
    }

    @Test
    @DisplayName("flags duplicate invoice number")
    void flagsDuplicateInvoice() {
        String text = """
                Acme Cloud Services
                Invoice #: INV-DUPLICATE-01
                Subtotal: $100.00
                Tax: $10.00
                Total: $110.00
                """;

        when(invoiceRepository.existsByInvoiceNumberIgnoreCase("INV-DUPLICATE-01")).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", text.getBytes()
        );

        InvoiceExtractionDTO dto = invoiceService.extractInvoiceData(file);

        assertThat(dto.isDuplicate()).isTrue();
        assertThat(dto.getWarnings()).anyMatch(w -> w.contains("Duplicate invoice detected"));
    }
}
