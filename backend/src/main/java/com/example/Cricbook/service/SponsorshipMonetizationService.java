package com.example.Cricbook.service;

import com.example.Cricbook.dto.SponsorshipRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SponsorshipMonetizationService {

    private static final Set<String> PAID_INVOICE_STATUSES = Set.of("PAID", "SETTLED", "COLLECTED");

    private final BrandPartnerRepository brandPartnerRepository;
    private final SponsorshipOpportunityRepository opportunityRepository;
    private final SponsorshipProposalRepository proposalRepository;
    private final SponsoredCampaignRepository campaignRepository;
    private final SponsoredAdEventRepository adEventRepository;
    private final SponsorLeadRepository leadRepository;
    private final SponsorshipContractRepository contractRepository;
    private final SponsorInvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<BrandPartner> brands = brandPartnerRepository.findAll();
        List<SponsorshipOpportunity> opportunities = opportunityRepository.findAll();
        List<SponsorshipProposal> proposals = proposalRepository.findAll();
        List<SponsoredCampaign> campaigns = campaignRepository.findAll();
        List<SponsorLead> leads = leadRepository.findAll();
        List<SponsorshipContract> contracts = contractRepository.findAll();
        List<SponsorInvoice> invoices = invoiceRepository.findAll();
        List<SponsoredAdEvent> events = adEventRepository.findAll();

        long impressions = countEvents(events, "IMPRESSION");
        long clicks = countEvents(events, "CLICK");
        long conversions = countEvents(events, "CONVERSION");
        BigDecimal paidRevenue = invoices.stream()
                .filter(invoice -> PAID_INVOICE_STATUSES.contains(normalize(invoice.getStatus(), "PENDING")))
                .map(SponsorInvoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal adSpend = campaigns.stream()
                .map(SponsoredCampaign::getSpend)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pipeline = proposals.stream()
                .filter(proposal -> List.of("SUBMITTED", "COUNTERED", "ACCEPTED").contains(normalize(proposal.getStatus(), "SUBMITTED")))
                .map(SponsorshipProposal::getProposedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(brands, opportunities, proposals, campaigns, leads, contracts, invoices, impressions, clicks, conversions, paidRevenue, pipeline, adSpend));
        response.put("revenueBreakdown", revenueBreakdown(invoices));
        response.put("marketplace", opportunities.stream().sorted(Comparator.comparing(SponsorshipOpportunity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapOpportunity).toList());
        response.put("brandPipeline", brands.stream().sorted(Comparator.comparing(BrandPartner::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapBrand).toList());
        response.put("proposalBoard", proposals.stream().sorted(Comparator.comparing(SponsorshipProposal::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapProposal).toList());
        response.put("campaignPerformance", campaigns.stream().sorted(Comparator.comparing(SponsoredCampaign::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(campaign -> mapCampaign(campaign, events)).toList());
        response.put("leadFunnel", leadFunnel(leads));
        response.put("contracts", contracts.stream().sorted(Comparator.comparing(SponsorshipContract::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapContract).toList());
        response.put("invoices", invoices.stream().sorted(Comparator.comparing(SponsorInvoice::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapInvoice).toList());
        response.put("priorityActions", priorityActions(brands, opportunities, proposals, campaigns, leads, contracts, invoices));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> marketplace(String city, String sportCategory, BigDecimal maxAmount) {
        String normalizedCity = normalizeNullable(city);
        String normalizedSport = normalizeNullable(sportCategory);
        return opportunityRepository.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                .filter(opportunity -> normalizedCity == null || normalize(opportunity.getCity(), "").contains(normalizedCity))
                .filter(opportunity -> normalizedSport == null || normalize(opportunity.getSportCategory(), "").contains(normalizedSport))
                .filter(opportunity -> maxAmount == null || safeAmount(opportunity.getAmount()).compareTo(maxAmount) <= 0)
                .map(this::mapOpportunity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> brands() {
        return brandPartnerRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapBrand).toList();
    }

    @Transactional
    public Map<String, Object> registerBrand(SponsorshipRequests.BrandRequest request, String actor) {
        validateBrand(request);
        String email = required(request.getEmail(), "Brand email is required").toLowerCase(Locale.ROOT);
        if (brandPartnerRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Brand email already exists");
        }
        BrandPartner brand = BrandPartner.builder()
                .companyName(required(request.getCompanyName(), "Company name is required"))
                .brandType(normalize(request.getBrandType(), "LOCAL_BUSINESS"))
                .gstNumber(blankToNull(request.getGstNumber()))
                .website(blankToNull(request.getWebsite()))
                .email(email)
                .phone(blankToNull(request.getPhone()))
                .city(blankToNull(request.getCity()))
                .logoUrl(blankToNull(request.getLogoUrl()))
                .documentUrl(blankToNull(request.getDocumentUrl()))
                .status("PENDING_VERIFICATION")
                .build();
        BrandPartner saved = brandPartnerRepository.save(brand);
        audit(actor, "BRAND_REGISTERED", "BrandPartner #" + saved.getId());
        return mapBrand(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> opportunities() {
        return opportunityRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapOpportunity).toList();
    }

    @Transactional
    public Map<String, Object> createOpportunity(SponsorshipRequests.OpportunityRequest request, String actor) {
        SponsorshipOpportunity opportunity = SponsorshipOpportunity.builder()
                .title(required(request.getTitle(), "Opportunity title is required"))
                .opportunityType(normalize(request.getOpportunityType(), "TOURNAMENT"))
                .description(blankToNull(request.getDescription()))
                .city(blankToNull(request.getCity()))
                .sportCategory(normalize(request.getSportCategory(), "CRICKET"))
                .audienceSize(request.getAudienceSize() == null ? 0 : Math.max(0, request.getAudienceSize()))
                .amount(nonNegative(request.getAmount()))
                .benefits(blankToNull(request.getBenefits()))
                .ownerReference(blankToNull(request.getOwnerReference()))
                .status("OPEN")
                .expiresAt(request.getExpiresAt())
                .build();
        SponsorshipOpportunity saved = opportunityRepository.save(opportunity);
        audit(actor, "SPONSORSHIP_OPPORTUNITY_CREATED", "SponsorshipOpportunity #" + saved.getId());
        return mapOpportunity(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> proposals() {
        return proposalRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapProposal).toList();
    }

    @Transactional
    public Map<String, Object> submitProposal(Long opportunityId, SponsorshipRequests.ProposalRequest request, String actor) {
        BrandPartner brand = brandPartnerRepository.findById(requiredId(request.getBrandId(), "Brand is required"))
                .orElseThrow(() -> ApiException.notFound("Brand not found"));
        SponsorshipOpportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> ApiException.notFound("Sponsorship opportunity not found"));
        if (!"OPEN".equalsIgnoreCase(opportunity.getStatus())) {
            throw ApiException.conflict("Sponsorship opportunity is not open");
        }
        SponsorshipProposal proposal = SponsorshipProposal.builder()
                .brand(brand)
                .opportunity(opportunity)
                .message(blankToNull(request.getMessage()))
                .proposedAmount(nonNegative(request.getProposedAmount()))
                .deliverables(blankToNull(request.getDeliverables()))
                .documentUrl(blankToNull(request.getDocumentUrl()))
                .status("SUBMITTED")
                .build();
        SponsorshipProposal saved = proposalRepository.save(proposal);
        audit(actor, "SPONSORSHIP_PROPOSAL_SUBMITTED", "SponsorshipProposal #" + saved.getId());
        return mapProposal(saved);
    }

    @Transactional
    public Map<String, Object> updateProposalStatus(Long proposalId, SponsorshipRequests.StatusRequest request, String actor) {
        SponsorshipProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> ApiException.notFound("Sponsorship proposal not found"));
        String status = normalize(request.getStatus(), "SUBMITTED");
        if (!Set.of("SUBMITTED", "ACCEPTED", "REJECTED", "COUNTERED", "CONTRACTED").contains(status)) {
            throw ApiException.badRequest("Unsupported proposal status");
        }
        proposal.setStatus(status);
        proposal.setDecisionNotes(blankToNull(request.getNotes()));
        SponsorshipProposal saved = proposalRepository.save(proposal);
        audit(actor, "SPONSORSHIP_PROPOSAL_" + status, "SponsorshipProposal #" + saved.getId());
        return mapProposal(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> campaigns() {
        List<SponsoredAdEvent> events = adEventRepository.findAll();
        return campaignRepository.findAllByOrderByCreatedAtDesc().stream().map(campaign -> mapCampaign(campaign, events)).toList();
    }

    @Transactional
    public Map<String, Object> createCampaign(SponsorshipRequests.CampaignRequest request, String actor) {
        BrandPartner brand = brandPartnerRepository.findById(requiredId(request.getBrandId(), "Brand is required"))
                .orElseThrow(() -> ApiException.notFound("Brand not found"));
        SponsoredCampaign campaign = SponsoredCampaign.builder()
                .brand(brand)
                .name(required(request.getName(), "Campaign name is required"))
                .campaignType(normalize(request.getCampaignType(), "BANNER_AD"))
                .placement(normalize(request.getPlacement(), "HOMEPAGE"))
                .targetAudience(normalize(request.getTargetAudience(), "ALL_USERS"))
                .city(blankToNull(request.getCity()))
                .creativeUrl(blankToNull(request.getCreativeUrl()))
                .destinationUrl(blankToNull(request.getDestinationUrl()))
                .dailyBudget(nonNegative(request.getDailyBudget()))
                .lifetimeBudget(nonNegative(request.getLifetimeBudget()))
                .status("ACTIVE")
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();
        SponsoredCampaign saved = campaignRepository.save(campaign);
        audit(actor, "SPONSORED_CAMPAIGN_CREATED", "SponsoredCampaign #" + saved.getId());
        return mapCampaign(saved, List.of());
    }

    @Transactional
    public Map<String, Object> recordAdEvent(Long campaignId, SponsorshipRequests.EventRequest request, String actor) {
        SponsoredCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> ApiException.notFound("Sponsored campaign not found"));
        String eventType = normalize(request.getEventType(), "IMPRESSION");
        if (!Set.of("IMPRESSION", "CLICK", "CONVERSION").contains(eventType)) {
            throw ApiException.badRequest("Unsupported ad event type");
        }
        BigDecimal cost = nonNegative(request.getCost());
        SponsoredAdEvent event = SponsoredAdEvent.builder()
                .campaign(campaign)
                .eventType(eventType)
                .userReference(blankToNull(request.getUserReference()))
                .sessionId(blankToNull(request.getSessionId()))
                .leadReference(blankToNull(request.getLeadReference()))
                .cost(cost)
                .build();
        campaign.setSpend(safeAmount(campaign.getSpend()).add(cost));
        campaignRepository.save(campaign);
        SponsoredAdEvent saved = adEventRepository.save(event);
        audit(actor, "SPONSORED_AD_EVENT_" + eventType, "SponsoredAdEvent #" + saved.getId());
        return mapCampaign(campaign, adEventRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> leads() {
        return leadRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapLead).toList();
    }

    @Transactional
    public Map<String, Object> createLead(SponsorshipRequests.LeadRequest request, String actor) {
        BrandPartner brand = brandPartnerRepository.findById(requiredId(request.getBrandId(), "Brand is required"))
                .orElseThrow(() -> ApiException.notFound("Brand not found"));
        SponsoredCampaign campaign = null;
        if (request.getCampaignId() != null) {
            campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> ApiException.notFound("Sponsored campaign not found"));
        }
        SponsorLead lead = SponsorLead.builder()
                .brand(brand)
                .campaign(campaign)
                .leadType(normalize(request.getLeadType(), "PLAYER"))
                .name(required(request.getName(), "Lead name is required"))
                .email(blankToNull(request.getEmail()))
                .phone(blankToNull(request.getPhone()))
                .city(blankToNull(request.getCity()))
                .notes(blankToNull(request.getNotes()))
                .estimatedValue(nonNegative(request.getEstimatedValue()))
                .status("NEW")
                .build();
        SponsorLead saved = leadRepository.save(lead);
        audit(actor, "SPONSOR_LEAD_CREATED", "SponsorLead #" + saved.getId());
        return mapLead(saved);
    }

    @Transactional
    public Map<String, Object> updateLeadStatus(Long leadId, SponsorshipRequests.StatusRequest request, String actor) {
        SponsorLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> ApiException.notFound("Sponsor lead not found"));
        String status = normalize(request.getStatus(), "NEW");
        if (!Set.of("NEW", "CONTACTED", "QUALIFIED", "CONVERTED", "LOST").contains(status)) {
            throw ApiException.badRequest("Unsupported lead status");
        }
        lead.setStatus(status);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            lead.setNotes(request.getNotes().trim());
        }
        SponsorLead saved = leadRepository.save(lead);
        audit(actor, "SPONSOR_LEAD_" + status, "SponsorLead #" + saved.getId());
        return mapLead(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> contracts() {
        return contractRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapContract).toList();
    }

    @Transactional
    public Map<String, Object> createContract(SponsorshipRequests.ContractRequest request, String actor) {
        BrandPartner brand = brandPartnerRepository.findById(requiredId(request.getBrandId(), "Brand is required"))
                .orElseThrow(() -> ApiException.notFound("Brand not found"));
        SponsorshipProposal proposal = null;
        if (request.getProposalId() != null) {
            proposal = proposalRepository.findById(request.getProposalId())
                    .orElseThrow(() -> ApiException.notFound("Sponsorship proposal not found"));
        }
        String contractNumber = required(request.getContractNumber(), "Contract number is required").toUpperCase(Locale.ROOT);
        if (contractRepository.existsByContractNumber(contractNumber)) {
            throw ApiException.conflict("Contract number already exists");
        }
        SponsorshipContract contract = SponsorshipContract.builder()
                .brand(brand)
                .proposal(proposal)
                .contractNumber(contractNumber)
                .contractType(normalize(request.getContractType(), "SPONSORSHIP"))
                .value(nonNegative(request.getValue()))
                .documentUrl(blankToNull(request.getDocumentUrl()))
                .status("SIGNED")
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();
        SponsorshipContract saved = contractRepository.save(contract);
        audit(actor, "SPONSORSHIP_CONTRACT_CREATED", "SponsorshipContract #" + saved.getId());
        return mapContract(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> invoices() {
        return invoiceRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapInvoice).toList();
    }

    @Transactional
    public Map<String, Object> createInvoice(SponsorshipRequests.InvoiceRequest request, String actor) {
        BrandPartner brand = brandPartnerRepository.findById(requiredId(request.getBrandId(), "Brand is required"))
                .orElseThrow(() -> ApiException.notFound("Brand not found"));
        SponsorshipContract contract = null;
        if (request.getContractId() != null) {
            contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> ApiException.notFound("Sponsorship contract not found"));
        }
        String invoiceNumber = required(request.getInvoiceNumber(), "Invoice number is required").toUpperCase(Locale.ROOT);
        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            throw ApiException.conflict("Invoice number already exists");
        }
        BigDecimal subtotal = nonNegative(request.getSubtotal());
        BigDecimal gstAmount = request.getGstAmount() == null ? subtotal.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP) : nonNegative(request.getGstAmount());
        SponsorInvoice invoice = SponsorInvoice.builder()
                .brand(brand)
                .contract(contract)
                .invoiceNumber(invoiceNumber)
                .revenueCategory(normalize(request.getRevenueCategory(), "SPONSORSHIP"))
                .subtotal(subtotal)
                .gstAmount(gstAmount)
                .totalAmount(subtotal.add(gstAmount))
                .status(normalize(request.getStatus(), "PENDING"))
                .notes(blankToNull(request.getNotes()))
                .build();
        SponsorInvoice saved = invoiceRepository.save(invoice);
        audit(actor, "SPONSOR_INVOICE_CREATED", "SponsorInvoice #" + saved.getId());
        return mapInvoice(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reports() {
        List<SponsorInvoice> invoices = invoiceRepository.findAll();
        List<SponsoredAdEvent> events = adEventRepository.findAll();
        List<SponsorLead> leads = leadRepository.findAll();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("revenue", revenueBreakdown(invoices));
        response.put("adFunnel", Map.of(
                "impressions", countEvents(events, "IMPRESSION"),
                "clicks", countEvents(events, "CLICK"),
                "conversions", countEvents(events, "CONVERSION"),
                "ctr", percent(countEvents(events, "CLICK"), Math.max(countEvents(events, "IMPRESSION"), 1)),
                "conversionRate", percent(countEvents(events, "CONVERSION"), Math.max(countEvents(events, "CLICK"), 1))
        ));
        response.put("leadFunnel", leadFunnel(leads));
        response.put("brandSafety", Map.of(
                "pendingVerification", brandPartnerRepository.findByStatusOrderByCreatedAtDesc("PENDING_VERIFICATION").size(),
                "pendingProposals", proposalRepository.findByStatusOrderByCreatedAtDesc("SUBMITTED").size(),
                "activeCampaigns", campaignRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").size()
        ));
        return response;
    }

    private Map<String, Object> kpis(List<BrandPartner> brands,
                                     List<SponsorshipOpportunity> opportunities,
                                     List<SponsorshipProposal> proposals,
                                     List<SponsoredCampaign> campaigns,
                                     List<SponsorLead> leads,
                                     List<SponsorshipContract> contracts,
                                     List<SponsorInvoice> invoices,
                                     long impressions,
                                     long clicks,
                                     long conversions,
                                     BigDecimal paidRevenue,
                                     BigDecimal pipeline,
                                     BigDecimal adSpend) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("brands", brands.size());
        kpis.put("verifiedBrands", brands.stream().filter(brand -> "VERIFIED".equalsIgnoreCase(brand.getStatus())).count());
        kpis.put("openOpportunities", opportunities.stream().filter(opportunity -> "OPEN".equalsIgnoreCase(opportunity.getStatus())).count());
        kpis.put("pendingProposals", proposals.stream().filter(proposal -> "SUBMITTED".equalsIgnoreCase(proposal.getStatus())).count());
        kpis.put("activeCampaigns", campaigns.stream().filter(campaign -> "ACTIVE".equalsIgnoreCase(campaign.getStatus())).count());
        kpis.put("leads", leads.size());
        kpis.put("convertedLeads", leads.stream().filter(lead -> "CONVERTED".equalsIgnoreCase(lead.getStatus())).count());
        kpis.put("signedContracts", contracts.stream().filter(contract -> "SIGNED".equalsIgnoreCase(contract.getStatus())).count());
        kpis.put("invoices", invoices.size());
        kpis.put("paidRevenue", paidRevenue);
        kpis.put("sponsorshipPipeline", pipeline);
        kpis.put("adSpend", adSpend);
        kpis.put("impressions", impressions);
        kpis.put("clicks", clicks);
        kpis.put("conversions", conversions);
        kpis.put("ctr", percent(clicks, Math.max(impressions, 1)));
        kpis.put("conversionRate", percent(conversions, Math.max(clicks, 1)));
        kpis.put("roi", adSpend.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : paidRevenue.subtract(adSpend).multiply(BigDecimal.valueOf(100)).divide(adSpend, 2, RoundingMode.HALF_UP));
        return kpis;
    }

    private Map<String, BigDecimal> revenueBreakdown(List<SponsorInvoice> invoices) {
        Map<String, BigDecimal> response = new LinkedHashMap<>();
        for (String category : List.of("SPONSORSHIP", "ADVERTISING", "FEATURED_LISTING", "PROMOTION", "AFFILIATE")) {
            BigDecimal total = invoices.stream()
                    .filter(invoice -> category.equalsIgnoreCase(invoice.getRevenueCategory()))
                    .filter(invoice -> PAID_INVOICE_STATUSES.contains(normalize(invoice.getStatus(), "PENDING")))
                    .map(SponsorInvoice::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.put(category, total);
        }
        return response;
    }

    private Map<String, Long> leadFunnel(List<SponsorLead> leads) {
        Map<String, Long> response = new LinkedHashMap<>();
        for (String status : List.of("NEW", "CONTACTED", "QUALIFIED", "CONVERTED", "LOST")) {
            response.put(status, leads.stream().filter(lead -> status.equalsIgnoreCase(lead.getStatus())).count());
        }
        return response;
    }

    private List<Map<String, Object>> priorityActions(List<BrandPartner> brands,
                                                       List<SponsorshipOpportunity> opportunities,
                                                       List<SponsorshipProposal> proposals,
                                                       List<SponsoredCampaign> campaigns,
                                                       List<SponsorLead> leads,
                                                       List<SponsorshipContract> contracts,
                                                       List<SponsorInvoice> invoices) {
        List<Map<String, Object>> actions = new ArrayList<>();
        long pendingBrands = brands.stream().filter(brand -> "PENDING_VERIFICATION".equalsIgnoreCase(brand.getStatus())).count();
        long pendingProposals = proposals.stream().filter(proposal -> "SUBMITTED".equalsIgnoreCase(proposal.getStatus())).count();
        long unsignedContracts = contracts.stream().filter(contract -> !"SIGNED".equalsIgnoreCase(contract.getStatus())).count();
        long unpaidInvoices = invoices.stream().filter(invoice -> !"PAID".equalsIgnoreCase(invoice.getStatus())).count();
        long activeCampaigns = campaigns.stream().filter(campaign -> "ACTIVE".equalsIgnoreCase(campaign.getStatus())).count();
        if (pendingBrands > 0) {
            actions.add(action("Brand verification queue", pendingBrands + " brand partners need verification before sponsorship activation.", "HIGH"));
        }
        if (pendingProposals > 0) {
            actions.add(action("Proposal decisions pending", pendingProposals + " proposals need accept, reject, or counter actions.", "HIGH"));
        }
        if (unsignedContracts > 0) {
            actions.add(action("Contract lifecycle gap", unsignedContracts + " contracts are not signed yet.", "MEDIUM"));
        }
        if (unpaidInvoices > 0) {
            actions.add(action("Billing follow-up", unpaidInvoices + " sponsor invoices are not paid.", "MEDIUM"));
        }
        if (opportunities.stream().noneMatch(opportunity -> "OPEN".equalsIgnoreCase(opportunity.getStatus()))) {
            actions.add(action("Marketplace supply low", "Create sponsorship opportunities for tournaments, teams, and featured turfs.", "MEDIUM"));
        }
        if (activeCampaigns == 0 && !leads.isEmpty()) {
            actions.add(action("Lead nurture missing", "Create active campaigns for qualified brand leads.", "LOW"));
        }
        if (actions.isEmpty()) {
            actions.add(action("Monetization engine stable", "Sponsorship, ad tracking, contracts, and billing queues are current.", "LOW"));
        }
        return actions;
    }

    private Map<String, Object> mapBrand(BrandPartner brand) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", brand.getId());
        item.put("companyName", brand.getCompanyName());
        item.put("brandType", brand.getBrandType());
        item.put("gstNumber", brand.getGstNumber());
        item.put("website", brand.getWebsite());
        item.put("email", brand.getEmail());
        item.put("phone", brand.getPhone());
        item.put("city", brand.getCity());
        item.put("logoUrl", brand.getLogoUrl());
        item.put("documentUrl", brand.getDocumentUrl());
        item.put("status", brand.getStatus());
        item.put("verificationNotes", brand.getVerificationNotes());
        item.put("createdAt", brand.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapOpportunity(SponsorshipOpportunity opportunity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", opportunity.getId());
        item.put("title", opportunity.getTitle());
        item.put("opportunityType", opportunity.getOpportunityType());
        item.put("description", opportunity.getDescription());
        item.put("city", opportunity.getCity());
        item.put("sportCategory", opportunity.getSportCategory());
        item.put("audienceSize", opportunity.getAudienceSize());
        item.put("amount", opportunity.getAmount());
        item.put("benefits", opportunity.getBenefits());
        item.put("ownerReference", opportunity.getOwnerReference());
        item.put("status", opportunity.getStatus());
        item.put("expiresAt", opportunity.getExpiresAt());
        item.put("createdAt", opportunity.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapProposal(SponsorshipProposal proposal) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", proposal.getId());
        item.put("brandId", proposal.getBrand().getId());
        item.put("brandName", proposal.getBrand().getCompanyName());
        item.put("opportunityId", proposal.getOpportunity().getId());
        item.put("opportunityTitle", proposal.getOpportunity().getTitle());
        item.put("message", proposal.getMessage());
        item.put("proposedAmount", proposal.getProposedAmount());
        item.put("deliverables", proposal.getDeliverables());
        item.put("documentUrl", proposal.getDocumentUrl());
        item.put("status", proposal.getStatus());
        item.put("decisionNotes", proposal.getDecisionNotes());
        item.put("createdAt", proposal.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapCampaign(SponsoredCampaign campaign, List<SponsoredAdEvent> events) {
        long impressions = events.stream().filter(event -> campaign.getId().equals(event.getCampaign().getId())).filter(event -> "IMPRESSION".equalsIgnoreCase(event.getEventType())).count();
        long clicks = events.stream().filter(event -> campaign.getId().equals(event.getCampaign().getId())).filter(event -> "CLICK".equalsIgnoreCase(event.getEventType())).count();
        long conversions = events.stream().filter(event -> campaign.getId().equals(event.getCampaign().getId())).filter(event -> "CONVERSION".equalsIgnoreCase(event.getEventType())).count();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", campaign.getId());
        item.put("brandId", campaign.getBrand().getId());
        item.put("brandName", campaign.getBrand().getCompanyName());
        item.put("name", campaign.getName());
        item.put("campaignType", campaign.getCampaignType());
        item.put("placement", campaign.getPlacement());
        item.put("targetAudience", campaign.getTargetAudience());
        item.put("city", campaign.getCity());
        item.put("creativeUrl", campaign.getCreativeUrl());
        item.put("destinationUrl", campaign.getDestinationUrl());
        item.put("dailyBudget", campaign.getDailyBudget());
        item.put("lifetimeBudget", campaign.getLifetimeBudget());
        item.put("spend", campaign.getSpend());
        item.put("status", campaign.getStatus());
        item.put("startsAt", campaign.getStartsAt());
        item.put("endsAt", campaign.getEndsAt());
        item.put("impressions", impressions);
        item.put("clicks", clicks);
        item.put("conversions", conversions);
        item.put("ctr", percent(clicks, Math.max(impressions, 1)));
        item.put("conversionRate", percent(conversions, Math.max(clicks, 1)));
        item.put("createdAt", campaign.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapLead(SponsorLead lead) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", lead.getId());
        item.put("brandId", lead.getBrand().getId());
        item.put("brandName", lead.getBrand().getCompanyName());
        item.put("campaignId", lead.getCampaign() == null ? null : lead.getCampaign().getId());
        item.put("leadType", lead.getLeadType());
        item.put("name", lead.getName());
        item.put("email", lead.getEmail());
        item.put("phone", lead.getPhone());
        item.put("city", lead.getCity());
        item.put("notes", lead.getNotes());
        item.put("estimatedValue", lead.getEstimatedValue());
        item.put("status", lead.getStatus());
        item.put("createdAt", lead.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapContract(SponsorshipContract contract) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", contract.getId());
        item.put("brandId", contract.getBrand().getId());
        item.put("brandName", contract.getBrand().getCompanyName());
        item.put("proposalId", contract.getProposal() == null ? null : contract.getProposal().getId());
        item.put("contractNumber", contract.getContractNumber());
        item.put("contractType", contract.getContractType());
        item.put("value", contract.getValue());
        item.put("documentUrl", contract.getDocumentUrl());
        item.put("status", contract.getStatus());
        item.put("startsAt", contract.getStartsAt());
        item.put("endsAt", contract.getEndsAt());
        item.put("createdAt", contract.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapInvoice(SponsorInvoice invoice) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", invoice.getId());
        item.put("brandId", invoice.getBrand().getId());
        item.put("brandName", invoice.getBrand().getCompanyName());
        item.put("contractId", invoice.getContract() == null ? null : invoice.getContract().getId());
        item.put("invoiceNumber", invoice.getInvoiceNumber());
        item.put("revenueCategory", invoice.getRevenueCategory());
        item.put("subtotal", invoice.getSubtotal());
        item.put("gstAmount", invoice.getGstAmount());
        item.put("totalAmount", invoice.getTotalAmount());
        item.put("status", invoice.getStatus());
        item.put("notes", invoice.getNotes());
        item.put("createdAt", invoice.getCreatedAt());
        return item;
    }

    private Map<String, Object> action(String title, String description, String severity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("severity", severity);
        return item;
    }

    private long countEvents(List<SponsoredAdEvent> events, String eventType) {
        return events.stream().filter(event -> eventType.equalsIgnoreCase(event.getEventType())).count();
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(((double) value / total) * 1000.0) / 10.0;
    }

    private void validateBrand(SponsorshipRequests.BrandRequest request) {
        if (request.getGstNumber() != null && !request.getGstNumber().isBlank() && !request.getGstNumber().trim().toUpperCase(Locale.ROOT).matches("[0-9A-Z]{15}")) {
            throw ApiException.badRequest("GST number must be 15 uppercase alphanumeric characters");
        }
        if (request.getWebsite() != null && !request.getWebsite().isBlank() && !request.getWebsite().startsWith("http://") && !request.getWebsite().startsWith("https://")) {
            throw ApiException.badRequest("Website must start with http:// or https://");
        }
    }

    private void audit(String actor, String action, String entity) {
        auditLogRepository.save(AuditLog.builder()
                .adminId(actor == null || actor.isBlank() ? "system" : actor)
                .action(action)
                .entity(entity)
                .build());
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(message);
        }
        return value.trim();
    }

    private Long requiredId(Long value, String message) {
        if (value == null) {
            throw ApiException.badRequest(message);
        }
        return value;
    }

    private String normalize(String value, String fallback) {
        String candidate = value == null || value.isBlank() ? fallback : value;
        return candidate.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Amount must be greater than or equal to zero");
        }
        return value;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
