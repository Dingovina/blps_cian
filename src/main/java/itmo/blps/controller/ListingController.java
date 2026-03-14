package itmo.blps.controller;

import itmo.blps.dto.*;
import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.entity.User;
import itmo.blps.service.ListingService;
import itmo.blps.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ListingController {

    private final ListingService listingService;
    private final PaymentService paymentService;

    public ListingController(ListingService listingService, PaymentService paymentService) {
        this.listingService = listingService;
        this.paymentService = paymentService;
    }

    @PostMapping("/listings")
    public ResponseEntity<ListingResponse> create(@Valid @RequestBody ListingCreateRequest request,
                                                   @AuthenticationPrincipal User user) {
        Listing l = listingService.create(user, request);
        return ResponseEntity.status(201).body(ListingResponse.from(l));
    }

    @PutMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ListingCreateRequest request,
                                                  @AuthenticationPrincipal User user) {
        Listing l = listingService.update(id, user, request);
        return ResponseEntity.ok(ListingResponse.from(l));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> get(@PathVariable Long id) {
        Listing l = listingService.getById(id);
        return ResponseEntity.ok(ListingResponse.from(l));
    }

    @GetMapping("/seller/listings")
    public ResponseEntity<PageResponse<ListingResponse>> myListings(
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = listingService.findBySeller(user.getId(), status, pageable);
        List<ListingResponse> content = p.getContent().stream().map(ListingResponse::from).collect(Collectors.toList());
        PageResponse<ListingResponse> resp = new PageResponse<>(
                content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/listings/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id,
                                     @RequestBody(required = false) PublishRequest request,
                                     @AuthenticationPrincipal User user) {
        boolean forceReject = request != null && request.isForceReject();
        Listing l = listingService.publish(id, user, forceReject);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("id", l.getId());
        body.put("status", l.getStatus().name());
        body.put("publishedAt", l.getPublishedAt());
        body.put("message", "Объявление размещено. Доступно платное продвижение (Топ/Премиум).");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/listings/{id}/promotion/choice")
    public ResponseEntity<?> promotionChoice(@PathVariable Long id,
                                             @RequestBody java.util.Map<String, Boolean> body,
                                             @AuthenticationPrincipal User user) {
        listingService.getListingOwnedBy(id, user);
        boolean withPromotion = body != null && Boolean.TRUE.equals(body.get("withPromotion"));
        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("listingId", id);
        resp.put("withPromotion", withPromotion);
        resp.put("nextStep", withPromotion ? "pay" : "none");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/listings/{id}/confirm-relevance")
    public ResponseEntity<?> confirmRelevance(@PathVariable Long id,
                                             @RequestBody(required = false) ConfirmRelevanceRequest request,
                                             @AuthenticationPrincipal User user) {
        boolean relevant = request == null || request.isRelevant();
        Listing l = listingService.confirmRelevance(id, user, relevant);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("listingId", l.getId());
        body.put("action", relevant ? "EXTENDED" : "ARCHIVED");
        body.put("expiresAt", l.getExpiresAt());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/listings/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Listing l = listingService.close(id, user);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("listingId", l.getId());
        body.put("status", l.getStatus().name());
        body.put("closedAt", l.getClosedAt());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/listings/{id}/promotion/pay")
    public ResponseEntity<?> promotionPay(@PathVariable Long id,
                                         @Valid @RequestBody PromotionPayRequest request,
                                         @AuthenticationPrincipal User user) {
        var result = paymentService.pay(id, user, request.getPromotionType());
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("paymentId", result.paymentId());
        body.put("status", result.status().name());
        body.put("message", result.message());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/listings/search")
    public ResponseEntity<PageResponse<ListingResponse>> search(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer rooms,
            @RequestParam(required = false) BigDecimal minAreaSqm,
            @RequestParam(required = false) BigDecimal maxAreaSqm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("promotion"), Sort.Order.desc("publishedAt")));
        var p = listingService.search(region, minPrice, maxPrice, rooms, minAreaSqm, maxAreaSqm, pageable);
        List<ListingResponse> content = p.getContent().stream().map(ListingResponse::from).collect(Collectors.toList());
        PageResponse<ListingResponse> resp = new PageResponse<>(
                content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }
}
