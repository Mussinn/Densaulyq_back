package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для создания платежной ссылки CloudPayments
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudPaymentsLinkRequest {
    private String amount;
    private String currency;
    private String invoiceId;
    private String description;
    private String accountId; // patient email or ID
    private String email;
}
