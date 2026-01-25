package com.capstone.paymentservice.controller.payos;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.core.FileDownloadResponse;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.invoices.InvoicesInfo;
import vn.payos.model.webhooks.ConfirmWebhookResponse;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PayOSController {
    private final PayOS payOS;
    private final PayOSService payOSService;

    @PostMapping(path = "/create")
    public ResponseEntity<BaseResponse<CreatePaymentLinkResponse>> createPaymentLink(
            @RequestParam Long orderId
    ) {
        CreatePaymentLinkResponse response = payOSService.createPaymentLink(orderId);
        return ResponseEntity.ok(BaseResponse.created("Tạo thanh toán", response));
    }

    @GetMapping(path = "/{orderId}")
    public ResponseEntity<BaseResponse<PaymentLink>> getOrderById(
            @PathVariable("orderId") long orderId
    ) {
        PaymentLink order = payOS.paymentRequests().get(orderId);
        return ResponseEntity.ok(BaseResponse.ok("Lấy thanh toán thành công", order));
    }

    @PutMapping(path = "/{orderId}")
    public ResponseEntity<BaseResponse<PaymentLink>> cancelOrder(
            @PathVariable("orderId") long orderId
    ) {
        PaymentLink order = payOS.paymentRequests().cancel(orderId, "change my mind");
        return ResponseEntity.ok(BaseResponse.ok("Lấy thanh toán thành công", order));
    }

    @PostMapping(path = "/confirm-webhook")
    public ResponseEntity<BaseResponse<ConfirmWebhookResponse>> confirmWebhook(
            @RequestBody Map<String, String> requestBody
    ) {
        ConfirmWebhookResponse result = payOS.webhooks().confirm(requestBody.get("webhookUrl"));
        return ResponseEntity.ok(BaseResponse.ok("Ok", result));
    }

    @GetMapping(path = "/{orderId}/invoices")
    public ResponseEntity<BaseResponse<InvoicesInfo>> retrieveInvoices(@PathVariable("orderId") long orderId) {
        InvoicesInfo invoicesInfo = payOS.paymentRequests().invoices().get(orderId);
        return ResponseEntity.ok(BaseResponse.ok("Ok", invoicesInfo));
    }

    @GetMapping(path = "/{orderId}/invoices/{invoiceId}/download")
    public ResponseEntity<BaseResponse<ByteArrayResource>> downloadInvoice(
            @PathVariable("orderId") long orderId,
            @PathVariable("invoiceId") String invoiceId
    ) {
        FileDownloadResponse invoiceFile =
                payOS.paymentRequests().invoices().download(invoiceId, orderId);

        if (invoiceFile == null || invoiceFile.getData() == null) {
            return ResponseEntity.status(404).body(BaseResponse.badRequest("invoice not found or empty"));
        }

        ByteArrayResource resource = new ByteArrayResource(invoiceFile.getData());

        HttpHeaders headers = buildInvoiceHeaders(invoiceFile);

        return ResponseEntity.ok().headers(headers)
                .body(BaseResponse.ok("ok", resource));
    }

    private HttpHeaders buildInvoiceHeaders(FileDownloadResponse invoiceFile) {
        HttpHeaders headers = new HttpHeaders();

        String contentType = invoiceFile.getContentType() == null
                ? MediaType.APPLICATION_PDF_VALUE
                : invoiceFile.getContentType();

        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + invoiceFile.getFilename() + "\""
        );
        if (invoiceFile.getSize() != null) {
            headers.setContentLength(invoiceFile.getSize());
        }

        return headers;
    }
}
