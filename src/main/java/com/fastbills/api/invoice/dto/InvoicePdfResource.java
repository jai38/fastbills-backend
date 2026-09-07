package com.fastbills.api.invoice.dto;

import org.springframework.core.io.Resource;

public record InvoicePdfResource(
        Resource resource,
        String filename
) {}
