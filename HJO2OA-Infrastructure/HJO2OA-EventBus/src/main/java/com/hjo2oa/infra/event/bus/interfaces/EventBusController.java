package com.hjo2oa.infra.event.bus.interfaces;

import com.hjo2oa.infra.event.bus.application.EventBusManagementApplicationService;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.OperatorContext;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.EventDetailView;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.EventStatisticsView;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.EventSummaryView;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.ReplayResultView;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxPage;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxQuery;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxStatus;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.PageData;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/event-bus")
public class EventBusController {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String IDEMPOTENCY_HEADER_STANDARD = "Idempotency-Key";
    private static final String OPERATOR_ACCOUNT_HEADER = "X-Operator-Account-Id";
    private static final String OPERATOR_PERSON_HEADER = "X-Operator-Person-Id";

    private final EventBusManagementApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public EventBusController(
            EventBusManagementApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/events")
    public ApiResponse<PageData<EventSummaryView>> listEvents(
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        EventOutboxQuery query = new EventOutboxQuery(
                eventId,
                eventType,
                aggregateType,
                aggregateId,
                tenantId,
                traceId,
                EventBusDtos.parseStatus(status),
                occurredFrom,
                occurredTo,
                page,
                size
        );
        EventOutboxPage result = applicationService.listEvents(query);
        return ApiResponse.page(
                result.items().stream().map(applicationService::toSummary).toList(),
                Pagination.of(query.page(), query.size(), result.total()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/events/failed")
    public ApiResponse<PageData<EventSummaryView>> listFailedEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return listByStatus(EventOutboxStatus.FAILED, eventType, tenantId, page, size, request);
    }

    @GetMapping("/dead-letters")
    public ApiResponse<PageData<EventSummaryView>> listDeadLetters(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return listByStatus(EventOutboxStatus.DEAD, eventType, tenantId, page, size, request);
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<EventDetailView> detail(
            @PathVariable UUID eventId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.detail(eventId), responseMetaFactory.create(request));
    }

    @PostMapping("/events/{eventId}/retry")
    public ApiResponse<EventDetailView> retry(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventBusDtos.EventOperationRequest body,
            @RequestHeader(value = OPERATOR_ACCOUNT_HEADER, required = false) UUID operatorAccountId,
            @RequestHeader(value = OPERATOR_PERSON_HEADER, required = false) UUID operatorPersonId,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = IDEMPOTENCY_HEADER_STANDARD, required = false) String standardIdempotencyKey,
            HttpServletRequest request
    ) {
        var meta = responseMetaFactory.create(request);
        return ApiResponse.success(
                applicationService.retry(body.toCommand(eventId, operatorContext(
                        operatorAccountId,
                        operatorPersonId,
                        meta.requestId(),
                        firstText(idempotencyKey, standardIdempotencyKey)
                ))),
                meta
        );
    }

    @PostMapping("/events/{eventId}/dead-letter")
    public ApiResponse<EventDetailView> deadLetter(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventBusDtos.EventOperationRequest body,
            @RequestHeader(value = OPERATOR_ACCOUNT_HEADER, required = false) UUID operatorAccountId,
            @RequestHeader(value = OPERATOR_PERSON_HEADER, required = false) UUID operatorPersonId,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = IDEMPOTENCY_HEADER_STANDARD, required = false) String standardIdempotencyKey,
            HttpServletRequest request
    ) {
        var meta = responseMetaFactory.create(request);
        return ApiResponse.success(
                applicationService.deadLetter(body.toCommand(eventId, operatorContext(
                        operatorAccountId,
                        operatorPersonId,
                        meta.requestId(),
                        firstText(idempotencyKey, standardIdempotencyKey)
                ))),
                meta
        );
    }

    @PostMapping("/replay")
    public ApiResponse<ReplayResultView> replay(
            @Valid @RequestBody EventBusDtos.ReplayRequest body,
            @RequestHeader(value = OPERATOR_ACCOUNT_HEADER, required = false) UUID operatorAccountId,
            @RequestHeader(value = OPERATOR_PERSON_HEADER, required = false) UUID operatorPersonId,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = IDEMPOTENCY_HEADER_STANDARD, required = false) String standardIdempotencyKey,
            HttpServletRequest request
    ) {
        var meta = responseMetaFactory.create(request);
        return ApiResponse.success(
                applicationService.replay(body.toCommand(operatorContext(
                        operatorAccountId,
                        operatorPersonId,
                        meta.requestId(),
                        firstText(idempotencyKey, standardIdempotencyKey)
                ))),
                meta
        );
    }

    @GetMapping("/statistics")
    public ApiResponse<EventStatisticsView> statistics(HttpServletRequest request) {
        return ApiResponse.success(applicationService.statistics(), responseMetaFactory.create(request));
    }

    private ApiResponse<PageData<EventSummaryView>> listByStatus(
            EventOutboxStatus status,
            String eventType,
            String tenantId,
            int page,
            int size,
            HttpServletRequest request
    ) {
        EventOutboxQuery query = new EventOutboxQuery(
                null,
                eventType,
                null,
                null,
                tenantId,
                null,
                status,
                null,
                null,
                page,
                size
        );
        EventOutboxPage result = applicationService.listEvents(query);
        return ApiResponse.page(
                result.items().stream().map(applicationService::toSummary).toList(),
                Pagination.of(query.page(), query.size(), result.total()),
                responseMetaFactory.create(request)
        );
    }

    private OperatorContext operatorContext(
            UUID operatorAccountId,
            UUID operatorPersonId,
            String requestId,
            String idempotencyKey
    ) {
        return new OperatorContext(operatorAccountId, operatorPersonId, requestId, idempotencyKey);
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
