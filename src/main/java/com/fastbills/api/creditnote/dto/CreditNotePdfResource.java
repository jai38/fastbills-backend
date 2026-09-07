package com.fastbills.api.creditnote.dto;

import org.springframework.core.io.Resource;

public record CreditNotePdfResource(
        Resource resource,
        String filename
) {}
