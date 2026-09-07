package com.fastbills.api.customer;

import com.fastbills.api.customer.dto.CreateCustomerRequest;
import com.fastbills.api.customer.dto.CustomerResponse;
import com.fastbills.api.customer.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer master management with GST compliance")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Create customer", description = "Creates a new customer with GST validation and uniqueness checks")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List customers", description = "Returns a paginated list of customers with search and archival filters")
    public ResponseEntity<Page<CustomerResponse>> listCustomers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean isArchived,
            @PageableDefault(size = 20, sort = "legalName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(customerService.listCustomers(query, isArchived, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Fetches a customer record within the authenticated tenant")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Updates customer details with GST consistency validation")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive customer", description = "Archives a customer so they can no longer be used on new invoices")
    public ResponseEntity<CustomerResponse> archiveCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.archiveCustomer(id));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive customer", description = "Explicit archive action for a customer")
    public ResponseEntity<CustomerResponse> archiveCustomerExplicit(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.archiveCustomer(id));
    }

    @PostMapping("/{id}/unarchive")
    @Operation(summary = "Unarchive customer", description = "Restores an archived customer to active status")
    public ResponseEntity<CustomerResponse> unarchiveCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.unarchiveCustomer(id));
    }
}
