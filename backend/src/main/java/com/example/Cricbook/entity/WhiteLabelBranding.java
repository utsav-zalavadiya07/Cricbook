package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "white_label_brandings", uniqueConstraints = {
        @UniqueConstraint(name = "unique_branding_tenant", columnNames = {"tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhiteLabelBranding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "primary_color")
    private String primaryColor; // e.g. HSL or HEX codes

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "font_family")
    private String fontFamily;

    @Builder.Default
    @Column(nullable = false)
    private Boolean published = false;
}
