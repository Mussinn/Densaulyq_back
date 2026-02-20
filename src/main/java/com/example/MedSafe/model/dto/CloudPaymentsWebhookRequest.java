package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для webhook от CloudPayments
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudPaymentsWebhookRequest {
    private String TransactionId;
    private String Amount;
    private String Currency;
    private String DateTime;
    private String CardFirstSix;
    private String CardLastFour;
    private String CardType;
    private String Status;
    private String InvoiceId;
    private String Description;
    private String Email;
}
