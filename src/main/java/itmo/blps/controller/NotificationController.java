package itmo.blps.controller;

import itmo.blps.dto.NotificationResponse;
import itmo.blps.dto.PageResponse;
import itmo.blps.entity.Notification;
import itmo.blps.entity.User;
import itmo.blps.service.NotificationService;
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
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = notificationService.findByUserId(user.getId(), unreadOnly, pageable);
        List<NotificationResponse> content = p.getContent().stream().map(NotificationResponse::from).collect(Collectors.toList());
        PageResponse<NotificationResponse> resp = new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id,
                                                           @AuthenticationPrincipal User user) {
        Notification n = notificationService.markRead(id, user.getId());
        return ResponseEntity.ok(NotificationResponse.from(n));
    }
}
