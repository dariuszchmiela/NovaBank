package com.dch.training.novabank.repository;

import com.dch.training.novabank.entity.ProcessedTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedTransferRepository extends JpaRepository<ProcessedTransfer, UUID> {
}