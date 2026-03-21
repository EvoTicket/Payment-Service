package com.capstone.paymentservice.controller.payos;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.response.PaymentTransactionResponse;
import com.capstone.paymentservice.entity.PaymentTransaction;
import com.capstone.paymentservice.service.PayOSService;
import com.capstone.paymentservice.service.PaymentTransactionService;
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
    private final PaymentTransactionService paymentTransactionService;

    @PostMapping(path = "/create")
    public ResponseEntity<BaseResponse<CreatePaymentLinkResponse>> createPaymentLink(
            @RequestParam String orderCode
    ) {
        CreatePaymentLinkResponse response = payOSService.createPaymentLink(orderCode);
        return ResponseEntity.ok(BaseResponse.created("Tạo thanh toán", response));
    }

    @GetMapping(path = "/{orderCode}")
    public ResponseEntity<BaseResponse<PaymentLink>> getOrderByCode(
            @PathVariable String orderCode
    ) {
        PaymentLink order = payOS.paymentRequests().get(Long.parseLong(orderCode));
        return ResponseEntity.ok(BaseResponse.ok("Lấy thanh toán thành công", order));
    }

    @PutMapping(path = "/{orderCode}")
    public ResponseEntity<BaseResponse<PaymentLink>> cancelOrder(
            @PathVariable String orderCode
    ) {
        PaymentLink order = payOS.paymentRequests().cancel(Long.parseLong(orderCode), "change my mind");
        return ResponseEntity.ok(BaseResponse.ok("Hủy thanh toán thành công", order));
    }

    @PostMapping(path = "/confirm-webhook")
    public ResponseEntity<BaseResponse<ConfirmWebhookResponse>> confirmWebhook(
            @RequestBody Map<String, String> requestBody
    ) {
        ConfirmWebhookResponse result = payOS.webhooks().confirm(requestBody.get("webhookUrl"));
        return ResponseEntity.ok(BaseResponse.ok("Ok", result));
    }

    @GetMapping(path = "/{orderCode}/invoices")
    public ResponseEntity<BaseResponse<InvoicesInfo>> retrieveInvoices(
            @PathVariable String orderCode
    ) {
        InvoicesInfo invoicesInfo = payOS.paymentRequests().invoices().get(Long.parseLong(orderCode));
        return ResponseEntity.ok(BaseResponse.ok("Ok", invoicesInfo));
    }

    @GetMapping(path = "/{orderCode}/invoices/{invoiceId}/download")
    public ResponseEntity<BaseResponse<ByteArrayResource>> downloadInvoice(
            @PathVariable String orderCode,
            @PathVariable String invoiceId
    ) {
        FileDownloadResponse invoiceFile =
                payOS.paymentRequests().invoices().download(invoiceId, Long.parseLong(orderCode));

        if (invoiceFile == null || invoiceFile.getData() == null) {
            return ResponseEntity.status(404).body(BaseResponse.badRequest("invoice not found or empty"));
        }

        ByteArrayResource resource = new ByteArrayResource(invoiceFile.getData());

        HttpHeaders headers = buildInvoiceHeaders(invoiceFile);

        return ResponseEntity.ok().headers(headers)
                .body(BaseResponse.ok("ok", resource));
    }

    @GetMapping(path = "/status")
    public ResponseEntity<BaseResponse<PaymentTransactionResponse>> getPaymentStatus(
            @RequestParam String orderCode
    ) {
        PaymentTransaction transaction = paymentTransactionService.getByOrderCode(orderCode);
        PaymentTransactionResponse response = PaymentTransactionResponse.fromEntity(transaction);
        return ResponseEntity.ok(BaseResponse.ok("Lấy trạng thái thanh toán thành công", response));
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
