package com.fastbills.api.customer;

import com.fastbills.api.common.tenant.TenantContext;
import com.fastbills.api.customer.dto.CreateCustomerRequest;
import com.fastbills.api.customer.dto.CustomerResponse;
import com.fastbills.api.customer.dto.UpdateCustomerRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        UUID orgId = TenantContext.getRequiredTenant();

        // 1. Uniqueness check within organisation (case-insensitive)
        String trimmedLegalName = request.legalName().trim();
        if (customerRepository.existsByOrganisationIdAndLegalNameIgnoreCase(orgId, trimmedLegalName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A customer with legal name '" + trimmedLegalName + "' already exists"
            );
        }

        // 2. GSTIN and state code validation
        validateGstinAndState(request.isRegistered(), request.gstin(), request.pan(), request.billingStateCode());

        Customer customer = new Customer();
        customer.setOrganisationId(orgId);
        customer.setLegalName(trimmedLegalName);
        customer.setTradeName(request.tradeName() != null && !request.tradeName().isBlank() ? request.tradeName().trim() : null);
        customer.setGstin(cleanString(request.gstin()));
        customer.setPan(cleanString(request.pan()));
        customer.setIsRegistered(Boolean.TRUE.equals(request.isRegistered()));
        customer.setBillingAddressLine1(request.billingAddressLine1());
        customer.setBillingAddressLine2(request.billingAddressLine2());
        customer.setBillingCity(request.billingCity());
        customer.setBillingStateCode(request.billingStateCode().trim());
        customer.setBillingPincode(cleanString(request.billingPincode()));
        customer.setShippingAddressLine1(request.shippingAddressLine1());
        customer.setShippingAddressLine2(request.shippingAddressLine2());
        customer.setShippingCity(request.shippingCity());
        customer.setShippingStateCode(cleanString(request.shippingStateCode()));
        customer.setShippingPincode(cleanString(request.shippingPincode()));
        customer.setContactPerson(request.contactPerson());
        customer.setPhone(cleanString(request.phone()));
        customer.setEmail(cleanString(request.email()));
        
        String posState = cleanString(request.defaultPlaceOfSupplyStateCode());
        customer.setDefaultPlaceOfSupplyStateCode(posState != null ? posState : customer.getBillingStateCode());
        customer.setCreditDays(request.creditDays() != null && request.creditDays() >= 0 ? request.creditDays() : 30);
        customer.setNotes(request.notes());
        customer.setIsArchived(false);

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request) {
        UUID orgId = TenantContext.getRequiredTenant();

        Customer customer = customerRepository.findByIdAndOrganisationId(id, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        String trimmedLegalName = request.legalName().trim();
        if (customerRepository.existsByOrganisationIdAndLegalNameIgnoreCaseAndIdNot(orgId, trimmedLegalName, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another customer with legal name '" + trimmedLegalName + "' already exists"
            );
        }

        validateGstinAndState(request.isRegistered(), request.gstin(), request.pan(), request.billingStateCode());

        customer.setLegalName(trimmedLegalName);
        customer.setTradeName(request.tradeName() != null && !request.tradeName().isBlank() ? request.tradeName().trim() : null);
        customer.setGstin(cleanString(request.gstin()));
        customer.setPan(cleanString(request.pan()));
        customer.setIsRegistered(Boolean.TRUE.equals(request.isRegistered()));
        customer.setBillingAddressLine1(request.billingAddressLine1());
        customer.setBillingAddressLine2(request.billingAddressLine2());
        customer.setBillingCity(request.billingCity());
        customer.setBillingStateCode(request.billingStateCode().trim());
        customer.setBillingPincode(cleanString(request.billingPincode()));
        customer.setShippingAddressLine1(request.shippingAddressLine1());
        customer.setShippingAddressLine2(request.shippingAddressLine2());
        customer.setShippingCity(request.shippingCity());
        customer.setShippingStateCode(cleanString(request.shippingStateCode()));
        customer.setShippingPincode(cleanString(request.shippingPincode()));
        customer.setContactPerson(request.contactPerson());
        customer.setPhone(cleanString(request.phone()));
        customer.setEmail(cleanString(request.email()));
        
        String posState = cleanString(request.defaultPlaceOfSupplyStateCode());
        customer.setDefaultPlaceOfSupplyStateCode(posState != null ? posState : customer.getBillingStateCode());
        if (request.creditDays() != null && request.creditDays() >= 0) {
            customer.setCreditDays(request.creditDays());
        }
        customer.setNotes(request.notes());

        Customer updated = customerRepository.save(customer);
        return CustomerResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        UUID orgId = TenantContext.getRequiredTenant();
        Customer customer = customerRepository.findByIdAndOrganisationId(id, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public CustomerResponse archiveCustomer(UUID id) {
        UUID orgId = TenantContext.getRequiredTenant();
        Customer customer = customerRepository.findByIdAndOrganisationId(id, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        customer.setIsArchived(true);
        return CustomerResponse.fromEntity(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse unarchiveCustomer(UUID id) {
        UUID orgId = TenantContext.getRequiredTenant();
        Customer customer = customerRepository.findByIdAndOrganisationId(id, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        customer.setIsArchived(false);
        return CustomerResponse.fromEntity(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(String query, Boolean isArchived, Pageable pageable) {
        UUID orgId = TenantContext.getRequiredTenant();

        Specification<Customer> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), orgId));

            if (isArchived != null) {
                predicates.add(cb.equal(root.get("isArchived"), isArchived));
            } else {
                predicates.add(cb.equal(root.get("isArchived"), false));
            }

            if (query != null && !query.trim().isEmpty()) {
                String searchPattern = "%" + query.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("legalName")), searchPattern),
                        cb.like(cb.lower(root.get("tradeName")), searchPattern),
                        cb.like(cb.lower(root.get("gstin")), searchPattern),
                        cb.like(cb.lower(root.get("phone")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return customerRepository.findAll(spec, pageable).map(CustomerResponse::fromEntity);
    }

    private void validateGstinAndState(Boolean isRegistered, String rawGstin, String rawPan, String billingStateCode) {
        String gstin = cleanString(rawGstin);
        String pan = cleanString(rawPan);
        boolean registered = Boolean.TRUE.equals(isRegistered);

        if (registered) {
            if (gstin == null || gstin.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "GSTIN is required when customer is marked as registered under GST"
                );
            }
        } else {
            if (pan == null || pan.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "PAN is mandatory for customers not registered under GST"
                );
            }
            if (!pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid PAN format (expected 10 characters: 5 letters, 4 digits, 1 letter)"
                );
            }
        }

        if (gstin != null && !gstin.isEmpty()) {
            if (gstin.length() != 15) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GSTIN must be exactly 15 characters long");
            }
            String gstinState = gstin.substring(0, 2);
            if (!gstinState.equals(billingStateCode)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Billing state code (" + billingStateCode + ") must match first 2 digits of GSTIN (" + gstinState + ")"
                );
            }
        }
    }

    private String cleanString(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
