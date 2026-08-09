package com.dch.training.novabank.controller;

import com.dch.training.novabank.dto.TransferRequest;
import com.dch.training.novabank.service.TransferProducerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private static final Logger log = LoggerFactory.getLogger(TransferController.class);

    private final TransferProducerService transferProducerService;

    public TransferController(TransferProducerService transferProducerService) {
        this.transferProducerService = transferProducerService;
    }

    @PostMapping(version = "1")
    public ResponseEntity<Map<String, String>> requestTransfer(@Valid @RequestBody TransferRequest request) {
        log.info("Received transfer request from account {}", request.sourceAccountId());
        String transferId = transferProducerService.publishTransferRequested(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("transferId", transferId, "status", "ACCEPTED"));
    }

}
