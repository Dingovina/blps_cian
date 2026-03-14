package itmo.blps.controller;

import itmo.blps.dto.*;
import itmo.blps.entity.Inquiry;
import itmo.blps.entity.InquiryStatus;
import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;
import itmo.blps.exception.ForbiddenException;
import itmo.blps.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping("/inquiries")
    public ResponseEntity<InquiryResponse> create(@Valid @RequestBody InquiryCreateRequest request,
                                                  @AuthenticationPrincipal User user) {
        Inquiry i = inquiryService.create(user, request.getListingId(), request.getMessage());
        return ResponseEntity.status(201).body(InquiryResponse.from(i));
    }

    @GetMapping("/inquiries/{id}")
    public ResponseEntity<InquiryResponse> get(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Inquiry i = inquiryService.getById(id);
        if (user.getRole() == UserRole.ADMIN
                || i.getBuyer().getId().equals(user.getId())
                || i.getListing().getSeller().getId().equals(user.getId())) {
            return ResponseEntity.ok(InquiryResponse.from(i));
        }
        throw new ForbiddenException("Not your inquiry");
    }

    @GetMapping("/inquiries")
    public ResponseEntity<PageResponse<InquiryResponse>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Inquiry> p;
        if (listingId != null) {
            if (UserRole.SELLER.equals(user.getRole()) || "SELLER".equalsIgnoreCase(role)) {
                p = inquiryService.findByListingIdAndSeller(listingId, user.getId(), status, pageable);
            } else {
                p = inquiryService.findByListingIdAndBuyer(listingId, user.getId(), status, pageable);
            }
        } else if (UserRole.SELLER.equals(user.getRole()) || "SELLER".equalsIgnoreCase(role)) {
            p = inquiryService.findBySeller(user.getId(), status, pageable);
        } else {
            p = inquiryService.findByBuyer(user.getId(), status, pageable);
        }
        List<InquiryResponse> content = p.getContent().stream().map(InquiryResponse::from).collect(Collectors.toList());
        PageResponse<InquiryResponse> resp = new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/inquiries/{id}/confirm-showing")
    public ResponseEntity<?> confirmShowing(@PathVariable Long id,
                                            @RequestBody(required = false) ConfirmShowingRequest request,
                                            @AuthenticationPrincipal User user) {
        Inquiry i = inquiryService.confirmShowing(id, user,
                request != null ? request.getScheduledAt() : null,
                request != null ? request.getContactInfo() : null);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("inquiryId", i.getId());
        body.put("status", i.getStatus().name());
        body.put("scheduledAt", i.getScheduledAt());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/inquiries/{id}/reject-showing")
    public ResponseEntity<?> rejectShowing(@PathVariable Long id,
                                          @RequestBody(required = false) RejectShowingRequest request,
                                          @AuthenticationPrincipal User user) {
        Inquiry i = inquiryService.rejectShowing(id, user, request != null ? request.getReason() : null);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("inquiryId", i.getId());
        body.put("status", i.getStatus().name());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/inquiries/{id}/visit-result")
    public ResponseEntity<?> visitResult(@PathVariable Long id,
                                         @RequestBody(required = false) VisitResultRequest request,
                                         @AuthenticationPrincipal User user) {
        Boolean willBuy = request != null ? request.getWillBuy() : null;
        Inquiry i = inquiryService.visitResult(id, user, willBuy);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("inquiryId", i.getId());
        body.put("willBuy", Boolean.TRUE.equals(i.getWillBuy()));
        body.put("status", i.getStatus().name());
        return ResponseEntity.ok(body);
    }
}
