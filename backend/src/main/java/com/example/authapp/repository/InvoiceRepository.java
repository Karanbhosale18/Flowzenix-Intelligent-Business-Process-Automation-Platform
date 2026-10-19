package com.example.authapp.repository;

import com.example.authapp.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNumberIgnoreCase(String invoiceNumber);

    List<Invoice> findByInvoiceNumberIgnoreCase(String invoiceNumber);

    Optional<Invoice> findByRequestId(Long requestId);

    List<Invoice> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
}
