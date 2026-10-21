package com.example.Cricbook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cricbook.invoice")
@Getter
@Setter
public class InvoiceDocumentProperties {

    private String supplierName = "Cricbook";
    private String supplierAddress = "India";
    private String supplierGstin = "";
    private String supplierEmail = "support@cricbook.com";
    private String serviceAccountingCode = "";
    private String placeOfSupply = "India";
}
