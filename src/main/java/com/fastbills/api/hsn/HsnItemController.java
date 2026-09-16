package com.fastbills.api.hsn;

import com.fastbills.api.auth.AuthenticatedUser;
import com.fastbills.api.hsn.dto.CreateHsnItemRequest;
import com.fastbills.api.hsn.dto.HsnItemResponse;
import com.fastbills.api.hsn.dto.HsnLookupResponse;
import com.fastbills.api.hsn.dto.UpdateHsnItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hsn")
@Tag(name = "HSN Management", description = "Endpoints for managing HSN/SAC master directory and GST slab linking")
public class HsnItemController {

    private final HsnItemService hsnItemService;

    public HsnItemController(HsnItemService hsnItemService) {
        this.hsnItemService = hsnItemService;
    }

    @GetMapping
    @Operation(summary = "List active HSN items", description = "Returns all active HSN/SAC items configured for the authenticated organisation")
    public ResponseEntity<List<HsnItemResponse>> listActive(@AuthenticationPrincipal AuthenticatedUser user) {
        List<HsnItemResponse> items = hsnItemService.listActive(user.organisationId());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/all")
    @Operation(summary = "List all HSN items", description = "Returns all HSN/SAC items (active and inactive) for management")
    public ResponseEntity<List<HsnItemResponse>> listAll(@AuthenticationPrincipal AuthenticatedUser user) {
        List<HsnItemResponse> items = hsnItemService.listAll(user.organisationId());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/default")
    @Operation(summary = "Get default HSN item", description = "Returns the active default HSN item for the organisation if configured")
    public ResponseEntity<HsnItemResponse> getDefault(@AuthenticationPrincipal AuthenticatedUser user) {
        HsnItemResponse item = hsnItemService.getDefault(user.organisationId());
        return ResponseEntity.ok(item);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get HSN item by ID")
    public ResponseEntity<HsnItemResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(hsnItemService.getById(user.organisationId(), id));
    }

    @PostMapping
    @Operation(summary = "Register new HSN/SAC code", description = "Adds a validated HSN/SAC code with its fixed GST tax rate slab to the directory")
    public ResponseEntity<HsnItemResponse> create(
            @Valid @RequestBody CreateHsnItemRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        HsnItemResponse created = hsnItemService.create(user.organisationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update HSN/SAC item")
    public ResponseEntity<HsnItemResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHsnItemRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(hsnItemService.update(user.organisationId(), id, request));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set HSN item as default")
    public ResponseEntity<HsnItemResponse> setDefault(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(hsnItemService.setDefault(user.organisationId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete HSN item")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        hsnItemService.delete(user.organisationId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate/{code}")
    @Operation(summary = "Validate and lookup HSN code from GST master", description = "Retrieves official GST goods/services classification, description and standard GST rate slab")
    public ResponseEntity<HsnLookupResponse> validateCode(@PathVariable String code) {
        return ResponseEntity.ok(hsnItemService.validate(code));
    }
}
