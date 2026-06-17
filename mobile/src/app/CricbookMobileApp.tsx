import { StatusBar } from "expo-status-bar";
import React, { useEffect, useMemo, useState } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import {
  ActivityIndicator,
  Alert,
  Appearance,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  RefreshControl,
  StatusBar as NativeStatusBar,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from "react-native";
import {
  acceptMatchChallenge,
  addMoney,
  addOwnerExpense,
  applyRecruitment,
  applySlotPricing,
  askBusinessCoach,
  awardGamificationXp,
  blockSlots,
  cancelBooking,
  cancelOwnerBooking,
  cancelPublicMatch,
  checkInOwnerBooking,
  completeOwnerBooking,
  createAutoWaitlist,
  createBooking,
  createBookingTemplate,
  createLiveScore,
  createPublicMatch,
  createRecruitment,
  createRecurringBooking,
  createSplitPayment,
  createSlots,
  createTeam,
  createTurf,
  connectPlayer,
  discoverPlayers,
  discoverTeams,
  disableSlot,
  getAvailability,
  getAutoWaitlists,
  getBookingIntelligence,
  getBookingPredictions,
  getBookingTemplates,
  getCoupons,
  getFavorites,
  getFavoriteStatus,
  getFrequentBookings,
  getGamificationProfile,
  getPlayerPerformance,
  getPlayerPerformanceLeaderboard,
  getMatchMessages,
  getLiveScore,
  getLiveScores,
  startMatch,
  pauseMatch,
  resumeMatch,
  cancelMatch,
  undoBall,
  editBall,
  deleteBall,
  getMatchAnalytics,
  getMatchRecommendations,
  getOpenMatchmaking,
  overrideMatchResult,
  lockMatch,
  recordBall,
  completeScore,
  getMyAvailability,
  getMyBookings,
  getMyMatchHistory,
  getMyNetwork,
  getMyTeams,
  getNearbyPlayers,
  getActiveAnnouncements,
  getNotificationSummary,
  getNotifications,
  getOpenMatches,
  getOpenRecruitments,
  getOpenTournaments,
  getOwnerBookings,
  getOwnerDashboard,
  getOwnerFinanceDashboard,
  getOwnerInsights,
  getOwnerPaymentRules,
  getOwnerSettings,
  getOwnerBookingIntelligence,
  getOwnerReviews,
  getOwnerTurfs,
  getOwnerWeatherDashboard,
  getOwnerWeatherPolicy,
  getOwnerGamificationAnalytics,
  markOwnerNoShow,
  getPersonalization,
  getProfile,
  getRecurringBookings,
  getSocialFeed,
  getCommunityAnalytics,
  getTeamMessages,
  getTeamAnalytics,
  getAdminTeamAnalytics,
  getAdminCommunityModeration,
  getAdminBookingIntelligence,
  setTeamAvailability,
  createTeamPoll,
  getTournamentFixtures,
  getTournamentStandings,
  getTurfDetail,
  getTurfSlots,
  getUserReviews,
  getWallet,
  getWeatherDashboard,
  joinPublicMatch,
  joinMatchmakingMatch,
  markAllNotificationsRead,
  markNotificationRead,
  registerTeamForTournament,
  rejectMatchChallenge,
  removeFavorite,
  rebookBooking,
  requestToJoinTeam,
  saveFavorite,
  saveRecentSearch,
  schedulePublicMatch,
  searchTurfs,
  setTurfStatus,
  startPublicMatch,
  submitMatchResult,
  triggerRefund,
  unblockSlots,
  updateProfile,
  updateMyAvailability,
  updateSlot,
  validateCoupon,
  getMembershipPlans,
  getMyMemberships,
  subscribeMembership,
  getGrowthDashboard,
  getSmartRecommendations,
  chatAIAssistant,
  sendAIRecommendationFeedback,
  getAdminAnalytics,
  getAdminBIWorkspace,
  getAdminGrowthDashboard,
  getAdminGrowthAutomation,
  getAdminOwners,
  updateAdminUserStatus,
  getAdminTurfs,
  updateAdminTurfStatus,
  getAdminReviews,
  deleteAdminReview,
  getCampaigns,
  createCampaign,
  sendCampaign,
  getAdminOpsUsers,
  warnUser,
  banUser,
  unbanUser,
  getOwnerVerifications,
  approveOwner,
  rejectOwner,
  getTurfModerations,
  approveTurf,
  rejectTurf,
  getOpsBookings,
  overrideBookingStatus,
  triggerManualRefund,
  getOpsFinance,
  getOpsSettings,
  saveOpsSetting,
  getFraudAlerts,
  dismissFraudAlert,
  getOpsCmsContent,
  addOpsCmsContent,
  deleteOpsCmsContent,
  getOpsAuditLogs,
  getAdminGovernanceDashboard,
  getSupportTickets,
  replySupportTicket,
  getIncidents,
  createIncident,
  resolveIncident,
  getExpansionConfigs,
  getTranslations,
  getExchangeRates,
  getExpansionAnalytics,
  getAdminEnterpriseDashboard,
  getAdminEnterpriseCenters,
  getAdminEnterpriseStaff,
  getAdminEnterpriseFranchises,
  getAdminEnterpriseReports,
  getAdminEnterpriseGovernance,
  saveBranding,
  getBranding,
  getPlatformEpics,
  getAdminMonetizationDashboard,
  getAdminMonetizationBrands,
  getAdminMonetizationOpportunities,
  getAdminMonetizationProposals,
  getAdminMonetizationCampaigns,
  getAdminMonetizationLeads,
  getAdminMonetizationContracts,
  getAdminMonetizationInvoices,
  getAdminMonetizationReports,
  getAdminSportsDashboard,
  getAdminSportsCatalog,
  getAdminSportsAnalytics,
  getAdminSportsAudit,
  getAdminDeveloperPlatformDashboard,
  getAdminDevelopers,
  getAdminDeveloperApplications,
  getAdminDeveloperApiKeys,
  getAdminDeveloperWebhooks,
  getAdminDeveloperAnalytics,
  getAdminDeveloperBilling,
  getAdminDeveloperMarketplace,
  getAdminDeveloperAudit,
  getAdminAcademyDashboard,
  getAdminAcademies,
  getAdminCoaches,
  getAdminTrainingPrograms,
  getAdminTrainingBatches,
  getAdminAcademyEnrollments,
  getAdminAcademyAttendance,
  getAdminAcademyAssessments,
  getAdminAcademyVideos,
  getAdminTalentProfiles,
  getAdminAcademyCertificates,
  getAdminAcademyAnalytics,
  getAdminAcademyRevenue,
  getAdminAcademyAudit,
  getAdminCorporateDashboard,
  getAdminCorporateCompanies,
  getAdminCorporateLocations,
  getAdminCorporateDepartments,
  getAdminCorporateEmployees,
  getAdminCorporateTeams,
  getAdminCorporateTeamMemberships,
  getAdminCorporateBookingPlans,
  getAdminCorporateTournaments,
  getAdminCorporateWellnessPrograms,
  getAdminCorporateWellnessActivities,
  getAdminCorporateRewards,
  getAdminCorporateInvoices,
  getAdminCorporateContracts,
  getAdminCorporateLeagues,
  getAdminCorporateAnalytics,
  getAdminCorporateBilling,
  getAdminCorporateAudit,
  getAdminSaasDashboard,
  createAdminSaasTenant,
  saveAdminSaasBranding,
  addAdminSaasDomain,
  verifyAdminSaasDomain,
  setAdminSaasFeature,
  createAdminSaasSubscription,
  generateAdminSaasInvoice,
  payAdminSaasInvoice,
  installAdminSaasModule,
  requestAdminSaasAppBuild,
  createAdminSaasComplianceRequest,
  getMatchmakingAnalytics,
  getResaleListings,
  getRecommendedResaleListings,
  getMyResaleListings,
  createResaleListing,
  buyResaleListing,
  joinResaleWaitlist,
  getMyResaleWaitlist,
  getCommerceProducts,
  getCommerceCart,
  addCommerceCartItem,
  checkoutCommerceCart,
  getTravelPackages,
  getTravelEvents,
  getTravelAccommodations,
  bookTravelPackage,
  buyTravelTicket,
  bookTravelAccommodation,
  getCareerMarketplace,
  getCareerOpportunities,
  createCareerProfile,
  generateCareerResume,
  addCareerAchievement,
  applyCareerOpportunity,
  getGovernanceFederations,
  getGovernanceRankings,
  getSuperAppDashboard,
  getSportsOsDashboard,
  searchSportsOsFacilities,
  registerSportsVolunteer,
  applyGovernmentGrant,
  getFanEconomyDashboard,
  createFanEconomyProfile,
  joinFanEconomyClub,
  claimFanEconomyCollectible,
  submitFanEconomyPrediction,
  createFanEconomyWatchParty,
  joinFanEconomyWatchParty,
  createSportsIdentity,
  generateSportsPassport,
  creditUniversalWallet,
  earnUniversalRewards,
  createUnifiedMembership,
  addSportsTimelineEvent,
  getOwnerResaleAnalytics,
  getAdminResaleAnalytics,
  getAdminOwnerInsights,
  getAdminPaymentMonitoring,
  getAdminGamificationAnalytics,
  getOwnerResaleRules,
  updateOwnerResaleRules,
  getMySplitPayments,
  getPlayerSettings,
  paySplitShare,
  requestPlayerDataExport,
  requestOwnerDataExport,
  getSplitAnalytics,
  updatePlayerSettings,
  updateOwnerPaymentRules,
  updateOwnerSettings,
  getAdminWeatherDashboard,
  updateOwnerWeatherPolicy,
  claimGamificationReward,
  getStaffOpsDashboard,
  getStaffOpsSchedule,
  getStaffOpsBookings,
  staffOpsCheckIn,
  getStaffOpsTasks,
  updateStaffOpsTaskStatus,
  getStaffOpsMaintenance,
  createStaffOpsMaintenance,
  updateStaffOpsMaintenanceStatus,
  startStaffOpsAttendance,
  endStaffOpsAttendance,
  getStaffOpsAttendance,
  getStaffOpsEquipment,
  updateStaffOpsEquipment,
  getStaffOpsEmergencies,
  createStaffOpsEmergency,
  resolveStaffOpsEmergency,
  getStaffOpsAnalytics,
} from "../services/appService";
import {
  enableBiometricLogin,
  getStoredSession,
  isBiometricEnabled,
  login,
  logout,
  unlockWithBiometrics,
} from "../services/authService";

// EPIC 16 - REDUX, OFFLINE SQL CACHE, REALTIME FCM PUSH, AND SECURITY HARDENING IMPORTS
import {
  useDispatch,
  useSelector,
  setSession as reduxSetSession,
  setBiometricEnabled as reduxSetBiometricEnabled,
  setPortal as reduxSetPortal,
  setProfile as reduxSetProfile,
  setSearchFilters,
  setSearchResults,
  setSelectedTurf,
  setSelectedAvailability,
  setSelectedSlotId,
  setBookings as reduxSetBookings,
  setFavorites as reduxSetFavorites,
  setPersonalization as reduxSetPersonalization,
  setLiveScores as reduxSetLiveScores,
  setTournaments,
  setSelectedTournamentId,
  setTournamentFixtures,
  setTournamentStandings,
  setTeams,
  setMatches,
  setMatchHistory,
  setNotifications as reduxSetNotifications,
  setSocialFeed,
  setDiscoverablePlayers,
  setSelectedLiveScoreId,
  setLiveScoreDetail,
} from "../store";
import {
  cacheProfile,
  getCachedProfile,
  cacheBookings,
  getCachedBookings,
  cacheFavorites,
  getCachedFavorites,
  cacheHistory,
  getCachedHistory,
  enqueueSyncAction,
  syncOfflineQueue,
  checkConnectivity,
  startOfflineSync,
  stopOfflineSync,
} from "../services/offlineService";
import {
  initializeFCM,
  simulateFCMAlert,
  setupNotificationHandlers,
} from "../services/fcmService";
import {
  checkJailbroken,
} from "../services/securityService";
import {
  AutoWaitlist,
  Booking,
  BookingIntelligence,
  BookingPrediction,
  BookingTemplate,
  ChatMessage,
  CommunityAnalytics,
  Coupon,
  CouponValidation,
  FeedEvent,
  FrequentBooking,
  GamificationAnalytics,
  GamificationProfile,
  LiveScore,
  LiveScoreDetail,
  MatchmakingAnalytics,
  MatchmakingMatch,
  CommerceCart,
  CommerceProduct,
  NotificationAnnouncement,
  NotificationItem,
  NotificationSummary,
  OwnerDashboard,
  OwnerFinanceDashboard,
  OwnerInsights,
  OwnerSettings,
  PaymentRule,
  PaymentSplit,
  PlayerConnection,
  PlatformEpic,
  PersonalizationPayload,
  PlayerAvailability,
  PlayerDiscover,
  PlayerPerformance,
  PlayerPerformanceLeaderboardEntry,
  PlayerSettings,
  PortalKind,
  PublicMatch,
  Review,
  RecurringBooking,
  ResaleAnalytics,
  ResaleListing,
  ResaleRule,
  SearchFilters,
  Session,
  SplitAnalytics,
  Slot,
  SlotAvailability,
  SlotWaitlist,
  Team,
  TeamAnalytics,
  TeamRecruitment,
  Tournament,
  TournamentFixture,
  TournamentStanding,
  TravelAccommodation,
  TravelEvent,
  TravelPackage,
  CareerMap,
  GovernanceMap,
  SuperAppMap,
  SportsOsMap,
  FanEconomyMap,
  SaasMap,
  CareerProfilePayload,
  Turf,
  UserProfile,
  Wallet,
  WeatherDashboard,
  WeatherPolicy,
} from "../types/models";
import { AppTheme, ThemeMode, ThemeProvider, resolveThemeMode, theme, themePalettes } from "../theme";
import {
  AppButton,
  BottomNavigation,
  Chip,
  ConfirmDialog,
  EmptyState,
  FieldPair,
  FeatureLauncher,
  LabeledInput,
  type MobileFeatureGroup,
  type MobileNavItem,
  Metric,
  OptionPicker,
  ProfileHero,
  QuickActionRail,
  Screen,
  Section,
  SelectField,
  SheetModal,
  Surface,
} from "../components/ui";

const androidTopInset = Platform.OS === "android" ? NativeStatusBar.currentHeight || 0 : 0;
const today = new Date().toISOString().split("T")[0];
const mobileThemeStorageKey = "cricbook.mobile.theme";
const mobileLanguageStorageKey = "cricbook.mobile.language";
type MobilePortalKind = "player" | "owner" | "admin" | "staff";
type OwnerTab = "dashboard" | "insights" | "rebook" | "weather" | "engagement" | "payments" | "turfs" | "slots" | "bookings" | "reviews" | "earnings" | "community" | "teams" | "resale" | "settings";
type AdminTab = "analytics" | "ownerInsights" | "bookingIntelligence" | "weather" | "gamification" | "payments" | "epics" | "enterprise" | "monetization" | "sports" | "developers" | "academy" | "corporate" | "saas" | "owners" | "turfs" | "reviews" | "campaigns" | "bookings" | "support" | "incidents" | "matchmaking" | "teams" | "community" | "resale";
type StaffTab = "dashboard" | "checkins" | "tasks" | "maintenance" | "attendance" | "equipment" | "emergency";


type PlayerTab =
  | "teams"
  | "matches"
  | "feed"
  | "search"
  | "bookings"
  | "rebook"
  | "weather"
  | "progress"
  | "career"
  | "governance"
  | "superapp"
  | "sportsos"
  | "faneconomy"
  | "splits"
  | "favorites"
  | "foryou"
  | "tournaments"
  | "scoring"
  | "membership"
  | "marketplace"
  | "smart"
  | "wallet"
  | "notifications"
  | "settings"
  | "profile";

const playerTabs: Array<{ key: PlayerTab; label: string; icon: string }> = [
  { key: "teams", label: "Teams", icon: "TM" },
  { key: "matches", label: "Matches", icon: "MT" },
  { key: "feed", label: "Feed", icon: "FD" },
  { key: "search", label: "Search", icon: "SR" },
  { key: "bookings", label: "Bookings", icon: "BK" },
  { key: "rebook", label: "Rebook", icon: "RB" },
  { key: "weather", label: "Weather", icon: "WX" },
  { key: "progress", label: "Progress", icon: "XP" },
  { key: "career", label: "Career", icon: "CR" },
  { key: "governance", label: "Governance", icon: "GV" },
  { key: "superapp", label: "Super App", icon: "ID" },
  { key: "sportsos", label: "Sports OS", icon: "OS" },
  { key: "faneconomy", label: "Fans", icon: "FN" },
  { key: "splits", label: "Splits", icon: "SP" },
  { key: "favorites", label: "Favorites", icon: "FV" },
  { key: "foryou", label: "For You", icon: "AI" },
  { key: "tournaments", label: "Tournaments", icon: "TR" },
  { key: "scoring", label: "Scoring", icon: "SC" },
  { key: "membership", label: "Plans", icon: "PL" },
  { key: "marketplace", label: "Market", icon: "MK" },
  { key: "smart", label: "Smart", icon: "SM" },
  { key: "wallet", label: "Wallet", icon: "WT" },
  { key: "notifications", label: "Alerts", icon: "NT" },
  { key: "settings", label: "Settings", icon: "ST" },
  { key: "profile", label: "Profile", icon: "ME" },
];

const ownerTabs: Array<{ key: OwnerTab; label: string; icon: string }> = [
  { key: "dashboard", label: "Home", icon: "DB" },
  { key: "insights", label: "Insights", icon: "BI" },
  { key: "rebook", label: "Rebook", icon: "RB" },
  { key: "weather", label: "Weather", icon: "WX" },
  { key: "engagement", label: "Engage", icon: "XP" },
  { key: "payments", label: "Payments", icon: "PY" },
  { key: "turfs", label: "Turfs", icon: "TF" },
  { key: "slots", label: "Slots", icon: "SL" },
  { key: "bookings", label: "Bookings", icon: "BK" },
  { key: "reviews", label: "Reviews", icon: "RV" },
  { key: "earnings", label: "Earnings", icon: "RS" },
  { key: "community", label: "Community", icon: "CM" },
  { key: "teams", label: "Teams", icon: "TM" },
  { key: "resale", label: "Resale", icon: "MK" },
  { key: "settings", label: "Settings", icon: "ST" },
];

const adminTabs: Array<{ key: AdminTab; label: string; icon: string }> = [
  { key: "analytics", label: "Analytics", icon: "AN" },
  { key: "ownerInsights", label: "Owner BI", icon: "BI" },
  { key: "bookingIntelligence", label: "Rebook", icon: "RB" },
  { key: "weather", label: "Weather", icon: "WX" },
  { key: "gamification", label: "Game", icon: "XP" },
  { key: "payments", label: "Payments", icon: "PY" },
  { key: "epics", label: "Epics", icon: "EP" },
  { key: "enterprise", label: "Enterprise", icon: "EN" },
  { key: "monetization", label: "Monetize", icon: "AD" },
  { key: "sports", label: "Sports OS", icon: "SP" },
  { key: "developers", label: "API Hub", icon: "API" },
  { key: "academy", label: "Academy", icon: "AC" },
  { key: "corporate", label: "Corporate", icon: "CO" },
  { key: "saas", label: "SaaS", icon: "SAAS" },
  { key: "owners", label: "Owners", icon: "OW" },
  { key: "turfs", label: "Turfs", icon: "TF" },
  { key: "reviews", label: "Reviews", icon: "RV" },
  { key: "campaigns", label: "Campaigns", icon: "CP" },
  { key: "bookings", label: "Bookings", icon: "BK" },
  { key: "support", label: "Support", icon: "SP" },
  { key: "incidents", label: "Incidents", icon: "IN" },
  { key: "matchmaking", label: "Matchmaking", icon: "MM" },
  { key: "teams", label: "Teams", icon: "TM" },
  { key: "community", label: "Community", icon: "CN" },
  { key: "resale", label: "Resale", icon: "MK" },
];

const staffTabs: Array<{ key: StaffTab; label: string; icon: string }> = [
  { key: "dashboard", label: "Dashboard", icon: "DB" },
  { key: "checkins", label: "Check-In", icon: "QR" },
  { key: "tasks", label: "Tasks", icon: "TS" },
  { key: "maintenance", label: "Maintenance", icon: "MT" },
  { key: "attendance", label: "Attendance", icon: "AT" },
  { key: "equipment", label: "Equipment", icon: "EQ" },
  { key: "emergency", label: "Emergency", icon: "SOS" },
];

type TabDefinition<T extends string> = { key: T; label: string; icon: string };
type AnyMobileTab = PlayerTab | OwnerTab | AdminTab | StaffTab;

function selectTabs<T extends string>(items: Array<TabDefinition<T>>, keys: T[]): Array<TabDefinition<T>> {
  return keys
    .map((key) => items.find((item) => item.key === key))
    .filter((item): item is TabDefinition<T> => Boolean(item));
}

function groupTabs<T extends string>(
  items: Array<TabDefinition<T>>,
  groups: Array<{ title: string; subtitle?: string; keys: T[] }>
): Array<MobileFeatureGroup<T>> {
  return groups.map((group) => ({
    title: group.title,
    subtitle: group.subtitle,
    items: selectTabs(items, group.keys),
  }));
}

const mobileTranslationFallbacks: Record<string, Record<string, string>> = {
  en: {
    player_portal: "Player Portal",
    owner_portal: "Owner Portal",
    super_admin: "Super Admin",
    logout: "Logout",
    login: "Login",
    welcome_back: "Welcome back",
    auth_subtitle: "Website-level cricket workflows, now shaped for mobile. Player and owner portals are separated after login.",
    email_or_phone: "Email or phone",
    password: "Password",
    signing_in: "Signing in...",
    player: "Player",
    owner: "Owner",
    admin: "Admin",
    biometric_on: "Biometric on",
    enable_biometric: "Enable biometric",
    light: "Light",
    dark: "Dark",
    auto: "Auto",
    mobile_workspace: "Mobile workspace",
    play_more_cricket: "Play more cricket",
    player_workspace_subtitle: "Teams, matches, bookings, wallet, and personalized turf discovery.",
    run_turf_business: "Run your turf business",
    owner_workspace_subtitle: "Slots, bookings, earnings, reviews, and turf settings in one mobile workspace.",
    operate_cricbook: "Operate Cricbook",
    admin_workspace_subtitle: "Moderation, campaigns, support tickets, and platform incidents.",
    city: "City",
    language: "Language",
    select_city: "Select City",
    select_language: "Select Language",
    select_sport: "Select Sport Category",
    search_turfs: "Search Turfs",
    tab_teams: "Teams",
    tab_matches: "Matches",
    tab_feed: "Feed",
    tab_search: "Search",
    tab_bookings: "Bookings",
    tab_favorites: "Favorites",
    tab_foryou: "For You",
    tab_tournaments: "Tournaments",
    tab_scoring: "Scoring",
    tab_membership: "Plans",
    tab_marketplace: "Market",
    tab_smart: "Smart",
    tab_wallet: "Wallet",
    tab_notifications: "Alerts",
    tab_profile: "Profile",
    tab_dashboard: "Home",
    tab_turfs: "Turfs",
    tab_slots: "Slots",
    tab_reviews: "Reviews",
    tab_earnings: "Earnings",
    tab_settings: "Settings",
    tab_analytics: "Analytics",
    tab_epics: "Epics",
    tab_owners: "Owners",
    tab_campaigns: "Campaigns",
    tab_support: "Support",
    tab_incidents: "Incidents",
    tab_resale: "Resale",
  },
  hi: {
    player_portal: "प्लेयर पोर्टल",
    owner_portal: "ओनर पोर्टल",
    super_admin: "सुपर एडमिन",
    logout: "लॉगआउट",
    login: "लॉगिन",
    welcome_back: "वापसी पर स्वागत है",
    auth_subtitle: "मोबाइल के लिए बनाया गया क्रिकेट वर्कस्पेस। लॉगिन के बाद प्लेयर और ओनर पोर्टल अलग रहेंगे।",
    email_or_phone: "ईमेल या फोन",
    password: "पासवर्ड",
    signing_in: "साइन इन हो रहा है...",
    player: "प्लेयर",
    owner: "ओनर",
    admin: "एडमिन",
    biometric_on: "बायोमेट्रिक चालू",
    enable_biometric: "बायोमेट्रिक चालू करें",
    light: "लाइट",
    dark: "डार्क",
    auto: "ऑटो",
    mobile_workspace: "मोबाइल वर्कस्पेस",
    play_more_cricket: "ज्यादा क्रिकेट खेलें",
    player_workspace_subtitle: "टीम, मैच, बुकिंग, वॉलेट और आपके लिए सुझाए गए टर्फ।",
    run_turf_business: "अपना टर्फ बिजनेस चलाएं",
    owner_workspace_subtitle: "स्लॉट, बुकिंग, कमाई, रिव्यू और टर्फ सेटिंग्स एक जगह।",
    operate_cricbook: "Cricbook चलाएं",
    admin_workspace_subtitle: "मॉडरेशन, कैंपेन, सपोर्ट टिकट और प्लेटफॉर्म घटनाएं।",
    city: "शहर",
    language: "भाषा",
    select_city: "शहर चुनें",
    select_language: "भाषा चुनें",
    select_sport: "खेल श्रेणी चुनें",
    search_turfs: "टर्फ खोजें",
    tab_teams: "टीम",
    tab_matches: "मैच",
    tab_feed: "फीड",
    tab_search: "खोज",
    tab_bookings: "बुकिंग",
    tab_favorites: "पसंदीदा",
    tab_foryou: "आपके लिए",
    tab_tournaments: "टूर्नामेंट",
    tab_scoring: "स्कोरिंग",
    tab_membership: "प्लान",
    tab_smart: "स्मार्ट",
    tab_wallet: "वॉलेट",
    tab_notifications: "अलर्ट",
    tab_profile: "प्रोफाइल",
    tab_dashboard: "होम",
    tab_turfs: "टर्फ",
    tab_slots: "स्लॉट",
    tab_reviews: "रिव्यू",
    tab_earnings: "कमाई",
    tab_settings: "सेटिंग्स",
    tab_analytics: "एनालिटिक्स",
    tab_epics: "एपिक",
    tab_owners: "ओनर",
    tab_campaigns: "कैंपेन",
    tab_support: "सपोर्ट",
    tab_incidents: "घटनाएं",
  },
  gu: {
    player_portal: "પ્લેયર પોર્ટલ",
    owner_portal: "ઓનર પોર્ટલ",
    super_admin: "સુપર એડમિન",
    logout: "લૉગઆઉટ",
    login: "લૉગિન",
    welcome_back: "પાછા આવવા બદલ સ્વાગત",
    auth_subtitle: "મોબાઇલ માટે બનાવેલું ક્રિકેટ વર્કસ્પેસ. લૉગિન પછી પ્લેયર અને ઓનર પોર્ટલ અલગ રહેશે.",
    email_or_phone: "ઇમેઇલ અથવા ફોન",
    password: "પાસવર્ડ",
    signing_in: "સાઇન ઇન થઈ રહ્યું છે...",
    player: "પ્લેયર",
    owner: "ઓનર",
    admin: "એડમિન",
    biometric_on: "બાયોમેટ્રિક ચાલુ",
    enable_biometric: "બાયોમેટ્રિક ચાલુ કરો",
    light: "લાઇટ",
    dark: "ડાર્ક",
    auto: "ઓટો",
    mobile_workspace: "મોબાઇલ વર્કસ્પેસ",
    play_more_cricket: "વધુ ક્રિકેટ રમો",
    player_workspace_subtitle: "ટીમ, મેચ, બુકિંગ, વૉલેટ અને તમારા માટે સૂચવેલા ટર્ફ.",
    run_turf_business: "તમારો ટર્ફ બિઝનેસ ચલાવો",
    owner_workspace_subtitle: "સ્લોટ, બુકિંગ, કમાણી, રિવ્યુ અને ટર્ફ સેટિંગ્સ એક જગ્યાએ.",
    operate_cricbook: "Cricbook ચલાવો",
    admin_workspace_subtitle: "મોડરેશન, કેમ્પેન, સપોર્ટ ટિકિટ અને પ્લેટફોર્મ ઘટનાઓ.",
    city: "શહેર",
    language: "ભાષા",
    select_city: "શહેર પસંદ કરો",
    select_language: "ભાષા પસંદ કરો",
    select_sport: "રમત શ્રેણી પસંદ કરો",
    search_turfs: "ટર્ફ શોધો",
    tab_teams: "ટીમ",
    tab_matches: "મેચ",
    tab_feed: "ફીડ",
    tab_search: "શોધ",
    tab_bookings: "બુકિંગ",
    tab_favorites: "મનપસંદ",
    tab_foryou: "તમારા માટે",
    tab_tournaments: "ટૂર્નામેન્ટ",
    tab_scoring: "સ્કોરિંગ",
    tab_membership: "પ્લાન",
    tab_smart: "સ્માર્ટ",
    tab_wallet: "વૉલેટ",
    tab_notifications: "અલર્ટ",
    tab_profile: "પ્રોફાઇલ",
    tab_dashboard: "હોમ",
    tab_turfs: "ટર્ફ",
    tab_slots: "સ્લોટ",
    tab_reviews: "રિવ્યુ",
    tab_earnings: "કમાણી",
    tab_settings: "સેટિંગ્સ",
    tab_analytics: "એનલિટિક્સ",
    tab_epics: "એપિક",
    tab_owners: "ઓનર",
    tab_campaigns: "કેમ્પેન",
    tab_support: "સપોર્ટ",
    tab_incidents: "ઘટનાઓ",
  },
};

type PickerChoice = { label: string; value: string; hint?: string };
type PickerState = {
  visible: boolean;
  title: string;
  subtitle?: string;
  options: PickerChoice[];
  onSelect?: (value: string) => void;
};

type ConfirmState = {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  tone: "primary" | "owner" | "danger";
  onConfirm?: () => void;
};

type InfoSheetState = {
  visible: boolean;
  title: string;
  subtitle?: string;
  lines: string[];
};

const boxTypeChoices: PickerChoice[] = [
  { label: "5v5", value: "5v5" },
  { label: "6v6", value: "6v6" },
  { label: "7v7", value: "7v7" },
  { label: "11v11", value: "11v11" },
];

const pitchTypeChoices: PickerChoice[] = [
  { label: "Turf", value: "TURF" },
  { label: "Concrete", value: "CONCRETE" },
  { label: "Matting", value: "MATTING" },
];

const sportChoices: PickerChoice[] = [
  { label: "Cricket", value: "Cricket" },
  { label: "Box Cricket", value: "Box Cricket" },
  { label: "Practice Match", value: "Practice Match" },
];

const matchVisibilityChoices: PickerChoice[] = [
  { label: "Public", value: "PUBLIC" },
  { label: "Private", value: "PRIVATE" },
  { label: "Paid", value: "PAID" },
];

const matchFormatChoices: PickerChoice[] = [
  { label: "Friendly", value: "FRIENDLY" },
  { label: "Practice", value: "PRACTICE" },
  { label: "T20", value: "T20" },
  { label: "One Day", value: "ONE_DAY" },
  { label: "Box Cricket", value: "BOX_CRICKET" },
];

const memberChoices: PickerChoice[] = Array.from({ length: 15 }, (_, index) => {
  const count = index + 8;
  return { label: `${count} players`, value: String(count) };
});

const durationChoices: PickerChoice[] = [
  { label: "30 minutes", value: "30" },
  { label: "60 minutes", value: "60" },
  { label: "90 minutes", value: "90" },
  { label: "120 minutes", value: "120" },
];

const topUpChoices: PickerChoice[] = ["500", "1000", "1500", "2000", "3000"].map((value) => ({
  label: `Rs ${value}`,
  value,
}));

const languageChoices: PickerChoice[] = [
  { label: "English", value: "en" },
  { label: "Hindi", value: "hi" },
  { label: "Gujarati", value: "gu" },
  { label: "Marathi", value: "mr" },
  { label: "Tamil", value: "ta" },
  { label: "Telugu", value: "te" },
];

function formatEnumLabel(value?: string) {
  if (!value) return "--";
  return value
    .replace(/_/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatSlotTime(value?: string) {
  return value ? value.slice(0, 5) : "--";
}

function averageGovernanceScore(items: GovernanceMap[]) {
  const values = items.map((item) => Number(item.complianceScore || 0)).filter((value) => Number.isFinite(value) && value > 0);
  if (!values.length) {
    return 0;
  }
  return Math.round(values.reduce((sum, value) => sum + value, 0) / values.length);
}

function buildDateChoices(days = 30): PickerChoice[] {
  return Array.from({ length: days }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() + index);
    const iso = date.toISOString().split("T")[0];
    const label = date.toLocaleDateString("en-IN", {
      weekday: "short",
      day: "2-digit",
      month: "short",
    });
    return { label, value: iso, hint: iso };
  });
}

function buildTimeChoices(): PickerChoice[] {
  const choices: PickerChoice[] = [];
  for (let hour = 5; hour <= 23; hour += 1) {
    for (let minute = 0; minute < 60; minute += 30) {
      const hh = String(hour).padStart(2, "0");
      const mm = String(minute).padStart(2, "0");
      const value = `${hh}:${mm}`;
      choices.push({
        label: value,
        value,
        hint: new Date(`2026-01-01T${value}:00`).toLocaleTimeString("en-IN", {
          hour: "numeric",
          minute: "2-digit",
        }),
      });
    }
  }
  return choices;
}

const dateChoices = buildDateChoices();
const timeChoices = buildTimeChoices();

export function CricbookMobileApp() {
  const systemScheme = useColorScheme();
  const [systemTheme, setSystemTheme] = useState(() => Appearance.getColorScheme() || systemScheme || "light");
  const [booting, setBooting] = useState(true);
  const [themeMode, setThemeMode] = useState<ThemeMode>("system");
  const resolvedTheme = resolveThemeMode(themeMode, systemTheme === "dark");
  const activeTheme = themePalettes[resolvedTheme];
  const styles = useMemo(() => createAppStyles(activeTheme), [activeTheme]);
  const appStyles = useMemo(() => createMobileStyles(activeTheme), [activeTheme]);
  const statusBarStyle = resolvedTheme === "dark" ? "light" : "dark";
  const theme = activeTheme;

  // === EPIC 22 GLOBAL SPORTS STATES ===
  const [selectedCity, setSelectedCity] = useState("Ahmedabad");
  const [selectedLang, setSelectedLang] = useState("en");
  const [expansionConfigs, setExpansionConfigs] = useState<any[]>([]);
  const [exchangeRates, setExchangeRates] = useState<any[]>([]);
  const [translations, setTranslations] = useState<Record<string, string>>({});
  const [selectedSport, setSelectedSport] = useState("All");
  const [activeTenantId, setActiveTenantId] = useState<number>(1);
  const [whitelabelBranding, setWhitelabelBranding] = useState<any>(null);
  const [primaryColor, setPrimaryColor] = useState(theme.colors.player);
  const [secondaryColor, setSecondaryColor] = useState(theme.colors.playerSoft);

  // EPIC 16 - REDUX SYNC LAYER CONFIGURATION
  const dispatch = useDispatch();
  const [session, setSession] = useState<Session | null>(null);
  const [portal, setPortal] = useState<MobilePortalKind>("player");
  const [featureSheetVisible, setFeatureSheetVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [authenticating, setAuthenticating] = useState(false);
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [playerTab, setPlayerTab] = useState<PlayerTab>("foryou");
  const [ownerTab, setOwnerTab] = useState<OwnerTab>("dashboard");
  const [searchFilters, setSearchFilters] = useState<SearchFilters>({ city: "", date: today, boxType: "", pitchType: "" });
  const [searchResults, setSearchResults] = useState<Turf[]>([]);
  const [favorites, setFavorites] = useState<Turf[]>([]);
  const [selectedTurf, setSelectedTurf] = useState<Turf | null>(null);
  const [selectedAvailability, setSelectedAvailability] = useState<SlotAvailability[]>([]);
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);
  const [selectedTurfIsFavorite, setSelectedTurfIsFavorite] = useState(false);
  const [personalization, setPersonalization] = useState<PersonalizationPayload | null>(null);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [frequentBookings, setFrequentBookings] = useState<FrequentBooking[]>([]);
  const [bookingPredictions, setBookingPredictions] = useState<BookingPrediction[]>([]);
  const [recurringBookings, setRecurringBookings] = useState<RecurringBooking[]>([]);
  const [bookingTemplates, setBookingTemplates] = useState<BookingTemplate[]>([]);
  const [autoWaitlists, setAutoWaitlists] = useState<AutoWaitlist[]>([]);
  const [bookingIntelligence, setBookingIntelligence] = useState<BookingIntelligence | null>(null);
  const [weatherDashboard, setWeatherDashboard] = useState<WeatherDashboard | null>(null);
  const [weatherPolicy, setWeatherPolicy] = useState<WeatherPolicy | null>(null);
  const [gamificationProfile, setGamificationProfile] = useState<GamificationProfile | null>(null);
  const [gamificationAnalytics, setGamificationAnalytics] = useState<GamificationAnalytics | null>(null);
  const [weatherCity, setWeatherCity] = useState("Ahmedabad");
  const [rebookTargetDate, setRebookTargetDate] = useState(today);
  const [templateDraft, setTemplateDraft] = useState({ templateName: "", turfId: "", preferredTime: "20:00", players: "12" });
  const [recurringDraft, setRecurringDraft] = useState({ turfId: "", slotId: "", nextBookingDate: today, frequency: "WEEKLY" });
  const [autoWaitlistDraft, setAutoWaitlistDraft] = useState({ slotId: "", preferredDate: today, autoBookEnabled: false });
  const [splitPayments, setSplitPayments] = useState<PaymentSplit[]>([]);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [couponCode, setCouponCode] = useState("");
  const [couponAmount, setCouponAmount] = useState("1500");
  const [couponValidation, setCouponValidation] = useState<CouponValidation | null>(null);
  const [mobileChatMessage, setMobileChatMessage] = useState("");
  const [mobileChatReply, setMobileChatReply] = useState("");
  const [mobileChatAction, setMobileChatAction] = useState("");
  const [mobileChatLink, setMobileChatLink] = useState("");
  const [mobileChatIntent, setMobileChatIntent] = useState("");
  const [mobileChatConfidence, setMobileChatConfidence] = useState<number | null>(null);
  const [mobileChatConversationId, setMobileChatConversationId] = useState("");
  const [mobileChatExplainability, setMobileChatExplainability] = useState<string[]>([]);
  const [mobileChatBusy, setMobileChatBusy] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [notificationSummary, setNotificationSummary] = useState<NotificationSummary | null>(null);
  const [notificationAnnouncements, setNotificationAnnouncements] = useState<NotificationAnnouncement[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [discoverTeamsDraft, setDiscoverTeamsDraft] = useState({ query: "", city: "", visibility: "", message: "" });
  const [discoverableTeams, setDiscoverableTeams] = useState<Team[]>([]);
  const [teamAnalytics, setTeamAnalytics] = useState<TeamAnalytics | null>(null);
  const [teamAvailabilityDraft, setTeamAvailabilityDraft] = useState({ teamId: "", date: today, status: "AVAILABLE" });
  const [teamPollDraft, setTeamPollDraft] = useState({ teamId: "", question: "", options: "Friday, Saturday" });
  const [matches, setMatches] = useState<PublicMatch[]>([]);
  const [matchHistory, setMatchHistory] = useState<PublicMatch[]>([]);
  const [matchRecommendations, setMatchRecommendations] = useState<MatchmakingMatch[]>([]);
  const [resaleListings, setResaleListings] = useState<ResaleListing[]>([]);
  const [recommendedResaleListings, setRecommendedResaleListings] = useState<ResaleListing[]>([]);
  const [myResaleListings, setMyResaleListings] = useState<ResaleListing[]>([]);
  const [myResaleWaitlist, setMyResaleWaitlist] = useState<SlotWaitlist[]>([]);
  const [commerceProducts, setCommerceProducts] = useState<CommerceProduct[]>([]);
  const [commerceCart, setCommerceCart] = useState<CommerceCart | null>(null);
  const [travelPackages, setTravelPackages] = useState<TravelPackage[]>([]);
  const [travelEvents, setTravelEvents] = useState<TravelEvent[]>([]);
  const [travelAccommodations, setTravelAccommodations] = useState<TravelAccommodation[]>([]);
  const [careerMarketplace, setCareerMarketplace] = useState<CareerMap[]>([]);
  const [careerOpportunities, setCareerOpportunities] = useState<CareerMap[]>([]);
  const [careerProfile, setCareerProfile] = useState<CareerMap | null>(null);
  const [careerResume, setCareerResume] = useState<CareerMap | null>(null);
  const [governanceFederations, setGovernanceFederations] = useState<GovernanceMap[]>([]);
  const [governanceRankings, setGovernanceRankings] = useState<GovernanceMap[]>([]);
  const [superAppDashboard, setSuperAppDashboard] = useState<SuperAppMap | null>(null);
  const [sportsOsDashboard, setSportsOsDashboard] = useState<SportsOsMap | null>(null);
  const [sportsOsFacilities, setSportsOsFacilities] = useState<SportsOsMap[]>([]);
  const [fanEconomyDashboard, setFanEconomyDashboard] = useState<FanEconomyMap | null>(null);
  const [fanEconomyProfileDraft, setFanEconomyProfileDraft] = useState({
    displayName: "",
    email: "",
    city: selectedCity,
    favoriteSport: "CRICKET",
    favoriteTeam: "",
  });
  const [fanEconomyActiveFanId, setFanEconomyActiveFanId] = useState("");
  const [fanEconomyPredictionValue, setFanEconomyPredictionValue] = useState("Team A wins");
  const [fanEconomyWatchPartyTitle, setFanEconomyWatchPartyTitle] = useState("");
  const [sportsOsVolunteerDraft, setSportsOsVolunteerDraft] = useState({
    fullName: "",
    email: "",
    phone: "",
    stateName: "Gujarat",
    districtName: "Ahmedabad",
    category: "EVENT",
  });
  const [sportsOsGrantDraft, setSportsOsGrantDraft] = useState({
    applicantName: "",
    applicantType: "DISTRICT_AUTHORITY",
    programType: "SPORTS_INFRASTRUCTURE",
    stateName: "Gujarat",
    districtName: "Ahmedabad",
    requestedAmount: "100000",
  });
  const [superAppIdentityDraft, setSuperAppIdentityDraft] = useState({
    displayName: "",
    primaryEmail: "",
    phone: "",
    city: selectedCity,
    primarySport: "CRICKET",
    ageGroup: "OPEN",
  });
  const [careerProfileDraft, setCareerProfileDraft] = useState<CareerProfilePayload>({
    playerName: "",
    playerEmail: "",
    sport: "CRICKET",
    specialization: "Batting",
    playerRole: "Top order batter",
    city: selectedCity,
    ageGroup: "OPEN",
    bio: "",
    scoutOptIn: true,
    visibility: "PUBLIC",
  });
  const [careerAchievementTitle, setCareerAchievementTitle] = useState("");
  const [resaleDraft, setResaleDraft] = useState({ bookingId: "", price: "", notes: "" });
  const [waitlistDraft, setWaitlistDraft] = useState({ slotId: "", date: today, priority: "5" });
  const [socialFeed, setSocialFeed] = useState<FeedEvent[]>([]);
  const [discoverablePlayers, setDiscoverablePlayers] = useState<PlayerDiscover[]>([]);
  const [nearbyPlayers, setNearbyPlayers] = useState<PlayerDiscover[]>([]);
  const [playerNetwork, setPlayerNetwork] = useState<PlayerConnection[]>([]);
  const [teamRecruitments, setTeamRecruitments] = useState<TeamRecruitment[]>([]);
  const [communityAnalytics, setCommunityAnalytics] = useState<CommunityAnalytics | null>(null);
  const [recruitmentDraft, setRecruitmentDraft] = useState({ teamId: "", roleNeeded: "", city: "", availability: "Weekend evening", skillLevel: "Intermediate" });
  const [tournaments, setTournaments] = useState<Tournament[]>([]);
  const [liveScores, setLiveScores] = useState<LiveScore[]>([]);
  const [playerPerformance, setPlayerPerformance] = useState<PlayerPerformance | null>(null);
  const [performanceLeaderboard, setPerformanceLeaderboard] = useState<PlayerPerformanceLeaderboardEntry[]>([]);
  const [selectedTournamentId, setSelectedTournamentId] = useState<number | null>(null);
  const [tournamentFixtures, setTournamentFixtures] = useState<TournamentFixture[]>([]);
  const [tournamentStandings, setTournamentStandings] = useState<TournamentStanding[]>([]);
  const [selectedLiveScoreId, setSelectedLiveScoreId] = useState<number | null>(null);
  const [liveScoreDetail, setLiveScoreDetail] = useState<LiveScoreDetail | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [playerSettings, setPlayerSettings] = useState<PlayerSettings | null>(null);
  const [playerAvailability, setPlayerAvailability] = useState<PlayerAvailability | null>(null);
  const [profileReviews, setProfileReviews] = useState<Review[]>([]);
  const [profileDraft, setProfileDraft] = useState({ name: "", email: "", phone: "" });
  const [availabilityDraft, setAvailabilityDraft] = useState({
    availableDays: "",
    preferredWindow: "",
    homeCity: "",
    canTravel: true,
    notes: "",
  });
  const [ownerDashboard, setOwnerDashboard] = useState<OwnerDashboard | null>(null);
  const [ownerFinance, setOwnerFinance] = useState<OwnerFinanceDashboard | null>(null);
  const [ownerInsights, setOwnerInsights] = useState<OwnerInsights | null>(null);
  const [ownerSettings, setOwnerSettings] = useState<OwnerSettings | null>(null);
  const [paymentRules, setPaymentRules] = useState<Partial<PaymentRule>>({
    minimumPercentage: 50,
    autoCancel: false,
    fullPaymentRequired: false,
    reminderHoursBefore: 24,
  });
  const [splitAnalytics, setSplitAnalytics] = useState<SplitAnalytics | null>(null);
  const [splitDraft, setSplitDraft] = useState({ bookingId: "", players: "", dueDate: today });
  const [ownerExpenseDraft, setOwnerExpenseDraft] = useState({ type: "Electricity", amount: "", expenseDate: today });
  const [businessCoachQuestion, setBusinessCoachQuestion] = useState("Why is weekday revenue low?");
  const [ownerTurfs, setOwnerTurfs] = useState<Turf[]>([]);
  const [ownerBookings, setOwnerBookings] = useState<Booking[]>([]);
  const [ownerReviews, setOwnerReviews] = useState<Review[]>([]);
  const [matchmakingAnalytics, setMatchmakingAnalytics] = useState<MatchmakingAnalytics | null>(null);
  const [resaleAnalytics, setResaleAnalytics] = useState<ResaleAnalytics | null>(null);
  const [resaleRules, setResaleRules] = useState<Partial<ResaleRule>>({
    allowResale: true,
    autoApproveTransfer: true,
    restrictLowTrustUsers: true,
    maxMarkupPercent: 0,
    minHoursBeforeStart: 2,
  });
  const [ownerSelectedTurfId, setOwnerSelectedTurfId] = useState<number | null>(null);
  const [ownerSlots, setOwnerSlots] = useState<Slot[]>([]);
  const [ownerSlotAvailability, setOwnerSlotAvailability] = useState<SlotAvailability[]>([]);
  const [slotManagerDate, setSlotManagerDate] = useState(today);
  const [slotForm, setSlotForm] = useState({ startTime: "06:00", endTime: "22:00", slotDuration: "60", price: "800" });
  const [slotPriceDate, setSlotPriceDate] = useState(today);
  const [slotPriceValue, setSlotPriceValue] = useState("1100");
  const [slotSelection, setSlotSelection] = useState<number[]>([]);
  const [turfDraft, setTurfDraft] = useState({
    name: "",
    description: "",
    city: "",
    location: "",
    latitude: "",
    longitude: "",
    pricePerHour: "",
    pitchType: "TURF",
    boxType: "7v7",
    amenities: "",
  });
  const [teamDraft, setTeamDraft] = useState({ name: "", sport: "Cricket", city: "", description: "", maxMembers: "11", visibility: "PUBLIC" });
  const [matchDraft, setMatchDraft] = useState({
    title: "",
    sport: "Cricket",
    city: "",
    location: "",
    matchDate: today,
    startTime: "18:00",
    endTime: "20:00",
    maxPlayers: "14",
    visibility: "PUBLIC",
    format: "BOX_CRICKET",
    overs: "10",
    entryFee: "0",
    homeTeamId: "",
    awayTeamId: "",
    challengeMessage: "",
    notes: "",
  });
  const [playerSearchDraft, setPlayerSearchDraft] = useState({ city: "", position: "", availability: "" });
  const [liveScoreDraft, setLiveScoreDraft] = useState({ title: "", teamAName: "", teamBName: "", battingTeam: "", maxOvers: "20" });
  const [matchCenterTab, setMatchCenterTab] = useState<"scoreboard" | "timeline" | "commentary" | "analytics">("scoreboard");
  const [timelineOver, setTimelineOver] = useState(1);
  const [ballBatter, setBallBatter] = useState("");
  const [ballBowler, setBallBowler] = useState("");
  const [ballNote, setBallNote] = useState("");
  const [analyticsData, setAnalyticsData] = useState<any>(null);
  const [editingEventId, setEditingEventId] = useState<number | null>(null);
  const [editRuns, setEditRuns] = useState("0");
  const [editExtraRuns, setEditExtraRuns] = useState("0");
  const [editExtraType, setEditExtraType] = useState<string | null>(null);
  const [editWicket, setEditWicket] = useState(false);
  const [editWicketType, setEditWicketType] = useState<string | null>(null);
  const [biometricEnabled, setBiometricEnabled] = useState(false);
  const [pickerState, setPickerState] = useState<PickerState>({ visible: false, title: "", options: [] });
  const [confirmState, setConfirmState] = useState<ConfirmState>({
    visible: false,
    title: "",
    message: "",
    confirmLabel: "Confirm",
    tone: "primary",
  });
  const [infoSheet, setInfoSheet] = useState<InfoSheetState>({ visible: false, title: "", lines: [] });

  // === EPIC 16 ADDITIONS: MEMBERSHIP, SMART, AND ADMIN STATES ===
  const [plans, setPlans] = useState<any[]>([]);
  const [myMemberships, setMyMemberships] = useState<any[]>([]);
  const [membershipTurfIds, setMembershipTurfIds] = useState<Record<number, string>>({});
  const [smartData, setSmartData] = useState<any>(null);
  const [smartCity, setSmartCity] = useState("");
  const [smartTurfId, setSmartTurfId] = useState("");
  const [adminAnalytics, setAdminAnalytics] = useState<any>(null);
  const [adminOwners, setAdminOwners] = useState<any[]>([]);
  const [adminTurfs, setAdminTurfs] = useState<Turf[]>([]);
  const [adminReviews, setAdminReviews] = useState<any[]>([]);
  const [adminCampaigns, setAdminCampaigns] = useState<any[]>([]);
  const [adminBookings, setAdminBookings] = useState<any[]>([]);
  const [adminSupportTickets, setAdminSupportTickets] = useState<any[]>([]);
  const [adminIncidents, setAdminIncidents] = useState<any[]>([]);
  const [platformEpics, setPlatformEpics] = useState<PlatformEpic[]>([]);
  const [adminAuditLogs, setAdminAuditLogs] = useState<any[]>([]);
  const [adminFraudAlerts, setAdminFraudAlerts] = useState<any[]>([]);
  const [adminGovernance, setAdminGovernance] = useState<any>(null);
  const [adminBIWorkspace, setAdminBIWorkspace] = useState<any>(null);
  const [growthDashboard, setGrowthDashboard] = useState<any>(null);
  const [adminGrowth, setAdminGrowth] = useState<any>(null);
  const [adminGrowthAutomation, setAdminGrowthAutomation] = useState<any[]>([]);
  const [adminEnterprise, setAdminEnterprise] = useState<any>(null);
  const [adminEnterpriseCenters, setAdminEnterpriseCenters] = useState<any[]>([]);
  const [adminEnterpriseStaff, setAdminEnterpriseStaff] = useState<any[]>([]);
  const [adminEnterpriseFranchises, setAdminEnterpriseFranchises] = useState<any[]>([]);
  const [adminEnterpriseReports, setAdminEnterpriseReports] = useState<any>(null);
  const [adminEnterpriseGovernance, setAdminEnterpriseGovernance] = useState<any>(null);
  const [adminMonetization, setAdminMonetization] = useState<any>(null);
  const [adminMonetizationBrands, setAdminMonetizationBrands] = useState<any[]>([]);
  const [adminMonetizationOpportunities, setAdminMonetizationOpportunities] = useState<any[]>([]);
  const [adminMonetizationProposals, setAdminMonetizationProposals] = useState<any[]>([]);
  const [adminMonetizationCampaigns, setAdminMonetizationCampaigns] = useState<any[]>([]);
  const [adminMonetizationLeads, setAdminMonetizationLeads] = useState<any[]>([]);
  const [adminMonetizationContracts, setAdminMonetizationContracts] = useState<any[]>([]);
  const [adminMonetizationInvoices, setAdminMonetizationInvoices] = useState<any[]>([]);
  const [adminMonetizationReports, setAdminMonetizationReports] = useState<any>(null);
  const [adminSports, setAdminSports] = useState<any>(null);
  const [adminSportsCatalog, setAdminSportsCatalog] = useState<any[]>([]);
  const [adminSportsAnalytics, setAdminSportsAnalytics] = useState<any>(null);
  const [adminSportsAudit, setAdminSportsAudit] = useState<any[]>([]);
  const [adminDeveloperPlatform, setAdminDeveloperPlatform] = useState<any>(null);
  const [adminDevelopers, setAdminDevelopers] = useState<any[]>([]);
  const [adminDeveloperApplications, setAdminDeveloperApplications] = useState<any[]>([]);
  const [adminDeveloperApiKeys, setAdminDeveloperApiKeys] = useState<any[]>([]);
  const [adminDeveloperWebhooks, setAdminDeveloperWebhooks] = useState<any[]>([]);
  const [adminDeveloperAnalytics, setAdminDeveloperAnalytics] = useState<any>(null);
  const [adminDeveloperBilling, setAdminDeveloperBilling] = useState<any>(null);
  const [adminDeveloperMarketplace, setAdminDeveloperMarketplace] = useState<any[]>([]);
  const [adminDeveloperAudit, setAdminDeveloperAudit] = useState<any[]>([]);
  const [adminAcademy, setAdminAcademy] = useState<any>(null);
  const [adminAcademies, setAdminAcademies] = useState<any[]>([]);
  const [adminCoaches, setAdminCoaches] = useState<any[]>([]);
  const [adminTrainingPrograms, setAdminTrainingPrograms] = useState<any[]>([]);
  const [adminTrainingBatches, setAdminTrainingBatches] = useState<any[]>([]);
  const [adminAcademyEnrollments, setAdminAcademyEnrollments] = useState<any[]>([]);
  const [adminAcademyAttendance, setAdminAcademyAttendance] = useState<any[]>([]);
  const [adminAcademyAssessments, setAdminAcademyAssessments] = useState<any[]>([]);
  const [adminAcademyVideos, setAdminAcademyVideos] = useState<any[]>([]);
  const [adminTalentProfiles, setAdminTalentProfiles] = useState<any[]>([]);
  const [adminAcademyCertificates, setAdminAcademyCertificates] = useState<any[]>([]);
  const [adminAcademyAnalytics, setAdminAcademyAnalytics] = useState<any>(null);
  const [adminAcademyRevenue, setAdminAcademyRevenue] = useState<any>(null);
  const [adminAcademyAudit, setAdminAcademyAudit] = useState<any[]>([]);
  const [adminCorporate, setAdminCorporate] = useState<any>(null);
  const [adminCorporateCompanies, setAdminCorporateCompanies] = useState<any[]>([]);
  const [adminCorporateLocations, setAdminCorporateLocations] = useState<any[]>([]);
  const [adminCorporateDepartments, setAdminCorporateDepartments] = useState<any[]>([]);
  const [adminCorporateEmployees, setAdminCorporateEmployees] = useState<any[]>([]);
  const [adminCorporateTeams, setAdminCorporateTeams] = useState<any[]>([]);
  const [adminCorporateMemberships, setAdminCorporateMemberships] = useState<any[]>([]);
  const [adminCorporateBookingPlans, setAdminCorporateBookingPlans] = useState<any[]>([]);
  const [adminCorporateTournaments, setAdminCorporateTournaments] = useState<any[]>([]);
  const [adminCorporateWellnessPrograms, setAdminCorporateWellnessPrograms] = useState<any[]>([]);
  const [adminCorporateWellnessActivities, setAdminCorporateWellnessActivities] = useState<any[]>([]);
  const [adminCorporateRewards, setAdminCorporateRewards] = useState<any[]>([]);
  const [adminCorporateInvoices, setAdminCorporateInvoices] = useState<any[]>([]);
  const [adminCorporateContracts, setAdminCorporateContracts] = useState<any[]>([]);
  const [adminCorporateLeagues, setAdminCorporateLeagues] = useState<any[]>([]);
  const [adminCorporateAnalytics, setAdminCorporateAnalytics] = useState<any>(null);
  const [adminCorporateBilling, setAdminCorporateBilling] = useState<any>(null);
  const [adminCorporateAudit, setAdminCorporateAudit] = useState<any[]>([]);
  const [adminSaasDashboard, setAdminSaasDashboard] = useState<SaasMap | null>(null);
  const [adminSaasTenantDraft, setAdminSaasTenantDraft] = useState({
    name: "",
    domain: "",
    tenantType: "ENTERPRISE",
    countryCode: "IN",
    regionCode: "IN-GJ",
    currency: "INR",
  });
  const [adminSaasTenantId, setAdminSaasTenantId] = useState("");
  const [adminSaasDomain, setAdminSaasDomain] = useState("");
  const [adminSaasFeatureKey, setAdminSaasFeatureKey] = useState("BOOKINGS");
  const [adminSaasPlanCode, setAdminSaasPlanCode] = useState("ENTERPRISE");
  const [adminSaasInvoiceAmount, setAdminSaasInvoiceAmount] = useState("9999");
  const [adminSaasPaymentAmount, setAdminSaasPaymentAmount] = useState("11798.82");
  const [adminSaasAppVersion, setAdminSaasAppVersion] = useState("1.0.0");
  const [adminTab, setAdminTab] = useState<AdminTab>("analytics");
  const [staffTab, setStaffTab] = useState<StaffTab>("dashboard");
  const [staffDashboard, setStaffDashboard] = useState<any>(null);
  const [staffSchedule, setStaffSchedule] = useState<any[]>([]);
  const [staffBookings, setStaffBookings] = useState<any[]>([]);
  const [staffTasks, setStaffTasks] = useState<any[]>([]);
  const [staffMaintenance, setStaffMaintenance] = useState<any[]>([]);
  const [staffAttendance, setStaffAttendance] = useState<any[]>([]);
  const [staffEquipment, setStaffEquipment] = useState<any[]>([]);
  const [staffEmergencies, setStaffEmergencies] = useState<any[]>([]);
  const [staffAnalytics, setStaffAnalytics] = useState<any>(null);
  const [staffCheckInCode, setStaffCheckInCode] = useState("");
  const [staffMaintenanceTitle, setStaffMaintenanceTitle] = useState("");
  const [staffMaintenanceDescription, setStaffMaintenanceDescription] = useState("");
  const [staffEmergencyType, setStaffEmergencyType] = useState("MEDICAL");
  const [staffEmergencyMessage, setStaffEmergencyMessage] = useState("");
  const [campaignForm, setCampaignForm] = useState({
    name: "",
    title: "",
    message: "",
    channel: "EMAIL" as "EMAIL" | "PUSH" | "SMS" | "WHATSAPP",
    targetRole: "",
  });
  const [supportReplyText, setSupportReplyText] = useState("");
  const [mobileIncidentName, setMobileIncidentName] = useState("");
  const [mobileIncidentSeverity, setMobileIncidentSeverity] = useState("HIGH");
  const [mobileIncidentMessage, setMobileIncidentMessage] = useState("");

  const t = (key: string, defaultValue: string) => {
    return translations[key] || mobileTranslationFallbacks[selectedLang]?.[key] || mobileTranslationFallbacks.en[key] || defaultValue;
  };

  const playerTabItems = useMemo(
    () => playerTabs.map((item) => ({ ...item, label: t(`tab_${item.key}`, item.label) })),
    [selectedLang, translations]
  );
  const ownerTabItems = useMemo(
    () => ownerTabs.map((item) => ({ ...item, label: t(`tab_${item.key}`, item.label) })),
    [selectedLang, translations]
  );
  const adminTabItems = useMemo(
    () => adminTabs.map((item) => ({ ...item, label: t(`tab_${item.key}`, item.label) })),
    [selectedLang, translations]
  );
  const staffTabItems = useMemo(
    () => staffTabs.map((item) => ({ ...item, label: t(`tab_${item.key}`, item.label) })),
    [selectedLang, translations]
  );
  const playerBottomTabs = useMemo(
    () => selectTabs(playerTabItems, ["foryou", "search", "bookings", "profile"]),
    [playerTabItems]
  );
  const ownerBottomTabs = useMemo(
    () => selectTabs(ownerTabItems, ["dashboard", "turfs", "bookings", "earnings"]),
    [ownerTabItems]
  );
  const adminBottomTabs = useMemo(
    () => selectTabs(adminTabItems, ["analytics", "bookings", "support", "incidents"]),
    [adminTabItems]
  );
  const staffBottomTabs = useMemo(
    () => selectTabs(staffTabItems, ["dashboard", "checkins", "tasks", "emergency"]),
    [staffTabItems]
  );
  const playerQuickActions = useMemo(
    () => selectTabs(playerTabItems, ["search", "rebook", "tournaments", "wallet"]).map((item) => ({
      ...item,
      hint:
        item.key === "search"
          ? "Find and book in three taps"
          : item.key === "rebook"
            ? "Repeat your last slot"
            : item.key === "tournaments"
              ? "Join competitive events"
              : "Balance, rewards and payments",
    })),
    [playerTabItems]
  );
  const ownerQuickActions = useMemo(
    () => selectTabs(ownerTabItems, ["slots", "bookings", "earnings", "reviews"]).map((item) => ({
      ...item,
      hint:
        item.key === "slots"
          ? "Open, block and price slots"
          : item.key === "bookings"
            ? "Check-in, complete, refund"
            : item.key === "earnings"
              ? "Revenue and settlements"
              : "Protect venue reputation",
    })),
    [ownerTabItems]
  );
  const adminQuickActions = useMemo(
    () => selectTabs(adminTabItems, ["owners", "turfs", "support", "incidents"]).map((item) => ({
      ...item,
      hint:
        item.key === "owners"
          ? "Approve and monitor owners"
          : item.key === "turfs"
            ? "Moderate venue inventory"
            : item.key === "support"
              ? "Resolve customer issues"
              : "Operational response",
    })),
    [adminTabItems]
  );
  const staffQuickActions = useMemo(
    () => selectTabs(staffTabItems, ["checkins", "tasks", "maintenance", "emergency"]).map((item) => ({
      ...item,
      hint:
        item.key === "checkins"
          ? "Verify player arrivals"
          : item.key === "tasks"
            ? "Daily work queue"
            : item.key === "maintenance"
              ? "Fix venue issues"
              : "Escalate urgent events",
    })),
    [staffTabItems]
  );
  const playerFeatureGroups = useMemo(
    () => groupTabs(playerTabItems, [
      { title: "Play", subtitle: "Find, book, repeat, and manage your sports calendar.", keys: ["foryou", "search", "bookings", "rebook", "favorites", "weather"] },
      { title: "Compete", subtitle: "Teams, public matches, tournaments, and live scoring.", keys: ["teams", "matches", "tournaments", "scoring", "progress"] },
      { title: "Community", subtitle: "Feed, fan identity, career, governance, and sports OS.", keys: ["feed", "career", "governance", "superapp", "sportsos", "faneconomy"] },
      { title: "Money & AI", subtitle: "Wallet, memberships, marketplace, split payments, alerts, and smart assistant.", keys: ["wallet", "splits", "membership", "marketplace", "smart", "notifications", "profile", "settings"] },
    ]),
    [playerTabItems]
  );
  const ownerFeatureGroups = useMemo(
    () => groupTabs(ownerTabItems, [
      { title: "Operate Venue", subtitle: "Core owner workflows for turfs, slots, and bookings.", keys: ["dashboard", "turfs", "slots", "bookings"] },
      { title: "Revenue", subtitle: "Payments, earnings, intelligence, and weather decisions.", keys: ["earnings", "payments", "insights", "rebook", "weather"] },
      { title: "Growth", subtitle: "Reviews, community, teams, resale, and engagement loops.", keys: ["reviews", "community", "teams", "resale", "engagement"] },
      { title: "Account", keys: ["settings"] },
    ]),
    [ownerTabItems]
  );
  const adminFeatureGroups = useMemo(
    () => groupTabs(adminTabItems, [
      { title: "Command Center", subtitle: "KPIs, support, incidents, users, and turfs.", keys: ["analytics", "owners", "turfs", "bookings", "support", "incidents"] },
      { title: "Business Intelligence", subtitle: "Payments, owner BI, rebooking intelligence, weather, and gamification.", keys: ["payments", "ownerInsights", "bookingIntelligence", "weather", "gamification"] },
      { title: "Platform Growth", subtitle: "Enterprise, monetization, sports OS, developers, academy, corporate, and SaaS.", keys: ["enterprise", "monetization", "sports", "developers", "academy", "corporate", "saas", "epics"] },
      { title: "Community Controls", subtitle: "Reviews, campaigns, matchmaking, teams, community, and resale.", keys: ["reviews", "campaigns", "matchmaking", "teams", "community", "resale"] },
    ]),
    [adminTabItems]
  );
  const staffFeatureGroups = useMemo(
    () => groupTabs(staffTabItems, [
      { title: "Daily Operations", subtitle: "Everything ground staff needs during a shift.", keys: ["dashboard", "checkins", "tasks", "maintenance"] },
      { title: "Workforce", subtitle: "Attendance, equipment, and emergency execution.", keys: ["attendance", "equipment", "emergency"] },
    ]),
    [staffTabItems]
  );

  const canAccessOwnerPortal = session?.role === "OWNER" || session?.role === "ADMIN";

  useEffect(() => {
    AsyncStorage.getItem(mobileThemeStorageKey)
      .then((storedMode) => {
        if (storedMode === "light" || storedMode === "dark" || storedMode === "system") {
          setThemeMode(storedMode);
        }
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    startOfflineSync();
    return () => stopOfflineSync();
  }, []);

  useEffect(() => {
    setSystemTheme(Appearance.getColorScheme() || systemScheme || "light");
  }, [systemScheme]);

  useEffect(() => {
    const subscription = Appearance.addChangeListener(({ colorScheme }) => {
      setSystemTheme(colorScheme || "light");
    });
    return () => subscription.remove();
  }, []);

  useEffect(() => {
    if (!whitelabelBranding?.primaryColor) {
      setPrimaryColor(theme.colors.player);
    }
    if (!whitelabelBranding?.secondaryColor) {
      setSecondaryColor(theme.colors.playerSoft);
    }
  }, [resolvedTheme, whitelabelBranding?.primaryColor, whitelabelBranding?.secondaryColor]);

  useEffect(() => {
    AsyncStorage.getItem(mobileLanguageStorageKey)
      .then((storedLang) => {
        if (storedLang && mobileTranslationFallbacks[storedLang]) {
          setSelectedLang(storedLang);
          void loadGlobalConfigs(storedLang, selectedCity);
        }
      })
      .catch(() => undefined);
  }, []);

  const updateThemeMode = (mode: ThemeMode) => {
    setThemeMode(mode);
    AsyncStorage.setItem(mobileThemeStorageKey, mode).catch(() => undefined);
  };

  const updateLanguage = async (lang: string) => {
    setSelectedLang(lang);
    await AsyncStorage.setItem(mobileLanguageStorageKey, lang).catch(() => undefined);
    await loadGlobalConfigs(lang, selectedCity);
  };

  useEffect(() => {
    if (reduxSetSession) dispatch(reduxSetSession(session));
  }, [session]);

  useEffect(() => {
    if (reduxSetBiometricEnabled) dispatch(reduxSetBiometricEnabled(biometricEnabled));
  }, [biometricEnabled]);

  useEffect(() => {
    if (reduxSetPortal) dispatch(reduxSetPortal(portal));
  }, [portal]);

  useEffect(() => {
    if (reduxSetProfile) dispatch(reduxSetProfile(profile));
  }, [profile]);

  useEffect(() => {
    if (setSearchFilters) dispatch(setSearchFilters(searchFilters));
  }, [searchFilters]);

  useEffect(() => {
    if (setSearchResults) dispatch(setSearchResults(searchResults));
  }, [searchResults]);

  useEffect(() => {
    if (setSelectedTurf) dispatch(setSelectedTurf(selectedTurf));
  }, [selectedTurf]);

  useEffect(() => {
    if (setSelectedAvailability) dispatch(setSelectedAvailability(selectedAvailability));
  }, [selectedAvailability]);

  useEffect(() => {
    if (setSelectedSlotId) dispatch(setSelectedSlotId(selectedSlotId));
  }, [selectedSlotId]);

  useEffect(() => {
    if (reduxSetBookings) dispatch(reduxSetBookings(bookings));
  }, [bookings]);

  useEffect(() => {
    if (reduxSetFavorites) dispatch(reduxSetFavorites(favorites));
  }, [favorites]);

  useEffect(() => {
    if (reduxSetPersonalization) dispatch(reduxSetPersonalization(personalization));
  }, [personalization]);

  useEffect(() => {
    if (reduxSetLiveScores) dispatch(reduxSetLiveScores(liveScores));
  }, [liveScores]);

  useEffect(() => {
    if (reduxSetNotifications) dispatch(reduxSetNotifications(notifications));
  }, [notifications]);

  useEffect(() => {
    if (setTeams) dispatch(setTeams(teams));
  }, [teams]);

  useEffect(() => {
    if (setMatches) dispatch(setMatches(matches));
  }, [matches]);

  useEffect(() => {
    if (setMatchHistory) dispatch(setMatchHistory(matchHistory));
  }, [matchHistory]);

  useEffect(() => {
    if (setSocialFeed) dispatch(setSocialFeed(socialFeed));
  }, [socialFeed]);

  useEffect(() => {
    if (setDiscoverablePlayers) dispatch(setDiscoverablePlayers(discoverablePlayers));
  }, [discoverablePlayers]);

  useEffect(() => {
    if (setTournaments) dispatch(setTournaments(tournaments));
  }, [tournaments]);

  useEffect(() => {
    if (setSelectedTournamentId) dispatch(setSelectedTournamentId(selectedTournamentId));
  }, [selectedTournamentId]);

  useEffect(() => {
    if (setTournamentFixtures) dispatch(setTournamentFixtures(tournamentFixtures));
  }, [tournamentFixtures]);

  useEffect(() => {
    if (setTournamentStandings) dispatch(setTournamentStandings(tournamentStandings));
  }, [tournamentStandings]);

  useEffect(() => {
    if (setSelectedLiveScoreId) dispatch(setSelectedLiveScoreId(selectedLiveScoreId));
  }, [selectedLiveScoreId]);

  useEffect(() => {
    if (setLiveScoreDetail) dispatch(setLiveScoreDetail(liveScoreDetail));
  }, [liveScoreDetail]);

  useEffect(() => {
    if (!session) return;
    if (portal === "admin") {
      void refreshAdmin(adminTab, false);
    }
  }, [session, portal, adminTab]);

  useEffect(() => {
    if (!session || portal !== "staff") return;
    void refreshStaff(staffTab, false);
  }, [session, portal, staffTab]);

  useEffect(() => {
    bootstrap();
    void loadGlobalConfigs();
  }, []);

  useEffect(() => {
    if (!session) return;
    if (portal === "player") {
      void refreshPlayer(playerTab, false);
    } else if (portal === "owner" && canAccessOwnerPortal) {
      void refreshOwner(ownerTab, false);
    }
  }, [session, portal, playerTab, ownerTab, ownerSelectedTurfId]);

  function openPicker(title: string, options: PickerChoice[], onSelect: (value: string) => void, subtitle?: string) {
    setPickerState({
      visible: true,
      title,
      subtitle,
      options,
      onSelect,
    });
  }

  function confirmAction(
    title: string,
    message: string,
    confirmLabel: string,
    tone: "primary" | "owner" | "danger",
    onConfirm: () => void
  ) {
    setConfirmState({
      visible: true,
      title,
      message,
      confirmLabel,
      tone,
      onConfirm,
    });
  }

  function showInfo(title: string, lines: string[], subtitle?: string) {
    setInfoSheet({
      visible: true,
      title,
      subtitle,
      lines,
    });
  }

  // === EPIC 22 GLOBAL SPORTS HELPERS ===
  const getTargetCurrencyForCity = (city: string) => {
    switch (city.toLowerCase()) {
      case "dubai": return "AED";
      case "london": return "GBP";
      case "singapore": return "SGD";
      case "sydney": return "AUD";
      default: return "INR";
    }
  };

  const getConvertedPrice = (amount: number, city: string) => {
    const targetCurrency = getTargetCurrencyForCity(city);
    if (targetCurrency === "INR") return `Rs ${amount}`;

    // Find rate in exchangeRates
    const rateObj = exchangeRates.find(
      r => r.baseCurrency === "INR" && r.targetCurrency === targetCurrency
    );
    const rate = rateObj ? rateObj.rate : 1.0;
    const converted = (amount * rate).toFixed(2);
    return `${targetCurrency} ${converted}`;
  };

  const getTimezoneSuffixForCity = (city: string) => {
    switch (city.toLowerCase()) {
      case "dubai": return "GST";
      case "london": return "BST";
      case "singapore": return "SGT";
      case "sydney": return "AEST";
      default: return "IST";
    }
  };

  const formatSlotTimeWithTimezone = (time: string, city: string) => {
    if (!time) return "--";
    const suffix = getTimezoneSuffixForCity(city);
    return `${time.slice(0, 5)} ${suffix}`;
  };

  async function loadGlobalConfigs(lang = selectedLang, city = selectedCity) {
    try {
      const [configs, rates, trans] = await Promise.all([
        getExpansionConfigs().catch(() => [] as any[]),
        getExchangeRates().catch(() => [] as any[]),
        getTranslations(lang).catch(() => ({}) as Record<string, string>),
      ]);
      setExpansionConfigs(configs);
      setExchangeRates(rates);
      setTranslations({ ...trans, ...(mobileTranslationFallbacks[lang] || mobileTranslationFallbacks.en) });

      // Determine tenant ID from selected city
      const matchedCity = configs.find((c: any) => c.cityName.toLowerCase() === city.toLowerCase());
      if (matchedCity && matchedCity.tenantId) {
        const tenantId = matchedCity.tenantId;
        setActiveTenantId(tenantId);
        await AsyncStorage.setItem("tenantId", String(tenantId));

        // Fetch whitelabel branding for resolved tenant
        const brand = await getBranding(tenantId).catch(() => null);
        if (brand) {
          setWhitelabelBranding(brand);
          if (brand.primaryColor) {
            setPrimaryColor(brand.primaryColor.startsWith("hsl") || brand.primaryColor.startsWith("#") ? brand.primaryColor : `hsl(${brand.primaryColor})`);
          }
          if (brand.secondaryColor) {
            setSecondaryColor(brand.secondaryColor.startsWith("hsl") || brand.secondaryColor.startsWith("#") ? brand.secondaryColor : `hsl(${brand.secondaryColor})`);
          }
        }
      } else {
        setActiveTenantId(1);
        await AsyncStorage.setItem("tenantId", "1");
        setPrimaryColor(theme.colors.player);
        setSecondaryColor(theme.colors.playerSoft);
      }
    } catch (err) {
      console.log("Global configs load error", err);
    }
  }

  async function bootstrap() {
    try {
      // 1. Jailbreak / Root detection warning
      const isJailbroken = await checkJailbroken();
      if (isJailbroken) {
        Alert.alert(
          "Security Hardening Active",
          "Your mobile device has signature warnings (jailbroken/rooted). Operations will proceed under strict security isolation."
        );
      }

      // 2. Network connectivity check
      const isConnected = await checkConnectivity();
      if (!isConnected) {
        // Load local offline SQLite simulation databases into state
        const cachedProfile = await getCachedProfile();
        const cachedBookings = await getCachedBookings();
        const cachedFavorites = await getCachedFavorites();
        const cachedHistory = await getCachedHistory();
        if (cachedProfile) setProfile(cachedProfile);
        if (cachedBookings.length > 0) setBookings(cachedBookings);
        if (cachedFavorites.length > 0) setFavorites(cachedFavorites);
        if (cachedHistory.length > 0) setMatchHistory(cachedHistory);
        Alert.alert("Offline Cache Active", "Internet unavailable. Loaded User Profile and Bookings from local cache.");
      }

      // 3. FCM push alerts registration
      initializeFCM((notification) => {
        setNotifications((current) => [
          {
            id: Math.floor(Math.random() * 1000000),
            title: notification.title,
            message: notification.body,
            status: "SENT",
            type: notification.type,
            createdAt: new Date().toISOString(),
          },
          ...current,
        ]);
        Alert.alert(notification.title, notification.body);
      });

      setupNotificationHandlers((type) => {
        if (type === "BOOKING") setPlayerTab("bookings");
        else if (type === "PAYMENT") setPlayerTab("wallet");
        else if (type === "MATCH") setPlayerTab("matches");
        else if (type === "TOURNAMENT") setPlayerTab("tournaments");
      });

      const storedSession = await getStoredSession();
      const biometricsOn = await isBiometricEnabled();
      setBiometricEnabled(biometricsOn);
      if (!storedSession) {
        setBooting(false);
        return;
      }
      const unlocked = biometricsOn ? await unlockWithBiometrics() : true;
      if (!unlocked) {
        setBooting(false);
        return;
      }
      setSession(storedSession);
      setPortal(storedSession.role === "OWNER" || storedSession.role === "ADMIN" ? "owner" : "player");

      // 4. Flush offline queued items
      if (isConnected) {
        const syncRes = await syncOfflineQueue();
        if (syncRes.syncedCount > 0) {
          Alert.alert("Synchronized", `Synced ${syncRes.syncedCount} queued booking/profile changes.`);
        }
      }
    } finally {
      setBooting(false);
    }
  }

  async function handleLogin() {
    try {
      setAuthenticating(true);
      const nextSession = await login(loginId, password);
      setSession(nextSession);
      setPortal(nextSession.role === "OWNER" || nextSession.role === "ADMIN" ? "owner" : "player");
    } catch (error) {
      Alert.alert("Login failed", errorMessage(error));
    } finally {
      setAuthenticating(false);
    }
  }

  async function refreshPlayer(tab = playerTab, showSpinner = true) {
    try {
      if (showSpinner) setBusy(true);
      if (tab === "search") {
        const [results, favoriteData] = await Promise.all([
          searchTurfs(searchFilters),
          getFavorites().catch(() => [] as Turf[]),
        ]);
        setSearchResults(results);
        setFavorites(favoriteData);
        await cacheFavorites(favoriteData);
        if (results.length > 0 && !selectedTurf) {
          await openTurf(results[0].id ?? 0);
        }
      }
      if (tab === "favorites") {
        const favoriteData = await getFavorites().catch(() => [] as Turf[]);
        setFavorites(favoriteData);
        await cacheFavorites(favoriteData);
      }
      if (tab === "teams") {
        const [teamData, analytics, discoverData] = await Promise.all([
          getMyTeams(),
          getTeamAnalytics().catch(() => null),
          discoverTeams({ openOnly: true }).catch(() => [] as Team[]),
        ]);
        setTeams(teamData);
        setTeamAnalytics(analytics);
        setDiscoverableTeams(discoverData);
      }
      if (tab === "matches") {
        const [matchData, teamData, recommendationData, recruitmentData, nearbyData] = await Promise.all([
          getOpenMatches(),
          getMyTeams().catch(() => [] as Team[]),
          getMatchRecommendations().catch(() => [] as MatchmakingMatch[]),
          getOpenRecruitments().catch(() => [] as TeamRecruitment[]),
          getNearbyPlayers().catch(() => [] as PlayerDiscover[])
        ]);
        setMatches(matchData);
        setTeams(teamData);
        setMatchRecommendations(recommendationData);
        setTeamRecruitments(recruitmentData);
        setNearbyPlayers(nearbyData);
      }
      if (tab === "feed") {
        const [historyData, feedData, networkData, analyticsData] = await Promise.all([
          getMyMatchHistory(),
          getSocialFeed(),
          getMyNetwork().catch(() => [] as PlayerConnection[]),
          getCommunityAnalytics().catch(() => null),
        ]);
        setMatchHistory(historyData);
        await cacheHistory(historyData);
        setSocialFeed(feedData);
        setPlayerNetwork(networkData);
        setCommunityAnalytics(analyticsData);
      }
      if (tab === "tournaments") {
        const [tournamentData, teamData] = await Promise.all([
          getOpenTournaments(),
          getMyTeams().catch(() => [] as Team[])
        ]);
        setTournaments(tournamentData);
        setTeams(teamData);
      }
      if (tab === "scoring") {
        setLiveScores(await getLiveScores());
      }
      if (tab === "foryou") {
        setPersonalization(await getPersonalization());
      }
      if (tab === "bookings") {
        const bookingsData = await getMyBookings();
        setBookings(bookingsData);
        await cacheBookings(bookingsData);
      }
      if (tab === "rebook") {
        const [bookingData, frequentData, predictionData, recurringData, templateData, waitlistData, intelligenceData] = await Promise.all([
          getMyBookings().catch(() => [] as Booking[]),
          getFrequentBookings().catch(() => [] as FrequentBooking[]),
          getBookingPredictions().catch(() => [] as BookingPrediction[]),
          getRecurringBookings().catch(() => [] as RecurringBooking[]),
          getBookingTemplates().catch(() => [] as BookingTemplate[]),
          getAutoWaitlists().catch(() => [] as AutoWaitlist[]),
          getBookingIntelligence().catch(() => null),
        ]);
        setBookings(bookingData);
        setFrequentBookings(frequentData);
        setBookingPredictions(predictionData);
        setRecurringBookings(recurringData);
        setBookingTemplates(templateData);
        setAutoWaitlists(waitlistData);
        setBookingIntelligence(intelligenceData);
      }
      if (tab === "weather") {
        setWeatherDashboard(await getWeatherDashboard(weatherCity).catch(() => null));
      }
      if (tab === "progress") {
        const [gamificationData, performanceData, leaderboardData] = await Promise.all([
          getGamificationProfile().catch(() => null),
          getPlayerPerformance().catch(() => null),
          getPlayerPerformanceLeaderboard({ type: "IMPACT", limit: 5 }).catch(() => [] as PlayerPerformanceLeaderboardEntry[]),
        ]);
        setGamificationProfile(gamificationData);
        setPlayerPerformance(performanceData);
        setPerformanceLeaderboard(leaderboardData);
      }
      if (tab === "career") {
        const [talentData, opportunityData] = await Promise.all([
          getCareerMarketplace({ city: selectedCity, sport: "CRICKET" }).catch(() => [] as CareerMap[]),
          getCareerOpportunities({ sport: "CRICKET" }).catch(() => [] as CareerMap[]),
        ]);
        setCareerMarketplace(talentData);
        setCareerOpportunities(opportunityData);
      }
      if (tab === "governance") {
        const [federationData, rankingData] = await Promise.all([
          getGovernanceFederations({ sport: "CRICKET" }).catch(() => [] as GovernanceMap[]),
          getGovernanceRankings({ sport: "CRICKET" }).catch(() => [] as GovernanceMap[]),
        ]);
        setGovernanceFederations(federationData);
        setGovernanceRankings(rankingData);
      }
      if (tab === "superapp") {
        setSuperAppDashboard(await getSuperAppDashboard().catch(() => null));
      }
      if (tab === "sportsos") {
        const [dashboardData, facilityData] = await Promise.all([
          getSportsOsDashboard().catch(() => null),
          searchSportsOsFacilities().catch(() => []),
        ]);
        setSportsOsDashboard(dashboardData);
        setSportsOsFacilities(facilityData);
      }
      if (tab === "faneconomy") {
        setFanEconomyDashboard(await getFanEconomyDashboard().catch(() => null));
      }
      if (tab === "splits") {
        const [bookingsData, splitData] = await Promise.all([getMyBookings(), getMySplitPayments()]);
        setBookings(bookingsData);
        await cacheBookings(bookingsData);
        setSplitPayments(splitData);
      }
      if (tab === "wallet") {
        const [walletData, couponData, growthData] = await Promise.all([
          getWallet(),
          getCoupons(),
          getGrowthDashboard().catch(() => null),
        ]);
        setWallet(walletData);
        setCoupons(couponData);
        setGrowthDashboard(growthData);
      }
      if (tab === "membership") {
        const [plansRes, myMembershipsRes, growthData] = await Promise.all([
          getMembershipPlans(),
          getMyMemberships(),
          getGrowthDashboard().catch(() => null),
        ]);
        setPlans(plansRes);
        setMyMemberships(myMembershipsRes);
        setGrowthDashboard(growthData);
      }
      if (tab === "marketplace") {
        const [listings, recommended, mine, waitlist, products, cart, packages, events, accommodations] = await Promise.all([
          getResaleListings(selectedCity).catch(() => [] as ResaleListing[]),
          getRecommendedResaleListings().catch(() => [] as ResaleListing[]),
          getMyResaleListings().catch(() => [] as ResaleListing[]),
          getMyResaleWaitlist().catch(() => [] as SlotWaitlist[]),
          getCommerceProducts({ sport: "Cricket" }).catch(() => [] as CommerceProduct[]),
          getCommerceCart().catch(() => null),
          getTravelPackages({ sport: "Cricket" }).catch(() => [] as TravelPackage[]),
          getTravelEvents({ sport: "Cricket", city: selectedCity }).catch(() => [] as TravelEvent[]),
          getTravelAccommodations({ city: selectedCity }).catch(() => [] as TravelAccommodation[]),
        ]);
        setResaleListings(listings);
        setRecommendedResaleListings(recommended);
        setMyResaleListings(mine);
        setMyResaleWaitlist(waitlist);
        setCommerceProducts(products);
        setCommerceCart(cart);
        setTravelPackages(packages);
        setTravelEvents(events);
        setTravelAccommodations(accommodations);
      }
      if (tab === "smart") {
        setSmartData(await getSmartRecommendations({ city: smartCity || undefined, turfId: smartTurfId ? Number(smartTurfId) : undefined }));
      }
      if (tab === "notifications") {
        const [items, summary, announcements] = await Promise.all([
          getNotifications().catch(() => []),
          getNotificationSummary().catch(() => null),
          getActiveAnnouncements().catch(() => []),
        ]);
        setNotifications(items);
        setNotificationSummary(summary);
        setNotificationAnnouncements(announcements);
      }
      if (tab === "settings") {
        setPlayerSettings(await getPlayerSettings());
      }
      if (tab === "profile") {
        const [profileData, bookingData, reviewData, availabilityData] = await Promise.all([getProfile(), getMyBookings(), getUserReviews(), getMyAvailability()]);
        setProfile(profileData);
        await cacheProfile(profileData);
        setBookings(bookingData);
        await cacheBookings(bookingData);
        setProfileReviews(reviewData);
        setPlayerAvailability(availabilityData);
        setProfileDraft({
          name: profileData.name || "",
          email: profileData.email || "",
          phone: profileData.phone || "",
        });
        setAvailabilityDraft({
          availableDays: availabilityData.availableDays || "",
          preferredWindow: availabilityData.preferredWindow || "",
          homeCity: availabilityData.homeCity || "",
          canTravel: availabilityData.canTravel ?? true,
          notes: availabilityData.notes || "",
        });
      }
    } catch (error) {
      Alert.alert("Load failed", errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function refreshOwner(tab = ownerTab, showSpinner = true) {
    try {
      if (showSpinner) setBusy(true);
      if (tab === "dashboard") setOwnerDashboard(await getOwnerDashboard());
      if (tab === "insights") setOwnerInsights(await getOwnerInsights());
      if (tab === "rebook") setBookingIntelligence(await getOwnerBookingIntelligence());
      if (tab === "weather") {
        const [dashboard, policy] = await Promise.all([
          getOwnerWeatherDashboard().catch(() => null),
          getOwnerWeatherPolicy().catch(() => null),
        ]);
        setWeatherDashboard(dashboard);
        setWeatherPolicy(policy);
      }
      if (tab === "engagement") setGamificationAnalytics(await getOwnerGamificationAnalytics());
      if (tab === "payments") {
        const [rules, analytics] = await Promise.all([getOwnerPaymentRules(), getSplitAnalytics()]);
        setPaymentRules(rules);
        setSplitAnalytics(analytics);
      }
      if (tab === "turfs") {
        const turfs = await getOwnerTurfs();
        setOwnerTurfs(turfs);
        if (!ownerSelectedTurfId && turfs[0]?.id) setOwnerSelectedTurfId(turfs[0].id);
      }
      if (tab === "slots") {
        const turfs = ownerTurfs.length > 0 ? ownerTurfs : await getOwnerTurfs();
        if (ownerTurfs.length === 0) setOwnerTurfs(turfs);
        const turfId = ownerSelectedTurfId || turfs[0]?.id || null;
        setOwnerSelectedTurfId(turfId);
        if (turfId) {
          const [slots, availability] = await Promise.all([getTurfSlots(turfId), getAvailability(turfId, slotManagerDate)]);
          setOwnerSlots(slots);
          setOwnerSlotAvailability(availability);
        }
      }
      if (tab === "bookings") setOwnerBookings(await getOwnerBookings());
      if (tab === "earnings") {
        const [bookings, finance] = await Promise.all([
          getOwnerBookings(),
          getOwnerFinanceDashboard().catch(() => null),
        ]);
        setOwnerBookings(bookings);
        setOwnerFinance(finance);
      }
      if (tab === "reviews") setOwnerReviews(await getOwnerReviews());
      if (tab === "community") setMatchmakingAnalytics(await getMatchmakingAnalytics());
      if (tab === "teams") setTeamAnalytics(await getTeamAnalytics());
      if (tab === "resale") {
        const [analytics, rules] = await Promise.all([getOwnerResaleAnalytics(), getOwnerResaleRules()]);
        setResaleAnalytics(analytics);
        setResaleRules(rules);
      }
      if (tab === "settings") {
        const [profileData, settingsData] = await Promise.all([getProfile(), getOwnerSettings()]);
        setProfile(profileData);
        setOwnerSettings(settingsData);
        setProfileDraft({
          name: profileData.name || "",
          email: profileData.email || "",
          phone: profileData.phone || "",
        });
      }
    } catch (error) {
      Alert.alert("Load failed", errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function refreshAdmin(tab = adminTab, showSpinner = true) {
    try {
      if (showSpinner) setBusy(true);
      if (tab === "analytics") {
        setAdminAnalytics(await getAdminAnalytics());
        setAdminBIWorkspace(await getAdminBIWorkspace().catch(() => null));
        setAdminFraudAlerts(await getFraudAlerts().catch(() => []));
        setAdminAuditLogs(await getOpsAuditLogs().catch(() => []));
        setAdminGovernance(await getAdminGovernanceDashboard().catch(() => null));
      }
      if (tab === "ownerInsights") setOwnerInsights(await getAdminOwnerInsights());
      if (tab === "bookingIntelligence") setBookingIntelligence(await getAdminBookingIntelligence());
      if (tab === "weather") setWeatherDashboard(await getAdminWeatherDashboard(weatherCity));
      if (tab === "gamification") setGamificationAnalytics(await getAdminGamificationAnalytics());
      if (tab === "payments") setSplitAnalytics(await getAdminPaymentMonitoring());
      if (tab === "epics") setPlatformEpics(await getPlatformEpics().catch(() => []));
      if (tab === "enterprise") {
        const [enterpriseData, centerData, staffData, franchiseData, reportData, governanceData] = await Promise.all([
          getAdminEnterpriseDashboard().catch(() => null),
          getAdminEnterpriseCenters().catch(() => []),
          getAdminEnterpriseStaff().catch(() => []),
          getAdminEnterpriseFranchises().catch(() => []),
          getAdminEnterpriseReports().catch(() => null),
          getAdminEnterpriseGovernance().catch(() => null),
        ]);
        setAdminEnterprise(enterpriseData);
        setAdminEnterpriseCenters(centerData);
        setAdminEnterpriseStaff(staffData);
        setAdminEnterpriseFranchises(franchiseData);
        setAdminEnterpriseReports(reportData);
        setAdminEnterpriseGovernance(governanceData);
      }
      if (tab === "monetization") {
        const [dashboardData, brandData, opportunityData, proposalData, campaignData, leadData, contractData, invoiceData, reportData] = await Promise.all([
          getAdminMonetizationDashboard().catch(() => null),
          getAdminMonetizationBrands().catch(() => []),
          getAdminMonetizationOpportunities().catch(() => []),
          getAdminMonetizationProposals().catch(() => []),
          getAdminMonetizationCampaigns().catch(() => []),
          getAdminMonetizationLeads().catch(() => []),
          getAdminMonetizationContracts().catch(() => []),
          getAdminMonetizationInvoices().catch(() => []),
          getAdminMonetizationReports().catch(() => null),
        ]);
        setAdminMonetization(dashboardData);
        setAdminMonetizationBrands(brandData);
        setAdminMonetizationOpportunities(opportunityData);
        setAdminMonetizationProposals(proposalData);
        setAdminMonetizationCampaigns(campaignData);
        setAdminMonetizationLeads(leadData);
        setAdminMonetizationContracts(contractData);
        setAdminMonetizationInvoices(invoiceData);
        setAdminMonetizationReports(reportData);
      }
      if (tab === "sports") {
        const [dashboardData, catalogData, analyticsData, auditData] = await Promise.all([
          getAdminSportsDashboard().catch(() => null),
          getAdminSportsCatalog().catch(() => []),
          getAdminSportsAnalytics().catch(() => null),
          getAdminSportsAudit().catch(() => []),
        ]);
        setAdminSports(dashboardData);
        setAdminSportsCatalog(catalogData);
        setAdminSportsAnalytics(analyticsData);
        setAdminSportsAudit(auditData);
      }
      if (tab === "developers") {
        const [dashboardData, developerData, appData, keyData, webhookData, analyticsData, billingData, marketplaceData, auditData] = await Promise.all([
          getAdminDeveloperPlatformDashboard().catch(() => null),
          getAdminDevelopers().catch(() => []),
          getAdminDeveloperApplications().catch(() => []),
          getAdminDeveloperApiKeys().catch(() => []),
          getAdminDeveloperWebhooks().catch(() => []),
          getAdminDeveloperAnalytics().catch(() => null),
          getAdminDeveloperBilling().catch(() => null),
          getAdminDeveloperMarketplace().catch(() => []),
          getAdminDeveloperAudit().catch(() => []),
        ]);
        setAdminDeveloperPlatform(dashboardData);
        setAdminDevelopers(developerData);
        setAdminDeveloperApplications(appData);
        setAdminDeveloperApiKeys(keyData);
        setAdminDeveloperWebhooks(webhookData);
        setAdminDeveloperAnalytics(analyticsData);
        setAdminDeveloperBilling(billingData);
        setAdminDeveloperMarketplace(marketplaceData);
        setAdminDeveloperAudit(auditData);
      }
      if (tab === "academy") {
        const [dashboardData, academyData, coachData, programData, batchData, enrollmentData, attendanceData, assessmentData, videoData, talentData, certificateData, analyticsData, revenueData, auditData] = await Promise.all([
          getAdminAcademyDashboard().catch(() => null),
          getAdminAcademies().catch(() => []),
          getAdminCoaches().catch(() => []),
          getAdminTrainingPrograms().catch(() => []),
          getAdminTrainingBatches().catch(() => []),
          getAdminAcademyEnrollments().catch(() => []),
          getAdminAcademyAttendance().catch(() => []),
          getAdminAcademyAssessments().catch(() => []),
          getAdminAcademyVideos().catch(() => []),
          getAdminTalentProfiles().catch(() => []),
          getAdminAcademyCertificates().catch(() => []),
          getAdminAcademyAnalytics().catch(() => null),
          getAdminAcademyRevenue().catch(() => null),
          getAdminAcademyAudit().catch(() => []),
        ]);
        setAdminAcademy(dashboardData);
        setAdminAcademies(academyData);
        setAdminCoaches(coachData);
        setAdminTrainingPrograms(programData);
        setAdminTrainingBatches(batchData);
        setAdminAcademyEnrollments(enrollmentData);
        setAdminAcademyAttendance(attendanceData);
        setAdminAcademyAssessments(assessmentData);
        setAdminAcademyVideos(videoData);
        setAdminTalentProfiles(talentData);
        setAdminAcademyCertificates(certificateData);
        setAdminAcademyAnalytics(analyticsData);
        setAdminAcademyRevenue(revenueData);
        setAdminAcademyAudit(auditData);
      }
      if (tab === "corporate") {
        const [dashboardData, companyData, locationData, departmentData, employeeData, teamData, membershipData, bookingPlanData, tournamentData, wellnessData, activityData, rewardData, invoiceData, contractData, leagueData, analyticsData, billingData, auditData] = await Promise.all([
          getAdminCorporateDashboard().catch(() => null),
          getAdminCorporateCompanies().catch(() => []),
          getAdminCorporateLocations().catch(() => []),
          getAdminCorporateDepartments().catch(() => []),
          getAdminCorporateEmployees().catch(() => []),
          getAdminCorporateTeams().catch(() => []),
          getAdminCorporateTeamMemberships().catch(() => []),
          getAdminCorporateBookingPlans().catch(() => []),
          getAdminCorporateTournaments().catch(() => []),
          getAdminCorporateWellnessPrograms().catch(() => []),
          getAdminCorporateWellnessActivities().catch(() => []),
          getAdminCorporateRewards().catch(() => []),
          getAdminCorporateInvoices().catch(() => []),
          getAdminCorporateContracts().catch(() => []),
          getAdminCorporateLeagues().catch(() => []),
          getAdminCorporateAnalytics().catch(() => null),
          getAdminCorporateBilling().catch(() => null),
          getAdminCorporateAudit().catch(() => []),
        ]);
        setAdminCorporate(dashboardData);
        setAdminCorporateCompanies(companyData);
        setAdminCorporateLocations(locationData);
        setAdminCorporateDepartments(departmentData);
        setAdminCorporateEmployees(employeeData);
        setAdminCorporateTeams(teamData);
        setAdminCorporateMemberships(membershipData);
        setAdminCorporateBookingPlans(bookingPlanData);
        setAdminCorporateTournaments(tournamentData);
        setAdminCorporateWellnessPrograms(wellnessData);
        setAdminCorporateWellnessActivities(activityData);
        setAdminCorporateRewards(rewardData);
        setAdminCorporateInvoices(invoiceData);
        setAdminCorporateContracts(contractData);
        setAdminCorporateLeagues(leagueData);
        setAdminCorporateAnalytics(analyticsData);
        setAdminCorporateBilling(billingData);
        setAdminCorporateAudit(auditData);
      }
      if (tab === "saas") {
        const dashboardData = await getAdminSaasDashboard().catch(() => null);
        setAdminSaasDashboard(dashboardData);
        const firstTenant = dashboardData?.tenants?.[0];
        if (firstTenant?.id) {
          setAdminSaasTenantId(String(firstTenant.id));
        }
      }
      if (tab === "owners") setAdminOwners(await getAdminOwners());
      if (tab === "turfs") setAdminTurfs(await getAdminTurfs());
      if (tab === "reviews") setAdminReviews(await getAdminReviews());
      if (tab === "campaigns") {
        const [campaignData, growthData, automationData] = await Promise.all([
          getCampaigns(),
          getAdminGrowthDashboard().catch(() => null),
          getAdminGrowthAutomation().catch(() => []),
        ]);
        setAdminCampaigns(campaignData);
        setAdminGrowth(growthData);
        setAdminGrowthAutomation(automationData);
      }
      if (tab === "bookings") setAdminBookings(await getOpsBookings().catch(() => []));
      if (tab === "support") setAdminSupportTickets(await getSupportTickets().catch(() => []));
      if (tab === "incidents") setAdminIncidents(await getIncidents().catch(() => []));
      if (tab === "matchmaking") setMatchmakingAnalytics(await getMatchmakingAnalytics());
      if (tab === "teams") setTeamAnalytics(await getAdminTeamAnalytics());
      if (tab === "community") setCommunityAnalytics(await getAdminCommunityModeration());
      if (tab === "resale") setResaleAnalytics(await getAdminResaleAnalytics());
    } catch (error) {
      Alert.alert("Admin Load Failed", errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function refreshStaff(tab = staffTab, showSpinner = true) {
    try {
      if (showSpinner) setBusy(true);
      if (tab === "dashboard") {
        const [dashboardData, analyticsData] = await Promise.all([
          getStaffOpsDashboard().catch(() => null),
          getStaffOpsAnalytics().catch(() => null),
        ]);
        setStaffDashboard(dashboardData);
        setStaffAnalytics(analyticsData);
        setStaffSchedule(dashboardData?.schedule || []);
        setStaffTasks(dashboardData?.tasks || []);
        setStaffMaintenance(dashboardData?.maintenance || []);
      }
      if (tab === "checkins") setStaffBookings(await getStaffOpsBookings().catch(() => []));
      if (tab === "tasks") setStaffTasks(await getStaffOpsTasks().catch(() => []));
      if (tab === "maintenance") setStaffMaintenance(await getStaffOpsMaintenance().catch(() => []));
      if (tab === "attendance") setStaffAttendance(await getStaffOpsAttendance().catch(() => []));
      if (tab === "equipment") setStaffEquipment(await getStaffOpsEquipment().catch(() => []));
      if (tab === "emergency") setStaffEmergencies(await getStaffOpsEmergencies().catch(() => []));
      if (tab !== "dashboard") {
        setStaffAnalytics(await getStaffOpsAnalytics().catch(() => null));
      }
    } catch (error) {
      Alert.alert("Staff Load Failed", errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function onRefresh() {
    setRefreshing(true);
    if (portal === "player") {
      await refreshPlayer(playerTab, false);
    } else if (portal === "owner") {
      await refreshOwner(ownerTab, false);
    } else if (portal === "staff") {
      await refreshStaff(staffTab, false);
    } else {
      await refreshAdmin(adminTab, false);
    }
    setRefreshing(false);
  }

  async function openTurf(turfId: number) {
    if (!turfId) return;
    try {
      setBusy(true);
      const [detail, availability, favoriteState] = await Promise.all([
        getTurfDetail(turfId),
        getAvailability(turfId, searchFilters.date || today),
        getFavoriteStatus(turfId).catch(() => ({ favorite: false })),
      ]);
      setSelectedTurf(detail);
      setSelectedAvailability(availability);
      setSelectedTurfIsFavorite(Boolean(favoriteState.favorite ?? favoriteState.isFavorite));
      setSelectedSlotId(availability.find((slot) => slot.available && !slot.blocked)?.slotId ?? null);
    } catch (error) {
      Alert.alert("Turf load failed", errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function runAction(action: () => Promise<unknown>, successMessage: string, refreshCurrent = true) {
    try {
      setBusy(true);
      await action();
      Alert.alert("Success", successMessage);
      if (refreshCurrent) {
        if (portal === "player") await refreshPlayer(playerTab, false);
        else if (portal === "owner") await refreshOwner(ownerTab, false);
        else if (portal === "staff") await refreshStaff(staffTab, false);
        else await refreshAdmin(adminTab, false);
      }
    } catch (error) {
      Alert.alert("Action failed", errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function saveMobileSetting(payload: Partial<PlayerSettings>) {
    await runAction(async () => {
      const next = await updatePlayerSettings(payload);
      setPlayerSettings(next);
    }, "Settings saved.", false);
  }

  async function saveOwnerSetting(payload: Partial<OwnerSettings>) {
    await runAction(async () => {
      const next = await updateOwnerSettings(payload);
      setOwnerSettings(next);
    }, "Owner settings saved.", false);
  }

  async function loadMatchCenter(scoreId: number) {
    try {
      setBusy(true);
      const detail = await getLiveScore(scoreId);
      setLiveScoreDetail(detail);
      setSelectedLiveScoreId(scoreId);
      try {
        const analytics = await getMatchAnalytics(scoreId);
        setAnalyticsData(analytics);
      } catch (ae) {
        console.log("Analytics loading failed", ae);
      }
    } catch (err) {
      Alert.alert("Load failed", errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const paidOwnerBookings = useMemo(
    () => ownerBookings.filter((booking) => ["CONFIRMED", "CHECKED_IN", "COMPLETED", "NO_SHOW"].includes(String(booking.status))),
    [ownerBookings]
  );

  const confirmedOwnerRevenue = useMemo(
    () => ownerFinance?.netCollectedRevenue ?? paidOwnerBookings.reduce((sum, booking) => sum + Number(booking.amount ?? booking.totalAmount ?? 0), 0),
    [ownerFinance, paidOwnerBookings]
  );

  const selectedOwnerTurf = useMemo(
    () => ownerTurfs.find((item) => item.id === ownerSelectedTurfId) || null,
    [ownerTurfs, ownerSelectedTurfId]
  );
  const filteredSearchResults = useMemo(() => {
    if (!selectedSport || selectedSport === "All") {
      return searchResults;
    }
    const sport = selectedSport.toLowerCase();
    return searchResults.filter((turf) => {
      const matchesName = turf.name.toLowerCase().includes(sport);
      const matchesDesc = (turf.description || "").toLowerCase().includes(sport);
      const matchesSport = (turf as any).sports?.some((item: any) => {
        const value = typeof item === "string" ? item : item?.name ?? item?.code;
        return String(value || "").toLowerCase() === sport;
      });
      return matchesName || matchesDesc || matchesSport;
    });
  }, [searchResults, selectedSport]);
  const currentBottomTabs = useMemo<Array<MobileNavItem<AnyMobileTab>>>(() => {
    if (portal === "owner") return ownerBottomTabs;
    if (portal === "admin") return adminBottomTabs;
    if (portal === "staff") return staffBottomTabs;
    return playerBottomTabs;
  }, [adminBottomTabs, ownerBottomTabs, playerBottomTabs, portal, staffBottomTabs]);
  const currentQuickActions = useMemo<Array<MobileNavItem<AnyMobileTab>>>(() => {
    if (portal === "owner") return ownerQuickActions;
    if (portal === "admin") return adminQuickActions;
    if (portal === "staff") return staffQuickActions;
    return playerQuickActions;
  }, [adminQuickActions, ownerQuickActions, playerQuickActions, portal, staffQuickActions]);
  const currentFeatureGroups = useMemo<Array<MobileFeatureGroup<AnyMobileTab>>>(() => {
    if (portal === "owner") return ownerFeatureGroups;
    if (portal === "admin") return adminFeatureGroups;
    if (portal === "staff") return staffFeatureGroups;
    return playerFeatureGroups;
  }, [adminFeatureGroups, ownerFeatureGroups, playerFeatureGroups, portal, staffFeatureGroups]);
  const currentTabValue = (portal === "owner" ? ownerTab : portal === "admin" ? adminTab : portal === "staff" ? staffTab : playerTab) as AnyMobileTab;
  const currentTone = portal === "player" ? "player" : "owner";
  const currentPortalLabel =
    portal === "owner"
      ? t("owner_portal", "Owner Portal")
      : portal === "admin"
        ? t("super_admin", "Super Admin")
        : portal === "staff"
          ? t("staff_portal", "Staff Portal")
          : t("player_portal", "Player Portal");

  function selectPortalTab(tab: AnyMobileTab) {
    if (portal === "owner") {
      setOwnerTab(tab as OwnerTab);
      return;
    }
    if (portal === "admin") {
      setAdminTab(tab as AdminTab);
      return;
    }
    if (portal === "staff") {
      setStaffTab(tab as StaffTab);
      return;
    }
    setPlayerTab(tab as PlayerTab);
  }
  const sessionUser = session?.user;

  if (booting) {
    return (
      <ThemeProvider value={activeTheme}>
        <SafeAreaView style={[appStyles.shell, styles.shellInset]}>
          <StatusBar style={statusBarStyle} />
          <View style={styles.centered}>
            <ActivityIndicator color={activeTheme.colors.player} />
            <Text style={[styles.bootText, { color: activeTheme.colors.muted }]}>Loading Cricbook mobile workspace...</Text>
          </View>
        </SafeAreaView>
      </ThemeProvider>
    );
  }

  if (!session) {
    return (
      <ThemeProvider value={activeTheme}>
      <SafeAreaView style={[appStyles.shell, styles.shellInset]}>
        <StatusBar style={statusBarStyle} />
        <ScrollView contentContainerStyle={styles.authWrap}>
          <ThemeModeControl mode={themeMode} onChange={updateThemeMode} palette={activeTheme} labels={{ light: t("light", "Light"), dark: t("dark", "Dark"), auto: t("auto", "Auto") }} />
          <Surface tone="player">
            <Text style={styles.brand}>Cricbook</Text>
            <Text style={styles.authSubtitle}>
              {t("auth_subtitle", "Website-level cricket workflows, now shaped for mobile. Player and owner portals are separated after login.")}
            </Text>
            <LabeledInput label={t("email_or_phone", "Email or phone")} value={loginId} onChangeText={setLoginId} placeholder="you@example.com" keyboardType="email-address" />
            <LabeledInput label={t("password", "Password")} value={password} onChangeText={setPassword} placeholder={t("password", "Password")} secureTextEntry />
            <AppButton label={authenticating ? t("signing_in", "Signing in...") : t("login", "Login")} onPress={handleLogin} disabled={authenticating} />
            <View style={styles.previewRow}>
              <Chip label={t("player_portal", "Player Portal")} active tone="player" />
              <Chip label={t("owner_portal", "Owner Portal")} active tone="owner" />
            </View>
            <Text style={styles.helperText}>
              Biometric unlock stays available once you enable it from inside the app.
            </Text>
          </Surface>
        </ScrollView>
      </SafeAreaView>
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider value={activeTheme}>
    <SafeAreaView style={[appStyles.shell, styles.shellInset]}>
      <StatusBar style={statusBarStyle} />
      <Screen>
        <View style={[styles.header, appStyles.header]}>
          <View style={styles.brandBlock}>
            <View style={[
              styles.brandBadge, 
              portal === "owner" ? styles.brandBadgeOwner : portal === "admin" ? styles.brandBadgeAdmin : styles.brandBadgePlayer
            ]}>
              <Text style={styles.brandBadgeText}>CB</Text>
            </View>
            <View>
              <Text style={[styles.brand, { color: activeTheme.colors.text }]}>Cricbook</Text>
              <Text style={[styles.portalLabel, { color: activeTheme.colors.muted }]}>{currentPortalLabel}</Text>
            </View>
          </View>
          <View style={styles.headerActions}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Search and actions"
              accessibilityHint="Open universal shortcuts for this portal"
              hitSlop={{ top: 8, right: 8, bottom: 8, left: 8 }}
              onPress={() => setFeatureSheetVisible(true)}
              style={[styles.commandButton, { borderColor: activeTheme.colors.border }]}
            >
              <Text style={[styles.commandText, { color: activeTheme.colors.text }]}>Actions</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t("logout", "Logout")}
              accessibilityHint="Sign out of Cricbook"
              hitSlop={{ top: 8, right: 8, bottom: 8, left: 8 }}
              onPress={async () => { await logout(); setSession(null); }}
              style={[styles.logoutButton, { borderColor: activeTheme.colors.border }]}
            >
              <Text style={[styles.logoutText, { color: activeTheme.colors.text }]}>{t("logout", "Logout")}</Text>
            </Pressable>
          </View>
        </View>

        <View style={styles.portalSwitch}>
          <Chip label={t("player", "Player")} active={portal === "player"} onPress={() => setPortal("player")} tone="player" />
          {canAccessOwnerPortal ? (
            <Chip label={t("owner", "Owner")} active={portal === "owner"} onPress={() => setPortal("owner")} tone="owner" />
          ) : null}
          <Chip label={t("staff", "Staff")} active={portal === "staff"} onPress={() => setPortal("staff")} tone="owner" />
          {session?.role === "ADMIN" ? (
            <Chip label={t("admin", "Admin")} active={portal === "admin"} onPress={() => setPortal("admin")} tone="owner" />
          ) : null}
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={biometricEnabled ? t("biometric_on", "Biometric on") : t("enable_biometric", "Enable biometric")}
            accessibilityHint="Enable Face ID or fingerprint unlock for this device"
            hitSlop={{ top: 8, right: 8, bottom: 8, left: 8 }}
            onPress={async () => {
              try {
                await enableBiometricLogin();
                setBiometricEnabled(true);
                Alert.alert("Biometric enabled", "Face ID / fingerprint unlock is ready for this device.");
              } catch (error) {
                Alert.alert("Biometric setup failed", errorMessage(error));
              }
            }}
          >
            <Text style={styles.inlineAction}>{biometricEnabled ? t("biometric_on", "Biometric on") : t("enable_biometric", "Enable biometric")}</Text>
          </Pressable>
          <ThemeModeControl mode={themeMode} onChange={updateThemeMode} palette={activeTheme} labels={{ light: t("light", "Light"), dark: t("dark", "Dark"), auto: t("auto", "Auto") }} compact />
        </View>

        <KeyboardAvoidingView
          style={styles.content}
          behavior={Platform.OS === "ios" ? "padding" : "height"}
          keyboardVerticalOffset={androidTopInset + 12}
        >
          <ScrollView
            style={styles.content}
            contentContainerStyle={styles.contentInner}
            keyboardShouldPersistTaps="handled"
            refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={activeTheme.colors.player} />}
          >
            {portal === "player" ? (
              <>
                <MobileWorkspaceHero portal={portal} palette={activeTheme} t={t} />
                <View style={styles.globalContextBar}>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`${t("city", "City")}: ${selectedCity}`}
                    accessibilityHint="Change active region for local search and currency"
                    hitSlop={{ top: 8, right: 8, bottom: 8, left: 8 }}
                    onPress={() => {
                      const cityChoices = [
                        { label: "Ahmedabad", value: "Ahmedabad" },
                        { label: "Mumbai", value: "Mumbai" },
                        { label: "Dubai", value: "Dubai" },
                        { label: "London", value: "London" },
                        { label: "Singapore", value: "Singapore" },
                        { label: "Sydney", value: "Sydney" },
                      ];
                      openPicker(
                        t("select_city", "Select City"),
                        cityChoices,
                        async (city) => {
                          setSelectedCity(city);
                          setSearchFilters((current) => ({ ...current, city }));
                          await loadGlobalConfigs(selectedLang, city);
                          const results = await searchTurfs({ ...searchFilters, city });
                          setSearchResults(results);
                        },
                        "Select active region for timezones and currency exchange."
                      );
                    }}
                    style={styles.contextButton}
                  >
                    <Text style={styles.contextButtonText}>{t("city", "City")}: {selectedCity}</Text>
                  </Pressable>

                  <Pressable
                    onPress={() => {
                      const langChoices = [
                        { label: "English", value: "en" },
                        { label: "Hindi", value: "hi" },
                        { label: "Gujarati", value: "gu" },
                        { label: "Arabic", value: "ar" },
                        { label: "French", value: "fr" },
                      ];
                      openPicker(
                        t("select_language", "Select Language"),
                        langChoices,
                        async (lang) => {
                          await updateLanguage(lang);
                        },
                        "Configure app layout translated language."
                      );
                    }}
                    style={styles.contextButton}
                  >
                    <Text style={styles.contextButtonText}>{t("language", "Language")}: {selectedLang.toUpperCase()}</Text>
                  </Pressable>
                </View>

                <QuickActionRail
                  title={t("quick_actions", "Quick actions")}
                  subtitle="Fast access to the highest-frequency player workflows."
                  actions={playerQuickActions}
                  onSelect={(tab) => setPlayerTab(tab)}
                  tone="player"
                />
                {renderPlayerPortal()}
              </>
            ) : portal === "owner" ? (
              <>
                <MobileWorkspaceHero portal={portal} palette={activeTheme} t={t} />
                <QuickActionRail
                  title={t("quick_actions", "Quick actions")}
                  subtitle="Operate the venue without hunting through menus."
                  actions={ownerQuickActions}
                  onSelect={(tab) => setOwnerTab(tab)}
                  tone="owner"
                />
                {renderOwnerPortal()}
              </>
            ) : portal === "staff" ? (
              <>
                <MobileWorkspaceHero portal={portal} palette={activeTheme} t={t} />
                <QuickActionRail
                  title={t("quick_actions", "Quick actions")}
                  subtitle="Ground execution actions for the current shift."
                  actions={staffQuickActions}
                  onSelect={(tab) => setStaffTab(tab)}
                  tone="owner"
                />
                {renderStaffPortal()}
              </>
            ) : (
              <>
                <MobileWorkspaceHero portal={portal} palette={activeTheme} t={t} />
                <QuickActionRail
                  title={t("quick_actions", "Quick actions")}
                  subtitle="Mobile-first admin controls for approvals and operations."
                  actions={adminQuickActions}
                  onSelect={(tab) => setAdminTab(tab)}
                  tone="owner"
                />
                {renderAdminPortal()}
              </>
            )}
          </ScrollView>
        </KeyboardAvoidingView>
        <BottomNavigation
          value={currentTabValue}
          items={currentBottomTabs}
          onChange={selectPortalTab}
          onMore={() => setFeatureSheetVisible(true)}
          moreLabel={t("more", "More")}
          tone={currentTone}
        />
        {busy ? <ActivityIndicator color={portal === "owner" ? activeTheme.colors.owner : activeTheme.colors.player} style={styles.loader} /> : null}
        <OptionPicker
          visible={pickerState.visible}
          title={pickerState.title}
          subtitle={pickerState.subtitle}
          options={pickerState.options}
          onClose={() => setPickerState((current) => ({ ...current, visible: false }))}
          onSelect={(value) => pickerState.onSelect?.(value)}
        />
        <ConfirmDialog
          visible={confirmState.visible}
          title={confirmState.title}
          message={confirmState.message}
          confirmLabel={confirmState.confirmLabel}
          tone={confirmState.tone}
          onCancel={() => setConfirmState((current) => ({ ...current, visible: false }))}
          onConfirm={() => {
            setConfirmState((current) => ({ ...current, visible: false }));
            confirmState.onConfirm?.();
          }}
        />
        <SheetModal
          visible={infoSheet.visible}
          title={infoSheet.title}
          subtitle={infoSheet.subtitle}
          onClose={() => setInfoSheet((current) => ({ ...current, visible: false }))}
        >
          <ScrollView style={styles.sheetContent}>
            {infoSheet.lines.map((line, index) => (
              <Text key={`${line}-${index}`} style={styles.sheetLine}>
                {line}
              </Text>
            ))}
          </ScrollView>
        </SheetModal>
        <FeatureLauncher
          visible={featureSheetVisible}
          title={`${currentPortalLabel} ${t("features", "Features")}`}
          subtitle="All Cricbook capabilities remain available, grouped by job-to-be-done."
          groups={currentFeatureGroups}
          onSelect={selectPortalTab}
          onClose={() => setFeatureSheetVisible(false)}
        />
      </Screen>
    </SafeAreaView>
    </ThemeProvider>
  );
  function renderPlayerPortal() {
    if (playerTab === "teams") {
      return (
        <View style={styles.stack}>
          <Section title="Team Operating System" subtitle="Availability, polls, lineup planning, finance and attendance for your cricket squads.">
            <View style={styles.metricGrid}>
              <Metric label="Teams" value={teamAnalytics?.totalTeams ?? teams.length} tone="player" />
              <Metric label="Members" value={teamAnalytics?.activeMembers ?? 0} tone="player" />
              <Metric label="RSVPs" value={teamAnalytics?.availabilityResponses ?? 0} tone="player" />
              <Metric label="Attendance" value={`${teamAnalytics?.averageAttendanceRate ?? 0}%`} tone="player" />
            </View>
            <Surface tone="player">
              <FieldPair
                left={<LabeledInput label="Team ID" value={teamAvailabilityDraft.teamId} onChangeText={(value) => setTeamAvailabilityDraft((current) => ({ ...current, teamId: value }))} keyboardType="numeric" />}
                right={
                  <SelectField
                    label="Date"
                    value={teamAvailabilityDraft.date}
                    onPress={() => openPicker("Availability date", dateChoices, (value) => setTeamAvailabilityDraft((current) => ({ ...current, date: value })))}
                  />
                }
              />
              <SelectField
                label="Availability"
                value={teamAvailabilityDraft.status}
                onPress={() => openPicker("Availability", [
                  { label: "Available", value: "AVAILABLE" },
                  { label: "Maybe", value: "MAYBE" },
                  { label: "Unavailable", value: "UNAVAILABLE" },
                ], (value) => setTeamAvailabilityDraft((current) => ({ ...current, status: value })))}
              />
              <AppButton
                label="Save team RSVP"
                onPress={() =>
                  runAction(
                    () => setTeamAvailability(Number(teamAvailabilityDraft.teamId), { date: teamAvailabilityDraft.date, status: teamAvailabilityDraft.status as "AVAILABLE" | "MAYBE" | "UNAVAILABLE" }),
                    "Team availability saved."
                  )
                }
              />
            </Surface>
            <Surface tone="player">
              <FieldPair
                left={<LabeledInput label="Team ID" value={teamPollDraft.teamId} onChangeText={(value) => setTeamPollDraft((current) => ({ ...current, teamId: value }))} keyboardType="numeric" />}
                right={<LabeledInput label="Question" value={teamPollDraft.question} onChangeText={(value) => setTeamPollDraft((current) => ({ ...current, question: value }))} />}
              />
              <LabeledInput label="Options" value={teamPollDraft.options} onChangeText={(value) => setTeamPollDraft((current) => ({ ...current, options: value }))} />
              <AppButton
                label="Create team poll"
                tone="secondary"
                onPress={() =>
                  runAction(
                    () => createTeamPoll(Number(teamPollDraft.teamId), { question: teamPollDraft.question, options: teamPollDraft.options.split(",").map((item) => item.trim()).filter(Boolean) }),
                    "Team poll created."
                  )
                }
              />
            </Surface>
          </Section>
          <Section title="Teams Coordination" subtitle="Create squads, manage rosters, and chat with team members.">
            <Surface tone="player">
              <Text style={styles.cardTitle}>Create team</Text>
              <FieldPair
                left={<LabeledInput label="Team name" value={teamDraft.name} onChangeText={(value) => setTeamDraft((current) => ({ ...current, name: value }))} />}
                right={<LabeledInput label="City" value={teamDraft.city} onChangeText={(value) => setTeamDraft((current) => ({ ...current, city: value }))} />}
              />
              <FieldPair
                left={
                  <SelectField
                    label="Sport"
                    value={teamDraft.sport}
                    onPress={() => openPicker("Select sport", sportChoices, (value) => setTeamDraft((current) => ({ ...current, sport: value })))}
                  />
                }
                right={
                  <SelectField
                    label="Max members"
                    value={teamDraft.maxMembers}
                    onPress={() => openPicker("Choose team size", memberChoices, (value) => setTeamDraft((current) => ({ ...current, maxMembers: value })))}
                  />
                }
              />
              <SelectField
                label="Visibility"
                value={formatEnumLabel(teamDraft.visibility)}
                onPress={() =>
                  openPicker(
                    "Team visibility",
                    [
                      { label: "Public", value: "PUBLIC" },
                      { label: "Invite only", value: "INVITE_ONLY" },
                      { label: "Private", value: "PRIVATE" },
                    ],
                    (value) => setTeamDraft((current) => ({ ...current, visibility: value }))
                  )
                }
              />
              <LabeledInput label="Description" value={teamDraft.description} onChangeText={(value) => setTeamDraft((current) => ({ ...current, description: value }))} multiline />
              <AppButton
                label="Create team"
                onPress={() =>
                  runAction(
                    () =>
                      createTeam({
                        name: teamDraft.name,
                        sport: teamDraft.sport,
                        city: teamDraft.city,
                        description: teamDraft.description,
                        maxMembers: Number(teamDraft.maxMembers),
                        visibility: teamDraft.visibility,
                      }),
                    "Team created."
                  )
                }
              />
            </Surface>
            <Surface tone="player">
              <Text style={styles.cardTitle}>Discover teams</Text>
              <FieldPair
                left={<LabeledInput label="Search" value={discoverTeamsDraft.query} onChangeText={(value) => setDiscoverTeamsDraft((current) => ({ ...current, query: value }))} />}
                right={<LabeledInput label="City" value={discoverTeamsDraft.city} onChangeText={(value) => setDiscoverTeamsDraft((current) => ({ ...current, city: value }))} />}
              />
              <FieldPair
                left={
                  <SelectField
                    label="Visibility"
                    value={discoverTeamsDraft.visibility ? formatEnumLabel(discoverTeamsDraft.visibility) : "Any"}
                    onPress={() =>
                      openPicker(
                        "Discovery visibility",
                        [
                          { label: "Any", value: "" },
                          { label: "Public", value: "PUBLIC" },
                          { label: "Invite only", value: "INVITE_ONLY" },
                          { label: "Private", value: "PRIVATE" },
                        ],
                        (value) => setDiscoverTeamsDraft((current) => ({ ...current, visibility: value }))
                      )
                    }
                  />
                }
                right={<LabeledInput label="Request note" value={discoverTeamsDraft.message} onChangeText={(value) => setDiscoverTeamsDraft((current) => ({ ...current, message: value }))} />}
              />
              <AppButton
                label="Search teams"
                tone="secondary"
                onPress={() =>
                  runAction(async () => {
                    const data = await discoverTeams({
                      query: discoverTeamsDraft.query || undefined,
                      city: discoverTeamsDraft.city || undefined,
                      visibility: discoverTeamsDraft.visibility || undefined,
                      openOnly: true,
                    });
                    setDiscoverableTeams(data);
                  }, "Team discovery updated.", false)
                }
              />
              {discoverableTeams.length === 0 ? <EmptyState text="No discoverable teams found." /> : null}
              {discoverableTeams.slice(0, 5).map((team) => (
                <View key={team.id} style={styles.listRow}>
                  <Text style={styles.listTitle}>{team.name}</Text>
                  <Text style={styles.cardMeta}>
                    {team.city} | {team.memberCount}/{team.maxMembers} members | Open {team.openSpots ?? Math.max(team.maxMembers - team.memberCount, 0)} | Rep {team.reputationScore ?? 75}%
                  </Text>
                  <AppButton
                    label="Request to join"
                    tone="secondary"
                    onPress={() =>
                      runAction(
                        () => requestToJoinTeam(team.id, discoverTeamsDraft.message || undefined),
                        "Join request sent."
                      )
                    }
                  />
                </View>
              ))}
            </Surface>
            {teams.length === 0 ? <EmptyState text="You haven't joined any teams yet. Create one above to get started." /> : null}
            {teams.map((team) => (
              <Surface key={team.id} tone="player">
                <Text style={styles.cardTitle}>{team.name}</Text>
                <Text style={styles.cardMeta}>{team.city} | {team.memberCount}/{team.maxMembers} members | Open {team.openSpots ?? Math.max(team.maxMembers - team.memberCount, 0)} | {formatEnumLabel(team.visibility || "PUBLIC")}</Text>
                <Text style={styles.cardMeta}>Captain {team.captainName} | Reputation {team.reputationScore ?? 75}%</Text>
                <AppButton
                  label="Open team chat"
                  tone="secondary"
                  onPress={() =>
                    runAction(async () => {
                      const messages = await getTeamMessages(team.id);
                      showInfo(
                        `${team.name} chat`,
                        messages.length > 0
                          ? messages.map((message: ChatMessage) => `${message.senderName}: ${message.message}`)
                          : ["No team messages yet."]
                      );
                    }, "Team chat loaded.", false)
                  }
                />
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "matches") {
      return (
        <View style={styles.stack}>
          <Section title="Smart Matchmaking" subtitle="Recommended open matches ranked by availability, timing, skill, and trust signals.">
            {matchRecommendations.length === 0 ? <EmptyState text="No smart match recommendations yet." /> : null}
            {matchRecommendations.map((item) => (
              <Surface key={item.id} tone="player">
                <Text style={styles.cardTitle}>{item.title}</Text>
                <Text style={styles.cardMeta}>{item.city} | {item.matchDate} {formatSlotTime(item.startTime)} | {item.slotsLeft} slots left</Text>
                <Text style={styles.cardMeta}>Match score {item.matchScore}% | {item.skillLevel || "Any skill"} | {item.trustedOnly ? "Trusted only" : "Open community"}</Text>
                <Text style={styles.cardMeta}>{item.recommendationReason}</Text>
                {item.candidates?.slice(0, 2).map((candidate) => (
                  <Text key={candidate.userId} style={styles.cardMeta}>{candidate.name}: Trust {candidate.trustScore}, {candidate.skillLevel}</Text>
                ))}
                <AppButton
                  label="Quick join"
                  tone="secondary"
                  onPress={() => void runAction(() => joinMatchmakingMatch(item.id), "Joined recommended match.")}
                />
              </Surface>
            ))}
          </Section>
          <Section title="Public Matches" subtitle="Organize a friendly match, set entries, and find other players.">
            <Surface tone="player">
              <FieldPair
                left={<LabeledInput label="Title" value={matchDraft.title} onChangeText={(value) => setMatchDraft((current) => ({ ...current, title: value }))} />}
                right={<LabeledInput label="City" value={matchDraft.city} onChangeText={(value) => setMatchDraft((current) => ({ ...current, city: value }))} />}
              />
              <FieldPair
                left={<LabeledInput label="Location" value={matchDraft.location} onChangeText={(value) => setMatchDraft((current) => ({ ...current, location: value }))} />}
                right={
                  <SelectField
                    label="Max players"
                    value={matchDraft.maxPlayers}
                    onPress={() => openPicker("Choose player count", memberChoices, (value) => setMatchDraft((current) => ({ ...current, maxPlayers: value })))}
                  />
                }
              />
              <FieldPair
                left={
                  <SelectField
                    label="Visibility"
                    value={formatEnumLabel(matchDraft.visibility)}
                    onPress={() => openPicker("Match visibility", matchVisibilityChoices, (value) => setMatchDraft((current) => ({ ...current, visibility: value })))}
                  />
                }
                right={
                  <SelectField
                    label="Format"
                    value={formatEnumLabel(matchDraft.format)}
                    onPress={() => openPicker("Match format", matchFormatChoices, (value) => setMatchDraft((current) => ({ ...current, format: value })))}
                  />
                }
              />
              <FieldPair
                left={
                  <SelectField
                    label="Date"
                    value={matchDraft.matchDate}
                    onPress={() => openPicker("Match date", dateChoices, (value) => setMatchDraft((current) => ({ ...current, matchDate: value })))}
                  />
                }
                right={
                  <SelectField
                    label="Start time"
                    value={matchDraft.startTime}
                    onPress={() => openPicker("Start time", timeChoices, (value) => setMatchDraft((current) => ({ ...current, startTime: value })))}
                  />
                }
              />
              <FieldPair
                left={<LabeledInput label="Overs" value={matchDraft.overs} onChangeText={(value) => setMatchDraft((current) => ({ ...current, overs: value }))} keyboardType="numeric" />}
                right={<LabeledInput label="Entry fee" value={matchDraft.entryFee} onChangeText={(value) => setMatchDraft((current) => ({ ...current, entryFee: value }))} keyboardType="numeric" />}
              />
              <FieldPair
                left={
                  <SelectField
                    label="Home team"
                    value={teams.find((team) => String(team.id) === matchDraft.homeTeamId)?.name || "No team"}
                    onPress={() =>
                      openPicker("Select home team", [{ label: "No team", value: "" }, ...teams.map((team) => ({ label: team.name, value: String(team.id) }))], (value) =>
                        setMatchDraft((current) => ({ ...current, homeTeamId: value }))
                      )
                    }
                  />
                }
                right={
                  <SelectField
                    label="Away team"
                    value={teams.find((team) => String(team.id) === matchDraft.awayTeamId)?.name || "No team"}
                    onPress={() =>
                      openPicker("Select away team", [{ label: "No team", value: "" }, ...teams.map((team) => ({ label: team.name, value: String(team.id) }))], (value) =>
                        setMatchDraft((current) => ({ ...current, awayTeamId: value }))
                      )
                    }
                  />
                }
              />
              <LabeledInput
                label="Challenge message"
                value={matchDraft.challengeMessage}
                onChangeText={(value) => setMatchDraft((current) => ({ ...current, challengeMessage: value }))}
                multiline
              />
              <LabeledInput label="Notes" value={matchDraft.notes} onChangeText={(value) => setMatchDraft((current) => ({ ...current, notes: value }))} multiline />
              <AppButton
                label="Create public match"
                onPress={() =>
                  runAction(
                    () =>
                      createPublicMatch({
                        title: matchDraft.title,
                        sport: matchDraft.sport,
                        city: matchDraft.city,
                        location: matchDraft.location,
                        matchDate: matchDraft.matchDate,
                        startTime: matchDraft.startTime,
                        endTime: matchDraft.endTime,
                        maxPlayers: Number(matchDraft.maxPlayers),
                        visibility: matchDraft.visibility,
                        format: matchDraft.format,
                        overs: Number(matchDraft.overs),
                        entryFee: Number(matchDraft.entryFee),
                        homeTeamId: matchDraft.homeTeamId ? Number(matchDraft.homeTeamId) : undefined,
                        awayTeamId: matchDraft.awayTeamId ? Number(matchDraft.awayTeamId) : undefined,
                        challengeMessage: matchDraft.challengeMessage,
                        notes: matchDraft.notes,
                      }),
                    "Public match created."
                  )
                }
              />
            </Surface>
            {matches.map((match) => (
              <Surface key={match.id} tone="player">
                <Text style={styles.cardTitle}>{match.title}</Text>
                <Text style={styles.cardMeta}>{match.city} | {match.location}</Text>
                <Text style={styles.cardMeta}>{match.matchDate} {formatSlotTime(match.startTime)} | {match.joinedPlayers}/{match.maxPlayers}</Text>
                <Text style={styles.cardMeta}>{formatEnumLabel(match.visibility)} | {formatEnumLabel(match.format)} | {match.overs || "--"} overs | Rs {match.entryFee || 0}</Text>
                {(match.homeTeamName || match.awayTeamName) ? (
                  <Text style={styles.cardMeta}>{match.homeTeamName || "Open Squad"} vs {match.awayTeamName || "Need Opponent"}</Text>
                ) : null}
                {match.challengeMessage ? <Text style={styles.cardMeta}>{match.challengeMessage}</Text> : null}
                <Text style={styles.cardMeta}>
                  {formatEnumLabel(match.status)}
                  {match.homeScore !== undefined && match.awayScore !== undefined ? ` | ${match.homeScore}-${match.awayScore}` : ""}
                  {match.winnerTeamName ? ` | Winner ${match.winnerTeamName}` : ""}
                </Text>
                <View style={styles.actionRow}>
                  {match.status === "OPEN" ? (
                    <AppButton
                      label="Join match"
                      tone="secondary"
                      onPress={() =>
                        confirmAction(
                          "Join public match",
                          `Join ${match.title} in ${match.city} on ${match.matchDate}?`,
                          "Join match",
                          "primary",
                          () => void runAction(() => joinPublicMatch(match.id), "Joined public match.")
                        )
                      }
                    />
                  ) : null}
                  {match.status === "PENDING" ? (
                    <>
                      <AppButton label="Accept" tone="secondary" onPress={() => runAction(() => acceptMatchChallenge(match.id), "Challenge accepted.")} />
                      <AppButton label="Reject" tone="secondary" onPress={() => runAction(() => rejectMatchChallenge(match.id), "Challenge rejected.")} />
                    </>
                  ) : null}
                  {["PENDING", "ACCEPTED"].includes(match.status) ? (
                    <AppButton
                      label="Confirm schedule"
                      tone="secondary"
                      onPress={() =>
                        runAction(
                          () =>
                            schedulePublicMatch(match.id, {
                              turfId: match.turfId,
                              matchDate: match.matchDate,
                              startTime: match.startTime,
                              endTime: match.endTime,
                              location: match.location,
                              note: "Schedule confirmed from mobile match center.",
                            }),
                          "Match schedule confirmed."
                        )
                      }
                    />
                  ) : null}
                  {["ACCEPTED", "SCHEDULED", "FULL"].includes(match.status) ? (
                    <AppButton label="Start" tone="secondary" onPress={() => runAction(() => startPublicMatch(match.id), "Match started.")} />
                  ) : null}
                  {["PENDING", "ACCEPTED", "SCHEDULED", "OPEN", "FULL"].includes(match.status) ? (
                    <AppButton label="Cancel" tone="secondary" onPress={() => runAction(() => cancelPublicMatch(match.id), "Match cancelled.")} />
                  ) : null}
                  {match.status === "LIVE" && match.homeTeamId && match.awayTeamId ? (
                    <AppButton
                      label="Submit 1-0 result"
                      tone="secondary"
                      onPress={() =>
                        runAction(
                          () =>
                            submitMatchResult(match.id, {
                              homeScore: 1,
                              awayScore: 0,
                              winnerTeamId: match.homeTeamId,
                              resultSummary: "Result submitted from mobile quick action.",
                            }),
                          "Match result submitted."
                        )
                      }
                    />
                  ) : null}
                  <AppButton
                    label="Open match chat"
                    tone="secondary"
                    onPress={() =>
                      runAction(async () => {
                        const messages = await getMatchMessages(match.id);
                        showInfo(
                          `${match.title} chat`,
                          messages.length > 0
                            ? messages.map((chat: ChatMessage) => `${chat.senderName}: ${chat.message}`)
                            : ["No match messages yet."]
                        );
                      }, "Match chat loaded.", false)
                    }
                  />
                </View>
              </Surface>
            ))}
          </Section>

          <Section title="Find Players" subtitle="Use profile role and availability to fill empty team or match spots.">
            <Surface tone="player">
              <FieldPair
                left={<LabeledInput label="City" value={playerSearchDraft.city} onChangeText={(value) => setPlayerSearchDraft((current) => ({ ...current, city: value }))} />}
                right={<LabeledInput label="Position" value={playerSearchDraft.position} onChangeText={(value) => setPlayerSearchDraft((current) => ({ ...current, position: value }))} />}
              />
              <LabeledInput label="Availability" value={playerSearchDraft.availability} onChangeText={(value) => setPlayerSearchDraft((current) => ({ ...current, availability: value }))} placeholder="Evening, Weekend, Morning" />
              <AppButton label="Search players" onPress={() => runAction(async () => setDiscoverablePlayers(await discoverPlayers(playerSearchDraft)), "Players loaded.", false)} />
            </Surface>
            {discoverablePlayers.length === 0 ? <EmptyState text="No players discovered yet." /> : null}
            {discoverablePlayers.map((player) => (
              <Surface key={player.userId} tone="player">
                <Text style={styles.cardTitle}>{player.name}</Text>
                <Text style={styles.cardMeta}>{player.city || player.availability?.homeCity || "Unknown city"} | {formatEnumLabel(player.position || "FLEXIBLE")}</Text>
                <Text style={styles.cardMeta}>{formatEnumLabel(player.battingStyle || "NOT_SET")} | {formatEnumLabel(player.bowlingStyle || "NOT_SET")}</Text>
                <Text style={styles.cardMeta}>{player.matches} matches | {player.wins} wins | Rating {player.rating} | Trust {player.trustScore ?? 70}</Text>
                <Text style={styles.cardMeta}>Fit {player.matchScore ?? 0}% | {player.skillLevel || "Beginner"} | {player.connectionStatus || "NONE"}</Text>
                <Text style={styles.cardMeta}>{player.recommendationReason || "Good community player fit."}</Text>
                {player.availability ? <Text style={styles.cardMeta}>{player.availability.preferredWindow || "Any time"} | {player.availability.availableDays || "Flexible days"}</Text> : null}
                <View style={styles.actionRow}>
                  <AppButton label="Connect" tone="secondary" onPress={() => runAction(() => connectPlayer({ userId: player.userId, type: "FRIEND", message: "Let's connect on Cricbook." }), "Connection request sent.")} />
                  <AppButton label="Follow" tone="secondary" onPress={() => runAction(() => connectPlayer({ userId: player.userId, type: "FOLLOW" }), "Player followed.")} />
                </View>
              </Surface>
            ))}
          </Section>

          <Section title="Open Team Positions" subtitle="Captains recruit players and players apply without leaving Cricbook.">
            <Surface tone="player">
              <FieldPair
                left={
                  <SelectField
                    label="Captain team"
                    value={teams.find((team) => String(team.id) === recruitmentDraft.teamId)?.name || "Select team"}
                    onPress={() =>
                      openPicker("Recruiting team", teams.map((team) => ({ label: team.name, value: String(team.id) })), (value) =>
                        setRecruitmentDraft((current) => ({ ...current, teamId: value }))
                      )
                    }
                  />
                }
                right={<LabeledInput label="Role needed" value={recruitmentDraft.roleNeeded} onChangeText={(value) => setRecruitmentDraft((current) => ({ ...current, roleNeeded: value }))} />}
              />
              <FieldPair
                left={<LabeledInput label="City" value={recruitmentDraft.city} onChangeText={(value) => setRecruitmentDraft((current) => ({ ...current, city: value }))} />}
                right={<LabeledInput label="Availability" value={recruitmentDraft.availability} onChangeText={(value) => setRecruitmentDraft((current) => ({ ...current, availability: value }))} />}
              />
              <AppButton
                label="Post role"
                onPress={() =>
                  runAction(
                    () =>
                      createRecruitment({
                        teamId: Number(recruitmentDraft.teamId),
                        roleNeeded: recruitmentDraft.roleNeeded,
                        city: recruitmentDraft.city,
                        availability: recruitmentDraft.availability,
                        skillLevel: recruitmentDraft.skillLevel,
                      }),
                    "Recruitment posted."
                  )
                }
              />
            </Surface>
            {teamRecruitments.length === 0 ? <EmptyState text="No open team positions yet." /> : null}
            {teamRecruitments.map((item) => (
              <Surface key={item.id} tone="player">
                <Text style={styles.cardTitle}>{item.roleNeeded}</Text>
                <Text style={styles.cardMeta}>{item.teamName} | {item.city || "Any city"} | {item.skillLevel || "Any skill"}</Text>
                <Text style={styles.cardMeta}>{item.applicationCount} applications | {item.status}</Text>
                <AppButton label="Apply" tone="secondary" onPress={() => runAction(() => applyRecruitment(item.id, "Interested from mobile."), "Application sent.")} />
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "marketplace") {
      return (
        <View style={styles.stack}>
          <Section title="Sports Travel & Experiences" subtitle="Book sports trips, camps, tickets, and partner accommodation.">
            <View style={styles.metricGrid}>
              <Metric label="Packages" value={travelPackages.length} tone="player" />
              <Metric label="Events" value={travelEvents.length} tone="player" />
              <Metric label="Rooms" value={travelAccommodations.length} tone="player" />
              <Metric label="City" value={selectedCity} tone="player" />
            </View>
            {travelPackages.length === 0 ? <EmptyState text="No travel packages published yet." /> : null}
            {travelPackages.slice(0, 4).map((item) => (
              <Surface key={`travel-package-${item.id}`} tone="player">
                <Text style={styles.cardTitle}>{item.title}</Text>
                <Text style={styles.cardMeta}>{item.originCity || "Any city"} to {item.destination} | {item.startDate} - {item.endDate}</Text>
                <Text style={styles.cardMeta}>Rs {item.pricePerPerson}/person | {item.availableCapacity} seats | {item.includesAccommodation ? "Hotel" : "No hotel"} | {item.includesTransport ? "Transport" : "No transport"}</Text>
                <AppButton
                  label="Book package"
                  tone="secondary"
                  onPress={() =>
                    runAction(
                      () =>
                        bookTravelPackage(item.id, {
                          bookingType: "INDIVIDUAL",
                          participantCount: 1,
                          couponCode: "TRAVEL10",
                          paymentReference: `MOBILE-TRAVEL-${Date.now()}`,
                        }),
                      "Travel package booked."
                    )
                  }
                />
              </Surface>
            ))}
            {travelEvents.slice(0, 4).map((item) => {
              const ticket = item.ticketTypes?.find((entry) => entry.active && entry.availableInventory > 0);
              return (
                <Surface key={`travel-event-${item.id}`} tone="player">
                  <Text style={styles.cardTitle}>{item.title}</Text>
                  <Text style={styles.cardMeta}>{item.eventType} | {item.city} | {item.startsAt?.replace("T", " ").slice(0, 16)}</Text>
                  <Text style={styles.cardMeta}>{item.availableCapacity} spots | Ticket Rs {ticket?.price ?? item.basePrice}</Text>
                  <AppButton
                    label="Buy ticket"
                    tone="secondary"
                    onPress={() =>
                      ticket
                        ? runAction(() => buyTravelTicket(item.id, { ticketTypeId: ticket.id, quantity: 1 }), "Travel ticket issued.")
                        : Alert.alert("Tickets unavailable", "No ticket inventory is available for this event.")
                    }
                  />
                </Surface>
              );
            })}
            {travelAccommodations.slice(0, 3).map((item) => {
              const checkIn = new Date();
              checkIn.setDate(checkIn.getDate() + 7);
              const checkOut = new Date(checkIn);
              checkOut.setDate(checkIn.getDate() + 2);
              return (
                <Surface key={`travel-room-${item.id}`} tone="player">
                  <Text style={styles.cardTitle}>{item.name}</Text>
                  <Text style={styles.cardMeta}>{item.city} | {item.accommodationType} | {item.roomType}</Text>
                  <Text style={styles.cardMeta}>Rs {item.pricePerNight}/night | {item.availableRooms} rooms</Text>
                  <AppButton
                    label="Book room"
                    tone="secondary"
                    onPress={() =>
                      runAction(
                        () =>
                          bookTravelAccommodation(item.id, {
                            checkIn: checkIn.toISOString().slice(0, 10),
                            checkOut: checkOut.toISOString().slice(0, 10),
                            rooms: 1,
                          }),
                        "Accommodation booked."
                      )
                    }
                  />
                </Surface>
              );
            })}
          </Section>
          <Section title="Sports Commerce" subtitle="Buy equipment, rent kits, and reserve marketplace inventory from mobile.">
            <View style={styles.metricGrid}>
              <Metric label="Products" value={commerceProducts.length} tone="player" />
              <Metric label="Cart items" value={commerceCart?.itemCount ?? 0} tone="player" />
              <Metric label="Subtotal" value={`Rs ${commerceCart?.subtotal ?? 0}`} tone="player" />
              <Metric label="Deposits" value={`Rs ${commerceCart?.rentalDeposits ?? 0}`} tone="player" />
            </View>
            {commerceProducts.length === 0 ? <EmptyState text="No commerce products published yet." /> : null}
            {commerceProducts.map((product) => {
              const variant = product.variants?.find((item) => item.availableStock > 0) || product.variants?.[0];
              const isRental = product.productType === "RENTAL";
              return (
                <Surface key={product.id} tone="player">
                  <Text style={styles.cardTitle}>{product.name}</Text>
                  <Text style={styles.cardMeta}>{product.brand || product.sellerName} | {product.sport} | {product.category}</Text>
                  <Text style={styles.cardMeta}>
                    {isRental ? `Rs ${product.rentalFeePerDay ?? product.price}/day` : `Rs ${product.price}`} | Stock {product.availableStock} | Rating {product.rating ?? 0}
                  </Text>
                  <AppButton
                    label={product.productType === "AFFILIATE" ? "Affiliate product" : "Add to cart"}
                    tone="secondary"
                    onPress={() =>
                      variant
                        ? runAction(
                            async () => {
                              const endDate = new Date();
                              endDate.setDate(endDate.getDate() + 1);
                              setCommerceCart(
                                await addCommerceCartItem({
                                  productId: product.id,
                                  inventoryId: variant.id,
                                  quantity: 1,
                                  rentalStart: isRental ? today : undefined,
                                  rentalEnd: isRental ? endDate.toISOString().slice(0, 10) : undefined,
                                })
                              );
                            },
                            "Product added to cart."
                          )
                        : Alert.alert("Inventory unavailable", "No inventory variant is available for this product.")
                    }
                  />
                </Surface>
              );
            })}
            {(commerceCart?.items || []).map((item) => (
              <Surface key={`cart-${item.id}`} tone="player">
                <Text style={styles.cardTitle}>{item.product.name}</Text>
                <Text style={styles.cardMeta}>{item.inventory.variantLabel} | Qty {item.quantity} | Rs {item.lineTotal}</Text>
              </Surface>
            ))}
            <AppButton
              label="Checkout commerce cart"
              onPress={() =>
                runAction(
                  async () => {
                    const order = await checkoutCommerceCart({
                      shippingAddress: `${selectedCity} sports hub`,
                      paymentReference: `MOBILE-COM-${Date.now()}`,
                    });
                    setCommerceCart({ items: [], subtotal: 0, rentalDeposits: 0, itemCount: 0 });
                    return order;
                  },
                  "Commerce order placed."
                )
              }
            />
          </Section>
          <Section title="Last-minute Slot Marketplace" subtitle="Buy transferred slots, resell confirmed bookings, and join waitlists from mobile.">
            <FieldPair
              left={<LabeledInput label="Booking ID" value={resaleDraft.bookingId} onChangeText={(value) => setResaleDraft((current) => ({ ...current, bookingId: value }))} keyboardType="numeric" />}
              right={<LabeledInput label="Resale price" value={resaleDraft.price} onChangeText={(value) => setResaleDraft((current) => ({ ...current, price: value }))} keyboardType="numeric" />}
            />
            <LabeledInput label="Notes" value={resaleDraft.notes} onChangeText={(value) => setResaleDraft((current) => ({ ...current, notes: value }))} />
            <AppButton
              label="List confirmed booking"
              onPress={() =>
                runAction(
                  () =>
                    createResaleListing({
                      bookingId: Number(resaleDraft.bookingId),
                      price: Number(resaleDraft.price),
                      notes: resaleDraft.notes,
                    }),
                  "Slot listed in marketplace."
                )
              }
            />
          </Section>
          <Section title="Recommended Slots" subtitle="Ranked by price, trust, timing, and live transfer fit.">
            {recommendedResaleListings.length === 0 ? <EmptyState text="No recommended resale slots yet." /> : null}
            {recommendedResaleListings.map((item) => (
              <Surface key={`recommended-${item.id}`} tone="player">
                <Text style={styles.cardTitle}>{item.turfName}</Text>
                <Text style={styles.cardMeta}>{item.city} | {item.bookingDate} {formatSlotTime(item.startTime)} | Rs {item.price}</Text>
                <Text style={styles.cardMeta}>Seller trust {item.sellerTrustScore} | {item.recommendationScore}% fit</Text>
                <Text style={styles.cardMeta}>{item.recommendationReason}</Text>
                <AppButton
                  label="Buy slot"
                  tone="secondary"
                  onPress={() => void runAction(() => buyResaleListing(item.id, `MOBILE-${Date.now()}`), "Slot transferred to your bookings.")}
                />
              </Surface>
            ))}
          </Section>
          <Section title="Live Marketplace" subtitle={`Available resale slots in ${selectedCity}.`}>
            {resaleListings.length === 0 ? <EmptyState text="No live resale slots found." /> : null}
            {resaleListings.map((item) => (
              <Surface key={item.id} tone="player">
                <Text style={styles.cardTitle}>{item.turfName}</Text>
                <Text style={styles.cardMeta}>{item.location} | {item.bookingDate} | Rs {item.price}</Text>
                <Text style={styles.cardMeta}>{item.status} | expires {item.expiresAt?.slice(0, 16)}</Text>
                <AppButton
                  label="Buy transfer"
                  tone="secondary"
                  onPress={() => void runAction(() => buyResaleListing(item.id, `MOBILE-${Date.now()}`), "Slot transferred to your bookings.")}
                />
              </Surface>
            ))}
          </Section>
          <Section title="Priority Waitlist" subtitle="Tell Cricbook which slot you want if it becomes available.">
            <FieldPair
              left={<LabeledInput label="Slot ID" value={waitlistDraft.slotId} onChangeText={(value) => setWaitlistDraft((current) => ({ ...current, slotId: value }))} keyboardType="numeric" />}
              right={
                <SelectField
                  label="Date"
                  value={waitlistDraft.date}
                  onPress={() => openPicker("Waitlist date", dateChoices, (value) => setWaitlistDraft((current) => ({ ...current, date: value })))}
                />
              }
            />
            <LabeledInput label="Priority 1-10" value={waitlistDraft.priority} onChangeText={(value) => setWaitlistDraft((current) => ({ ...current, priority: value }))} keyboardType="numeric" />
            <AppButton
              label="Join waitlist"
              tone="secondary"
              onPress={() =>
                runAction(
                  () =>
                    joinResaleWaitlist({
                      slotId: Number(waitlistDraft.slotId),
                      date: waitlistDraft.date,
                      city: selectedCity,
                      priority: Number(waitlistDraft.priority),
                    }),
                  "Waitlist preference saved."
                )
              }
            />
            {myResaleWaitlist.map((item) => (
              <Surface key={item.id} tone="player">
                <Text style={styles.cardTitle}>{item.turfName}</Text>
                <Text style={styles.cardMeta}>{item.date} | {formatSlotTime(item.startTime)} | priority {item.priority}</Text>
              </Surface>
            ))}
          </Section>
          <Section title="My Listings" subtitle="Track your active and completed transfers.">
            {myResaleListings.length === 0 ? <EmptyState text="No resale listings created yet." /> : null}
            {myResaleListings.map((item) => (
              <Surface key={`mine-${item.id}`} tone="player">
                <Text style={styles.cardTitle}>{item.turfName}</Text>
                <Text style={styles.cardMeta}>{item.status} | Rs {item.price} | {item.bookingDate}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "feed") {
      return (
        <View style={styles.stack}>
          <Section title="Cricket Network" subtitle="Connections, follows and community health for daily player engagement.">
            <View style={styles.metricGrid}>
              <Metric label="Players" value={communityAnalytics?.discoverablePlayers ?? 0} tone="player" />
              <Metric label="Connections" value={communityAnalytics?.acceptedConnections ?? playerNetwork.length} tone="player" />
              <Metric label="Active posts" value={communityAnalytics?.activePosts ?? socialFeed.length} tone="player" />
              <Metric label="Engagement" value={`${communityAnalytics?.engagementRate ?? 0}%`} tone="player" />
            </View>
            {playerNetwork.map((connection) => (
              <Surface key={connection.id} tone="player">
                <Text style={styles.cardTitle}>{connection.fromUserName} to {connection.toUserName}</Text>
                <Text style={styles.cardMeta}>{connection.type} | {connection.status}</Text>
              </Surface>
            ))}
          </Section>
          <Section title="Match History & Activity Feed" subtitle="Your history of matches plus community updates.">
            {matchHistory.length === 0 ? <EmptyState text="No joined matches yet." /> : null}
            {matchHistory.map((match) => (
              <Surface key={`history-${match.id}`} tone="player">
                <Text style={styles.cardTitle}>{match.title}</Text>
                <Text style={styles.cardMeta}>{match.matchDate} | {match.city}</Text>
                <Text style={styles.cardMeta}>{match.status} | {match.joinedPlayers}/{match.maxPlayers} players</Text>
              </Surface>
            ))}
            {socialFeed.map((item) => (
              <Surface key={`feed-${item.id}`} tone="player">
                <Text style={styles.cardTitle}>{item.title}</Text>
                <Text style={styles.cardMeta}>{item.summary}</Text>
                {item.mediaUrls?.slice(0, 3).map((mediaUrl) => (
                  <Text key={mediaUrl} style={styles.cardMeta}>{mediaUrl}</Text>
                ))}
                <Text style={styles.cardMeta}>
                  {(item.reactionCount ?? 0)} reactions | {(item.commentCount ?? 0)} comments | score {(item.engagementScore ?? 0)}
                </Text>
                <Text style={styles.cardMeta}>
                  {formatEnumLabel(item.visibility || "PUBLIC")} | {item.ownPost ? "My post" : item.actorName} | {item.createdAt?.replace("T", " ").slice(0, 16)}
                </Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "search") {
      const filteredTurfs = filteredSearchResults;

      return (
        <View style={styles.stack}>
          {/* Horizontally scrollable Sport Selector Chips */}
          <View style={{ gap: 8, paddingHorizontal: 4 }}>
            <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 14 }}>
              {t("select_sport", "Select Sport Category")}:
            </Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 8 }}>
              {["All", "Cricket", "Football", "Badminton", "Padel", "Tennis", "Basketball"].map((sport) => {
                const isSelected = selectedSport === sport;
                return (
                  <Pressable
                    key={sport}
                    onPress={() => setSelectedSport(sport)}
                    style={{
                      paddingHorizontal: 16,
                      paddingVertical: 10,
                      borderRadius: 99,
                      borderWidth: 1,
                      borderColor: isSelected ? primaryColor : theme.colors.border,
                      backgroundColor: isSelected ? secondaryColor : theme.colors.panelSoft,
                      flexDirection: "row",
                      alignItems: "center",
                      gap: 6,
                    }}
                  >
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 13 }}>
                      {sport}
                    </Text>
                  </Pressable>
                );
              })}
            </ScrollView>
          </View>

          <Section title={t("search_turfs", "Search Turfs")} subtitle="Keep the visual language close to the website, but fit the mobile flow.">
            <Surface tone="player">
              <FieldPair
                left={<LabeledInput label="City" value={String(searchFilters.city || "")} onChangeText={(value) => setSearchFilters((current) => ({ ...current, city: value }))} placeholder="Surat" />}
                right={
                  <SelectField
                    label="Date"
                    value={String(searchFilters.date || "")}
                    onPress={() =>
                      openPicker("Choose date", dateChoices, (value) => setSearchFilters((current) => ({ ...current, date: value })), "Use quick mobile date choices.")
                    }
                  />
                }
              />
              <FieldPair
                left={
                  <SelectField
                    label="Box Type"
                    value={searchFilters.boxType ? formatEnumLabel(String(searchFilters.boxType)) : undefined}
                    placeholder="Choose box type"
                    onPress={() => openPicker("Box type", boxTypeChoices, (value) => setSearchFilters((current) => ({ ...current, boxType: value })))}
                  />
                }
                right={
                  <SelectField
                    label="Pitch Type"
                    value={searchFilters.pitchType ? formatEnumLabel(String(searchFilters.pitchType)) : undefined}
                    placeholder="Choose pitch"
                    onPress={() => openPicker("Pitch type", pitchTypeChoices, (value) => setSearchFilters((current) => ({ ...current, pitchType: value })))}
                  />
                }
              />
              <AppButton
                label="Search"
                onPress={() =>
                  runAction(async () => {
                    setSearchResults(await searchTurfs(searchFilters));
                    await saveRecentSearch(searchFilters);
                  }, "Search refreshed.", false)
                }
              />
            </Surface>
          </Section>

          <Section title={t("results", "Results")} subtitle={`${filteredTurfs.length} turf options loaded`}>
            {filteredTurfs.length === 0 ? (
              <EmptyState text="No turfs yet. Search by city or refresh after the backend has data." />
            ) : (
              filteredTurfs.map((turf) => (
                <Surface key={turf.id} tone="player">
                  <Text style={styles.cardTitle}>{turf.name}</Text>
                  <Text style={styles.cardMeta}>{turf.city} | {turf.location}</Text>
                  <Text style={styles.cardMeta}>
                    {getConvertedPrice(Number(turf.pricePerHour), selectedCity)}/hr | {formatEnumLabel(turf.boxType)} | {formatEnumLabel(turf.pitchType)}
                  </Text>
                  <View style={styles.actionRow}>
                    <AppButton label="View slots" onPress={() => openTurf(turf.id ?? 0)} />
                    <AppButton
                      label="Favorite"
                      tone="secondary"
                      onPress={() => runAction(() => saveFavorite(turf.id ?? 0), "Saved to favorites.")}
                    />
                  </View>
                </Surface>
              ))
            )}
          </Section>

          {selectedTurf ? (
            <Section title={selectedTurf.name} subtitle="Booking flow, turf detail and favorite status.">
              <Surface tone="player">
                <Text style={styles.cardMeta}>{selectedTurf.description || "No description yet."}</Text>
                <Text style={styles.cardMeta}>
                  {selectedTurf.city} | {selectedTurf.location} | Rating {selectedTurf.rating || 0}
                </Text>
                <View style={styles.actionRow}>
                  <AppButton
                    label={selectedTurfIsFavorite ? "Remove favorite" : "Save favorite"}
                    tone="secondary"
                    onPress={() =>
                      runAction(
                        async () => {
                          if (selectedTurfIsFavorite) await removeFavorite(selectedTurf.id ?? 0);
                          else await saveFavorite(selectedTurf.id ?? 0);
                          setSelectedTurfIsFavorite(!selectedTurfIsFavorite);
                        },
                        selectedTurfIsFavorite ? "Removed from favorites." : "Saved to favorites.",
                        false
                      )
                    }
                  />
                  <AppButton label="Reload detail" tone="secondary" onPress={() => openTurf(selectedTurf.id ?? 0)} />
                </View>
                <SelectField
                  label="Booking date"
                  value={String(searchFilters.date || "")}
                  onPress={() =>
                    openPicker("Select booking date", dateChoices, async (value) => {
                      setSearchFilters((current) => ({ ...current, date: value }));
                      if (selectedTurf.id) await openTurf(selectedTurf.id);
                    })
                  }
                />
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.tabScroller}>
                  {selectedAvailability.map((slot) => (
                    <Pressable
                      key={slot.slotId}
                      onPress={() => setSelectedSlotId(slot.slotId)}
                      style={[
                        styles.slotCard,
                        selectedSlotId === slot.slotId && styles.slotCardActive,
                        slot.blocked && styles.slotCardBlocked,
                      ]}
                    >
                      <Text style={styles.slotTitle}>
                        {formatSlotTimeWithTimezone(slot.startTime, selectedCity)} - {formatSlotTimeWithTimezone(slot.endTime, selectedCity)}
                      </Text>
                      <Text style={styles.slotMeta}>{getConvertedPrice(slot.price, selectedCity)}</Text>
                      <Text style={styles.slotMeta}>{slot.blocked ? "Blocked" : slot.available ? "Available" : "Booked"}</Text>
                    </Pressable>
                  ))}
                </ScrollView>
                <AppButton
                  label="Book selected slot"
                  disabled={!selectedSlotId}
                  onPress={() =>
                    confirmAction(
                      "Confirm booking",
                      `Book ${selectedTurf.name} on ${searchFilters.date || today} for slot ${selectedAvailability.find((item) => item.slotId === selectedSlotId)?.startTime || ""}?`,
                      "Book now",
                      "primary",
                      async () => {
                        const isConnected = await checkConnectivity();
                        if (!isConnected) {
                          await enqueueSyncAction("CREATE_BOOKING", "/bookings", "POST", {
                            turfId: selectedTurf.id ?? 0,
                            slotId: selectedSlotId ?? 0,
                            bookingDate: String(searchFilters.date || today),
                          });
                          const selectedSlotObj = selectedAvailability.find((item) => item.slotId === selectedSlotId);
                          const offlineBooking: Booking = {
                            id: Math.floor(Math.random() * -100000),
                            turfId: selectedTurf.id ?? 0,
                            turfName: selectedTurf.name,
                            turfCity: selectedTurf.city,
                            turfLocation: selectedTurf.location,
                            slotId: selectedSlotId ?? 0,
                            startTime: selectedSlotObj?.startTime || "",
                            endTime: selectedSlotObj?.endTime || "",
                            bookingDate: String(searchFilters.date || today),
                            amount: selectedSlotObj?.price || 0,
                            status: "PENDING (Offline)",
                            paymentStatus: "PENDING",
                          };
                          const nextBookings = [offlineBooking, ...bookings];
                          setBookings(nextBookings);
                          await cacheBookings(nextBookings);
                          await simulateFCMAlert(
                            "BOOKING",
                            "Booking Queued (Offline)",
                            `Your booking for ${selectedTurf.name} is queued and will sync when connectivity returns.`
                          );
                        } else {
                          await runAction(
                            async () => {
                              await createBooking(selectedTurf.id ?? 0, selectedSlotId ?? 0, String(searchFilters.date || today));
                              const next = await getMyBookings();
                              setBookings(next);
                              await cacheBookings(next);
                              await simulateFCMAlert(
                                "BOOKING",
                                "Booking Confirmed",
                                `Your booking for ${selectedTurf.name} has been confirmed successfully!`
                              );
                            },
                            "Booking created."
                          );
                        }
                      }
                    )
                  }
                />
              </Surface>
            </Section>
          ) : null}
        </View>
      );
    }

    if (playerTab === "bookings") {
      return (
        <View style={styles.stack}>
          <Section title="My Bookings" subtitle="Player booking history with mobile-friendly actions.">
            {bookings.length === 0 ? (
              <EmptyState text="No bookings found yet." />
            ) : (
              bookings.map((booking) => (
                <Surface key={booking.id} tone="player">
                  <Text style={styles.cardTitle}>{booking.turfName || `Turf #${booking.turfId}`}</Text>
                  <Text style={styles.cardMeta}>{booking.bookingDate} | {formatSlotTime(booking.startTime)} - {formatSlotTime(booking.endTime)}</Text>
                  <Text style={styles.cardMeta}>Status: {booking.status} | Amount: Rs {booking.amount ?? booking.totalAmount ?? 0}</Text>
                  {(booking.status === "PENDING" || booking.status === "CONFIRMED") ? (
                    <AppButton
                      label="Cancel booking"
                      tone="danger"
                      onPress={() =>
                        confirmAction(
                          "Cancel booking",
                          `Cancel booking for ${booking.turfName || `Turf #${booking.turfId}`} on ${booking.bookingDate}?`,
                          "Cancel booking",
                          "danger",
                          () => void runAction(() => cancelBooking(booking.id), "Booking cancelled.")
                        )
                      }
                    />
                  ) : null}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (playerTab === "rebook") {
      return (
        <View style={styles.stack}>
          <Section title="Smart Rebook" subtitle="One-tap repeat bookings, recurring schedules, templates, and auto waitlists.">
            <View style={styles.metricGrid}>
              <Metric label="Readiness" value={`${Math.round(bookingIntelligence?.rebookReadiness || 0)}%`} tone="player" />
              <Metric label="Frequent" value={bookingIntelligence?.frequentBookings || frequentBookings.length} tone="player" />
              <Metric label="Recurring" value={bookingIntelligence?.recurringBookings || recurringBookings.length} tone="player" />
              <Metric label="Templates" value={bookingIntelligence?.templates || bookingTemplates.length} tone="player" />
            </View>
            <Surface tone="player">
              <Text style={styles.cardTitle}>Your usual matches</Text>
              <LabeledInput label="Target date" value={rebookTargetDate} onChangeText={setRebookTargetDate} />
              {frequentBookings.length === 0 ? <EmptyState text="Book a slot first to generate rebook suggestions." /> : null}
              {frequentBookings.slice(0, 4).map((item) => (
                <View key={`${item.sourceBookingId}-${item.slotId}`} style={styles.listRow}>
                  <Text style={styles.listTitle}>{item.turfName}</Text>
                  <Text style={styles.cardMeta}>{item.city || "Preferred city"} | {formatSlotTime(item.startTime)} - {formatSlotTime(item.endTime)} | {item.timesBooked}x</Text>
                  <AppButton
                    label="Rebook"
                    onPress={() =>
                      runAction(
                        async () => {
                          await rebookBooking(item.sourceBookingId, rebookTargetDate);
                          const [nextBookings, nextFrequent] = await Promise.all([getMyBookings(), getFrequentBookings().catch(() => [] as FrequentBooking[])]);
                          setBookings(nextBookings);
                          setFrequentBookings(nextFrequent);
                        },
                        "Repeat booking reserved.",
                        false
                      )
                    }
                  />
                </View>
              ))}
            </Surface>
          </Section>

          <Section title="Predicted slots" subtitle="Habit-based suggestions from your booking history.">
            {bookingPredictions.length === 0 ? <EmptyState text="No predictions yet." /> : null}
            {bookingPredictions.slice(0, 4).map((item, index) => (
              <Surface key={`${item.turfId}-${item.slotId}-${index}`} tone="player">
                <Text style={styles.cardTitle}>{item.turfName || "Suggested turf"}</Text>
                <Text style={styles.cardMeta}>{item.predictedDate || "Next match"} | {formatSlotTime(item.startTime)} - {formatSlotTime(item.endTime)}</Text>
                <Text style={styles.helperText}>{item.reason || "Repeat timing pattern detected."}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Automation setup" subtitle="Save recurring schedules, templates, and auto waitlist rules.">
            <Surface tone="player">
              <Text style={styles.cardTitle}>Recurring booking</Text>
              <FieldPair
                left={<LabeledInput label="Turf ID" value={recurringDraft.turfId} onChangeText={(value) => setRecurringDraft((current) => ({ ...current, turfId: value }))} keyboardType="numeric" />}
                right={<LabeledInput label="Slot ID" value={recurringDraft.slotId} onChangeText={(value) => setRecurringDraft((current) => ({ ...current, slotId: value }))} keyboardType="numeric" />}
              />
              <LabeledInput label="Next date" value={recurringDraft.nextBookingDate} onChangeText={(value) => setRecurringDraft((current) => ({ ...current, nextBookingDate: value }))} />
              <AppButton
                label="Save recurring"
                disabled={!recurringDraft.turfId || !recurringDraft.slotId}
                onPress={() =>
                  runAction(
                    async () => {
                      await createRecurringBooking({
                        turfId: Number(recurringDraft.turfId),
                        slotId: Number(recurringDraft.slotId),
                        frequency: recurringDraft.frequency,
                        preferredDay: "FRIDAY",
                        nextBookingDate: recurringDraft.nextBookingDate,
                      });
                      setRecurringBookings(await getRecurringBookings());
                    },
                    "Recurring schedule saved.",
                    false
                  )
                }
              />
            </Surface>

            <Surface tone="player">
              <Text style={styles.cardTitle}>Booking template</Text>
              <FieldPair
                left={<LabeledInput label="Name" value={templateDraft.templateName} onChangeText={(value) => setTemplateDraft((current) => ({ ...current, templateName: value }))} />}
                right={<LabeledInput label="Turf ID" value={templateDraft.turfId} onChangeText={(value) => setTemplateDraft((current) => ({ ...current, turfId: value }))} keyboardType="numeric" />}
              />
              <AppButton
                label="Save template"
                disabled={!templateDraft.templateName || !templateDraft.turfId}
                onPress={() =>
                  runAction(
                    async () => {
                      await createBookingTemplate({
                        templateName: templateDraft.templateName,
                        turfId: Number(templateDraft.turfId),
                        preferredDay: "FRIDAY",
                        preferredTime: templateDraft.preferredTime,
                        players: Number(templateDraft.players),
                      });
                      setBookingTemplates(await getBookingTemplates());
                    },
                    "Template saved.",
                    false
                  )
                }
              />
            </Surface>

            <Surface tone="player">
              <Text style={styles.cardTitle}>Auto waitlist</Text>
              <FieldPair
                left={<LabeledInput label="Slot ID" value={autoWaitlistDraft.slotId} onChangeText={(value) => setAutoWaitlistDraft((current) => ({ ...current, slotId: value }))} keyboardType="numeric" />}
                right={<LabeledInput label="Date" value={autoWaitlistDraft.preferredDate} onChangeText={(value) => setAutoWaitlistDraft((current) => ({ ...current, preferredDate: value }))} />}
              />
              <AppButton
                label="Save waitlist"
                disabled={!autoWaitlistDraft.slotId}
                tone="secondary"
                onPress={() =>
                  runAction(
                    async () => {
                      await createAutoWaitlist({
                        slotId: Number(autoWaitlistDraft.slotId),
                        preferredDate: autoWaitlistDraft.preferredDate,
                        autoBookEnabled: autoWaitlistDraft.autoBookEnabled,
                      });
                      setAutoWaitlists(await getAutoWaitlists());
                    },
                    "Auto waitlist saved.",
                    false
                  )
                }
              />
            </Surface>
          </Section>
        </View>
      );
    }

    if (playerTab === "weather") {
      return (
        <View style={styles.stack}>
          <Section title="Weather Safe Booking" subtitle="Rain risk, heat, humidity, and playability for cricket decisions.">
            <Surface tone="player">
              <Text style={styles.cardTitle}>City forecast</Text>
              <FieldPair
                left={<LabeledInput label="City" value={weatherCity} onChangeText={setWeatherCity} />}
                right={<AppButton label="Refresh" onPress={() => runAction(async () => setWeatherDashboard(await getWeatherDashboard(weatherCity)), "Weather refreshed.", false)} />}
              />
              {weatherDashboard?.current ? (
                <>
                  <View style={styles.metricGrid}>
                    <Metric label="Rain" value={`${Math.round(weatherDashboard.current.rainProbability)}%`} tone="player" />
                    <Metric label="Heat" value={`${Math.round(weatherDashboard.current.temperature)}C`} tone="player" />
                    <Metric label="Risk" value={weatherDashboard.current.riskLevel} tone="player" />
                    <Metric label="Refunds" value={weatherDashboard.weatherRefunds} tone="player" />
                  </View>
                  <Text style={styles.helperText}>{weatherDashboard.insight}</Text>
                </>
              ) : <EmptyState text="Pull to refresh or tap Refresh to load weather." />}
            </Surface>
          </Section>
          <Section title="Playability forecast" subtitle="Best time windows for your city.">
            {weatherDashboard?.forecast?.map((item) => (
              <Surface key={`${item.date}-${item.hour}`} tone={item.riskLevel === "HIGH" || item.riskLevel === "UNSAFE" ? "owner" : "player"}>
                <Text style={styles.cardTitle}>{item.hour}:00 - {item.summary}</Text>
                <Text style={styles.cardMeta}>Rain {Math.round(item.rainProbability)}% | Heat {Math.round(item.temperature)}C | Humidity {Math.round(item.humidity)}%</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "progress") {
      return (
        <View style={styles.stack}>
          <Section title="Cricket Progress" subtitle="XP, streaks, challenges, rewards, badges, and leaderboard status.">
            <Surface tone="player">
              <Text style={styles.cardTitle}>Level {gamificationProfile?.xp?.level ?? 1} - {gamificationProfile?.xp?.title || "Beginner"}</Text>
              <Text style={styles.cardMeta}>{gamificationProfile?.xp?.xp ?? 0} XP | Next target {gamificationProfile?.xp?.nextLevelXp ?? 250} XP</Text>
              <Text style={styles.helperText}>{playerPerformance?.insight || gamificationProfile?.insight || "Play, book, review, and invite friends to build your cricket progress."}</Text>
              <AppButton
                label="Track match played"
                onPress={() =>
                  runAction(
                    async () => setGamificationProfile(await awardGamificationXp({ activityType: "MATCH_PLAYED", reason: "Mobile match progress update" })),
                    "Match progress tracked.",
                    false
                  )
                }
              />
            </Surface>
            <View style={styles.metricGrid}>
              <Metric label="XP" value={gamificationProfile?.xp?.xp ?? 0} tone="player" />
              <Metric label="Level" value={gamificationProfile?.xp?.level ?? 1} tone="player" />
              <Metric label="Streak" value={gamificationProfile?.streaks?.[0]?.currentStreak ?? 0} tone="player" />
              <Metric label="Badges" value={gamificationProfile?.badges?.length ?? 0} tone="player" />
            </View>
          </Section>
          <Section title="Career Scorecard" subtitle="Completed live scores power your cricket profile, rankings, and achievements.">
            <View style={styles.metricGrid}>
              <Metric label="Matches" value={playerPerformance?.career?.matches ?? 0} tone="player" />
              <Metric label="Runs" value={playerPerformance?.batting?.runs ?? 0} tone="player" />
              <Metric label="Wickets" value={playerPerformance?.bowling?.wickets ?? 0} tone="player" />
              <Metric label="Impact" value={`${playerPerformance?.career?.impactScore ?? 0}%`} tone="player" />
            </View>
            <Surface tone="default">
              <Text style={styles.cardTitle}>{playerPerformance?.profile?.name || "Cricket profile"}</Text>
              <Text style={styles.cardMeta}>
                SR {playerPerformance?.batting?.strikeRate ?? 0} | Avg {playerPerformance?.batting?.average ?? 0} | Best bowling {playerPerformance?.bowling?.bestFigures || "0/0"}
              </Text>
              <Text style={styles.helperText}>Reputation {playerPerformance?.career?.reputationScore ?? 0}/100 | Consistency {playerPerformance?.career?.consistencyScore ?? 0}/100</Text>
            </Surface>
          </Section>
          <Section title="Performance Achievements" subtitle="Unlocked from scored match history.">
            {playerPerformance?.achievements?.map((achievement) => (
              <Surface key={achievement.code} tone={achievement.unlocked ? "player" : "default"}>
                <Text style={styles.cardTitle}>{achievement.unlocked ? "Unlocked" : "Locked"} - {achievement.name}</Text>
                <Text style={styles.cardMeta}>{achievement.progress}/{achievement.target}</Text>
                <Text style={styles.helperText}>{achievement.description}</Text>
              </Surface>
            )) || <EmptyState text="Complete a scored match to unlock cricket achievements." />}
          </Section>
          <Section title="Recent Scorecards" subtitle="Latest completed scorecards attached to your profile.">
            {playerPerformance?.recentMatches?.map((match) => (
              <Surface key={`${match.liveScoreId}-${match.playedAt}`} tone="default">
                <Text style={styles.cardTitle}>{match.title}</Text>
                <Text style={styles.cardMeta}>{match.result} | {match.runs} runs | {match.wickets} wickets | {match.overs} overs</Text>
              </Surface>
            )) || <EmptyState text="No completed scorecards yet." />}
          </Section>
          <Section title="Weekly Challenges" subtitle="Complete activity goals to unlock XP and rewards.">
            {gamificationProfile?.challenges?.map((challenge) => (
              <Surface key={challenge.id} tone={challenge.status === "COMPLETED" ? "player" : "default"}>
                <Text style={styles.cardTitle}>{challenge.title}</Text>
                <Text style={styles.cardMeta}>{challenge.progress}/{challenge.target} | +{challenge.xpReward} XP | {challenge.status}</Text>
                <Text style={styles.helperText}>{challenge.description}</Text>
              </Surface>
            )) || <EmptyState text="Open Progress to load challenges." />}
          </Section>
          <Section title="Rewards and Leaderboard" subtitle="Claim rewards and compare progress.">
            {gamificationProfile?.rewards?.map((reward) => (
              <Surface key={reward.id} tone="player">
                <Text style={styles.cardTitle}>{reward.label}</Text>
                <Text style={styles.cardMeta}>{reward.rewardType} | {reward.status}</Text>
                {reward.status === "AVAILABLE" ? (
                  <AppButton
                    label="Claim reward"
                    tone="secondary"
                    onPress={() => runAction(async () => {
                      await claimGamificationReward(reward.id);
                      setGamificationProfile(await getGamificationProfile());
                    }, "Reward claimed.", false)}
                  />
                ) : null}
              </Surface>
            ))}
            {gamificationProfile?.leaderboard?.map((entry) => (
              <Surface key={entry.userId} tone="default">
                <Text style={styles.cardTitle}>#{entry.rank} {entry.playerName}</Text>
                <Text style={styles.cardMeta}>Level {entry.level} | {entry.score} XP</Text>
              </Surface>
            ))}
            {performanceLeaderboard.map((entry) => (
              <Surface key={`performance-${entry.userId}`} tone="default">
                <Text style={styles.cardTitle}>#{entry.rank} {entry.playerName}</Text>
                <Text style={styles.cardMeta}>{entry.statLabel} | {entry.runs} runs | {entry.wickets} wickets</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "career") {
      const profileId = Number(careerProfile?.id || 0);
      return (
        <View style={styles.stack}>
          <Section title="Sports Career Profile" subtitle="Create a scout-ready profile, generate resume links, and submit achievements for verification.">
            <Surface tone="player">
              <Text style={styles.cardTitle}>Digital sports profile</Text>
              <FieldPair
                left={<LabeledInput label="Player name" value={careerProfileDraft.playerName} onChangeText={(value) => setCareerProfileDraft((current) => ({ ...current, playerName: value }))} />}
                right={<LabeledInput label="Email" value={careerProfileDraft.playerEmail} onChangeText={(value) => setCareerProfileDraft((current) => ({ ...current, playerEmail: value }))} />}
              />
              <FieldPair
                left={<LabeledInput label="City" value={careerProfileDraft.city || ""} onChangeText={(value) => setCareerProfileDraft((current) => ({ ...current, city: value }))} />}
                right={<LabeledInput label="Role" value={careerProfileDraft.playerRole || ""} onChangeText={(value) => setCareerProfileDraft((current) => ({ ...current, playerRole: value }))} />}
              />
              <LabeledInput
                label="Career bio"
                value={careerProfileDraft.bio || ""}
                onChangeText={(value) => setCareerProfileDraft((current) => ({ ...current, bio: value }))}
                multiline
              />
              <AppButton
                label="Create scout-ready profile"
                disabled={!careerProfileDraft.playerName || !careerProfileDraft.playerEmail}
                onPress={() =>
                  runAction(
                    async () => {
                      const created = await createCareerProfile(careerProfileDraft);
                      setCareerProfile(created);
                      setCareerMarketplace(await getCareerMarketplace({ city: selectedCity, sport: "CRICKET" }));
                    },
                    "Career profile created.",
                    false
                  )
                }
              />
            </Surface>

            {careerProfile ? (
              <Surface tone="player">
                <Text style={styles.cardTitle}>{careerProfile.playerName}</Text>
                <Text style={styles.cardMeta}>{careerProfile.sport} | {careerProfile.city || selectedCity} | {careerProfile.rankingBand}</Text>
                <View style={styles.metricGrid}>
                  <Metric label="Complete" value={`${careerProfile.profileCompleteness ?? 0}%`} tone="player" />
                  <Metric label="Score" value={careerProfile.rankingScore ?? 0} tone="player" />
                  <Metric label="Views" value={careerProfile.scoutViews ?? 0} tone="player" />
                  <Metric label="Downloads" value={careerProfile.resumeDownloads ?? 0} tone="player" />
                </View>
                <AppButton
                  label="Generate resume + QR"
                  tone="secondary"
                  onPress={() =>
                    runAction(
                      async () => setCareerResume(await generateCareerResume(profileId)),
                      "Resume generated.",
                      false
                    )
                  }
                />
              </Surface>
            ) : null}

            {careerResume ? (
              <Surface tone="default">
                <Text style={styles.cardTitle}>Resume share link</Text>
                <Text style={styles.cardMeta}>{careerResume.shareLink}</Text>
                <Text style={styles.helperText}>{careerResume.qrCode}</Text>
              </Surface>
            ) : null}
          </Section>

          <Section title="Achievement Verification" subtitle="Submit tournament, academy, certificate, and selection achievements for admin verification.">
            <Surface tone="default">
              <LabeledInput label="Achievement title" value={careerAchievementTitle} onChangeText={setCareerAchievementTitle} />
              <AppButton
                label="Submit achievement"
                disabled={!profileId || !careerAchievementTitle}
                tone="secondary"
                onPress={() =>
                  runAction(
                    async () => {
                      await addCareerAchievement(profileId, {
                        title: careerAchievementTitle,
                        achievementType: "TOURNAMENT",
                        issuer: "Truf Sports",
                        notes: "Submitted from mobile career tab.",
                      });
                      setCareerAchievementTitle("");
                    },
                    "Achievement submitted.",
                    false
                  )
                }
              />
            </Surface>
          </Section>

          <Section title="Career Opportunities" subtitle="Apply to trials, scholarships, recruitment posts, and academy admissions with one profile.">
            {careerOpportunities.length === 0 ? <EmptyState text="No career opportunities loaded yet." /> : null}
            {careerOpportunities.slice(0, 6).map((opportunity) => (
              <Surface key={`career-opportunity-${opportunity.id}`} tone="player">
                <Text style={styles.cardTitle}>{opportunity.title}</Text>
                <Text style={styles.cardMeta}>{opportunity.opportunityType} | {opportunity.organizationName} | {opportunity.location || selectedCity}</Text>
                <Text style={styles.helperText}>{opportunity.eligibility || opportunity.description || "Eligibility will be reviewed by the organizer."}</Text>
                <AppButton
                  label="Apply"
                  tone="secondary"
                  disabled={!profileId}
                  onPress={() =>
                    runAction(
                      async () => {
                        await applyCareerOpportunity(Number(opportunity.id), {
                          profileId,
                          statement: "Interested from the Truf Sports mobile career tab.",
                        });
                        setCareerOpportunities(await getCareerOpportunities({ sport: "CRICKET" }));
                      },
                      "Application submitted.",
                      false
                    )
                  }
                />
              </Surface>
            ))}
          </Section>

          <Section title="Talent Marketplace" subtitle="Verified, scout-ready athletes from career profiles and academy talent discovery.">
            {careerMarketplace.length === 0 ? <EmptyState text="No scout-ready profiles found for this city yet." /> : null}
            {careerMarketplace.slice(0, 6).map((talent) => (
              <Surface key={`career-talent-${talent.source || "profile"}-${talent.id}`} tone="default">
                <Text style={styles.cardTitle}>{talent.playerName}</Text>
                <Text style={styles.cardMeta}>{talent.sport} | {talent.city || selectedCity} | {talent.ageGroup || "OPEN"}</Text>
                <Text style={styles.helperText}>Score {talent.rankingScore ?? 0} | {talent.rankingBand || "FOUNDATION"} | {talent.source || "CAREER_PROFILE"}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "governance") {
      const nationalBodies = governanceFederations.filter((item) => String(item.bodyType) === "NATIONAL_FEDERATION");
      const associationBodies = governanceFederations.filter((item) => String(item.bodyType) !== "NATIONAL_FEDERATION");
      return (
        <View style={styles.stack}>
          <Section title="Sports Governance" subtitle="Follow federation hierarchy, association coverage, national ranking records, and compliance visibility.">
            <View style={styles.metricGrid}>
              <Metric label="Federations" value={nationalBodies.length} tone="player" />
              <Metric label="Associations" value={associationBodies.length} tone="player" />
              <Metric label="Rankings" value={governanceRankings.length} tone="player" />
              <Metric label="Compliance" value={averageGovernanceScore(governanceFederations)} tone="player" />
            </View>
            {governanceFederations.length === 0 ? <EmptyState text="No active federation bodies are published yet." /> : null}
            {governanceFederations.slice(0, 8).map((body) => (
              <Surface key={`governance-body-${body.id}`} tone="player">
                <Text style={styles.cardTitle}>{body.name}</Text>
                <Text style={styles.cardMeta}>{body.bodyType} | {body.sport} | {body.region || "National"}</Text>
                <Text style={styles.helperText}>Compliance {body.complianceScore ?? 0} | Members {body.affiliatedMembers ?? 0} | {body.status}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Published Rankings" subtitle="National player, team, academy, and coach rankings from federation governance data.">
            {governanceRankings.length === 0 ? <EmptyState text="No public rankings are available yet." /> : null}
            {governanceRankings.slice(0, 8).map((ranking) => (
              <Surface key={`governance-ranking-${ranking.id}`} tone="default">
                <Text style={styles.cardTitle}>#{ranking.rankPosition || "--"} {ranking.entityType}</Text>
                <Text style={styles.cardMeta}>{ranking.rankingType} | {ranking.category || "OPEN"} | Entity {ranking.entityId}</Text>
                <Text style={styles.helperText}>{ranking.points ?? 0} points | Federation {ranking.federationId}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "superapp") {
      const dashboard = superAppDashboard || {};
      const kpis = dashboard.kpis || {};
      const identities = Array.isArray(dashboard.identities) ? dashboard.identities : [];
      const wallets = Array.isArray(dashboard.wallets) ? dashboard.wallets : [];
      const rewards = Array.isArray(dashboard.rewards) ? dashboard.rewards : [];
      const memberships = Array.isArray(dashboard.memberships) ? dashboard.memberships : [];
      const timeline = Array.isArray(dashboard.timeline) ? dashboard.timeline : [];
      const controls = Array.isArray(dashboard.priorityControls) ? dashboard.priorityControls : [];
      const identity = dashboard.myIdentity || identities[0];
      const identityId = Number(identity?.id || 0);
      const walletAccount = wallets.find((item) => String(item.identityId) === String(identityId));
      const rewardAccount = rewards.find((item) => String(item.identityId) === String(identityId));
      const membership = memberships.find((item) => String(item.identityId) === String(identityId));

      const refreshSuperApp = async () => {
        setSuperAppDashboard(await getSuperAppDashboard().catch(() => null));
      };

      return (
        <View style={styles.stack}>
          <Section title="Sports Super App" subtitle="One Sports ID, wallet, membership, rewards, reputation, passport and timeline across Cricbook.">
            <View style={styles.metricGrid}>
              <Metric label="Sports IDs" value={kpis.sportsIdentities ?? identities.length} tone="player" />
              <Metric label="Wallets" value={kpis.walletAccounts ?? wallets.length} tone="player" />
              <Metric label="Passports" value={kpis.publicPassports ?? 0} tone="player" />
              <Metric label="Reputation" value={`${kpis.averageReputation ?? 0}/100`} tone="player" />
            </View>
            {identity ? (
              <Surface tone="player">
                <Text style={styles.cardTitle}>{identity.displayName}</Text>
                <Text style={styles.cardMeta}>{identity.sportsId} | {identity.primarySport} | {identity.city || selectedCity}</Text>
                <Text style={styles.helperText}>Identity {identity.identityScore ?? 0}/100 | Reputation {identity.reputationScore ?? 0}/100 | {identity.status}</Text>
                <View style={styles.metricGrid}>
                  <Metric label="Wallet" value={`Rs ${walletAccount?.balance ?? 0}`} tone="player" />
                  <Metric label="Rewards" value={`${rewardAccount?.pointsBalance ?? 0} pts`} tone="player" />
                  <Metric label="Plan" value={membership?.tier || "FREE"} tone="player" />
                  <Metric label="Age" value={identity.ageGroup || "OPEN"} tone="player" />
                </View>
                <View style={styles.actionRow}>
                  <AppButton
                    label="Passport"
                    onPress={() => identityId && runAction(async () => {
                      await generateSportsPassport(identityId, { visibility: "PUBLIC" });
                      await refreshSuperApp();
                    }, "Sports Passport generated.", false)}
                  />
                  <AppButton
                    label="Credit Rs 500"
                    tone="secondary"
                    onPress={() => identityId && runAction(async () => {
                      await creditUniversalWallet(identityId, { amount: 500, sourceModule: "MOBILE_SUPER_APP", note: "Mobile universal wallet credit" });
                      await refreshSuperApp();
                    }, "Universal wallet credited.", false)}
                  />
                </View>
                <View style={styles.actionRow}>
                  <AppButton
                    label="Earn 100 pts"
                    tone="secondary"
                    onPress={() => identityId && runAction(async () => {
                      await earnUniversalRewards(identityId, { points: 100, sourceModule: "MOBILE_SUPER_APP", note: "Mobile reward credit" });
                      await refreshSuperApp();
                    }, "Universal rewards credited.", false)}
                  />
                  <AppButton
                    label="Gold Plan"
                    tone="secondary"
                    onPress={() => identityId && runAction(async () => {
                      await createUnifiedMembership(identityId, { tier: "GOLD", billingCycle: "MONTHLY", price: 999, autoRenew: true });
                      await refreshSuperApp();
                    }, "Unified membership activated.", false)}
                  />
                </View>
                <AppButton
                  label="Add Timeline Milestone"
                  tone="secondary"
                  onPress={() => identityId && runAction(async () => {
                    await addSportsTimelineEvent(identityId, {
                      eventType: "MOBILE_SUPER_APP_MILESTONE",
                      sourceModule: "MOBILE_SUPER_APP",
                      title: "Mobile Super App profile updated",
                      visibility: "PRIVATE",
                    });
                    await refreshSuperApp();
                  }, "Timeline updated.", false)}
                />
              </Surface>
            ) : (
              <EmptyState text="No Sports Identity exists yet. Create one below to unlock the unified wallet, rewards, passport and membership flow." />
            )}
          </Section>

          <Section title="Create Sports Identity" subtitle="Auto-generates one global Sports ID and creates wallet/reward accounts.">
            <FieldPair
              left={<LabeledInput label="Name" value={superAppIdentityDraft.displayName} onChangeText={(value) => setSuperAppIdentityDraft((current) => ({ ...current, displayName: value }))} placeholder="Player name" />}
              right={<LabeledInput label="Email" value={superAppIdentityDraft.primaryEmail} onChangeText={(value) => setSuperAppIdentityDraft((current) => ({ ...current, primaryEmail: value }))} placeholder="player@example.com" />}
            />
            <FieldPair
              left={<LabeledInput label="Phone" value={superAppIdentityDraft.phone} onChangeText={(value) => setSuperAppIdentityDraft((current) => ({ ...current, phone: value }))} placeholder="9999999999" keyboardType="numeric" />}
              right={<LabeledInput label="City" value={superAppIdentityDraft.city} onChangeText={(value) => setSuperAppIdentityDraft((current) => ({ ...current, city: value }))} placeholder={selectedCity} />}
            />
            <AppButton
              label="Create Sports ID"
              onPress={() =>
                runAction(async () => {
                  await createSportsIdentity(superAppIdentityDraft);
                  setSuperAppIdentityDraft((current) => ({ ...current, displayName: "", primaryEmail: "", phone: "" }));
                  await refreshSuperApp();
                }, "Sports Identity created.", false)
              }
            />
          </Section>

          <Section title="Controls & Timeline" subtitle="Double-spend prevention, youth guardrails, uniqueness controls and unified activity history.">
            {controls.length === 0 ? <EmptyState text="Controls will appear after the dashboard loads." /> : null}
            {controls.map((item, index) => (
              <Surface key={`super-control-${String(item.control || index)}`} tone="player">
                <Text style={styles.cardTitle}>{item.control}</Text>
                <Text style={styles.cardMeta}>{item.status}</Text>
                <Text style={styles.helperText}>{item.detail}</Text>
              </Surface>
            ))}
            {timeline.slice(0, 5).map((item) => (
              <Surface key={`super-timeline-${item.id}`} tone="default">
                <Text style={styles.cardTitle}>{item.title}</Text>
                <Text style={styles.cardMeta}>{item.sourceModule} | {item.eventType}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "sportsos") {
      const dashboard = sportsOsDashboard || {};
      const kpis = dashboard.kpis || {};
      const grants = Array.isArray(dashboard.grants) ? dashboard.grants : [];
      const athletes = Array.isArray(dashboard.athletes) ? dashboard.athletes : [];
      const controls = Array.isArray(dashboard.priorityControls) ? dashboard.priorityControls : [];

      const refreshSportsOs = async () => {
        const [dashboardData, facilityData] = await Promise.all([
          getSportsOsDashboard().catch(() => null),
          searchSportsOsFacilities().catch(() => []),
        ]);
        setSportsOsDashboard(dashboardData);
        setSportsOsFacilities(facilityData);
      };

      return (
        <View style={styles.stack}>
          <Section title="Sports OS" subtitle="Public sports infrastructure, grants, census, volunteers and athlete development tracking.">
            <View style={styles.metricGrid}>
              <Metric label="Facilities" value={kpis.facilities ?? sportsOsFacilities.length} tone="player" />
              <Metric label="Public" value={kpis.publicBookableFacilities ?? 0} tone="player" />
              <Metric label="Players" value={kpis.censusPlayers ?? 0} tone="player" />
              <Metric label="Volunteers" value={kpis.volunteers ?? 0} tone="player" />
            </View>
            {sportsOsFacilities.length === 0 ? <EmptyState text="No public Sports OS facilities are published yet." /> : null}
            {sportsOsFacilities.slice(0, 6).map((facility) => (
              <Surface key={`sports-os-facility-${facility.id}`} tone="player">
                <Text style={styles.cardTitle}>{facility.name}</Text>
                <Text style={styles.cardMeta}>{facility.facilityType} | {facility.districtName} | {facility.ownershipType}</Text>
                <Text style={styles.helperText}>Public booking {facility.publicBookingEnabled ? "enabled" : "not enabled"} | {facility.maintenanceStatus || "OPERATIONAL"}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Volunteer & Grant Requests" subtitle="Submit public sports volunteer interest or a government development grant request from mobile.">
            <FieldPair
              left={<LabeledInput label="Volunteer name" value={sportsOsVolunteerDraft.fullName} onChangeText={(value) => setSportsOsVolunteerDraft((current) => ({ ...current, fullName: value }))} placeholder="Volunteer name" />}
              right={<LabeledInput label="Volunteer email" value={sportsOsVolunteerDraft.email} onChangeText={(value) => setSportsOsVolunteerDraft((current) => ({ ...current, email: value }))} placeholder="volunteer@example.com" />}
            />
            <FieldPair
              left={<LabeledInput label="State" value={sportsOsVolunteerDraft.stateName} onChangeText={(value) => setSportsOsVolunteerDraft((current) => ({ ...current, stateName: value }))} />}
              right={<LabeledInput label="District" value={sportsOsVolunteerDraft.districtName} onChangeText={(value) => setSportsOsVolunteerDraft((current) => ({ ...current, districtName: value }))} />}
            />
            <AppButton
              label="Register Volunteer"
              onPress={() =>
                runAction(async () => {
                  await registerSportsVolunteer(sportsOsVolunteerDraft);
                  setSportsOsVolunteerDraft((current) => ({ ...current, fullName: "", email: "", phone: "" }));
                  await refreshSportsOs();
                }, "Volunteer registered.", false)
              }
            />

            <FieldPair
              left={<LabeledInput label="Grant applicant" value={sportsOsGrantDraft.applicantName} onChangeText={(value) => setSportsOsGrantDraft((current) => ({ ...current, applicantName: value }))} placeholder="District Sports Office" />}
              right={<LabeledInput label="Amount" value={sportsOsGrantDraft.requestedAmount} onChangeText={(value) => setSportsOsGrantDraft((current) => ({ ...current, requestedAmount: value }))} keyboardType="numeric" />}
            />
            <AppButton
              label="Apply Grant"
              tone="secondary"
              onPress={() =>
                runAction(async () => {
                  await applyGovernmentGrant({
                    ...sportsOsGrantDraft,
                    requestedAmount: Number(sportsOsGrantDraft.requestedAmount || 0),
                  });
                  setSportsOsGrantDraft((current) => ({ ...current, applicantName: "" }));
                  await refreshSportsOs();
                }, "Grant application submitted.", false)
              }
            />
          </Section>

          <Section title="Governance Signals" subtitle="Funding, athlete pipeline and public accountability controls.">
            {controls.map((item, index) => (
              <Surface key={`sports-os-control-${String(item.name || index)}`} tone="player">
                <Text style={styles.cardTitle}>{item.name}</Text>
                <Text style={styles.cardMeta}>{item.status}</Text>
                <Text style={styles.helperText}>{item.detail}</Text>
              </Surface>
            ))}
            {grants.slice(0, 4).map((grant) => (
              <Surface key={`sports-os-grant-${grant.id}`} tone="default">
                <Text style={styles.cardTitle}>{grant.applicantName}</Text>
                <Text style={styles.cardMeta}>{grant.grantCode} | {grant.status}</Text>
                <Text style={styles.helperText}>Requested Rs {grant.requestedAmount ?? 0} | Approved Rs {grant.approvedAmount ?? 0}</Text>
              </Surface>
            ))}
            {athletes.slice(0, 4).map((athlete) => (
              <Surface key={`sports-os-athlete-${athlete.id}`} tone="default">
                <Text style={styles.cardTitle}>{athlete.athleteName}</Text>
                <Text style={styles.cardMeta}>{athlete.sport} | {athlete.currentLevel} | {athlete.districtName}</Text>
                <Text style={styles.helperText}>Progress {athlete.progressScore ?? 0}/100 | {athlete.selectionStatus || "TRACKING"}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "faneconomy") {
      const dashboard = fanEconomyDashboard || {};
      const kpis = dashboard.kpis || {};
      const clubs = Array.isArray(dashboard.clubs) ? dashboard.clubs : [];
      const collectibles = Array.isArray(dashboard.collectibles) ? dashboard.collectibles : [];
      const predictions = Array.isArray(dashboard.predictionGames) ? dashboard.predictionGames : [];
      const watchParties = Array.isArray(dashboard.watchParties) ? dashboard.watchParties : [];
      const rankings = Array.isArray(dashboard.fanRanking) ? dashboard.fanRanking : [];
      const controls = Array.isArray(dashboard.economyControls) ? dashboard.economyControls : [];
      const fanId = Number(fanEconomyActiveFanId || 0);
      const firstClubId = Number(clubs[0]?.id || 0);
      const firstCollectibleId = Number(collectibles[0]?.id || 0);
      const firstPredictionId = Number(predictions[0]?.id || 0);
      const firstWatchPartyId = Number(watchParties[0]?.id || 0);

      const refreshFanEconomy = async () => {
        setFanEconomyDashboard(await getFanEconomyDashboard().catch(() => null));
      };

      return (
        <View style={styles.stack}>
          <Section title="Fan Economy" subtitle="Fan profiles, clubs, limited collectibles, prediction games and watch parties.">
            <View style={styles.metricGrid}>
              <Metric label="Fans" value={kpis.fans ?? 0} tone="player" />
              <Metric label="Clubs" value={kpis.fanClubs ?? 0} tone="player" />
              <Metric label="Minted" value={kpis.mintedCollectibles ?? 0} tone="player" />
              <Metric label="Predictions" value={kpis.predictionGames ?? 0} tone="player" />
            </View>
            <FieldPair
              left={<LabeledInput label="Fan name" value={fanEconomyProfileDraft.displayName} onChangeText={(value) => setFanEconomyProfileDraft((current) => ({ ...current, displayName: value }))} placeholder="Fan name" />}
              right={<LabeledInput label="Fan email" value={fanEconomyProfileDraft.email} onChangeText={(value) => setFanEconomyProfileDraft((current) => ({ ...current, email: value }))} placeholder="fan@example.com" />}
            />
            <FieldPair
              left={<LabeledInput label="City" value={fanEconomyProfileDraft.city} onChangeText={(value) => setFanEconomyProfileDraft((current) => ({ ...current, city: value }))} />}
              right={<LabeledInput label="Favorite team" value={fanEconomyProfileDraft.favoriteTeam} onChangeText={(value) => setFanEconomyProfileDraft((current) => ({ ...current, favoriteTeam: value }))} />}
            />
            <FieldPair
              left={<LabeledInput label="Active fan id" value={fanEconomyActiveFanId} onChangeText={setFanEconomyActiveFanId} placeholder="Create or paste fan id" keyboardType="numeric" />}
              right={<LabeledInput label="Prediction" value={fanEconomyPredictionValue} onChangeText={setFanEconomyPredictionValue} />}
            />
            <AppButton
              label="Create Fan Profile"
              onPress={() =>
                runAction(async () => {
                  const created = await createFanEconomyProfile(fanEconomyProfileDraft);
                  if (created?.id) setFanEconomyActiveFanId(String(created.id));
                  await refreshFanEconomy();
                }, "Fan profile created.", false)
              }
            />
          </Section>

          <Section title="Fan Actions" subtitle="Use the first available club, collectible, prediction and watch party from the dashboard.">
            <View style={styles.actionRow}>
              <AppButton
                label="Join Club"
                tone="secondary"
                disabled={!fanId || !firstClubId}
                onPress={() => runAction(async () => {
                  await joinFanEconomyClub(firstClubId, fanId);
                  await refreshFanEconomy();
                }, "Fan club joined.", false)}
              />
              <AppButton
                label="Claim Collectible"
                tone="secondary"
                disabled={!fanId || !firstCollectibleId}
                onPress={() => runAction(async () => {
                  await claimFanEconomyCollectible(firstCollectibleId, fanId);
                  await refreshFanEconomy();
                }, "Collectible claimed.", false)}
              />
            </View>
            <View style={styles.actionRow}>
              <AppButton
                label="Submit Prediction"
                tone="secondary"
                disabled={!fanId || !firstPredictionId}
                onPress={() => runAction(async () => {
                  await submitFanEconomyPrediction(firstPredictionId, fanId, fanEconomyPredictionValue);
                  await refreshFanEconomy();
                }, "Prediction submitted.", false)}
              />
              <AppButton
                label="Join Watch Party"
                tone="secondary"
                disabled={!fanId || !firstWatchPartyId}
                onPress={() => runAction(async () => {
                  await joinFanEconomyWatchParty(firstWatchPartyId, fanId);
                  await refreshFanEconomy();
                }, "Watch party joined.", false)}
              />
            </View>
            <FieldPair
              left={<LabeledInput label="New watch party" value={fanEconomyWatchPartyTitle} onChangeText={setFanEconomyWatchPartyTitle} placeholder="Final watch party" />}
              right={<LabeledInput label="Capacity" value="50" onChangeText={() => undefined} editable={false} />}
            />
            <AppButton
              label="Create Watch Party"
              disabled={!fanId}
              onPress={() =>
                runAction(async () => {
                  await createFanEconomyWatchParty({
                    title: fanEconomyWatchPartyTitle || "Fan Watch Party",
                    hostFanProfileId: fanId,
                    sport: "CRICKET",
                    capacity: 50,
                    platform: "IN_APP",
                    accessMode: "PUBLIC",
                  });
                  setFanEconomyWatchPartyTitle("");
                  await refreshFanEconomy();
                }, "Watch party created.", false)
              }
            />
          </Section>

          <Section title="Fan Clubs & Collectibles" subtitle="Live fan economy inventory exposed to mobile.">
            {clubs.length === 0 ? <EmptyState text="No fan clubs yet." /> : null}
            {clubs.slice(0, 4).map((club) => (
              <Surface key={`fan-club-${club.id}`} tone="player">
                <Text style={styles.cardTitle}>{club.name}</Text>
                <Text style={styles.cardMeta}>{club.sport} | {club.clubType} | {club.memberCount ?? 0} members</Text>
                <Text style={styles.helperText}>{club.description || "Public fan community"}</Text>
              </Surface>
            ))}
            {collectibles.slice(0, 4).map((collectible) => (
              <Surface key={`fan-collectible-${collectible.id}`} tone="default">
                <Text style={styles.cardTitle}>{collectible.title}</Text>
                <Text style={styles.cardMeta}>{collectible.collectibleType} | {collectible.rarity}</Text>
                <Text style={styles.helperText}>Minted {collectible.mintedCount ?? 0}/{collectible.supplyLimit ?? 0}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Prediction, Ranking & Controls" subtitle="Fan rank points, live predictions and economy integrity signals.">
            {predictions.slice(0, 4).map((prediction) => (
              <Surface key={`fan-prediction-${prediction.id}`} tone="player">
                <Text style={styles.cardTitle}>{prediction.title}</Text>
                <Text style={styles.cardMeta}>{prediction.predictionType} | +{prediction.pointsForCorrect ?? 0} pts</Text>
                <Text style={styles.helperText}>Deadline {prediction.deadlineAt || "Open"}</Text>
              </Surface>
            ))}
            {rankings.slice(0, 4).map((fan) => (
              <Surface key={`fan-ranking-${fan.id}`} tone="default">
                <Text style={styles.cardTitle}>{fan.displayName}</Text>
                <Text style={styles.cardMeta}>{fan.favoriteSport} | {fan.rankPoints ?? 0} rank points</Text>
                <Text style={styles.helperText}>Fan score {fan.fanScore ?? 0}</Text>
              </Surface>
            ))}
            {controls.slice(0, 4).map((item, index) => (
              <Surface key={`fan-control-${String(item.name || index)}`} tone="default">
                <Text style={styles.cardTitle}>{item.name}</Text>
                <Text style={styles.cardMeta}>{item.status}</Text>
                <Text style={styles.helperText}>{item.detail}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "splits") {
      const bookingChoices = bookings.map((booking) => ({
        label: `${booking.turfName || `Booking #${booking.id}`} - ${booking.bookingDate}`,
        value: String(booking.id),
      }));
      const pendingSplits = splitPayments.filter((split) => split.status !== "PAID");

      return (
        <View style={styles.stack}>
          <Section title="Split Payments" subtitle="Create team collections, pay your share, and track pending cricket expenses.">
            <Surface tone="player">
              <Text style={styles.cardTitle}>Create collection</Text>
              <SelectField
                label="Booking"
                value={bookingChoices.find((item) => item.value === splitDraft.bookingId)?.label || "Choose booking"}
                onPress={() =>
                  openPicker(
                    "Choose booking",
                    bookingChoices.length > 0 ? bookingChoices : [{ label: "No bookings loaded", value: "" }],
                    (value) => setSplitDraft((current) => ({ ...current, bookingId: value }))
                  )
                }
              />
              <LabeledInput
                label="Player emails or phone numbers"
                value={splitDraft.players}
                onChangeText={(value) => setSplitDraft((current) => ({ ...current, players: value }))}
                placeholder="rahul@mail.com, 98765..."
                multiline
              />
              <SelectField
                label="Due date"
                value={splitDraft.dueDate}
                onPress={() => openPicker("Collection due date", dateChoices, (value) => setSplitDraft((current) => ({ ...current, dueDate: value })))}
              />
              <AppButton
                label="Create equal split"
                disabled={!splitDraft.bookingId || !splitDraft.players.trim()}
                onPress={() =>
                  runAction(
                    async () => {
                      const shares = splitDraft.players
                        .split(/[,\n]/)
                        .map((loginId) => loginId.trim())
                        .filter(Boolean)
                        .map((loginId) => ({ loginId }));
                      await createSplitPayment({
                        bookingId: Number(splitDraft.bookingId),
                        splitType: "EQUAL",
                        dueDate: splitDraft.dueDate,
                        shares,
                      });
                      setSplitDraft({ bookingId: "", players: "", dueDate: today });
                      setSplitPayments(await getMySplitPayments());
                    },
                    "Split collection created.",
                    false
                  )
                }
              />
            </Surface>
          </Section>

          <Section title="Pending shares" subtitle={`${pendingSplits.length} unpaid share(s) need attention.`}>
            {splitPayments.length === 0 ? <EmptyState text="No split payments found yet." /> : null}
            {splitPayments.map((split) => (
              <Surface key={split.id} tone={split.status === "PAID" ? "player" : "owner"}>
                <Text style={styles.cardTitle}>{split.turfName || `Booking #${split.bookingId}`}</Text>
                <Text style={styles.cardMeta}>Rs {split.amount} | {split.status} | due {split.dueDate || "not set"}</Text>
                <Text style={styles.cardMeta}>{split.bookingDate} {formatSlotTime(split.startTime)} - {formatSlotTime(split.endTime)}</Text>
                {split.status !== "PAID" ? (
                  <AppButton
                    label="Pay share"
                    tone="secondary"
                    onPress={() =>
                      runAction(
                        async () => {
                          await paySplitShare({ splitId: split.id, paymentReference: `MOBILE-${Date.now()}`, useWallet: false });
                          setSplitPayments(await getMySplitPayments());
                        },
                        "Share marked paid.",
                        false
                      )
                    }
                  />
                ) : null}
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (playerTab === "favorites") {
      return (
        <View style={styles.stack}>
          <Section title="My Favorites" subtitle="Your favorite sports turfs with fast booking shortcuts.">
            {favorites.length === 0 ? (
              <EmptyState text="No favorite turfs added yet. Search turfs and tap 'Save favorite' to add them here." />
            ) : (
              favorites.map((turf) => (
                <Surface key={`fav-${turf.id}`} tone="player">
                  <Text style={styles.cardTitle}>{turf.name}</Text>
                  <Text style={styles.cardMeta}>{turf.city} | {turf.location}</Text>
                  <Text style={styles.cardMeta}>Rs {turf.pricePerHour}/hr | {formatEnumLabel(turf.boxType)} | {formatEnumLabel(turf.pitchType)}</Text>
                  <View style={styles.actionRow}>
                    <AppButton
                      label="Book Now"
                      onPress={async () => {
                        setPlayerTab("search");
                        await openTurf(turf.id ?? 0);
                      }}
                    />
                    <AppButton
                      label="Remove"
                      tone="secondary"
                      onPress={() =>
                        runAction(
                          async () => {
                            await removeFavorite(turf.id ?? 0);
                            const updated = favorites.filter((f) => f.id !== turf.id);
                            setFavorites(updated);
                            await cacheFavorites(updated);
                          },
                          "Removed from favorites."
                        )
                      }
                    />
                  </View>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (playerTab === "foryou") {
      return (
        <View style={styles.stack}>
          <Section title="For You" subtitle="Website personalization, adapted to mobile cards.">
            {!personalization ? (
              <EmptyState text="Pull to refresh or open this tab again to load recommendations." />
            ) : (
              <Surface tone="player">
                <Text style={styles.cardTitle}>Recommended turfs</Text>
                {personalization.recommendedTurfs.length === 0 ? <EmptyState text="No recommendations yet." /> : null}
                {personalization.recommendedTurfs.map((turf) => (
                  <View key={turf.id} style={styles.listRow}>
                    <Text style={styles.listTitle}>{turf.name}</Text>
                    <Text style={styles.cardMeta}>{turf.city} | Rs {turf.pricePerHour}/hr</Text>
                  </View>
                ))}
                <Text style={styles.cardTitle}>Recent searches</Text>
                {personalization.recentSearches.map((entry) => (
                  <View key={entry.id} style={styles.listRow}>
                    <Text style={styles.listTitle}>{entry.city || "Any city"}</Text>
                    <Text style={styles.cardMeta}>{entry.date || "Flexible"} | {entry.boxType || "Any box"} | {entry.pitchType || "Any pitch"}</Text>
                  </View>
                ))}
                <Text style={styles.cardTitle}>Preferred locations</Text>
                <View style={styles.chipWrap}>
                  {personalization.preferredLocations.map((item) => <Chip key={item} label={item} active tone="player" />)}
                </View>
                <Text style={styles.cardTitle}>Recommended timings</Text>
                <View style={styles.chipWrap}>
                  {personalization.recommendedTimings.map((item) => <Chip key={item} label={item} active />)}
                </View>
              </Surface>
            )}
          </Section>
        </View>
      );
    }

    if (playerTab === "tournaments") {
      return (
        <View style={styles.stack}>
          <Section title="Tournaments Bracket Desk" subtitle="Browse active championships and coordinate team registrations.">
            {tournaments.length === 0 ? <EmptyState text="No active tournaments registered on Cricbook currently." /> : null}
            {tournaments.map((tournament) => (
              <Surface key={tournament.id} tone="player">
                <Text style={styles.cardTitle}>{tournament.name}</Text>
                <Text style={styles.cardMeta}>{tournament.city} | {tournament.status} | Rs {tournament.entryFee}</Text>
                <View style={styles.actionRow}>
                  <AppButton
                    label="Fixtures"
                    tone="secondary"
                    onPress={() =>
                      runAction(async () => {
                        setSelectedTournamentId(tournament.id);
                        setTournamentFixtures(await getTournamentFixtures(tournament.id));
                        setTournamentStandings(await getTournamentStandings(tournament.id));
                      }, "Tournament details loaded.", false)
                    }
                  />
                  {teams[0] ? (
                    <AppButton
                      label="Register team"
                      onPress={() => runAction(() => registerTeamForTournament(tournament.id, teams[0].id), "Team registration sent.")}
                    />
                  ) : null}
                </View>
              </Surface>
            ))}
            {selectedTournamentId ? (
              <Surface tone="player">
                <Text style={styles.cardTitle}>Tournament detail</Text>
                {tournamentFixtures.map((fixture) => (
                  <View key={fixture.id} style={styles.listRow}>
                    <Text style={styles.listTitle}>{fixture.teamAName} vs {fixture.teamBName}</Text>
                    <Text style={styles.cardMeta}>{fixture.stageLabel || `Round ${fixture.round}`} | {fixture.status}</Text>
                    {fixture.status === "COMPLETED" ? (
                      <Text style={styles.cardMeta}>
                        {fixture.winnerTeamName ? `Winner ${fixture.winnerTeamName}` : "Draw"} | {fixture.teamAScore}-{fixture.teamBScore}
                      </Text>
                    ) : null}
                  </View>
                ))}
                {tournamentStandings.map((standing) => (
                  <View key={standing.teamId} style={styles.listRow}>
                    <Text style={styles.listTitle}>{standing.teamName}</Text>
                    <Text style={styles.cardMeta}>
                      P {standing.played} | W {standing.wins} | D {standing.draws || 0} | L {standing.losses} | Pts {standing.points} | NRR {standing.netRunRate ?? standing.netRuns}
                    </Text>
                  </View>
                ))}
              </Surface>
            ) : null}
          </Section>
        </View>
      );
    }

    if (playerTab === "scoring") {
      return (
        <View style={styles.stack}>
          <Section title="Live Scoring Center" subtitle="Real-time match scoring and detailed dynamic scoreboard panel.">
            <Surface tone="player">
              <Text style={styles.cardTitle}>Create live score</Text>
              <FieldPair
                left={<LabeledInput label="Team A" value={liveScoreDraft.teamAName} onChangeText={(value) => setLiveScoreDraft((current) => ({ ...current, teamAName: value }))} />}
                right={<LabeledInput label="Team B" value={liveScoreDraft.teamBName} onChangeText={(value) => setLiveScoreDraft((current) => ({ ...current, teamBName: value }))} />}
              />
              <FieldPair
                left={<LabeledInput label="Title" value={liveScoreDraft.title} onChangeText={(value) => setLiveScoreDraft((current) => ({ ...current, title: value }))} />}
                right={<LabeledInput label="Max overs" value={liveScoreDraft.maxOvers} onChangeText={(value) => setLiveScoreDraft((current) => ({ ...current, maxOvers: value }))} keyboardType="numeric" />}
              />
              <AppButton
                label="Create live score"
                onPress={() =>
                  runAction(
                    () =>
                      createLiveScore({
                        title: liveScoreDraft.title,
                        teamAName: liveScoreDraft.teamAName,
                        teamBName: liveScoreDraft.teamBName,
                        battingTeam: liveScoreDraft.battingTeam || liveScoreDraft.teamAName,
                        maxOvers: Number(liveScoreDraft.maxOvers),
                      }),
                    "Live score created."
                  )
                }
              />
            </Surface>
            {liveScores.map((score) => (
              <Surface key={score.id} tone="player">
                <Text style={styles.cardTitle}>{score.title}</Text>
                <Text style={styles.cardMeta}>{score.teamAName} vs {score.teamBName}</Text>
                <Text style={styles.cardMeta}>{score.runs}/{score.wickets} in {score.overs} | {score.status}</Text>
                <View style={styles.actionRow}>
                  {score.status === "NOT_STARTED" && score.scorerId === sessionUser?.id ? (
                    <AppButton
                      label="Start Match"
                      onPress={() =>
                        runAction(async () => {
                          await startMatch(score.id);
                          await refreshPlayer(playerTab, false);
                        }, "Match started successfully!")
                      }
                    />
                  ) : (
                    <AppButton
                      label="Open scorecard"
                      tone="secondary"
                      onPress={() => void loadMatchCenter(score.id)}
                    />
                  )}
                </View>
              </Surface>
            ))}
            {selectedLiveScoreId && liveScoreDetail ? (
              <Surface tone="player">
                <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                  <Text style={[styles.cardTitle, { fontSize: 20 }]}>{liveScoreDetail.score.title}</Text>
                  <Pressable onPress={() => { setSelectedLiveScoreId(null); setLiveScoreDetail(null); }} style={styles.logoutButton}>
                    <Text style={[styles.logoutText, { color: theme.colors.player }]}>Close scorecard</Text>
                  </Pressable>
                </View>

                {/* Pulsing Match Header Card */}
                <View style={{ backgroundColor: theme.colors.panelSoft, padding: 18, borderRadius: 20, marginBottom: 16, borderLeftWidth: 6, borderLeftColor: liveScoreDetail.score.status === "LIVE" ? theme.colors.player : theme.colors.border }}>
                  <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 6 }}>
                    {liveScoreDetail.score.status === "LIVE" ? (
                      <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: "#10b981" }} />
                    ) : null}
                    <Text style={{ color: theme.colors.text, fontWeight: "900", letterSpacing: 0.5, fontSize: 13 }}>
                      {liveScoreDetail.score.status === "LIVE" ? "LIVE MATCH" : liveScoreDetail.score.status}
                    </Text>
                  </View>
                  <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "baseline", marginTop: 4 }}>
                    <Text style={{ color: theme.colors.text, fontSize: 32, fontWeight: "900" }}>
                      {liveScoreDetail.score.runs}/{liveScoreDetail.score.wickets}
                    </Text>
                    <Text style={{ color: theme.colors.muted, fontSize: 16, fontWeight: "700" }}>
                      {liveScoreDetail.score.overs} Overs
                    </Text>
                  </View>
                  <Text style={{ color: theme.colors.muted, marginTop: 8, fontSize: 13 }}>
                    Innings: {liveScoreDetail.score.innings} | Run Rate: {liveScoreDetail.score.runRate}
                  </Text>
                  {liveScoreDetail.score.targetRuns ? (
                    <Text style={{ color: theme.colors.warning, fontWeight: "800", marginTop: 4, fontSize: 13 }}>
                      Target: {liveScoreDetail.score.targetRuns} | Runs needed: {liveScoreDetail.score.targetRuns - liveScoreDetail.score.runs} (Req Rate: {liveScoreDetail.requiredRunRate || "N/A"})
                    </Text>
                  ) : null}
                </View>

                {/* Scorecard Tabs */}
                <View style={{ flexDirection: "row", gap: 4, marginBottom: 16, backgroundColor: theme.colors.panelSoft, borderRadius: 12, padding: 4 }}>
                  {(["scoreboard", "timeline", "commentary", "analytics"] as const).map((tab) => (
                    <Pressable
                      key={tab}
                      onPress={() => setMatchCenterTab(tab)}
                      style={{
                        flex: 1,
                        paddingVertical: 10,
                        alignItems: "center",
                        borderRadius: 8,
                        backgroundColor: matchCenterTab === tab ? theme.colors.player : "transparent",
                      }}
                    >
                      <Text style={{ color: matchCenterTab === tab ? "#ffffff" : theme.colors.text, fontWeight: "bold", fontSize: 12, textTransform: "capitalize" }}>{tab}</Text>
                    </Pressable>
                  ))}
                </View>

                {/* Tab Contents */}
                {matchCenterTab === "scoreboard" && (
                  <View style={{ gap: 12 }}>
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 15, marginBottom: 4 }}>Batting: {liveScoreDetail.score.battingTeam}</Text>
                    <View style={{ flexDirection: "row", justifyContent: "space-between", borderBottomWidth: 1, borderBottomColor: theme.colors.border, paddingBottom: 6 }}>
                      <Text style={{ color: theme.colors.muted, fontWeight: "bold", flex: 2 }}>Batter</Text>
                      <Text style={{ color: theme.colors.muted, fontWeight: "bold", width: 40, textAlign: "right" }}>R</Text>
                      <Text style={{ color: theme.colors.muted, fontWeight: "bold", width: 40, textAlign: "right" }}>B</Text>
                      <Text style={{ color: theme.colors.muted, fontWeight: "bold", width: 40, textAlign: "right" }}>4s</Text>
                      <Text style={{ color: theme.colors.muted, fontWeight: "bold", width: 40, textAlign: "right" }}>6s</Text>
                    </View>
                    {liveScoreDetail.strikerName ? (
                      <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 4 }}>
                        <Text style={{ color: theme.colors.text, fontWeight: "bold", flex: 2 }}>*{liveScoreDetail.strikerName}</Text>
                        <Text style={{ color: theme.colors.text, width: 40, textAlign: "right" }}>{liveScoreDetail.strikerRuns}</Text>
                        <Text style={{ color: theme.colors.text, width: 40, textAlign: "right" }}>{liveScoreDetail.strikerBalls}</Text>
                        <Text style={{ color: theme.colors.text, width: 40, textAlign: "right" }}>{liveScoreDetail.strikerFours}</Text>
                        <Text style={{ color: theme.colors.text, width: 40, textAlign: "right" }}>{liveScoreDetail.strikerSixes}</Text>
                      </View>
                    ) : null}
                    {liveScoreDetail.nonStrikerName ? (
                      <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 4 }}>
                        <Text style={{ color: theme.colors.muted, flex: 2 }}>{liveScoreDetail.nonStrikerName}</Text>
                        <Text style={{ color: theme.colors.muted, width: 40, textAlign: "right" }}>{liveScoreDetail.nonStrikerRuns}</Text>
                        <Text style={{ color: theme.colors.muted, width: 40, textAlign: "right" }}>{liveScoreDetail.nonStrikerBalls}</Text>
                        <Text style={{ color: theme.colors.muted, width: 40, textAlign: "right" }}>{liveScoreDetail.nonStrikerFours}</Text>
                        <Text style={{ color: theme.colors.muted, width: 40, textAlign: "right" }}>{liveScoreDetail.nonStrikerSixes}</Text>
                      </View>
                    ) : null}

                    {liveScoreDetail.bowlerName ? (
                      <View style={{ marginTop: 12 }}>
                        <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 15, marginBottom: 4 }}>Bowling: {liveScoreDetail.score.bowlingTeam}</Text>
                        <View style={{ flexDirection: "row", justifyContent: "space-between", borderBottomWidth: 1, borderBottomColor: theme.colors.border, paddingBottom: 6 }}>
                          <Text style={{ color: theme.colors.muted, fontWeight: "bold", flex: 2 }}>Bowler</Text>
                          <Text style={{ color: theme.colors.muted, fontWeight: "bold", width: 50, textAlign: "right" }}>O</Text>
                          <Text style={{ color: theme.colors.muted, fontWeight: "bold", width: 50, textAlign: "right" }}>R</Text>
                          <Text style={{ color: theme.colors.muted, fontWeight: "bold", width: 50, textAlign: "right" }}>W</Text>
                        </View>
                        <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 4 }}>
                          <Text style={{ color: theme.colors.text, fontWeight: "bold", flex: 2 }}>{liveScoreDetail.bowlerName}</Text>
                          <Text style={{ color: theme.colors.text, width: 50, textAlign: "right" }}>{liveScoreDetail.bowlerOvers}</Text>
                          <Text style={{ color: theme.colors.text, width: 50, textAlign: "right" }}>{liveScoreDetail.bowlerRuns}</Text>
                          <Text style={{ color: theme.colors.text, width: 50, textAlign: "right" }}>{liveScoreDetail.bowlerWickets}</Text>
                        </View>
                      </View>
                    ) : null}

                    {liveScoreDetail.partnershipRuns !== undefined ? (
                      <View style={{ marginTop: 12, padding: 12, backgroundColor: theme.colors.panelSoft, borderRadius: 12 }}>
                        <Text style={{ color: theme.colors.text, fontSize: 13, fontWeight: "bold", textAlign: "center" }}>
                          Active Partnership: {liveScoreDetail.partnershipRuns} Runs off {liveScoreDetail.partnershipBalls} Balls
                        </Text>
                      </View>
                    ) : null}
                  </View>
                )}

                {matchCenterTab === "timeline" && (
                  <View style={{ gap: 8 }}>
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 14 }}>Filter by Over:</Text>
                    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 8 }}>
                      {Array.from({ length: Math.ceil(liveScoreDetail.events.length / 6) || 1 }).map((_, i) => (
                        <Chip key={i} label={`Over ${i + 1}`} active={timelineOver === i + 1} onPress={() => setTimelineOver(i + 1)} tone="player" />
                      ))}
                    </ScrollView>
                    {liveScoreDetail.events
                      .filter((e) => Math.ceil(e.eventSequence / 6) === timelineOver)
                      .map((event) => (
                        <View key={event.id} style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: theme.colors.border }}>
                          <View style={{ flex: 1, gap: 2 }}>
                            <Text style={{ color: theme.colors.text, fontWeight: "bold" }}>Ball {event.eventSequence} - {event.runs} Run(s)</Text>
                            <Text style={{ color: theme.colors.muted, fontSize: 12 }}>Batter: {event.batter} | Bowler: {event.bowler} {event.note ? `| ${event.note}` : ""}</Text>
                            {event.wicket ? <Text style={{ color: theme.colors.danger, fontWeight: "bold", fontSize: 12 }}>WICKET: {event.wicketType}</Text> : null}
                            {event.extraRuns ? <Text style={{ color: "#f59e0b", fontWeight: "bold", fontSize: 12 }}>EXTRAS: {event.extraRuns} ({event.extraType})</Text> : null}
                          </View>
                          {liveScoreDetail.score.scorerId === sessionUser?.id && !liveScoreDetail.score.locked && (
                            <View style={{ flexDirection: "row", gap: 6 }}>
                              <AppButton
                                label="Edit"
                                tone="secondary"
                                onPress={() => {
                                  setEditingEventId(event.id);
                                  setEditRuns(String(event.runs));
                                  setEditExtraRuns(String(event.extraRuns));
                                  setEditExtraType(event.extraType || null);
                                  setEditWicket(event.wicket);
                                  setEditWicketType(event.wicketType || null);
                                }}
                              />
                              <AppButton
                                label="Del"
                                tone="danger"
                                onPress={() =>
                                  confirmAction(
                                    "Delete delivery?",
                                    `Delete Ball #${event.eventSequence}?`,
                                    "Delete",
                                    "danger",
                                    () =>
                                      void runAction(async () => {
                                        const res = await deleteBall(liveScoreDetail.score.id, event.id);
                                        setLiveScoreDetail(res);
                                      }, "Ball deleted.")
                                  )
                                }
                              />
                            </View>
                          )}
                        </View>
                      ))}

                    {/* Correction Modal Panel */}
                    {editingEventId ? (
                      <View style={{ marginTop: 12 }}>
                        <Surface tone="player">
                          <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 16, marginBottom: 8 }}>Edit Ball Event Sequence</Text>
                          <FieldPair
                            left={<LabeledInput label="Runs" value={editRuns} onChangeText={setEditRuns} keyboardType="numeric" />}
                            right={<LabeledInput label="Extra Runs" value={editExtraRuns} onChangeText={setEditExtraRuns} keyboardType="numeric" />}
                          />
                          <FieldPair
                            left={
                              <SelectField
                                label="Extra Type"
                                value={editExtraType || "None"}
                                onPress={() =>
                                  openPicker("Extra Type", [{ label: "None", value: "" }, { label: "Wide", value: "WIDE" }, { label: "No Ball", value: "NO_BALL" }, { label: "Leg Bye", value: "LEG_BYE" }, { label: "Bye", value: "BYE" }], (val) => setEditExtraType(val || null))
                                }
                              />
                            }
                            right={
                              <SelectField
                                label="Wicket"
                                value={editWicket ? "Yes" : "No"}
                                onPress={() =>
                                  openPicker("Wicket", [{ label: "No", value: "NO" }, { label: "Yes", value: "YES" }], (val) => {
                                    setEditWicket(val === "YES");
                                    if (val === "YES") setEditWicketType("BOWLED");
                                  })
                                }
                              />
                            }
                          />
                          {editWicket ? (
                            <SelectField
                              label="Wicket Type"
                              value={editWicketType || "BOWLED"}
                              onPress={() =>
                                openPicker("Wicket Type", [{ label: "Bowled", value: "BOWLED" }, { label: "Caught", value: "CAUGHT" }, { label: "Run Out", value: "RUN_OUT" }, { label: "LBW", value: "LBW" }, { label: "Stumped", value: "STUMPED" }], setEditWicketType)
                              }
                            />
                          ) : null}
                          <View style={{ flexDirection: "row", gap: 10, marginTop: 10 }}>
                            <AppButton
                              label="Save corrections"
                              onPress={() =>
                                runAction(async () => {
                                  const res = await editBall(liveScoreDetail.score.id, editingEventId, {
                                    runs: Number(editRuns),
                                    extraRuns: Number(editExtraRuns),
                                    extraType: editExtraType,
                                    wicket: editWicket,
                                    wicketType: editWicketType,
                                    batter: liveScoreDetail.events.find((e) => e.id === editingEventId)?.batter || "Striker",
                                    bowler: liveScoreDetail.events.find((e) => e.id === editingEventId)?.bowler || "Bowler",
                                  });
                                  setLiveScoreDetail(res);
                                  setEditingEventId(null);
                                }, "Ball corrected.")
                              }
                            />
                            <AppButton label="Cancel" tone="secondary" onPress={() => setEditingEventId(null)} />
                          </View>
                        </Surface>
                      </View>
                    ) : null}
                  </View>
                )}

                {matchCenterTab === "commentary" && (
                  <View style={{ gap: 8 }}>
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 15, marginBottom: 4 }}>Live Commentary Feed</Text>
                    {liveScoreDetail.events.slice().reverse().map((event) => (
                      <View key={event.id} style={{ padding: 12, backgroundColor: theme.colors.panelSoft, borderRadius: 12, gap: 4 }}>
                        <Text style={{ color: theme.colors.player, fontWeight: "bold", fontSize: 13 }}>Ball {event.eventSequence}</Text>
                        <Text style={{ color: theme.colors.text, lineHeight: 20 }}>{event.note || "Recorded delivery by scorer."}</Text>
                      </View>
                    ))}
                  </View>
                )}

                {matchCenterTab === "analytics" && (
                  <View style={{ gap: 16 }}>
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 15 }}>Match Progress worm chart</Text>
                    {analyticsData ? (
                      <View style={{ gap: 12 }}>
                        {/* Worm graph representation (visual heights based on cumulative runs) */}
                        <Text style={{ color: theme.colors.player, fontWeight: "bold" }}>Team A worm: {analyticsData.wormTeamA.join(" -> ")}</Text>
                        <Text style={{ color: theme.colors.warning, fontWeight: "bold" }}>Team B worm: {analyticsData.wormTeamB.join(" -> ")}</Text>

                        <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 15, marginTop: 12 }}>Historical Partnerships</Text>
                        {analyticsData.partnerships.map((p: any, idx: number) => (
                          <View key={idx} style={{ padding: 10, backgroundColor: theme.colors.panelSoft, borderRadius: 10 }}>
                            <Text style={{ color: theme.colors.text, fontWeight: "bold" }}>{p.batsman1} & {p.batsman2}</Text>
                            <Text style={{ color: theme.colors.muted }}>{p.runs} runs off {p.balls} balls</Text>
                          </View>
                        ))}
                      </View>
                    ) : (
                      <AppButton label="Refresh analytics" onPress={() => runAction(async () => setAnalyticsData(await getMatchAnalytics(liveScoreDetail.score.id)), "Analytics loaded.", false)} />
                    )}
                  </View>
                )}

                {/* Scorer Console Grid (Module 2) */}
                {liveScoreDetail.score.scorerId === sessionUser?.id && !liveScoreDetail.score.locked && (
                  <View style={{ marginTop: 20, borderTopWidth: 2, borderTopColor: theme.colors.border, paddingTop: 16, gap: 12 }}>
                    <Text style={{ color: theme.colors.player, fontWeight: "bold", fontSize: 16 }}>Scorer Console Engine</Text>

                    <FieldPair
                      left={<LabeledInput label="Striker Name" value={ballBatter} onChangeText={setBallBatter} placeholder="Virat Kohli" />}
                      right={<LabeledInput label="Bowler Name" value={ballBowler} onChangeText={setBallBowler} placeholder="Bumrah" />}
                    />
                    <LabeledInput label="Scorer custom commentary note" value={ballNote} onChangeText={setBallNote} placeholder="Brilliant boundary through cover..." />

                    {/* Interactive Ball Scoring Grid */}
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 13, marginTop: 4 }}>Add Run / Event Button Grid:</Text>
                    <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
                      {["0", "1", "2", "3", "4", "6"].map((runs) => (
                        <Pressable
                          key={runs}
                          onPress={() =>
                            runAction(async () => {
                              const res = await recordBall(liveScoreDetail.score.id, {
                                runs: Number(runs),
                                extraRuns: 0,
                                wicket: false,
                                batter: ballBatter || "Striker",
                                bowler: ballBowler || "Bowler",
                                note: ballNote,
                              });
                              setLiveScoreDetail(res);
                              setBallNote("");
                            }, `Scored ${runs} runs.`, false)
                          }
                          style={{ width: 48, height: 48, borderRadius: 24, backgroundColor: theme.colors.player, alignItems: "center", justifyContent: "center" }}
                        >
                          <Text style={{ color: "#ffffff", fontWeight: "bold", fontSize: 16 }}>{runs}</Text>
                        </Pressable>
                      ))}

                      {/* Extras Grid */}
                      {["WD", "NB", "LB", "B"].map((extra) => (
                        <Pressable
                          key={extra}
                          onPress={() =>
                            openPicker(
                              `Select Extra Runs for ${extra}`,
                              [{ label: "1 run", value: "1" }, { label: "2 runs", value: "2" }, { label: "3 runs", value: "3" }, { label: "4 runs", value: "4" }, { label: "5 runs", value: "5" }],
                              (runs) =>
                                void runAction(async () => {
                                  const res = await recordBall(liveScoreDetail.score.id, {
                                    runs: 0,
                                    extraRuns: Number(runs),
                                    extraType: extra === "WD" ? "WIDE" : extra === "NB" ? "NO_BALL" : extra === "LB" ? "LEG_BYE" : "BYE",
                                    wicket: false,
                                    batter: ballBatter || "Striker",
                                    bowler: ballBowler || "Bowler",
                                    note: ballNote,
                                  });
                                  setLiveScoreDetail(res);
                                  setBallNote("");
                                }, `Recorded ${extra} delivery.`, false)
                            )
                          }
                          style={{ width: 48, height: 48, borderRadius: 24, backgroundColor: "#f59e0b", alignItems: "center", justifyContent: "center" }}
                        >
                          <Text style={{ color: "#ffffff", fontWeight: "bold", fontSize: 13 }}>{extra}</Text>
                        </Pressable>
                      ))}

                      {/* Wicket button */}
                      <Pressable
                        onPress={() =>
                          openPicker(
                            "Select Wicket Type",
                            [{ label: "Bowled", value: "BOWLED" }, { label: "Caught", value: "CAUGHT" }, { label: "LBW", value: "LBW" }, { label: "Stumped", value: "STUMPED" }, { label: "Run Out", value: "RUN_OUT" }],
                            (wicketType) =>
                              void runAction(async () => {
                                const res = await recordBall(liveScoreDetail.score.id, {
                                  runs: 0,
                                  extraRuns: 0,
                                  wicket: true,
                                  wicketType,
                                  batter: ballBatter || "Striker",
                                  bowler: ballBowler || "Bowler",
                                  note: ballNote,
                                });
                                setLiveScoreDetail(res);
                                  setBallNote("");
                              }, "Wicket recorded!", false)
                          )
                        }
                        style={{ width: 48, height: 48, borderRadius: 24, backgroundColor: theme.colors.danger, alignItems: "center", justifyContent: "center" }}
                      >
                        <Text style={{ color: "#ffffff", fontWeight: "bold", fontSize: 15 }}>W</Text>
                      </Pressable>
                    </View>

                    {/* Operational controls */}
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 13, marginTop: 8 }}>Match Scoring Operations:</Text>
                    <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}>
                      <AppButton
                        label="Undo Ball"
                        tone="secondary"
                        onPress={() =>
                          confirmAction(
                            "Undo last delivery?",
                            "Revert the score and erase the last ball sequence recorded?",
                            "Undo ball",
                            "danger",
                            () =>
                              void runAction(async () => {
                                const res = await undoBall(liveScoreDetail.score.id);
                                setLiveScoreDetail(res);
                              }, "Last delivery undone.")
                          )
                        }
                      />
                      {liveScoreDetail.score.status === "LIVE" ? (
                        <AppButton
                          label="Pause Match"
                          tone="secondary"
                          onPress={() =>
                            runAction(async () => {
                              const next = await pauseMatch(liveScoreDetail.score.id);
                              setLiveScoreDetail((prev) => prev ? { ...prev, score: next } : null);
                            }, "Match paused.")
                          }
                        />
                      ) : (
                        <AppButton
                          label="Resume Match"
                          tone="secondary"
                          onPress={() =>
                            runAction(async () => {
                              const next = await resumeMatch(liveScoreDetail.score.id);
                              setLiveScoreDetail((prev) => prev ? { ...prev, score: next } : null);
                            }, "Match resumed.")
                          }
                        />
                      )}
                      <AppButton
                        label="Complete Match"
                        onPress={() =>
                          confirmAction(
                            "Complete and lock scorecard?",
                            "This compiles stats, closes the match center and publishes results permanently.",
                            "Complete permanent",
                            "primary",
                            () =>
                              void runAction(async () => {
                                const next = await completeScore(liveScoreDetail.score.id, liveScoreDetail.score.id);
                                setLiveScoreDetail((prev) => prev ? { ...prev, score: next } : null);
                              }, "Match completed & archived.")
                          )
                        }
                      />
                      <AppButton
                        label="Cancel Match"
                        tone="danger"
                        onPress={() =>
                          confirmAction(
                            "Cancel Match?",
                            "This flags the match as cancelled permanently.",
                            "Cancel permanent",
                            "danger",
                            () =>
                              void runAction(async () => {
                                const next = await cancelMatch(liveScoreDetail.score.id);
                                setLiveScoreDetail((prev) => prev ? { ...prev, score: next } : null);
                              }, "Match cancelled.")
                          )
                        }
                      />
                    </View>
                  </View>
                )}

                {/* Admin controls panel */}
                {session?.role === "ADMIN" && (
                  <View style={{ marginTop: 16, borderTopWidth: 1, borderTopColor: theme.colors.border, paddingTop: 12, gap: 8 }}>
                    <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 14 }}>Super Admin Operations Console:</Text>
                    <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}>
                      <AppButton
                        label="Lock Match"
                        tone="danger"
                        onPress={() =>
                          confirmAction(
                            "Lock scorecard?",
                            "Permanently freezes all ball scoring and stats editing.",
                            "Lock Match",
                            "danger",
                            () =>
                              void runAction(async () => {
                                const next = await lockMatch(liveScoreDetail.score.id);
                                setLiveScoreDetail((prev) => prev ? { ...prev, score: next } : null);
                              }, "Match locked.")
                          )
                        }
                      />
                      <AppButton
                        label="Override Result"
                        tone="danger"
                        onPress={() =>
                          openPicker(
                            "Select Override Winner",
                            [{ label: liveScoreDetail.score.teamAName, value: liveScoreDetail.score.teamAName }, { label: liveScoreDetail.score.teamBName, value: liveScoreDetail.score.teamBName }, { label: "No Winner (Tie)", value: "Tie" }],
                            (winner) =>
                              void runAction(async () => {
                                const next = await overrideMatchResult(liveScoreDetail.score.id, winner);
                                setLiveScoreDetail((prev) => prev ? { ...prev, score: next } : null);
                              }, "Admin override applied.")
                          )
                        }
                      />
                    </View>
                  </View>
                )}
              </Surface>
            ) : null}
          </Section>
        </View>
      );
    }

    if (playerTab === "membership") {
      return (
        <View style={styles.stack}>
          <Section title="Customer Growth Status" subtitle="CLV, VIP tier, loyalty balance, and membership triggers.">
            <View style={styles.metricGrid}>
              <Metric label="Tier" value={growthDashboard?.membership?.tier || "BRONZE"} tone="player" />
              <Metric label="CLV" value={`Rs ${growthDashboard?.crm?.customerLifetimeValue ?? 0}`} tone="player" />
              <Metric label="Projected" value={`Rs ${growthDashboard?.crm?.projectedClv ?? 0}`} tone="player" />
              <Metric label="VIP" value={growthDashboard?.crm?.vipQualified ? "Yes" : "No"} tone="player" />
            </View>
          </Section>
          <Section title="Cricbook Premium Memberships" subtitle="Join premium plans for priority slot booking overrides, wallet cashbacks, and featured listing flags.">
            {plans.length === 0 ? (
              <EmptyState text="No membership plans currently available. Tap refresh or check back later." />
            ) : (
              plans.map((plan) => (
                <Surface key={`plan-${plan.id}`} tone="player">
                  <Text style={{ fontSize: 12, fontWeight: "800", color: primaryColor, textTransform: "uppercase" }}>
                    {formatEnumLabel(plan.type)}
                  </Text>
                  <Text style={[styles.cardTitle, { fontSize: 20, marginTop: 4 }]}>{plan.name}</Text>
                  <Text style={{ fontSize: 28, fontWeight: "900", color: theme.colors.text, marginTop: 6 }}>
                    {getConvertedPrice(plan.price, selectedCity)}
                  </Text>
                  <Text style={[styles.cardMeta, { marginTop: 4 }]}>
                    {plan.durationDays} Days Duration | {plan.discountPercent}% Instant Billing Discount
                  </Text>
                  <Text style={[styles.helperText, { marginTop: 4 }]}>{plan.description || "Priority turf slot override and platform discount rewards."}</Text>

                  {plan.type === "OWNER_FEATURED" ? (
                    <LabeledInput
                      label="Target Turf ID for Featured Boost"
                      value={membershipTurfIds[plan.id] || ""}
                      onChangeText={(value) => setMembershipTurfIds({ ...membershipTurfIds, [plan.id]: value })}
                      placeholder="Enter turf ID e.g. 5"
                      keyboardType="numeric"
                    />
                  ) : null}

                  <AppButton
                    label="Subscribe & Activate"
                    onPress={() =>
                      confirmAction(
                        "Activate Membership Plan",
                        `Purchase subscription to ${plan.name} for ${getConvertedPrice(plan.price, selectedCity)}?`,
                        "Activate plan",
                        "primary",
                        () =>
                          void runAction(async () => {
                            const turfId = plan.type === "OWNER_FEATURED" ? Number(membershipTurfIds[plan.id]) : undefined;
                            if (plan.type === "OWNER_FEATURED" && !turfId) {
                              throw new Error("Turf ID is required for owner featured listings.");
                            }
                            await subscribeMembership(plan.id, turfId);
                            const [p, m] = await Promise.all([getMembershipPlans(), getMyMemberships()]);
                            setPlans(p);
                            setMyMemberships(m);
                          }, "Membership activated successfully!")
                      )
                    }
                  />
                </Surface>
              ))
            )}

            <Section title="My Active Memberships">
              {myMemberships.length === 0 ? (
                <EmptyState text="You do not have any active Cricbook memberships at the moment." />
              ) : (
                myMemberships.map((membership) => (
                  <Surface key={`my-m-${membership.id}`} tone="player">
                    <Text style={styles.cardTitle}>{membership.planName || "Premium Club Plan"}</Text>
                    <Text style={styles.cardMeta}>
                      Status: {membership.status} | Ends: {membership.endsAt?.slice(0, 10) || "Lifetime"}
                    </Text>
                    {membership.turfName ? <Text style={styles.helperText}>Linked Turf: {membership.turfName}</Text> : null}
                  </Surface>
                ))
              )}
            </Section>
          </Section>
        </View>
      );
    }

    if (playerTab === "smart") {
      return (
        <View style={styles.stack}>
          <Section title="Smart Recommendation Platform" subtitle="Demand-based pricing models, slot suggestions, and premium turf rankings.">
            <Surface tone="player">
              <FieldPair
                left={<LabeledInput label="Filter by City" value={smartCity} onChangeText={setSmartCity} placeholder="Surat" />}
                right={<LabeledInput label="Turf ID for Pricing" value={smartTurfId} onChangeText={setSmartTurfId} placeholder="1" keyboardType="numeric" />}
              />
              <AppButton
                label="Generate Smart Insights"
                onPress={() =>
                  runAction(
                    async () => {
                      const res = await getSmartRecommendations({
                        city: smartCity || undefined,
                        turfId: smartTurfId ? Number(smartTurfId) : undefined,
                      });
                      setSmartData(res);
                    },
                    "Smart insights refreshed.",
                    false
                  )
                }
              />
            </Surface>

            {smartData ? (
              <View style={{ gap: 16 }}>
                <Surface tone="player">
                  <Text style={styles.cardTitle}>Dynamic Suggested Pricing</Text>
                  <Text style={{ fontSize: 32, fontWeight: "900", color: "#ffd700", marginTop: 8 }}>
                    Rs {smartData.suggestedPrice || smartData.pricingDetails?.suggestedPrice || 1200}
                  </Text>
                  <Text style={{ color: theme.colors.muted, marginTop: 6, lineHeight: 20 }}>
                    {smartData.pricingReason || smartData.pricingDetails?.reason || "Based on high slot booking frequency and optimal weekend utilization statistics."}
                  </Text>
                </Surface>

                <Surface tone="player">
                  <Text style={styles.cardTitle}>Peak Demand Slots</Text>
                  <View style={[styles.chipWrap, { marginTop: 10 }]}>
                    {(smartData.bestTimings || ["18:00 - 19:00", "19:00 - 20:00", "20:00 - 21:00"]).map((time: string) => (
                      <Chip key={time} label={time} active tone="player" />
                    ))}
                  </View>
                </Surface>

                <Section title="Premium Best Ranked Turfs">
                  {(!smartData.bestTurfs || smartData.bestTurfs.length === 0) ? (
                    <EmptyState text="No specific turfs cached for this region." />
                  ) : (
                    smartData.bestTurfs.map((turf: Turf) => (
                      <Surface key={`smart-turf-${turf.id}`} tone="player">
                        <Text style={styles.cardTitle}>{turf.name}</Text>
                        <Text style={styles.cardMeta}>{turf.city} | {turf.location}</Text>
                        <Text style={styles.cardMeta}>Rating: {turf.rating || 0}/5 | Rs {turf.pricePerHour}/hr</Text>
                      </Surface>
                    ))
                  )}
                </Section>
              </View>
            ) : (
              <EmptyState text="Enter filters and tap generate above to fetch smart recommender insights." />
            )}
          </Section>

          <Section title="🤖 Cricbook AI Chat Assistant" subtitle="Parse booking slots and direct checkouts instantly using natural chat.">
            <Surface tone="player">
              <LabeledInput
                label="Ask AI Assistant (e.g. 'Book Surat saturday 7PM')"
                value={mobileChatMessage}
                onChangeText={setMobileChatMessage}
                placeholder="Type message here..."
              />
              <AppButton
                label={mobileChatBusy ? "AI Analyzing..." : "Send to AI"}
                disabled={mobileChatBusy}
                onPress={() => {
                  setMobileChatBusy(true);
                  chatAIAssistant(mobileChatMessage, mobileChatConversationId || undefined)
                    .then((res) => {
                      setMobileChatReply(res.reply);
                      setMobileChatAction(res.action || "");
                      setMobileChatLink(res.checkoutLink || res.insightsLink || "");
                      setMobileChatIntent(res.intent || "GENERAL_ASSISTANT");
                      setMobileChatConfidence(typeof res.confidence === "number" ? res.confidence : null);
                      setMobileChatConversationId(res.conversationId || mobileChatConversationId);
                      setMobileChatExplainability(Array.isArray(res.explainability) ? res.explainability.map(String) : []);
                    })
                    .catch((err) => {
                      setMobileChatReply("Sorry, had trouble parsing your request.");
                      setMobileChatIntent("ERROR");
                      setMobileChatConfidence(null);
                      setMobileChatExplainability([]);
                    })
                    .finally(() => setMobileChatBusy(false));
                }}
              />
              {mobileChatReply ? (
                <View style={{ marginTop: 12, padding: 12, backgroundColor: theme.colors.border, borderRadius: 12, gap: 6 }}>
                  <Text style={{ color: theme.colors.text, fontWeight: "bold", fontSize: 13 }}>AI Response:</Text>
                  <View style={[styles.chipWrap, { marginTop: 0 }]}>
                    {mobileChatIntent ? <Chip label={mobileChatIntent} active tone="player" /> : null}
                    {mobileChatConfidence !== null ? <Chip label={`${Math.round(mobileChatConfidence * 100)}% confidence`} active tone="player" /> : null}
                  </View>
                  <Text style={{ color: theme.colors.text, fontSize: 13, lineHeight: 18 }}>{mobileChatReply}</Text>
                  {mobileChatExplainability.length > 0 ? (
                    <View style={{ gap: 4 }}>
                      <Text style={{ color: theme.colors.muted, fontWeight: "bold", fontSize: 12 }}>Why this answer</Text>
                      {mobileChatExplainability.map((item) => (
                        <Text key={item} style={{ color: theme.colors.muted, fontSize: 12, lineHeight: 17 }}>• {item}</Text>
                      ))}
                    </View>
                  ) : null}
                  <View style={styles.actionRow}>
                    <AppButton
                      label="Like"
                      tone="secondary"
                      onPress={() => runAction(() => sendAIRecommendationFeedback({ targetType: "CHAT", feedbackType: "LIKE", reason: mobileChatIntent || "assistant" }), "AI feedback saved.")}
                    />
                    <AppButton
                      label="Dislike"
                      tone="secondary"
                      onPress={() => runAction(() => sendAIRecommendationFeedback({ targetType: "CHAT", feedbackType: "DISLIKE", reason: mobileChatIntent || "assistant" }), "AI feedback saved.")}
                    />
                  </View>
                  {mobileChatAction === "CHECKOUT" && mobileChatLink ? (
                    <View style={{ marginTop: 10 }}>
                      <AppButton
                        label="⚡ Complete Instant Slot Checkout"
                        onPress={() => {
                          Alert.alert("Checkout redirect", `Booking is ready. Link parsed: ${mobileChatLink}`);
                        }}
                      />
                    </View>
                  ) : null}
                </View>
              ) : null}
            </Surface>
          </Section>
        </View>
      );
    }

    if (playerTab === "wallet") {
      return (
        <View style={styles.stack}>
          <Section title="Wallet" subtitle="Balance, top-up and coupon validation from the same app surface.">
            <View style={styles.metricGrid}>
              <Metric label="Balance" value={`Rs ${wallet?.balance ?? 0}`} tone="player" />
              <Metric label="Coupons" value={coupons.length} tone="player" />
              <Metric label="Points" value={growthDashboard?.loyalty?.pointsBalance ?? 0} tone="player" />
              <Metric label="Next Reward" value={`${growthDashboard?.loyalty?.nextMilestonePoints ?? 100} pts`} tone="player" />
            </View>
            <Surface tone="player">
              <FieldPair
                left={
                  <SelectField
                    label="Add money"
                    value={`Rs ${couponAmount}`}
                    onPress={() => openPicker("Top up wallet", topUpChoices, setCouponAmount, "Choose a quick top-up amount.")}
                  />
                }
                right={<LabeledInput label="Coupon code" value={couponCode} onChangeText={setCouponCode} placeholder="WELCOME10" />}
              />
              <View style={styles.actionRow}>
                <AppButton label="Add money" onPress={() => runAction(() => addMoney(Number(couponAmount)), "Wallet funded.")} />
                <AppButton
                  label="Validate coupon"
                  tone="secondary"
                  onPress={() =>
                    runAction(async () => {
                      setCouponValidation(await validateCoupon(couponCode, Number(couponAmount)));
                    }, "Coupon checked.", false)
                  }
                />
              </View>
              {couponValidation ? (
                <Text style={styles.helperText}>
                  {couponValidation.valid ? `Valid coupon. Final amount: Rs ${couponValidation.finalAmount ?? 0}` : couponValidation.message || "Coupon not valid."}
                </Text>
              ) : null}
            </Surface>
            <Section title="Active Coupons">
              {coupons.length === 0 ? <EmptyState text="No active coupons returned by the backend." /> : null}
              {coupons.map((coupon) => (
                <Surface key={coupon.code} tone="player">
                  <Text style={styles.cardTitle}>{coupon.code}</Text>
                  <Text style={styles.cardMeta}>{coupon.description || "No description"}</Text>
                  <Text style={styles.cardMeta}>Min booking: Rs {coupon.minBookingAmount ?? 0} | Expires: {coupon.expiryDate || "N/A"}</Text>
                </Surface>
              ))}
            </Section>
          </Section>
        </View>
      );
    }

    if (playerTab === "settings") {
      const settings = playerSettings;
      return (
        <View style={styles.stack}>
          <Section title="Settings" subtitle="Account, privacy, booking preferences, language, theme and data controls.">
            {!settings ? <EmptyState text="Settings are loading. Pull to refresh if they do not appear." /> : null}
            {settings ? (
              <>
                <Surface tone="player">
                  <ProfileHero
                    initials={(settings.profile.displayName || "CB").slice(0, 2).toUpperCase()}
                    name={settings.profile.displayName || "Cricbook Player"}
                    subtitle={`${settings.localization.regionCity || "India"} | ${settings.matchmaking.competitionLevel}`}
                    meta={settings.profile.email || ""}
                  />
                  <View style={styles.metricGrid}>
                    <Metric label="Trust visible" value={settings.privacy.showTrustScore ? "Yes" : "No"} tone="player" />
                    <Metric label="Language" value={(settings.localization.language || selectedLang).toUpperCase()} tone="player" />
                    <Metric label="Sessions" value={settings.devices.activeSessions} tone="player" />
                    <Metric label="Rebook" value={settings.booking.quickRebookEnabled ? "On" : "Off"} tone="player" />
                  </View>
                </Surface>

                <Surface tone="player">
                  <Text style={styles.cardTitle}>Theme & Language</Text>
                  <Text style={styles.cardMeta}>Auto follows your phone theme. Light and dark use the same design tokens as web.</Text>
                  <ThemeModeControl mode={themeMode} onChange={updateThemeMode} palette={activeTheme} labels={{ light: t("light", "Light"), dark: t("dark", "Dark"), auto: t("auto", "Auto") }} />
                  <SelectField
                    label="Language"
                    value={selectedLang.toUpperCase()}
                    onPress={() => openPicker("Language", languageChoices, (value) => {
                      void updateLanguage(value);
                      void runAction(() => updatePlayerSettings({ localization: { ...settings.localization, language: value } }), "Language preference saved.", false);
                    })}
                  />
                </Surface>

                <Surface tone="player">
                  <Text style={styles.cardTitle}>Privacy & Matchmaking</Text>
                  <Text style={styles.cardMeta}>Control discoverability without leaving the mobile app.</Text>
                  <FieldPair
                    left={<SelectField label="Profile visibility" value={settings.privacy.profileVisibility} onPress={() => openPicker("Profile visibility", [{ label: "Public", value: "PUBLIC" }, { label: "Friends", value: "FRIENDS" }, { label: "Private", value: "PRIVATE" }], (value) => void saveMobileSetting({ privacy: { ...settings.privacy, profileVisibility: value } }))} />}
                    right={<SelectField label="Skill filter" value={settings.matchmaking.preferredSkillLevel} onPress={() => openPicker("Skill filter", ["ANY", "BEGINNER", "INTERMEDIATE", "ADVANCED", "PROFESSIONAL"].map((value) => ({ label: value, value })), (value) => void saveMobileSetting({ matchmaking: { ...settings.matchmaking, preferredSkillLevel: value } }))} />}
                  />
                  <View style={styles.actionRow}>
                    <AppButton label={settings.privacy.showTrustScore ? "Hide trust score" : "Show trust score"} tone="secondary" onPress={() => void saveMobileSetting({ privacy: { ...settings.privacy, showTrustScore: !settings.privacy.showTrustScore } })} />
                    <AppButton label={settings.matchmaking.trustedOnly ? "Trusted only on" : "Trusted only off"} tone="secondary" onPress={() => void saveMobileSetting({ matchmaking: { ...settings.matchmaking, trustedOnly: !settings.matchmaking.trustedOnly } })} />
                  </View>
                </Surface>

                <Surface tone="player">
                  <Text style={styles.cardTitle}>Booking & Payments</Text>
                  <FieldPair
                    left={<LabeledInput label="Preferred city" value={settings.booking.preferredCity || ""} onChangeText={(value) => setPlayerSettings((current) => current ? { ...current, booking: { ...current.booking, preferredCity: value } } : current)} />}
                    right={<LabeledInput label="Preferred time" value={settings.booking.preferredTime || ""} onChangeText={(value) => setPlayerSettings((current) => current ? { ...current, booking: { ...current.booking, preferredTime: value } } : current)} />}
                  />
                  <View style={styles.actionRow}>
                    <AppButton label="Save booking prefs" onPress={() => void saveMobileSetting({ booking: playerSettings?.booking })} />
                    <AppButton label={settings.payments.autoPaySplits ? "Auto-pay on" : "Auto-pay off"} tone="secondary" onPress={() => void saveMobileSetting({ payments: { ...settings.payments, autoPaySplits: !settings.payments.autoPaySplits } })} />
                  </View>
                </Surface>

                <Surface tone="player">
                  <Text style={styles.cardTitle}>Data & Account</Text>
                  <Text style={styles.cardMeta}>Last export request: {settings.data.lastExportRequestedAt || "Never"}</Text>
                  <AppButton label="Request data export" tone="secondary" onPress={() => runAction(async () => setPlayerSettings(await requestPlayerDataExport()), "Data export requested.", false)} />
                </Surface>
              </>
            ) : null}
          </Section>
        </View>
      );
    }

    if (playerTab === "profile") {
      return (
        <View style={styles.stack}>
          <Section title="Profile" subtitle="User profile, booking history and reviews in one place.">
            <Surface tone="player">
              <ProfileHero
                initials={(profileDraft.name || sessionUser?.name || "CB").slice(0, 2).toUpperCase()}
                name={profileDraft.name || sessionUser?.name || "Cricbook Player"}
                subtitle="Manage your personal details, preferences, and account identity."
                meta={profileDraft.email || sessionUser?.email || ""}
              />
              <View style={styles.metricGrid}>
                <Metric label="Bookings" value={bookings.length} tone="player" />
                <Metric label="Reviews" value={profileReviews.length} tone="player" />
              </View>
              <FieldPair
                left={<LabeledInput label="Name" value={profileDraft.name} onChangeText={(value) => setProfileDraft((current) => ({ ...current, name: value }))} />}
                right={<LabeledInput label="Phone" value={profileDraft.phone} onChangeText={(value) => setProfileDraft((current) => ({ ...current, phone: value }))} keyboardType="numeric" />}
              />
              <LabeledInput label="Email" value={profileDraft.email} onChangeText={(value) => setProfileDraft((current) => ({ ...current, email: value }))} keyboardType="email-address" />
              <AppButton
                label="Update profile"
                onPress={() =>
                  runAction(
                    async () => {
                      const isConnected = await checkConnectivity();
                      if (!isConnected) {
                        await enqueueSyncAction("UPDATE_PROFILE", "/users/update", "PUT", profileDraft);
                        const offlineProfile = {
                          id: profile?.id ?? 0,
                          name: profileDraft.name,
                          email: profileDraft.email,
                          phone: profileDraft.phone,
                        };
                        setProfile(offlineProfile);
                        await cacheProfile(offlineProfile);
                        await simulateFCMAlert(
                          "PAYMENT",
                          "Profile Update Queued (Offline)",
                          "Profile updates are queued locally and will sync once internet returns."
                        );
                      } else {
                        await updateProfile(profileDraft);
                        const profileData = await getProfile();
                        setProfile(profileData);
                        await cacheProfile(profileData);
                      }
                    },
                    "Profile updated.",
                    false
                  )
                }
              />
            </Surface>
            <Surface tone="player">
              <Text style={styles.cardTitle}>Availability for matchmaking</Text>
              <Text style={styles.cardMeta}>This makes your account discoverable when captains are searching for players.</Text>
              <FieldPair
                left={<LabeledInput label="Available days" value={availabilityDraft.availableDays} onChangeText={(value) => setAvailabilityDraft((current) => ({ ...current, availableDays: value }))} />}
                right={<LabeledInput label="Preferred window" value={availabilityDraft.preferredWindow} onChangeText={(value) => setAvailabilityDraft((current) => ({ ...current, preferredWindow: value }))} />}
              />
              <FieldPair
                left={<LabeledInput label="Home city" value={availabilityDraft.homeCity} onChangeText={(value) => setAvailabilityDraft((current) => ({ ...current, homeCity: value }))} />}
                right={
                  <SelectField
                    label="Can travel"
                    value={availabilityDraft.canTravel ? "Yes" : "No"}
                    onPress={() => openPicker("Travel flexibility", [{ label: "Yes", value: "YES" }, { label: "No", value: "NO" }], (value) => setAvailabilityDraft((current) => ({ ...current, canTravel: value === "YES" })))}
                  />
                }
              />
              <LabeledInput label="Notes" value={availabilityDraft.notes} onChangeText={(value) => setAvailabilityDraft((current) => ({ ...current, notes: value }))} multiline />
              <AppButton
                label="Save availability"
                tone="secondary"
                onPress={() =>
                  runAction(
                    async () => {
                      const updated = await updateMyAvailability(availabilityDraft);
                      setPlayerAvailability(updated);
                    },
                    "Availability updated.",
                    false
                  )
                }
              />
              {playerAvailability ? (
                <Text style={styles.helperText}>
                  Current discovery card: {playerAvailability.preferredWindow || "Any time"} | {playerAvailability.availableDays || "Flexible days"} | {playerAvailability.canTravel ? "Can travel" : "Home city only"}
                </Text>
              ) : null}
            </Surface>
            <Section title="My Reviews">
              {profileReviews.length === 0 ? <EmptyState text="No reviews submitted yet." /> : null}
              {profileReviews.map((review) => (
                <Surface key={review.id} tone="player">
                  <Text style={styles.cardTitle}>{review.turfName || `Turf #${review.turfId}`}</Text>
                  <Text style={styles.cardMeta}>Rating {review.rating}/5 | {review.status || "PUBLISHED"}</Text>
                  {review.title ? <Text style={styles.cardMeta}>{review.title}</Text> : null}
                  <Text style={styles.cardMeta}>{review.comment || "No comment provided."}</Text>
                  {review.ownerReply ? <Text style={styles.cardMeta}>Owner response: {review.ownerReply}</Text> : null}
                </Surface>
              ))}
            </Section>
          </Section>
        </View>
      );
    }

    if (playerTab === "notifications") {
      return (
        <View style={styles.stack}>
          <Section title="My Notifications" subtitle="Receive realtime match starts, slot receipts, and promo alerts.">
            <View style={styles.metricGrid}>
              <Metric label="Unread" value={notificationSummary?.unread ?? notifications.filter((item) => !(item.read || item.status === "READ")).length} tone="player" />
              <Metric label="Sent" value={notificationSummary?.total ?? notifications.length} tone="player" />
              <Metric label="Failed" value={notificationSummary?.failed ?? 0} tone="player" />
              <Metric label="Archived" value={notificationSummary?.archived ?? 0} tone="player" />
            </View>
            {notificationAnnouncements.slice(0, 2).map((announcement) => (
              <Surface key={announcement.id} tone="player">
                <Text style={styles.cardTitle}>{announcement.title}</Text>
                <Text style={styles.cardMeta}>{announcement.message}</Text>
                <Text style={styles.helperText}>{announcement.priority} · {announcement.displayLocation}</Text>
              </Surface>
            ))}
            <View style={styles.actionRow}>
              <AppButton
                label="Mark all read"
                tone="secondary"
                onPress={() =>
                  runAction(
                    async () => {
                      await markAllNotificationsRead();
                      const next = await getNotifications().catch(() => []);
                      setNotifications(next);
                    },
                    "All notifications marked read.",
                    false
                  )
                }
              />
            </View>
            {notifications.length === 0 ? (
              <EmptyState text="No notifications received yet." />
            ) : (
              notifications.map((n) => (
                <Surface key={n.id} tone="player">
                  <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                    <Text style={[styles.cardTitle, { color: n.read || n.status === "READ" ? theme.colors.muted : theme.colors.text }]}>
                      {n.title}
                    </Text>
                    <Chip label={n.category || n.type || "GENERAL"} active={!(n.read || n.status === "READ")} tone="player" />
                  </View>
                  <Text style={[styles.cardMeta, { color: n.read || n.status === "READ" ? theme.colors.muted : theme.colors.text, marginTop: 4 }]}>
                    {n.message}
                  </Text>
                  <Text style={styles.helperText}>{n.channel || "EMAIL"} · {n.priority || "NORMAL"}</Text>
                  {!(n.read || n.status === "READ") && (
                    <View style={{ marginTop: 10 }}>
                      <AppButton
                        label="Mark Read"
                        tone="secondary"
                        onPress={() =>
                          runAction(
                            async () => {
                              await markNotificationRead(n.id);
                              n.status = "READ";
                              const next = await getNotifications().catch(() => []);
                              setNotifications(next);
                            },
                            "Notification marked read.",
                            false
                          )
                        }
                      />
                    </View>
                  )}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    return null;
  }

  function renderOwnerPortal() {
    if (ownerTab === "dashboard") {
      return (
        <View style={styles.stack}>
          <Section title="Owner Dashboard" subtitle="Analytics, occupancy, reviews and upcoming bookings.">
            {!ownerDashboard ? (
              <EmptyState text="Open this tab again or pull to refresh to load dashboard metrics." />
            ) : (
              <>
                <View style={styles.metricGrid}>
                  <Metric label="Today" value={ownerDashboard.todayBookings} tone="owner" />
                  <Metric label="Monthly Rs" value={ownerDashboard.monthlyRevenue} tone="owner" />
                  <Metric label="Occupancy" value={`${ownerDashboard.occupancyPercent}%`} tone="owner" />
                  <Metric label="Cancel Rate" value={`${ownerDashboard.cancellationPercent}%`} tone="owner" />
                  <Metric label="Active Turfs" value={ownerDashboard.activeTurfs} tone="owner" />
                  <Metric label="Inactive Turfs" value={ownerDashboard.inactiveTurfs ?? 0} tone="owner" />
                  <Metric label="Maintenance" value={ownerDashboard.maintenanceTurfs} tone="owner" />
                  <Metric label="Total Rs" value={ownerDashboard.totalRevenue} tone="owner" />
                </View>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Upcoming bookings</Text>
                  {ownerDashboard.upcomingBookings.map((booking) => (
                    <View key={booking.id} style={styles.listRow}>
                      <Text style={styles.listTitle}>{booking.turfName || `Turf #${booking.turfId}`}</Text>
                      <Text style={styles.cardMeta}>{booking.bookingDate} | {formatSlotTime(booking.startTime)} - {formatSlotTime(booking.endTime)}</Text>
                    </View>
                  ))}
                </Surface>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Recent reviews</Text>
                  {ownerDashboard.recentReviews.map((review) => (
                    <View key={review.id} style={styles.listRow}>
                      <Text style={styles.listTitle}>{review.turfName || `Turf #${review.turfId}`}</Text>
                      <Text style={styles.cardMeta}>Rating {review.rating}/5 | {review.comment || "No comment"}</Text>
                    </View>
                  ))}
                </Surface>
              </>
            )}
          </Section>
        </View>
      );
    }

    if (ownerTab === "insights") {
      return (
        <View style={styles.stack}>
          <Section title="Business Insights" subtitle="Revenue, occupancy, profit, pricing, and AI coach for turf owners.">
            {!ownerInsights ? (
              <EmptyState text="Open this tab again or pull to refresh to load business intelligence." />
            ) : (
              <>
                <View style={styles.metricGrid}>
                  <Metric label="Month Rs" value={ownerInsights.revenue.monthRevenue} tone="owner" />
                  <Metric label="Occupancy" value={`${ownerInsights.occupancy.occupancyPercent}%`} tone="owner" />
                  <Metric label="Profit Rs" value={ownerInsights.profit.profit} tone="owner" />
                  <Metric label="Demand" value={ownerInsights.forecast.demandLevel} tone="owner" />
                </View>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Next 7 days forecast</Text>
                  <Text style={styles.cardMeta}>Expected revenue Rs {ownerInsights.forecast.expectedRevenue} | occupancy {ownerInsights.forecast.expectedOccupancy}%</Text>
                  <Text style={styles.helperText}>{ownerInsights.forecast.insight}</Text>
                </Surface>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Peak hours</Text>
                  {ownerInsights.heatmap.length === 0 ? <Text style={styles.cardMeta}>No peak-hour data yet.</Text> : null}
                  {ownerInsights.heatmap.slice(0, 4).map((cell) => (
                    <View key={`${cell.dayOfWeek}-${cell.hour}`} style={styles.listRow}>
                      <Text style={styles.listTitle}>{cell.dayOfWeek} {cell.hourLabel}</Text>
                      <Text style={styles.cardMeta}>{cell.bookingCount} bookings | {cell.intensity} demand</Text>
                    </View>
                  ))}
                </Surface>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Pricing recommendations</Text>
                  {ownerInsights.pricingSuggestions.slice(0, 4).map((item) => (
                    <View key={item.slotId} style={styles.listRow}>
                      <Text style={styles.listTitle}>{item.turfName}</Text>
                      <Text style={styles.cardMeta}>Rs {item.currentPrice} to Rs {item.recommendedPrice}</Text>
                      <Text style={styles.helperText}>{item.reason}</Text>
                    </View>
                  ))}
                </Surface>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Expense tracking</Text>
                  <FieldPair
                    left={<LabeledInput label="Type" value={ownerExpenseDraft.type} onChangeText={(value) => setOwnerExpenseDraft((current) => ({ ...current, type: value }))} />}
                    right={<LabeledInput label="Amount" value={ownerExpenseDraft.amount} onChangeText={(value) => setOwnerExpenseDraft((current) => ({ ...current, amount: value }))} keyboardType="numeric" />}
                  />
                  <SelectField label="Date" value={ownerExpenseDraft.expenseDate} onPress={() => openPicker("Expense date", dateChoices, (value) => setOwnerExpenseDraft((current) => ({ ...current, expenseDate: value })))} />
                  <AppButton
                    label="Save expense"
                    tone="owner"
                    onPress={() =>
                      runAction(
                        async () => {
                          await addOwnerExpense({
                            type: ownerExpenseDraft.type,
                            amount: Number(ownerExpenseDraft.amount),
                            expenseDate: ownerExpenseDraft.expenseDate,
                          });
                          setOwnerExpenseDraft({ type: "Electricity", amount: "", expenseDate: today });
                          setOwnerInsights(await getOwnerInsights());
                        },
                        "Expense saved.",
                        false
                      )
                    }
                  />
                </Surface>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>AI Business Coach</Text>
                  <LabeledInput label="Question" value={businessCoachQuestion} onChangeText={setBusinessCoachQuestion} multiline />
                  <AppButton
                    label="Ask coach"
                    tone="owner"
                    onPress={() =>
                      runAction(
                        async () => {
                          const coach = await askBusinessCoach(businessCoachQuestion);
                          setOwnerInsights((current) => current ? { ...current, coach } : current);
                        },
                        "Coach refreshed.",
                        false
                      )
                    }
                  />
                  <Text style={[styles.cardMeta, { marginTop: 8 }]}>{ownerInsights.coach.answer}</Text>
                  {ownerInsights.coach.recommendedActions.map((action) => (
                    <Text key={action} style={styles.helperText}>- {action}</Text>
                  ))}
                </Surface>
              </>
            )}
          </Section>
        </View>
      );
    }

    if (ownerTab === "rebook") {
      return (
        <View style={styles.stack}>
          <Section title="Recurring Customers" subtitle="Repeat booking intelligence and habit-based revenue signals.">
            <View style={styles.metricGrid}>
              <Metric label="Recurring" value={bookingIntelligence?.recurringBookings || 0} tone="owner" />
              <Metric label="Frequent" value={bookingIntelligence?.frequentBookings || 0} tone="owner" />
              <Metric label="Retention" value={`${Math.round(bookingIntelligence?.rebookReadiness || 0)}%`} tone="owner" />
              <Metric label="Conflicts" value={bookingIntelligence?.conflicts || 0} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Retention playbook</Text>
              <Text style={styles.helperText}>
                Encourage captains to save weekly schedules. Recurring customers make peak-hour demand easier to forecast and protect.
              </Text>
            </Surface>
          </Section>
        </View>
      );
    }

    if (ownerTab === "weather") {
      return (
        <View style={styles.stack}>
          <Section title="Weather Policy" subtitle="Rain refunds, flexible rescheduling, and safety controls.">
            <View style={styles.metricGrid}>
              <Metric label="Rain" value={`${Math.round(weatherDashboard?.current?.rainProbability || 0)}%`} tone="owner" />
              <Metric label="Heat" value={`${Math.round(weatherDashboard?.current?.temperature || 0)}C`} tone="owner" />
              <Metric label="Risk" value={weatherDashboard?.current?.riskLevel || "--"} tone="owner" />
              <Metric label="Refunds" value={weatherDashboard?.weatherRefunds || 0} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Owner weather rules</Text>
              <Text style={styles.cardMeta}>Rain threshold: {weatherPolicy?.rainThreshold ?? 70}% | Heat: {weatherPolicy?.heatThresholdCelsius ?? 38}C</Text>
              <Text style={styles.helperText}>{weatherDashboard?.insight || "Set weather policy to reduce disputes."}</Text>
              <AppButton
                label="Enable flexible reschedule"
                tone="secondary"
                onPress={() =>
                  runAction(
                    async () => setWeatherPolicy(await updateOwnerWeatherPolicy({ ...(weatherPolicy || {}), flexibleReschedule: true, rainRefundAllowed: true })),
                    "Weather policy updated.",
                    false
                  )
                }
              />
            </Surface>
          </Section>
        </View>
      );
    }

    if (ownerTab === "engagement") {
      return (
        <View style={styles.stack}>
          <Section title="Engagement Analytics" subtitle="XP leaders, challenge completion, rewards, and team competitions.">
            <View style={styles.metricGrid}>
              <Metric label="Players" value={gamificationAnalytics?.activePlayers ?? 0} tone="owner" />
              <Metric label="Challenges" value={gamificationAnalytics?.completedChallenges ?? 0} tone="owner" />
              <Metric label="Rewards" value={gamificationAnalytics?.availableRewards ?? 0} tone="owner" />
              <Metric label="Completion" value={`${Math.round(gamificationAnalytics?.challengeCompletionRate || 0)}%`} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Community signal</Text>
              <Text style={styles.cardMeta}>{gamificationAnalytics?.insight || "No engagement analytics loaded yet."}</Text>
            </Surface>
          </Section>
          <Section title="Top Players" subtitle="Most engaged players by XP.">
            {gamificationAnalytics?.topPlayers?.map((entry) => (
              <Surface key={entry.userId} tone="owner">
                <Text style={styles.cardTitle}>#{entry.rank} {entry.playerName}</Text>
                <Text style={styles.cardMeta}>Level {entry.level} | {entry.score} XP</Text>
              </Surface>
            )) || <EmptyState text="No leaderboard signal yet." />}
          </Section>
        </View>
      );
    }

    if (ownerTab === "payments") {
      return (
        <View style={styles.stack}>
          <Section title="Payment Rules" subtitle="Control split-payment risk, partial payment thresholds, and team collection behavior.">
            <View style={styles.metricGrid}>
              <Metric label="Collected" value={`Rs ${splitAnalytics?.totalCollected ?? 0}`} tone="owner" />
              <Metric label="Pending" value={`Rs ${splitAnalytics?.totalPending ?? 0}`} tone="owner" />
              <Metric label="Paid" value={splitAnalytics?.paidSplits ?? 0} tone="owner" />
              <Metric label="Success" value={`${splitAnalytics?.collectionSuccessRate ?? 0}%`} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Owner collection policy</Text>
              <FieldPair
                left={
                  <LabeledInput
                    label="Minimum payment %"
                    value={String(paymentRules.minimumPercentage ?? 50)}
                    onChangeText={(value) => setPaymentRules((current) => ({ ...current, minimumPercentage: Number(value) || 0 }))}
                    keyboardType="numeric"
                  />
                }
                right={
                  <LabeledInput
                    label="Reminder hours"
                    value={String(paymentRules.reminderHoursBefore ?? 24)}
                    onChangeText={(value) => setPaymentRules((current) => ({ ...current, reminderHoursBefore: Number(value) || 0 }))}
                    keyboardType="numeric"
                  />
                }
              />
              <View style={styles.chipWrap}>
                <Chip
                  label="Auto-cancel unpaid"
                  active={Boolean(paymentRules.autoCancel)}
                  tone="owner"
                  onPress={() => setPaymentRules((current) => ({ ...current, autoCancel: !current.autoCancel }))}
                />
                <Chip
                  label="Full payment required"
                  active={Boolean(paymentRules.fullPaymentRequired)}
                  tone="owner"
                  onPress={() => setPaymentRules((current) => ({ ...current, fullPaymentRequired: !current.fullPaymentRequired }))}
                />
              </View>
              <AppButton
                label="Save payment rules"
                tone="owner"
                onPress={() =>
                  runAction(
                    async () => {
                      const saved = await updateOwnerPaymentRules(paymentRules);
                      setPaymentRules(saved);
                    },
                    "Payment rules saved.",
                    false
                  )
                }
              />
            </Surface>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Collection insight</Text>
              <Text style={styles.cardMeta}>{splitAnalytics?.insight || "No collection analytics available yet."}</Text>
            </Surface>
          </Section>
        </View>
      );
    }

    if (ownerTab === "turfs") {
      return (
        <View style={styles.stack}>
          <Section title="Turf Portfolio" subtitle="Owner turf inventory plus basic mobile create flow.">
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Add turf</Text>
              <FieldPair
                left={<LabeledInput label="Name" value={turfDraft.name} onChangeText={(value) => setTurfDraft((current) => ({ ...current, name: value }))} />}
                right={<LabeledInput label="City" value={turfDraft.city} onChangeText={(value) => setTurfDraft((current) => ({ ...current, city: value }))} />}
              />
              <FieldPair
                left={<LabeledInput label="Location" value={turfDraft.location} onChangeText={(value) => setTurfDraft((current) => ({ ...current, location: value }))} />}
                right={<LabeledInput label="Price per hour" value={turfDraft.pricePerHour} onChangeText={(value) => setTurfDraft((current) => ({ ...current, pricePerHour: value }))} keyboardType="numeric" />}
              />
              <FieldPair
                left={
                  <SelectField
                    label="Pitch type"
                    value={formatEnumLabel(turfDraft.pitchType)}
                    onPress={() => openPicker("Pitch type", pitchTypeChoices, (value) => setTurfDraft((current) => ({ ...current, pitchType: value })))}
                  />
                }
                right={
                  <SelectField
                    label="Box type"
                    value={formatEnumLabel(turfDraft.boxType)}
                    onPress={() => openPicker("Box type", boxTypeChoices, (value) => setTurfDraft((current) => ({ ...current, boxType: value })))}
                  />
                }
              />
              <LabeledInput label="Amenities" value={turfDraft.amenities} onChangeText={(value) => setTurfDraft((current) => ({ ...current, amenities: value }))} placeholder="Parking, Lights, Washroom" />
              <LabeledInput label="Description" value={turfDraft.description} onChangeText={(value) => setTurfDraft((current) => ({ ...current, description: value }))} multiline />
              <AppButton label="Create turf" tone="owner" onPress={() => runAction(() => createTurf(turfDraft), "Turf created.")} />
            </Surface>
            {ownerTurfs.length === 0 ? <EmptyState text="No owner turfs found yet." /> : null}
            {ownerTurfs.map((turf) => (
              <Surface key={turf.id} tone="owner">
                <Text style={styles.cardTitle}>{turf.name}</Text>
                <Text style={styles.cardMeta}>{turf.city} | {turf.location}</Text>
                <Text style={styles.cardMeta}>Rs {turf.pricePerHour}/hr | Status {formatEnumLabel(turf.status || "ACTIVE")}</Text>
                <View style={styles.actionRow}>
                  <AppButton
                    label="Active"
                    tone="owner"
                    onPress={() => confirmAction("Activate turf", `Mark ${turf.name} as active?`, "Activate", "owner", () => void runAction(() => setTurfStatus(turf.id ?? 0, "activate"), "Turf activated."))}
                  />
                  <AppButton
                    label="Inactive"
                    tone="secondary"
                    onPress={() => confirmAction("Deactivate turf", `Mark ${turf.name} as inactive?`, "Deactivate", "danger", () => void runAction(() => setTurfStatus(turf.id ?? 0, "deactivate"), "Turf deactivated."))}
                  />
                  <AppButton
                    label="Maintenance"
                    tone="secondary"
                    onPress={() => confirmAction("Maintenance mode", `Move ${turf.name} into maintenance?`, "Move to maintenance", "owner", () => void runAction(() => setTurfStatus(turf.id ?? 0, "maintenance"), "Turf moved to maintenance."))}
                  />
                </View>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (ownerTab === "slots") {
      return (
        <View style={styles.stack}>
          <Section title="Slot Manager" subtitle="Slot generation, blocking and pricing without touching backend code.">
            <Surface tone="owner">
              <SelectField
                label="Working date"
                value={slotManagerDate}
                onPress={() => openPicker("Working date", dateChoices, setSlotManagerDate)}
              />
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.tabScroller}>
                {ownerTurfs.map((turf) => (
                  <Chip key={turf.id} label={turf.name} active={ownerSelectedTurfId === turf.id} onPress={() => setOwnerSelectedTurfId(turf.id ?? null)} tone="owner" />
                ))}
              </ScrollView>
              <FieldPair
                left={<SelectField label="Open" value={slotForm.startTime} onPress={() => openPicker("Open time", timeChoices, (value) => setSlotForm((current) => ({ ...current, startTime: value })))} />}
                right={<SelectField label="Close" value={slotForm.endTime} onPress={() => openPicker("Close time", timeChoices, (value) => setSlotForm((current) => ({ ...current, endTime: value })))} />}
              />
              <FieldPair
                left={<SelectField label="Duration mins" value={slotForm.slotDuration} onPress={() => openPicker("Slot duration", durationChoices, (value) => setSlotForm((current) => ({ ...current, slotDuration: value })))} />}
                right={<LabeledInput label="Price" value={slotForm.price} onChangeText={(value) => setSlotForm((current) => ({ ...current, price: value }))} keyboardType="numeric" />}
              />
              <AppButton
                label="Generate slots"
                tone="owner"
                onPress={() =>
                  confirmAction(
                    "Generate slots",
                    `Generate slots for ${selectedOwnerTurf?.name || "selected turf"} on ${slotManagerDate}?`,
                    "Generate",
                    "owner",
                    () =>
                      void runAction(
                        () =>
                          createSlots(ownerSelectedTurfId ?? 0, {
                            startTime: slotForm.startTime,
                            endTime: slotForm.endTime,
                            slotDuration: Number(slotForm.slotDuration),
                            price: Number(slotForm.price),
                          }),
                        "Slots generated."
                      )
                  )
                }
              />
              <Text style={styles.helperText}>Selected turf: {selectedOwnerTurf?.name || "None"}</Text>
            </Surface>

            <Surface tone="owner">
              <Text style={styles.cardTitle}>Availability on {slotManagerDate}</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.tabScroller}>
                {ownerSlotAvailability.map((slot) => (
                  <Pressable
                    key={slot.slotId}
                    onPress={() =>
                      setSlotSelection((current) => current.includes(slot.slotId) ? current.filter((id) => id !== slot.slotId) : [...current, slot.slotId])
                    }
                    style={[styles.slotCard, slotSelection.includes(slot.slotId) && styles.slotCardOwnerActive]}
                  >
                    <Text style={styles.slotTitle}>{slot.startTime} - {slot.endTime}</Text>
                    <Text style={styles.slotMeta}>Rs {slot.price}</Text>
                    <Text style={styles.slotMeta}>{slot.blocked ? "Blocked" : slot.available ? "Open" : "Booked"}</Text>
                  </Pressable>
                ))}
              </ScrollView>
              <FieldPair
                left={<SelectField label="Pricing date" value={slotPriceDate} onPress={() => openPicker("Pricing date", dateChoices, setSlotPriceDate)} />}
                right={<LabeledInput label="Price override" value={slotPriceValue} onChangeText={setSlotPriceValue} keyboardType="numeric" />}
              />
              <View style={styles.actionRow}>
                <AppButton
                  label="Apply pricing"
                  tone="owner"
                  disabled={slotSelection.length === 0}
                  onPress={() =>
                    confirmAction(
                      "Apply slot pricing",
                      `Apply Rs ${slotPriceValue} to ${slotSelection.length} selected slots for ${slotPriceDate}?`,
                      "Apply pricing",
                      "owner",
                      () =>
                        void runAction(
                          () =>
                            applySlotPricing(ownerSelectedTurfId ?? 0, {
                              slotIds: slotSelection,
                              date: slotPriceDate,
                              price: Number(slotPriceValue),
                              reason: "Mobile owner pricing",
                            }),
                          "Slot pricing updated."
                        )
                    )
                  }
                />
                <AppButton
                  label="Block"
                  tone="danger"
                  disabled={slotSelection.length === 0}
                  onPress={() =>
                    confirmAction(
                      "Block selected slots",
                      `Block ${slotSelection.length} slots on ${slotManagerDate}?`,
                      "Block slots",
                      "danger",
                      () =>
                        void runAction(
                          () => blockSlots(ownerSelectedTurfId ?? 0, { date: slotManagerDate, slotIds: slotSelection, blocked: true }),
                          "Selected slots blocked."
                        )
                    )
                  }
                />
                <AppButton
                  label="Unblock"
                  tone="secondary"
                  disabled={slotSelection.length === 0}
                  onPress={() =>
                    confirmAction(
                      "Unblock selected slots",
                      `Unblock ${slotSelection.length} slots on ${slotManagerDate}?`,
                      "Unblock slots",
                      "owner",
                      () =>
                        void runAction(
                          () => unblockSlots(ownerSelectedTurfId ?? 0, { date: slotManagerDate, slotIds: slotSelection }),
                          "Selected slots unblocked."
                        )
                    )
                  }
                />
              </View>
            </Surface>

            {ownerSlots.map((slot) => (
              <Surface key={slot.id} tone="owner">
                <Text style={styles.cardTitle}>{formatSlotTime(slot.startTime)} - {formatSlotTime(slot.endTime)}</Text>
                <Text style={styles.cardMeta}>Rs {slot.price} | {slot.isActive ? "Active" : "Disabled"}</Text>
                <View style={styles.actionRow}>
                  <AppButton
                    label="Disable"
                    tone="danger"
                    onPress={() =>
                      confirmAction(
                        "Disable slot",
                        `Disable slot ${formatSlotTime(slot.startTime)} - ${formatSlotTime(slot.endTime)}?`,
                        "Disable",
                        "danger",
                        () => void runAction(() => disableSlot(ownerSelectedTurfId ?? 0, slot.id), "Slot disabled.")
                      )
                    }
                  />
                  <AppButton label="Raise 100" tone="secondary" onPress={() => runAction(() => updateSlot(ownerSelectedTurfId ?? 0, slot.id, { price: slot.price + 100 }), "Slot price updated.")} />
                </View>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (ownerTab === "bookings") {
      return (
        <View style={styles.stack}>
          <Section title="Owner Bookings" subtitle="Owner-side operations including cancel and refund trigger.">
            {ownerBookings.length === 0 ? <EmptyState text="No owner bookings yet." /> : null}
            {ownerBookings.map((booking) => (
              <Surface key={booking.id} tone="owner">
                <Text style={styles.cardTitle}>{booking.turfName || `Turf #${booking.turfId}`}</Text>
                <Text style={styles.cardMeta}>{booking.bookingDate} | {formatSlotTime(booking.startTime)} - {formatSlotTime(booking.endTime)}</Text>
                <Text style={styles.cardMeta}>Status {booking.status} | Rs {booking.amount ?? booking.totalAmount ?? 0}</Text>
                <View style={styles.actionRow}>
                  {booking.status === "CONFIRMED" ? (
                    <AppButton
                      label="Check In"
                      tone="owner"
                      onPress={() =>
                        confirmAction(
                          "Check in player",
                          `Confirm arrival for booking ${booking.id}?`,
                          "Check in",
                          "owner",
                          () => void runAction(() => checkInOwnerBooking(booking.id), "Player checked in.")
                        )
                      }
                    />
                  ) : null}
                  {(booking.status === "CONFIRMED" || booking.status === "CHECKED_IN") ? (
                    <AppButton
                      label="Complete"
                      tone="secondary"
                      onPress={() =>
                        confirmAction(
                          "Complete booking",
                          `Mark booking ${booking.id} as completed?`,
                          "Complete",
                          "owner",
                          () => void runAction(() => completeOwnerBooking(booking.id), "Booking completed.")
                        )
                      }
                    />
                  ) : null}
                  {booking.status === "CONFIRMED" ? (
                    <AppButton
                      label="No-show"
                      tone="danger"
                      onPress={() =>
                        confirmAction(
                          "Mark no-show",
                          `Record no-show for booking ${booking.id}?`,
                          "No-show",
                          "danger",
                          () => void runAction(() => markOwnerNoShow(booking.id), "No-show recorded.")
                        )
                      }
                    />
                  ) : null}
                  {(booking.status === "PENDING" || booking.status === "CONFIRMED") ? (
                    <AppButton
                      label="Cancel"
                      tone="danger"
                      onPress={() =>
                        confirmAction(
                          "Cancel owner booking",
                          `Cancel booking for ${booking.turfName || `Turf #${booking.turfId}`} on ${booking.bookingDate}?`,
                          "Cancel booking",
                          "danger",
                          () => void runAction(() => cancelOwnerBooking(booking.id), "Owner booking cancelled.")
                        )
                      }
                    />
                  ) : null}
                  <AppButton
                    label="Refund"
                    tone="secondary"
                    onPress={() =>
                      confirmAction(
                        "Trigger refund",
                        `Request refund for booking ${booking.id}?`,
                        "Request refund",
                        "owner",
                        () => void runAction(() => triggerRefund(booking.id), "Refund requested.")
                      )
                    }
                  />
                </View>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (ownerTab === "reviews") {
      return (
        <View style={styles.stack}>
          <Section title="Owner Reviews" subtitle="Review monitoring from the owner portal.">
            {ownerReviews.length === 0 ? <EmptyState text="No reviews yet." /> : null}
            {ownerReviews.map((review) => (
              <Surface key={review.id} tone="owner">
                <Text style={styles.cardTitle}>{review.turfName || `Turf #${review.turfId}`}</Text>
                <Text style={styles.cardMeta}>Rating {review.rating}/5 | {review.status || "PUBLISHED"}</Text>
                <Text style={styles.cardMeta}>Player: {review.userName || "Player"} {review.verifiedBooking ? "| Verified" : ""}</Text>
                {review.title ? <Text style={styles.cardMeta}>{review.title}</Text> : null}
                <Text style={styles.cardMeta}>{review.comment || "No comment provided."}</Text>
                {review.ownerReply ? <Text style={styles.cardMeta}>Your response: {review.ownerReply}</Text> : <Text style={styles.cardMeta}>No owner response yet.</Text>}
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (ownerTab === "earnings") {
      return (
        <View style={styles.stack}>
          <Section title="Earnings" subtitle="Revenue, refund impact, commission, and settlement readiness.">
            <View style={styles.metricGrid}>
              <Metric label="Net Collected" value={`Rs ${confirmedOwnerRevenue}`} tone="owner" />
              <Metric label="Net Payable" value={`Rs ${ownerFinance?.netPayable ?? confirmedOwnerRevenue}`} tone="owner" />
              <Metric label="Pending" value={`Rs ${ownerFinance?.pendingSettlementAmount ?? 0}`} tone="owner" />
              <Metric label="Paid Count" value={ownerFinance?.paidBookings ?? paidOwnerBookings.length} tone="owner" />
            </View>
            {ownerFinance?.insight ? (
              <Surface tone="owner">
                <Text style={styles.cardTitle}>Finance Insight</Text>
                <Text style={styles.cardMeta}>{ownerFinance.insight}</Text>
                <Text style={styles.cardMeta}>Commission {ownerFinance.commissionRate}% | GST on commission {ownerFinance.gstRate}%</Text>
              </Surface>
            ) : null}
            {(ownerFinance?.settlements || []).slice(0, 4).map((settlement) => (
              <Surface key={settlement.id} tone="owner">
                <Text style={styles.cardTitle}>{settlement.reference || `Settlement #${settlement.id}`}</Text>
                <Text style={styles.cardMeta}>Booking #{settlement.bookingId || "--"} | {settlement.status}</Text>
                <Text style={styles.cardMeta}>Net payout Rs {settlement.netAmount} | Commission Rs {settlement.commission}</Text>
              </Surface>
            ))}
            {paidOwnerBookings.map((booking) => (
              <Surface key={booking.id} tone="owner">
                <Text style={styles.cardTitle}>{booking.turfName || `Turf #${booking.turfId}`}</Text>
                <Text style={styles.cardMeta}>{booking.bookingDate} | {booking.status}</Text>
                <Text style={styles.cardMeta}>Rs {booking.amount ?? booking.totalAmount ?? 0}</Text>
              </Surface>
            ))}
            {paidOwnerBookings.length === 0 && !ownerFinance?.settlements?.length ? <EmptyState text="No paid owner revenue yet." /> : null}
          </Section>
        </View>
      );
    }

    if (ownerTab === "community") {
      return (
        <View style={styles.stack}>
          <Section title="Community Activity" subtitle="Open match demand, fill rate and local cricket activity.">
            <View style={styles.metricGrid}>
              <Metric label="Open" value={matchmakingAnalytics?.openMatches ?? 0} tone="owner" />
              <Metric label="Full" value={matchmakingAnalytics?.fullMatches ?? 0} tone="owner" />
              <Metric label="Players" value={matchmakingAnalytics?.totalParticipants ?? 0} tone="owner" />
              <Metric label="Fill Rate" value={`${matchmakingAnalytics?.averageFillRate ?? 0}%`} tone="owner" />
            </View>
            {(matchmakingAnalytics?.mostFillableMatches || []).map((match) => (
              <Surface key={match.id} tone="owner">
                <Text style={styles.cardTitle}>{match.title}</Text>
                <Text style={styles.cardMeta}>{match.city} | {match.matchDate} | {match.joinedPlayers}/{match.maxPlayers} joined</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (ownerTab === "resale") {
      return (
        <View style={styles.stack}>
          <Section title="Resale Rules" subtitle="Control slot transfers, trust restrictions, and cancellation recovery.">
            <View style={styles.metricGrid}>
              <Metric label="Active" value={resaleAnalytics?.activeListings ?? 0} tone="owner" />
              <Metric label="Sold" value={resaleAnalytics?.soldListings ?? 0} tone="owner" />
              <Metric label="Revenue" value={`Rs ${resaleAnalytics?.recoveredRevenue ?? 0}`} tone="owner" />
              <Metric label="Success" value={`${resaleAnalytics?.successRate ?? 0}%`} tone="owner" />
            </View>
            <Surface tone="owner">
              <Chip
                label={resaleRules.allowResale ? "Resale enabled" : "Resale disabled"}
                active={!!resaleRules.allowResale}
                onPress={() => setResaleRules((current) => ({ ...current, allowResale: !current.allowResale }))}
              />
              <Chip
                label={resaleRules.restrictLowTrustUsers ? "Trust restricted" : "Open buyers"}
                active={!!resaleRules.restrictLowTrustUsers}
                onPress={() => setResaleRules((current) => ({ ...current, restrictLowTrustUsers: !current.restrictLowTrustUsers }))}
              />
              <FieldPair
                left={<LabeledInput label="Max markup %" value={String(resaleRules.maxMarkupPercent ?? 0)} onChangeText={(value) => setResaleRules((current) => ({ ...current, maxMarkupPercent: Number(value) }))} keyboardType="numeric" />}
                right={<LabeledInput label="Min hours before start" value={String(resaleRules.minHoursBeforeStart ?? 2)} onChangeText={(value) => setResaleRules((current) => ({ ...current, minHoursBeforeStart: Number(value) }))} keyboardType="numeric" />}
              />
              <AppButton
                label="Save resale rules"
                tone="owner"
                onPress={() => runAction(() => updateOwnerResaleRules(resaleRules), "Resale rules saved.")}
              />
            </Surface>
            {(resaleAnalytics?.recentListings || []).map((item) => (
              <Surface key={item.id} tone="owner">
                <Text style={styles.cardTitle}>{item.turfName}</Text>
                <Text style={styles.cardMeta}>{item.bookingDate} | Rs {item.price} | {item.status}</Text>
                <Text style={styles.cardMeta}>Seller trust {item.sellerTrustScore} | commission Rs {item.commissionAmount}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (ownerTab === "teams") {
      return (
        <View style={styles.stack}>
          <Section title="Top Teams" subtitle="Community team activity, availability response and attendance reliability.">
            <View style={styles.metricGrid}>
              <Metric label="Teams" value={teamAnalytics?.totalTeams ?? 0} tone="owner" />
              <Metric label="Members" value={teamAnalytics?.activeMembers ?? 0} tone="owner" />
              <Metric label="RSVPs" value={teamAnalytics?.availabilityResponses ?? 0} tone="owner" />
              <Metric label="Attendance" value={`${teamAnalytics?.averageAttendanceRate ?? 0}%`} tone="owner" />
            </View>
            {(teamAnalytics?.mostActiveTeams || []).map((team) => (
              <Surface key={team.teamId} tone="owner">
                <Text style={styles.cardTitle}>{team.teamName}</Text>
                <Text style={styles.cardMeta}>{team.city} | {team.memberCount} players | {team.teamType}</Text>
                <Text style={styles.cardMeta}>Available today {team.availableToday} | Balance Rs {team.teamBalance || 0}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    return (
      <View style={styles.stack}>
        <Section title="Owner Control Center" subtitle="Business rules, pricing, payouts, weather, resale and automation from mobile.">
          {!ownerSettings ? <EmptyState text="Owner settings are loading. Pull to refresh if they do not appear." /> : null}
          {ownerSettings ? (
            <>
              <ProfileHero
                initials={(ownerSettings.businessName || "CB").slice(0, 2).toUpperCase()}
                name={ownerSettings.businessName || "Cricbook Owner"}
                subtitle={`${ownerSettings.businessType} | ${ownerSettings.pricingMode}`}
                meta={ownerSettings.supportEmail || ownerSettings.ownerEmail || ""}
              />
              <View style={styles.metricGrid}>
                <Metric label="Readiness" value={`${ownerSettings.readinessScore ?? 0}%`} tone="owner" />
                <Metric label="Settlement" value={ownerSettings.settlementSchedule} tone="owner" />
                <Metric label="Rain risk" value={`${ownerSettings.rainThresholdPercent}%`} tone="owner" />
                <Metric label="Markup" value={`${ownerSettings.maxResaleMarkupPercent}%`} tone="owner" />
              </View>

              <Surface tone="owner">
                <Text style={styles.cardTitle}>Business Profile</Text>
                <FieldPair
                  left={<LabeledInput label="Business name" value={ownerSettings.businessName || ""} onChangeText={(value) => setOwnerSettings((current) => current ? { ...current, businessName: value } : current)} />}
                  right={<LabeledInput label="Support phone" value={ownerSettings.supportPhone || ""} onChangeText={(value) => setOwnerSettings((current) => current ? { ...current, supportPhone: value } : current)} keyboardType="numeric" />}
                />
                <LabeledInput label="Support email" value={ownerSettings.supportEmail || ""} onChangeText={(value) => setOwnerSettings((current) => current ? { ...current, supportEmail: value } : current)} keyboardType="email-address" />
                <AppButton label="Save business profile" tone="owner" onPress={() => void saveOwnerSetting({ businessName: ownerSettings.businessName, supportPhone: ownerSettings.supportPhone, supportEmail: ownerSettings.supportEmail })} />
              </Surface>

              <Surface tone="owner">
                <Text style={styles.cardTitle}>Operations & Booking</Text>
                <FieldPair
                  left={<LabeledInput label="Open time" value={ownerSettings.openingTime || ""} onChangeText={(value) => setOwnerSettings((current) => current ? { ...current, openingTime: value } : current)} />}
                  right={<LabeledInput label="Close time" value={ownerSettings.closingTime || ""} onChangeText={(value) => setOwnerSettings((current) => current ? { ...current, closingTime: value } : current)} />}
                />
                <View style={styles.chipWrap}>
                  <Chip label={ownerSettings.allowInstantBooking ? "Instant booking" : "Manual booking"} active={ownerSettings.allowInstantBooking} onPress={() => void saveOwnerSetting({ allowInstantBooking: !ownerSettings.allowInstantBooking })} />
                  <Chip label={ownerSettings.trustedPlayersOnly ? "Trusted only" : "All players"} active={ownerSettings.trustedPlayersOnly} onPress={() => void saveOwnerSetting({ trustedPlayersOnly: !ownerSettings.trustedPlayersOnly })} />
                  <Chip label={ownerSettings.approvalRequired ? "Approval on" : "Approval off"} active={ownerSettings.approvalRequired} onPress={() => void saveOwnerSetting({ approvalRequired: !ownerSettings.approvalRequired })} />
                </View>
              </Surface>

              <Surface tone="owner">
                <Text style={styles.cardTitle}>Pricing, Weather & Resale</Text>
                <View style={styles.chipWrap}>
                  <Chip label={ownerSettings.dynamicPricingEnabled ? "Dynamic pricing" : "Manual pricing"} active={ownerSettings.dynamicPricingEnabled} onPress={() => void saveOwnerSetting({ dynamicPricingEnabled: !ownerSettings.dynamicPricingEnabled })} />
                  <Chip label={ownerSettings.rainRefundAllowed ? "Rain refund" : "No rain refund"} active={ownerSettings.rainRefundAllowed} onPress={() => void saveOwnerSetting({ rainRefundAllowed: !ownerSettings.rainRefundAllowed })} />
                  <Chip label={ownerSettings.resaleEnabled ? "Resale on" : "Resale off"} active={ownerSettings.resaleEnabled} onPress={() => void saveOwnerSetting({ resaleEnabled: !ownerSettings.resaleEnabled })} />
                </View>
                <FieldPair
                  left={<LabeledInput label="Min payment %" value={String(ownerSettings.minimumPaymentPercent ?? 50)} onChangeText={(value) => setOwnerSettings((current) => current ? { ...current, minimumPaymentPercent: Number(value) } : current)} keyboardType="numeric" />}
                  right={<LabeledInput label="Rain threshold %" value={String(ownerSettings.rainThresholdPercent ?? 70)} onChangeText={(value) => setOwnerSettings((current) => current ? { ...current, rainThresholdPercent: Number(value) } : current)} keyboardType="numeric" />}
                />
                <AppButton label="Save finance rules" tone="owner" onPress={() => void saveOwnerSetting({ minimumPaymentPercent: ownerSettings.minimumPaymentPercent, rainThresholdPercent: ownerSettings.rainThresholdPercent })} />
              </Surface>

              <Surface tone="owner">
                <Text style={styles.cardTitle}>Automation & Reports</Text>
                <View style={styles.chipWrap}>
                  <Chip label={ownerSettings.aiCoachEnabled ? "AI coach" : "AI off"} active={ownerSettings.aiCoachEnabled} onPress={() => void saveOwnerSetting({ aiCoachEnabled: !ownerSettings.aiCoachEnabled })} />
                  <Chip label={ownerSettings.autoRescheduleEnabled ? "Auto reschedule" : "Manual reschedule"} active={ownerSettings.autoRescheduleEnabled} onPress={() => void saveOwnerSetting({ autoRescheduleEnabled: !ownerSettings.autoRescheduleEnabled })} />
                  <Chip label={ownerSettings.gstReportsEnabled ? "GST reports" : "GST off"} active={ownerSettings.gstReportsEnabled} onPress={() => void saveOwnerSetting({ gstReportsEnabled: !ownerSettings.gstReportsEnabled })} />
                </View>
                <Text style={styles.helperText}>Last export request: {ownerSettings.lastExportRequestedAt || "Never"}</Text>
                <AppButton label="Request owner data export" tone="secondary" onPress={() => runAction(async () => setOwnerSettings(await requestOwnerDataExport()), "Owner export requested.", false)} />
              </Surface>
            </>
          ) : null}
        </Section>
      </View>
    );
  }

  function renderStaffPortal() {
    if (staffTab === "dashboard") {
      const kpis = staffDashboard?.kpis || {};
      const analytics = staffAnalytics || staffDashboard?.analytics || {};
      return (
        <View style={styles.stack}>
          <Section title="Staff Operations Dashboard" subtitle="Today bookings, check-ins, tasks, maintenance, announcements, and shift readiness.">
            <View style={styles.metricGrid}>
              <Metric label="Bookings" value={kpis.todayBookings ?? 0} tone="owner" />
              <Metric label="Check-ins" value={kpis.pendingCheckIns ?? 0} tone="owner" />
              <Metric label="Tasks" value={kpis.openTasks ?? 0} tone="owner" />
              <Metric label="Maintenance" value={kpis.maintenanceIssues ?? 0} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Execution score</Text>
              <Text style={styles.cardMeta}>
                Attendance {Math.round(Number(analytics.attendancePercent || 0))}% | Tasks {Math.round(Number(analytics.taskCompletionPercent || 0))}% | Performance {Math.round(Number(analytics.performanceScore || 0))}%
              </Text>
              {(staffDashboard?.announcements || []).map((item: any) => (
                <View key={item.title} style={styles.listRow}>
                  <Text style={styles.listTitle}>{item.title}</Text>
                  <Text style={styles.cardMeta}>{item.priority} | {item.message}</Text>
                </View>
              ))}
            </Surface>
          </Section>
          <Section title="Schedule" subtitle="Shift, booking, and task items for the current operations window.">
            {(staffSchedule || []).slice(0, 8).map((item) => (
              <Surface key={`${item.type}-${item.id}`} tone="owner">
                <Text style={styles.cardTitle}>{item.title || item.centerName || item.type}</Text>
                <Text style={styles.cardMeta}>{item.type || "ITEM"} | {item.date || item.bookingDate || "--"} | {item.status || "--"}</Text>
              </Surface>
            ))}
            {(staffSchedule || []).length === 0 ? <EmptyState text="No schedule items loaded." /> : null}
          </Section>
        </View>
      );
    }

    if (staffTab === "checkins") {
      return (
        <View style={styles.stack}>
          <Section title="Player Check-In" subtitle="Scan QR payload, enter booking ID, or use the booking list to validate arrival.">
            <Surface tone="owner">
              <LabeledInput label="QR / Booking Code / Phone" value={staffCheckInCode} onChangeText={setStaffCheckInCode} placeholder="B-501 or phone number" />
              <AppButton
                label="Check In Player"
                tone="owner"
                onPress={() => runAction(async () => {
                  await staffOpsCheckIn({ bookingCode: staffCheckInCode, qrPayload: staffCheckInCode, phone: staffCheckInCode, note: "Checked in from staff mobile app." });
                  setStaffCheckInCode("");
                }, "Player checked in.", false).then(() => refreshStaff("checkins", false))}
              />
            </Surface>
          </Section>
          <Section title="Today's Booking Queue" subtitle="Confirmed and active bookings available for staff operations.">
            {staffBookings.length === 0 ? (
              <EmptyState text="No bookings loaded for check-in." />
            ) : (
              staffBookings.slice(0, 10).map((booking) => (
                <Surface key={booking.id} tone="owner">
                  <Text style={styles.cardTitle}>{booking.bookingCode} - {booking.playerName}</Text>
                  <Text style={styles.cardMeta}>{booking.turfName} | {booking.bookingDate} | {booking.startTime} - {booking.endTime}</Text>
                  <Text style={styles.cardMeta}>Status {booking.status} | Attendance {booking.attendanceStatus || "Pending"}</Text>
                  {booking.status === "CONFIRMED" ? (
                    <View style={{ marginTop: 8 }}>
                      <AppButton label="Check In" tone="owner" onPress={() => runAction(() => staffOpsCheckIn({ bookingId: Number(booking.id), note: "Checked in from booking queue." }), "Player checked in.").then(() => refreshStaff("checkins", false))} />
                    </View>
                  ) : null}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (staffTab === "tasks") {
      return (
        <View style={styles.stack}>
          <Section title="Task Queue" subtitle="Accept, start, complete, or escalate assigned work.">
            {staffTasks.length === 0 ? (
              <EmptyState text="No assigned tasks." />
            ) : (
              staffTasks.map((task) => (
                <Surface key={task.id} tone="owner">
                  <Text style={styles.cardTitle}>{task.title}</Text>
                  <Text style={styles.cardMeta}>{task.priority} | {task.category} | {task.centerName}</Text>
                  <Text style={styles.cardMeta}>Status: {task.status}</Text>
                  <View style={[styles.actionRow, { marginTop: 8 }]}>
                    {["ACCEPTED", "IN_PROGRESS", "COMPLETED", "ESCALATED"].map((status) => (
                      <AppButton key={status} label={formatEnumLabel(status)} tone="owner" onPress={() => runAction(() => updateStaffOpsTaskStatus(Number(task.id), status), `Task moved to ${status}.`).then(() => refreshStaff("tasks", false))} />
                    ))}
                  </View>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (staffTab === "maintenance") {
      return (
        <View style={styles.stack}>
          <Section title="Create Maintenance Ticket" subtitle="Report ground, floodlight, net, washroom, or safety issues.">
            <Surface tone="owner">
              <LabeledInput label="Issue Title" value={staffMaintenanceTitle} onChangeText={setStaffMaintenanceTitle} placeholder="e.g. Net damage on pitch 2" />
              <LabeledInput label="Description" value={staffMaintenanceDescription} onChangeText={setStaffMaintenanceDescription} placeholder="Add evidence notes for manager review" multiline />
              <AppButton
                label="Create Ticket"
                tone="owner"
                onPress={() => runAction(async () => {
                  await createStaffOpsMaintenance({ title: staffMaintenanceTitle, description: staffMaintenanceDescription, category: "GROUND", priority: "HIGH" });
                  setStaffMaintenanceTitle("");
                  setStaffMaintenanceDescription("");
                }, "Maintenance ticket created.").then(() => refreshStaff("maintenance", false))}
              />
            </Surface>
          </Section>
          <Section title="Maintenance SLA Queue" subtitle="Start, resolve, or close assigned tickets.">
            {staffMaintenance.length === 0 ? (
              <EmptyState text="No maintenance tickets." />
            ) : (
              staffMaintenance.map((ticket) => (
                <Surface key={ticket.id} tone="owner">
                  <Text style={styles.cardTitle}>{ticket.title}</Text>
                  <Text style={styles.cardMeta}>{ticket.priority} | {ticket.category} | {ticket.centerName}</Text>
                  <Text style={styles.cardMeta}>Status {ticket.status} | SLA {ticket.slaDueAt || "--"}</Text>
                  <View style={[styles.actionRow, { marginTop: 8 }]}>
                    {["IN_PROGRESS", "RESOLVED", "CLOSED"].map((status) => (
                      <AppButton key={status} label={formatEnumLabel(status)} tone="owner" onPress={() => runAction(() => updateStaffOpsMaintenanceStatus(Number(ticket.id), status), `Ticket moved to ${status}.`).then(() => refreshStaff("maintenance", false))} />
                    ))}
                  </View>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (staffTab === "attendance") {
      return (
        <View style={styles.stack}>
          <Section title="Shift Attendance" subtitle="Start/end shift from mobile. GPS payload can be attached by native shell later.">
            <Surface tone="owner">
              <View style={styles.actionRow}>
                <AppButton label="Start Shift" tone="owner" onPress={() => runAction(() => startStaffOpsAttendance({ source: "MOBILE" }), "Shift started.").then(() => refreshStaff("attendance", false))} />
                <AppButton label="End Shift" tone="danger" onPress={() => runAction(() => endStaffOpsAttendance({ source: "MOBILE" }), "Shift ended.").then(() => refreshStaff("attendance", false))} />
              </View>
            </Surface>
            {staffAttendance.map((record) => (
              <Surface key={record.id} tone="owner">
                <Text style={styles.cardTitle}>{record.workDate} - {record.status}</Text>
                <Text style={styles.cardMeta}>{record.centerName} | {record.durationMinutes ?? 0} minutes | {record.source}</Text>
              </Surface>
            ))}
            {staffAttendance.length === 0 ? <EmptyState text="No attendance records yet." /> : null}
          </Section>
        </View>
      );
    }

    if (staffTab === "equipment") {
      return (
        <View style={styles.stack}>
          <Section title="Equipment Inventory" subtitle="Track available, issued, returned, and damaged operations assets.">
            {staffEquipment.length === 0 ? (
              <EmptyState text="No equipment inventory loaded." />
            ) : (
              staffEquipment.map((item) => (
                <Surface key={item.id} tone="owner">
                  <Text style={styles.cardTitle}>{item.name}</Text>
                  <Text style={styles.cardMeta}>{item.equipmentType} | stock {item.stockCount} | in use {item.inUseCount} | damaged {item.damagedCount}</Text>
                  <Text style={styles.cardMeta}>Status {item.status} | {item.centerName}</Text>
                  <View style={[styles.actionRow, { marginTop: 8 }]}>
                    <AppButton label="Issue" tone="owner" onPress={() => runAction(() => updateStaffOpsEquipment(Number(item.id), "issue"), "Equipment issued.").then(() => refreshStaff("equipment", false))} />
                    <AppButton label="Return" tone="owner" onPress={() => runAction(() => updateStaffOpsEquipment(Number(item.id), "return"), "Equipment returned.").then(() => refreshStaff("equipment", false))} />
                    <AppButton label="Damage" tone="danger" onPress={() => runAction(() => updateStaffOpsEquipment(Number(item.id), "damage"), "Equipment marked damaged.").then(() => refreshStaff("equipment", false))} />
                  </View>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (staffTab === "emergency") {
      return (
        <View style={styles.stack}>
          <Section title="Emergency Alert" subtitle="Raise medical, fire, power, or security escalation alerts.">
            <Surface tone="owner">
              <SelectField
                label="Emergency Type"
                value={formatEnumLabel(staffEmergencyType)}
                onPress={() => openPicker("Emergency Type", [
                  { label: "Medical", value: "MEDICAL" },
                  { label: "Fire", value: "FIRE" },
                  { label: "Power Failure", value: "POWER_FAILURE" },
                  { label: "Security Threat", value: "SECURITY_THREAT" }
                ], setStaffEmergencyType)}
              />
              <LabeledInput label="Message" value={staffEmergencyMessage} onChangeText={setStaffEmergencyMessage} placeholder="Describe the emergency and location" multiline />
              <AppButton
                label="Raise Emergency Alert"
                tone="danger"
                onPress={() => runAction(async () => {
                  await createStaffOpsEmergency({ type: staffEmergencyType, severity: "HIGH", message: staffEmergencyMessage });
                  setStaffEmergencyMessage("");
                }, "Emergency alert raised.").then(() => refreshStaff("emergency", false))}
              />
            </Surface>
          </Section>
          <Section title="Active Emergency Queue" subtitle="Resolve alerts after manager or owner escalation is complete.">
            {staffEmergencies.length === 0 ? (
              <EmptyState text="No emergency alerts." />
            ) : (
              staffEmergencies.map((alert) => (
                <Surface key={alert.id} tone="owner">
                  <Text style={styles.cardTitle}>{formatEnumLabel(alert.type)} - {alert.severity}</Text>
                  <Text style={styles.cardMeta}>{alert.centerName} | {alert.escalationLevel} | {alert.status}</Text>
                  <Text style={styles.helperText}>{alert.message}</Text>
                  {alert.status !== "RESOLVED" ? (
                    <View style={{ marginTop: 8 }}>
                      <AppButton label="Resolve Alert" tone="owner" onPress={() => runAction(() => resolveStaffOpsEmergency(Number(alert.id)), "Emergency resolved.").then(() => refreshStaff("emergency", false))} />
                    </View>
                  ) : null}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    return null;
  }

  function renderAdminPortal() {
    const handleApproveOwner = (ownerId: number, name: string) => {
      confirmAction(
        "Approve Owner",
        `Approve ${name} as an active platform owner?`,
        "Approve",
        "owner",
        () => void runAction(async () => {
          await updateAdminUserStatus(ownerId, "ACTIVE");
          await refreshAdmin("owners", false);
        }, "Owner approved successfully!", false)
      );
    };

    const handleBlockOwner = (ownerId: number, name: string) => {
      confirmAction(
        "Block Owner",
        `Block ${name}? This will prevent them from accessing their owner dashboard.`,
        "Block",
        "danger",
        () => void runAction(async () => {
          await updateAdminUserStatus(ownerId, "BLOCKED");
          await refreshAdmin("owners", false);
        }, "Owner blocked successfully!", false)
      );
    };

    const handleVerifyTurf = (turfId: number, name: string) => {
      confirmAction(
        "Verify Turf",
        `Verify ${name} and make it available for bookings?`,
        "Verify",
        "owner",
        () => void runAction(async () => {
          await updateAdminTurfStatus(turfId, "ACTIVE");
          await refreshAdmin("turfs", false);
        }, "Turf verified and activated!", false)
      );
    };

    const handleDisableTurf = (turfId: number, name: string) => {
      confirmAction(
        "Disable Turf",
        `Disable ${name}? This will hide it from customer discover search feeds.`,
        "Disable",
        "danger",
        () => void runAction(async () => {
          await updateAdminTurfStatus(turfId, "INACTIVE");
          await refreshAdmin("turfs", false);
        }, "Turf disabled!", false)
      );
    };

    const handleDeleteReview = (reviewId: number) => {
      confirmAction(
        "Delete Review",
        "Are you sure you want to delete this review? This action cannot be undone.",
        "Delete",
        "danger",
        () => void runAction(async () => {
          await deleteAdminReview(reviewId);
          await refreshAdmin("reviews", false);
        }, "Review deleted successfully!", false)
      );
    };

    const handleSendCampaign = (campaignId: number, name: string) => {
      confirmAction(
        "Send Campaign",
        `Are you sure you want to broadcast the campaign "${name}" to all target users?`,
        "Send",
        "owner",
        () => void runAction(async () => {
          await sendCampaign(campaignId);
          await refreshAdmin("campaigns", false);
        }, "Campaign broadcasted successfully!", false)
      );
    };

    const handleWarnUser = (userId: number, name: string) => {
      confirmAction(
        "Warn User",
        `Send an administrative warning message to player "${name}"?`,
        "Warn",
        "owner",
        () => void runAction(async () => {
          await warnUser(userId, "Marketplace conduct alert.");
          await refreshAdmin("analytics", false);
        }, "Warning recorded in platform audits!", false)
      );
    };

    const handleBanUser = (userId: number, name: string) => {
      confirmAction(
        "Ban User",
        `Block customer "${name}"? This will suspend their session and prevent any new bookings.`,
        "Ban",
        "danger",
        () => void runAction(async () => {
          await banUser(userId, "Marketplace abuse ban.");
          await refreshAdmin("owners", false);
        }, "User suspended!", false)
      );
    };

    const handleOverrideBooking = (bookingId: number, nextStatus: string) => {
      confirmAction(
        "Override Status",
        `Force update Booking #${bookingId} status to ${nextStatus}?`,
        "Override",
        "owner",
        () => void runAction(async () => {
          await overrideBookingStatus(bookingId, nextStatus);
          await refreshAdmin("bookings", false);
        }, "Booking status overridden!", false)
      );
    };

    const handleRefundBooking = (bookingId: number) => {
      confirmAction(
        "Manual Refund",
        `Trigger full manual refund and cancellation payout for Booking #${bookingId}?`,
        "Refund",
        "danger",
        () => void runAction(async () => {
          await triggerManualRefund(bookingId);
          await refreshAdmin("bookings", false);
        }, "Manual refund processed successfully!", false)
      );
    };

    const handleSendSupportReply = (ticketId: number) => {
      if (!supportReplyText) {
        Alert.alert("Error", "Reply content is required.");
        return;
      }
      runAction(
        async () => {
          await replySupportTicket(ticketId, { reply: supportReplyText, status: "RESOLVED" });
          setSupportReplyText("");
          await refreshAdmin("support", false);
        },
        "Reply sent and ticket resolved!",
        false
      );
    };

    const handleCreateIncidentOutage = () => {
      if (!mobileIncidentName || !mobileIncidentMessage) {
        Alert.alert("Error", "All incident fields are required.");
        return;
      }
      runAction(
        async () => {
          await createIncident({
            name: mobileIncidentName,
            severity: mobileIncidentSeverity,
            message: mobileIncidentMessage,
          });
          setMobileIncidentName("");
          setMobileIncidentMessage("");
          await refreshAdmin("incidents", false);
        },
        "New incident registered!",
        false
      );
    };

    const handleResolveIncidentOutage = (id: number) => {
      confirmAction(
        "Resolve Incident",
        "Mark this system outage as fully RESOLVED?",
        "Resolve",
        "owner",
        () => void runAction(async () => {
          await resolveIncident(id);
          await refreshAdmin("incidents", false);
        }, "Incident marked as resolved!", false)
      );
    };

    const handleCreateCampaign = () => {
      if (!campaignForm.name || !campaignForm.title || !campaignForm.message) {
        Alert.alert("Validation Error", "Please fill in all campaign fields.");
        return;
      }
      runAction(
        async () => {
          await createCampaign({
            ...campaignForm,
            targetRole: campaignForm.targetRole || undefined,
          });
          setCampaignForm({
            name: "",
            title: "",
            message: "",
            channel: "EMAIL",
            targetRole: "",
          });
          await refreshAdmin("campaigns", false);
        },
        "Campaign created successfully!",
        false
      );
    };

    if (adminTab === "analytics") {
      return (
        <View style={styles.stack}>
          <Section title="Super Admin Analytics" subtitle="Platform metrics summary and real-time operations console.">
            <View style={styles.metricGrid}>
              <Metric label="Total Users" value={adminAnalytics?.totalUsers ?? 0} tone="owner" />
              <Metric label="Total Owners" value={adminAnalytics?.totalOwners ?? 0} tone="owner" />
              <Metric label="Revenue Value" value={`Rs ${adminAnalytics?.totalRevenue ?? 0}`} tone="owner" />
              <Metric label="Risk Signals" value={adminAnalytics?.flaggedRiskCount ?? 0} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>EPIC 18 BI Decision Layer</Text>
              <Text style={[styles.cardMeta, { marginTop: 6 }]}>
                Confidence {adminBIWorkspace?.scorecard?.analyticsConfidenceScore ?? 0}% | Data quality {adminBIWorkspace?.dataQuality?.score ?? 0}% | Forecast {adminBIWorkspace?.scorecard?.forecastConfidence ?? 0}%
              </Text>
              {(adminBIWorkspace?.insights || []).slice(0, 3).map((item: any) => (
                <View key={item.title} style={styles.listRow}>
                  <Text style={styles.listTitle}>{item.title}</Text>
                  <Text style={styles.cardMeta}>{item.severity} | {item.message}</Text>
                </View>
              ))}
              {(adminBIWorkspace?.forecastAccuracy || []).slice(0, 3).map((item: any) => (
                <View key={item.metric} style={styles.listRow}>
                  <Text style={styles.listTitle}>{item.metric} forecast</Text>
                  <Text style={styles.cardMeta}>Deviation {item.deviation} | confidence {item.confidence}% | {item.status}</Text>
                </View>
              ))}
            </Surface>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Reports & Reconciliation</Text>
              <Text style={[styles.cardMeta, { marginTop: 6 }]}>
                Reports {(adminBIWorkspace?.reportCatalog || []).length} | Reconciliation {adminBIWorkspace?.reconciliation?.status ?? "PENDING"} | mismatches {(adminBIWorkspace?.reconciliation?.mismatches || []).length}
              </Text>
              {(adminBIWorkspace?.dashboardBuilder?.widgets || []).slice(0, 4).map((item: any) => (
                <View key={item.id} style={styles.listRow}>
                  <Text style={styles.listTitle}>{item.title}</Text>
                  <Text style={styles.cardMeta}>{item.type} | {item.dataKey}</Text>
                </View>
              ))}
            </Surface>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>EPIC 17 Governance Control Tower</Text>
              <Text style={[styles.cardMeta, { marginTop: 6 }]}>
                Governance score {adminGovernance?.governanceScore ?? 0}% | Risk {adminGovernance?.riskLevel ?? "LOW"} | Audit events {adminGovernance?.kpis?.auditEvents ?? adminAuditLogs.length}
              </Text>
              {(adminGovernance?.priorityActions || []).slice(0, 4).map((item: any) => (
                <View key={item.label} style={styles.listRow}>
                  <Text style={styles.listTitle}>{item.label}</Text>
                  <Text style={styles.cardMeta}>{item.count} item(s) | {item.owner} | {item.recommendation}</Text>
                </View>
              ))}
              {!(adminGovernance?.priorityActions || []).length ? (
                <Text style={[styles.cardMeta, { marginTop: 8 }]}>No active governance queue items.</Text>
              ) : null}
            </Surface>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>System Health & Activity</Text>
              <Text style={[styles.cardMeta, { marginTop: 6 }]}>
                All systems are functional. Real-time FCM notifications and biometrics are active. Dynamic demand models are recalculating slot values for players.
              </Text>
            </Surface>
          </Section>
        </View>
      );
    }

    if (adminTab === "ownerInsights") {
      return (
        <View style={styles.stack}>
          <Section title="Owner Trends" subtitle="Platform-wide owner revenue, occupancy, pricing, and community demand.">
            {!ownerInsights ? (
              <EmptyState text="Open this tab again or pull to refresh to load owner trends." />
            ) : (
              <>
                <View style={styles.metricGrid}>
                  <Metric label="Month GMV" value={`Rs ${ownerInsights.revenue.monthRevenue}`} tone="owner" />
                  <Metric label="Occupancy" value={`${ownerInsights.occupancy.occupancyPercent}%`} tone="owner" />
                  <Metric label="Repeat" value={ownerInsights.community.repeatCustomers} tone="owner" />
                  <Metric label="Demand" value={ownerInsights.forecast.demandLevel} tone="owner" />
                </View>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Pricing optimization queue</Text>
                  {ownerInsights.pricingSuggestions.slice(0, 5).map((item) => (
                    <View key={item.slotId} style={styles.listRow}>
                      <Text style={styles.listTitle}>{item.turfName}</Text>
                      <Text style={styles.cardMeta}>Rs {item.currentPrice} to Rs {item.recommendedPrice} | confidence {item.confidence}%</Text>
                    </View>
                  ))}
                </Surface>
                <Surface tone="owner">
                  <Text style={styles.cardTitle}>Peak demand windows</Text>
                  {ownerInsights.heatmap.slice(0, 5).map((cell) => (
                    <View key={`${cell.dayOfWeek}-${cell.hour}`} style={styles.listRow}>
                      <Text style={styles.listTitle}>{cell.dayOfWeek} {cell.hourLabel}</Text>
                      <Text style={styles.cardMeta}>{cell.bookingCount} bookings | {cell.utilizationPercent}% utilization</Text>
                    </View>
                  ))}
                </Surface>
              </>
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "bookingIntelligence") {
      return (
        <View style={styles.stack}>
          <Section title="Booking Intelligence" subtitle="Platform rebook history, recurring schedules, templates, waitlists, and conflict protection.">
            <View style={styles.metricGrid}>
              <Metric label="Rebooks" value={bookingIntelligence?.frequentBookings || 0} tone="owner" />
              <Metric label="Recurring" value={bookingIntelligence?.recurringBookings || 0} tone="owner" />
              <Metric label="Templates" value={bookingIntelligence?.templates || 0} tone="owner" />
              <Metric label="Waitlists" value={bookingIntelligence?.autoWaitlists || 0} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Automation health</Text>
              <Text style={styles.cardMeta}>Readiness {Math.round(bookingIntelligence?.rebookReadiness || 0)}% | conflicts {bookingIntelligence?.conflicts || 0}</Text>
            </Surface>
          </Section>
        </View>
      );
    }

    if (adminTab === "weather") {
      return (
        <View style={styles.stack}>
          <Section title="Weather Analytics" subtitle="City weather risk, weather refunds, and reschedule telemetry.">
            <Surface tone="owner">
              <FieldPair
                left={<LabeledInput label="City" value={weatherCity} onChangeText={setWeatherCity} />}
                right={<AppButton label="Refresh" onPress={() => runAction(async () => setWeatherDashboard(await getAdminWeatherDashboard(weatherCity)), "Weather analytics refreshed.", false)} />}
              />
            </Surface>
            <View style={styles.metricGrid}>
              <Metric label="Rain" value={`${Math.round(weatherDashboard?.current?.rainProbability || 0)}%`} tone="owner" />
              <Metric label="Heat" value={`${Math.round(weatherDashboard?.current?.temperature || 0)}C`} tone="owner" />
              <Metric label="Reschedules" value={weatherDashboard?.highRiskBookings || 0} tone="owner" />
              <Metric label="Refunds" value={weatherDashboard?.weatherRefunds || 0} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Platform signal</Text>
              <Text style={styles.cardMeta}>{weatherDashboard?.insight || "No weather analytics loaded yet."}</Text>
            </Surface>
          </Section>
        </View>
      );
    }

    if (adminTab === "gamification") {
      return (
        <View style={styles.stack}>
          <Section title="Gamification Control" subtitle="Platform engagement, rewards, challenges, leaderboards, and team competitions.">
            <View style={styles.metricGrid}>
              <Metric label="Players" value={gamificationAnalytics?.activePlayers ?? 0} tone="owner" />
              <Metric label="Completed" value={gamificationAnalytics?.completedChallenges ?? 0} tone="owner" />
              <Metric label="Rewards" value={gamificationAnalytics?.availableRewards ?? 0} tone="owner" />
              <Metric label="Rate" value={`${Math.round(gamificationAnalytics?.challengeCompletionRate || 0)}%`} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Retention signal</Text>
              <Text style={styles.cardMeta}>{gamificationAnalytics?.insight || "No gamification telemetry loaded yet."}</Text>
            </Surface>
          </Section>
          <Section title="Platform leaderboard" subtitle="Top XP players across Cricbook.">
            {gamificationAnalytics?.topPlayers?.map((entry) => (
              <Surface key={entry.userId} tone="owner">
                <Text style={styles.cardTitle}>#{entry.rank} {entry.playerName}</Text>
                <Text style={styles.cardMeta}>Level {entry.level} | {entry.score} XP</Text>
              </Surface>
            )) || <EmptyState text="No XP leaders yet." />}
          </Section>
        </View>
      );
    }

    if (adminTab === "payments") {
      return (
        <View style={styles.stack}>
          <Section title="Payment Monitoring" subtitle="Platform-wide split payment, settlement, and collection health.">
            <View style={styles.metricGrid}>
              <Metric label="Total Splits" value={splitAnalytics?.totalSplits ?? 0} tone="owner" />
              <Metric label="Paid Splits" value={splitAnalytics?.paidSplits ?? 0} tone="owner" />
              <Metric label="Pending Rs" value={`Rs ${splitAnalytics?.totalPending ?? 0}`} tone="owner" />
              <Metric label="Success" value={`${splitAnalytics?.collectionSuccessRate ?? 0}%`} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Financial risk signal</Text>
              <Text style={styles.cardMeta}>{splitAnalytics?.insight || "No split-payment telemetry available yet."}</Text>
            </Surface>
          </Section>
        </View>
      );
    }

    if (adminTab === "community") {
      return (
        <View style={styles.stack}>
          <Section title="Community Moderation" subtitle="Player finder, recruitment, connection and feed health.">
            <View style={styles.metricGrid}>
              <Metric label="Players" value={communityAnalytics?.discoverablePlayers ?? 0} tone="owner" />
              <Metric label="Connections" value={communityAnalytics?.acceptedConnections ?? 0} tone="owner" />
              <Metric label="Pending" value={communityAnalytics?.pendingConnections ?? 0} tone="owner" />
              <Metric label="Open Roles" value={communityAnalytics?.openRecruitments ?? 0} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Network signal</Text>
              <Text style={styles.cardMeta}>{communityAnalytics?.insight || "No community analytics available yet."}</Text>
            </Surface>
          </Section>
        </View>
      );
    }

    if (adminTab === "epics") {
      const activeEpic = platformEpics.find((epic) => epic.releaseStage === "ACTIVE") || platformEpics[0];

      return (
        <View style={styles.stack}>
          <Section title="Platform Epic Governance" subtitle="One backend contract across player, owner, admin, mobile, notifications, and analytics.">
            {!activeEpic ? (
              <EmptyState text="No platform epic registry loaded." />
            ) : (
              <Surface tone="owner">
                <Text style={styles.cardMeta}>{activeEpic.epicId} | {activeEpic.releaseStage}</Text>
                <Text style={styles.cardTitle}>{activeEpic.name}</Text>
                <Text style={styles.cardMeta}>{activeEpic.objective}</Text>
                <View style={[styles.chipWrap, { marginTop: 8 }]}>
                  {activeEpic.sharedDomains.map((domain) => (
                    <Chip key={domain} label={domain} active tone="owner" />
                  ))}
                </View>
              </Surface>
            )}
          </Section>

          {activeEpic ? (
            <>
              <Section title="Surface Coverage" subtitle="Each epic must declare coverage before implementation.">
                {activeEpic.surfaces.map((surface) => (
                  <Surface key={surface.surface} tone="owner">
                    <Text style={styles.cardTitle}>{surface.surface.replaceAll("_", " ")}</Text>
                    <Text style={styles.cardMeta}>Status: {surface.status} | Owner: {surface.owner}</Text>
                    {surface.capabilities.map((capability) => (
                      <Text key={capability} style={styles.helperText}>- {capability}</Text>
                    ))}
                  </Surface>
                ))}
              </Section>

              <Section title="Readiness Checklist" subtitle="Backend, web, mobile, notification, analytics, and theme checks.">
                {activeEpic.readiness.map((item) => (
                  <Surface key={item.key} tone={item.status === "PASS" ? "player" : "owner"}>
                    <Text style={styles.cardTitle}>{item.status}</Text>
                    <Text style={styles.cardMeta}>{item.label}</Text>
                  </Surface>
                ))}
              </Section>
            </>
          ) : null}
        </View>
      );
    }

    if (adminTab === "enterprise") {
      const kpis = adminEnterprise?.kpis || {};
      const governance = adminEnterpriseGovernance || adminEnterprise?.governance || {};
      const controls = governance?.controls || [];
      const exportsReady = adminEnterpriseReports?.exports || [];

      return (
        <View style={styles.stack}>
          <Section title="Enterprise Control Tower" subtitle="Multi-center franchise, staff hierarchy, revenue roll-up, CRM, and governance telemetry.">
            <View style={styles.metricGrid}>
              <Metric label="Centers" value={kpis.totalCenters ?? 0} tone="owner" />
              <Metric label="Turfs" value={kpis.totalTurfs ?? 0} tone="owner" />
              <Metric label="Staff" value={kpis.totalStaff ?? 0} tone="owner" />
              <Metric label="Revenue" value={`Rs ${Math.round(Number(kpis.totalRevenue || 0))}`} tone="owner" />
            </View>
            <Surface tone="owner">
              <Text style={styles.cardTitle}>Governance readiness</Text>
              <Text style={styles.cardMeta}>
                Status {governance.status || "WATCH"} | Score {governance.score ?? 0} | Franchises {kpis.totalFranchises ?? 0} | Customers {kpis.customerCount ?? 0}
              </Text>
              {controls.slice(0, 4).map((control: any) => (
                <View key={control.name} style={styles.listRow}>
                  <Text style={styles.listTitle}>{control.name}</Text>
                  <Text style={styles.cardMeta}>{control.status} | {control.detail}</Text>
                </View>
              ))}
            </Surface>
          </Section>

          <Section title="Center Rankings" subtitle="Revenue, occupancy, turf count, and manager coverage across centers.">
            {adminEnterpriseCenters.length === 0 ? (
              <EmptyState text="No enterprise centers loaded." />
            ) : (
              adminEnterpriseCenters.slice(0, 6).map((center) => (
                <Surface key={String(center.id)} tone="owner">
                  <Text style={styles.cardTitle}>{center.title || "Center"}</Text>
                  <Text style={styles.cardMeta}>{center.city || "Unmapped city"} | {center.turfs ?? 0} turf(s) | {center.bookings ?? 0} bookings</Text>
                  <Text style={styles.cardMeta}>Revenue Rs {Math.round(Number(center.revenue || 0))} | Occupancy {Math.round(Number(center.occupancyPercent || 0))}% | Manager {center.managerName || "Needs assignment"}</Text>
                </Surface>
              ))
            )}
          </Section>

          <Section title="Staff & Franchise Operations" subtitle="Enterprise hierarchy and franchise lifecycle snapshot.">
            {adminEnterpriseStaff.slice(0, 4).map((member) => (
              <Surface key={String(member.id)} tone="owner">
                <Text style={styles.cardTitle}>{member.title || "Staff member"}</Text>
                <Text style={styles.cardMeta}>{member.role || "STAFF"} | {member.centerName || "All centers"} | {member.status || "ACTIVE"}</Text>
                <Text style={styles.helperText}>Permissions: {member.permissions || "Scoped enterprise operations"}</Text>
              </Surface>
            ))}
            {adminEnterpriseFranchises.slice(0, 4).map((franchise) => (
              <Surface key={String(franchise.id)} tone="owner">
                <Text style={styles.cardTitle}>{franchise.title || "Franchise agreement"}</Text>
                <Text style={styles.cardMeta}>{franchise.city || "Multi-city"} | {franchise.status || "PENDING"} | Revenue Rs {Math.round(Number(franchise.revenue || 0))}</Text>
                <Text style={styles.helperText}>Revenue share {franchise.revenueSharePercent ?? 0}% | Compliance lifecycle tracked.</Text>
              </Surface>
            ))}
            {adminEnterpriseStaff.length === 0 && adminEnterpriseFranchises.length === 0 ? <EmptyState text="No staff or franchise records loaded." /> : null}
          </Section>

          <Section title="Enterprise Reports" subtitle="Revenue, occupancy, staff performance, and customer growth export readiness.">
            {exportsReady.length === 0 ? (
              <EmptyState text="No enterprise exports generated yet." />
            ) : (
              exportsReady.map((item: any) => (
                <Surface key={String(item.fileName)} tone="owner">
                  <Text style={styles.cardTitle}>{item.format} export</Text>
                  <Text style={styles.cardMeta}>{item.fileName}</Text>
                  <Text style={styles.helperText}>Status: {item.status || "READY"}</Text>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "owners") {
      return (
        <View style={styles.stack}>
          <Section title="Registered Owners Desk" subtitle="Verify and manage business owners.">
            {adminOwners.length === 0 ? (
              <EmptyState text="No registered owners found." />
            ) : (
              adminOwners.map((owner) => (
                <Surface key={owner.id} tone="owner">
                  <Text style={styles.cardTitle}>{owner.name}</Text>
                  <Text style={styles.cardMeta}>{owner.email} | {owner.phone || "No phone number"}</Text>
                  <Text style={styles.cardMeta}>Status: {formatEnumLabel(owner.status)}</Text>
                  <View style={[styles.actionRow, { marginTop: 8 }]}>
                    {owner.status !== "ACTIVE" && (
                      <AppButton label="Approve" tone="owner" onPress={() => handleApproveOwner(owner.id, owner.name)} />
                    )}
                    {owner.status !== "BLOCKED" && (
                      <AppButton label="Block" tone="danger" onPress={() => handleBlockOwner(owner.id, owner.name)} />
                    )}
                  </View>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "turfs") {
      return (
        <View style={styles.stack}>
          <Section title="Pending & Active Turfs" subtitle="Listing verification queue.">
            {adminTurfs.length === 0 ? (
              <EmptyState text="No registered turfs found." />
            ) : (
              adminTurfs.map((turf) => (
                <Surface key={turf.id} tone="owner">
                  <Text style={styles.cardTitle}>{turf.name}</Text>
                  <Text style={styles.cardMeta}>{turf.city} | {turf.location}</Text>
                  <Text style={styles.cardMeta}>Rate: Rs {turf.pricePerHour}/hr | Pitch: {formatEnumLabel(turf.pitchType)}</Text>
                  <Text style={styles.cardMeta}>Status: {formatEnumLabel(turf.status || "ACTIVE")}</Text>
                  <View style={[styles.actionRow, { marginTop: 8 }]}>
                    {turf.status !== "ACTIVE" && (
                      <AppButton label="Verify" tone="owner" onPress={() => handleVerifyTurf(turf.id ?? 0, turf.name)} />
                    )}
                    {turf.status !== "INACTIVE" && (
                      <AppButton label="Disable" tone="danger" onPress={() => handleDisableTurf(turf.id ?? 0, turf.name)} />
                    )}
                  </View>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "reviews") {
      return (
        <View style={styles.stack}>
          <Section title="Review Moderation Center" subtitle="Submit reviews or delete inappropriate listings.">
            {adminReviews.length === 0 ? (
              <EmptyState text="No user reviews registered." />
            ) : (
              adminReviews.map((review) => (
                <Surface key={review.id} tone="owner">
                  <Text style={{ color: "#ffd700", fontWeight: "bold" }}>Rating {review.rating}/5 | {review.status || "PUBLISHED"}</Text>
                  {review.title ? <Text style={[styles.cardMeta, { marginTop: 4, color: theme.colors.text }]}>{review.title}</Text> : null}
                  <Text style={[styles.cardMeta, { marginTop: 4, color: theme.colors.text }]}>{review.comment || "No comment content."}</Text>
                  <Text style={styles.cardMeta}>Turf: {review.turfName || `Turf #${review.turfId}`}</Text>
                  {review.moderationReason ? <Text style={styles.cardMeta}>Reason: {review.moderationReason}</Text> : null}
                  <View style={{ marginTop: 8 }}>
                    <AppButton label="Remove Review" tone="danger" onPress={() => handleDeleteReview(review.id)} />
                  </View>
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "monetization") {
      const kpis = adminMonetization?.kpis || {};
      return (
        <View style={styles.stack}>
          <Section title="Sponsorship & Ads Control" subtitle="Brand partners, marketplace inventory, proposals, ad tracking, leads, contracts, and billing.">
            <View style={styles.metricGrid}>
              <Metric label="Paid Revenue" value={`Rs ${kpis.paidRevenue ?? 0}`} tone="owner" />
              <Metric label="Pipeline" value={`Rs ${kpis.sponsorshipPipeline ?? 0}`} tone="owner" />
              <Metric label="Brands" value={kpis.brands ?? 0} tone="owner" />
              <Metric label="CTR" value={`${kpis.ctr ?? 0}%`} tone="owner" />
            </View>
            {(adminMonetization?.priorityActions || []).slice(0, 3).map((action: any) => (
              <Surface key={action.title} tone="owner">
                <Text style={styles.cardTitle}>{action.title}</Text>
                <Text style={styles.cardMeta}>{action.severity}</Text>
                <Text style={styles.helperText}>{action.description}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Marketplace Opportunities" subtitle="Tournament, team, player, and featured turf inventory for sponsors.">
            {adminMonetizationOpportunities.length === 0 ? (
              <EmptyState text="No sponsorship opportunities yet." />
            ) : (
              adminMonetizationOpportunities.slice(0, 6).map((opportunity) => (
                <Surface key={opportunity.id} tone="owner">
                  <Text style={styles.cardTitle}>{opportunity.title}</Text>
                  <Text style={styles.cardMeta}>{formatEnumLabel(opportunity.opportunityType)} | {opportunity.city || "All cities"} | Audience {opportunity.audienceSize ?? 0}</Text>
                  <Text style={styles.helperText}>Rs {opportunity.amount ?? 0} | {opportunity.status}</Text>
                </Surface>
              ))
            )}
          </Section>

          <Section title="Brands & Proposals" subtitle="Verification pipeline and sponsorship proposal decisions.">
            <View style={styles.metricGrid}>
              <Metric label="Verified" value={kpis.verifiedBrands ?? 0} tone="owner" />
              <Metric label="Pending Proposals" value={kpis.pendingProposals ?? 0} tone="owner" />
            </View>
            {adminMonetizationBrands.slice(0, 3).map((brand) => (
              <Surface key={brand.id} tone="owner">
                <Text style={styles.cardTitle}>{brand.companyName}</Text>
                <Text style={styles.cardMeta}>{formatEnumLabel(brand.brandType)} | {brand.city || "All cities"} | {brand.status}</Text>
              </Surface>
            ))}
            {adminMonetizationProposals.slice(0, 3).map((proposal) => (
              <Surface key={`proposal-${proposal.id}`} tone="owner">
                <Text style={styles.cardTitle}>{proposal.brandName} → {proposal.opportunityTitle}</Text>
                <Text style={styles.cardMeta}>Rs {proposal.proposedAmount ?? 0} | {proposal.status}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Sponsored Campaigns & Leads" subtitle="Impressions, clicks, conversions, campaign spend, and lead funnel.">
            <View style={styles.metricGrid}>
              <Metric label="Impressions" value={kpis.impressions ?? 0} tone="owner" />
              <Metric label="Clicks" value={kpis.clicks ?? 0} tone="owner" />
              <Metric label="Conversions" value={kpis.conversions ?? 0} tone="owner" />
              <Metric label="Leads" value={kpis.leads ?? 0} tone="owner" />
            </View>
            {adminMonetizationCampaigns.slice(0, 4).map((campaign) => (
              <Surface key={campaign.id} tone="owner">
                <Text style={styles.cardTitle}>{campaign.name}</Text>
                <Text style={styles.cardMeta}>{campaign.placement} | {campaign.status} | Spend Rs {campaign.spend ?? 0}</Text>
                <Text style={styles.helperText}>{campaign.impressions ?? 0} impressions | {campaign.clicks ?? 0} clicks | {campaign.conversions ?? 0} conversions</Text>
              </Surface>
            ))}
            {adminMonetizationLeads.slice(0, 4).map((lead) => (
              <Surface key={`lead-${lead.id}`} tone="owner">
                <Text style={styles.cardTitle}>{lead.name}</Text>
                <Text style={styles.cardMeta}>{formatEnumLabel(lead.leadType)} | {lead.brandName} | {lead.status}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Contracts, Invoices & Reports" subtitle="Contract lifecycle, GST invoices, paid revenue, and brand-safety controls.">
            <View style={styles.metricGrid}>
              <Metric label="Contracts" value={kpis.signedContracts ?? 0} tone="owner" />
              <Metric label="Invoices" value={kpis.invoices ?? 0} tone="owner" />
              <Metric label="Converted" value={kpis.convertedLeads ?? 0} tone="owner" />
              <Metric label="ROI" value={`${kpis.roi ?? 0}%`} tone="owner" />
            </View>
            {adminMonetizationContracts.slice(0, 3).map((contract) => (
              <Surface key={contract.id} tone="owner">
                <Text style={styles.cardTitle}>{contract.contractNumber}</Text>
                <Text style={styles.cardMeta}>{contract.brandName} | Rs {contract.value ?? 0} | {contract.status}</Text>
              </Surface>
            ))}
            {adminMonetizationInvoices.slice(0, 3).map((invoice) => (
              <Surface key={`invoice-${invoice.id}`} tone="owner">
                <Text style={styles.cardTitle}>{invoice.invoiceNumber}</Text>
                <Text style={styles.cardMeta}>{invoice.revenueCategory} | Total Rs {invoice.totalAmount ?? 0} | {invoice.status}</Text>
              </Surface>
            ))}
            {adminMonetizationReports?.brandSafety ? (
              <Surface tone="owner">
                <Text style={styles.cardTitle}>Brand Safety</Text>
                <Text style={styles.cardMeta}>Pending brands: {adminMonetizationReports.brandSafety.pendingVerification ?? 0}</Text>
                <Text style={styles.cardMeta}>Pending proposals: {adminMonetizationReports.brandSafety.pendingProposals ?? 0}</Text>
                <Text style={styles.cardMeta}>Active campaigns: {adminMonetizationReports.brandSafety.activeCampaigns ?? 0}</Text>
              </Surface>
            ) : null}
          </Section>
        </View>
      );
    }

    if (adminTab === "sports") {
      const kpis = adminSports?.kpis || {};
      return (
        <View style={styles.stack}>
          <Section title="Sports OS Control Center" subtitle="Multi-sport catalog, rules, scoring, rankings, turf mappings, and no-code sport onboarding.">
            <View style={styles.metricGrid}>
              <Metric label="Configured" value={kpis.configuredSports ?? 0} tone="owner" />
              <Metric label="Active" value={kpis.activeSports ?? 0} tone="owner" />
              <Metric label="Multi-Sport Turfs" value={kpis.multiSportTurfs ?? 0} tone="owner" />
              <Metric label="Readiness" value={`${kpis.futureSportReadinessScore ?? 0}%`} tone="owner" />
            </View>
            {(adminSports?.priorityActions || []).slice(0, 3).map((action: any) => (
              <Surface key={action.title} tone="owner">
                <Text style={styles.cardTitle}>{action.title}</Text>
                <Text style={styles.cardMeta}>{action.severity}</Text>
                <Text style={styles.helperText}>{action.description}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Sport Catalog" subtitle="Cricket, Football, Badminton, Tennis, Pickleball, Basketball, Volleyball, and future sports.">
            {adminSportsCatalog.length === 0 ? (
              <EmptyState text="No sports configured yet." />
            ) : (
              adminSportsCatalog.map((sport) => (
                <Surface key={sport.id || sport.code} tone="owner">
                  <Text style={styles.cardTitle}>{sport.name}</Text>
                  <Text style={styles.cardMeta}>{sport.code} | {sport.active ? "ACTIVE" : "DISABLED"}</Text>
                  <Text style={styles.helperText}>
                    Players {sport.minPlayers ?? "-"}-{sport.maxPlayers ?? "-"} | {sport.scoringType || "CUSTOM"} | {sport.matchType || "CUSTOM"} | {sport.rankingType || "CUSTOM"}
                  </Text>
                </Surface>
              ))
            )}
          </Section>

          <Section title="Cross-Module Readiness" subtitle="Booking, teams, matches, tournaments, analytics, and catalog status.">
            {(adminSports?.crossModuleStatus || []).map((item: any) => (
              <Surface key={item.module} tone="owner">
                <Text style={styles.cardTitle}>{item.module}</Text>
                <Text style={styles.cardMeta}>{item.status} | {item.records ?? 0} records</Text>
                <Text style={styles.helperText}>{item.description}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Sport Analytics" subtitle="Sport-specific segmentation across teams, tournaments, matches, and turf mappings.">
            <View style={styles.metricGrid}>
              {(adminSportsAnalytics?.segmentation || []).slice(0, 6).map((row: any) => (
                <Metric key={row.sportCode} label={row.sportCode} value={`${row.teams ?? 0} teams`} tone="owner" />
              ))}
            </View>
            {adminSportsAudit.slice(0, 4).map((item) => (
              <Surface key={item.id} tone="owner">
                <Text style={styles.cardTitle}>{item.action}</Text>
                <Text style={styles.cardMeta}>{item.sportCode || "SPORT"} | {item.actor || "system"}</Text>
                <Text style={styles.helperText}>{item.summary}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (adminTab === "developers") {
      const kpis = adminDeveloperPlatform?.kpis || {};
      const usage = adminDeveloperAnalytics?.usageAnalytics || adminDeveloperPlatform?.usageAnalytics || {};
      return (
        <View style={styles.stack}>
          <Section title="Developer Ecosystem Control" subtitle="OAuth apps, API keys, webhooks, sandbox, usage, billing, and partner marketplace governance.">
            <View style={styles.metricGrid}>
              <Metric label="Developers" value={kpis.developers ?? 0} tone="owner" />
              <Metric label="Apps" value={kpis.applications ?? 0} tone="owner" />
              <Metric label="API Keys" value={kpis.activeApiKeys ?? 0} tone="owner" />
              <Metric label="Calls" value={kpis.apiCalls ?? 0} tone="owner" />
            </View>
            {(adminDeveloperPlatform?.priorityActions || []).slice(0, 3).map((action: any) => (
              <Surface key={action.title} tone="owner">
                <Text style={styles.cardTitle}>{action.title}</Text>
                <Text style={styles.cardMeta}>{action.severity}</Text>
                <Text style={styles.helperText}>{action.description}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Developers & OAuth Apps" subtitle="Partner accounts, client IDs, environments, scopes, and sandbox onboarding.">
            {adminDevelopers.length === 0 ? (
              <EmptyState text="No developer accounts yet." />
            ) : (
              adminDevelopers.slice(0, 5).map((developer) => (
                <Surface key={developer.id} tone="owner">
                  <Text style={styles.cardTitle}>{developer.name}</Text>
                  <Text style={styles.cardMeta}>{developer.companyName || "Independent"} | {developer.email}</Text>
                  <Text style={styles.helperText}>{developer.plan || "FREE"} | {developer.status || "ACTIVE"}</Text>
                </Surface>
              ))
            )}
            {adminDeveloperApplications.slice(0, 5).map((app) => (
              <Surface key={`app-${app.id}`} tone="owner">
                <Text style={styles.cardTitle}>{app.name}</Text>
                <Text style={styles.cardMeta}>{app.developerName} | {app.environment} | {app.status}</Text>
                <Text style={styles.helperText}>Client: {app.clientId}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Keys, Webhooks & Gateway" subtitle="Hashed API keys, rate limits, endpoint signatures, retry tracking, and external event delivery.">
            <View style={styles.metricGrid}>
              <Metric label="Webhooks" value={kpis.webhooks ?? 0} tone="owner" />
              <Metric label="Errors" value={usage.errorCount ?? kpis.errorCount ?? 0} tone="owner" />
              <Metric label="Latency" value={`${usage.avgLatencyMs ?? kpis.avgLatencyMs ?? 0} ms`} tone="owner" />
              <Metric label="Marketplace" value={kpis.marketplaceListings ?? 0} tone="owner" />
            </View>
            {adminDeveloperApiKeys.slice(0, 4).map((key) => (
              <Surface key={`key-${key.id}`} tone="owner">
                <Text style={styles.cardTitle}>{key.keyPrefix}</Text>
                <Text style={styles.cardMeta}>{key.applicationName} | {key.environment} | {key.status}</Text>
              </Surface>
            ))}
            {adminDeveloperWebhooks.slice(0, 4).map((webhook) => (
              <Surface key={`webhook-${webhook.id}`} tone="owner">
                <Text style={styles.cardTitle}>{webhook.applicationName}</Text>
                <Text style={styles.cardMeta}>{webhook.status} | retries {webhook.retryCount ?? 0}</Text>
                <Text style={styles.helperText}>{webhook.url}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Billing, Marketplace & Audit" subtitle="Rate plans, invoice reconciliation, partner listings, and immutable platform audit events.">
            <View style={styles.metricGrid}>
              <Metric label="Revenue" value={`Rs ${adminDeveloperBilling?.totalRevenue ?? 0}`} tone="owner" />
              <Metric label="Invoices" value={(adminDeveloperBilling?.invoices || []).length} tone="owner" />
              <Metric label="Approved" value={kpis.approvedListings ?? 0} tone="owner" />
              <Metric label="Audit" value={adminDeveloperAudit.length} tone="owner" />
            </View>
            {adminDeveloperMarketplace.slice(0, 4).map((listing) => (
              <Surface key={`marketplace-${listing.id}`} tone="owner">
                <Text style={styles.cardTitle}>{listing.title}</Text>
                <Text style={styles.cardMeta}>{listing.developerName} | {listing.category} | {listing.status}</Text>
              </Surface>
            ))}
            {adminDeveloperAudit.slice(0, 4).map((item) => (
              <Surface key={`dev-audit-${item.id}`} tone="owner">
                <Text style={styles.cardTitle}>{item.action}</Text>
                <Text style={styles.cardMeta}>{item.actor || "system"}</Text>
                <Text style={styles.helperText}>{item.summary}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (adminTab === "academy") {
      const kpis = adminAcademy?.kpis || {};
      const skill = adminAcademyAnalytics?.skillImprovementTrend || adminAcademy?.skillProgression || {};
      return (
        <View style={styles.stack}>
          <Section title="Academy & Talent Lifecycle" subtitle="Academies, coaches, programs, batches, enrollment, attendance, assessments, certificates, and discovery.">
            <View style={styles.metricGrid}>
              <Metric label="Academies" value={kpis.academies ?? 0} tone="owner" />
              <Metric label="Coaches" value={kpis.coaches ?? 0} tone="owner" />
              <Metric label="Programs" value={kpis.programs ?? 0} tone="owner" />
              <Metric label="Enrollments" value={kpis.enrollments ?? 0} tone="owner" />
            </View>
            {(adminAcademy?.priorityActions || []).slice(0, 3).map((action: any) => (
              <Surface key={action.title} tone="owner">
                <Text style={styles.cardTitle}>{action.title}</Text>
                <Text style={styles.cardMeta}>{action.severity}</Text>
                <Text style={styles.helperText}>{action.description}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Academies & Coaches" subtitle="Approval, accreditation, verification, certifications, marketplace visibility, and coach profiles.">
            {adminAcademies.length === 0 ? (
              <EmptyState text="No academies registered yet." />
            ) : (
              adminAcademies.slice(0, 5).map((academy) => (
                <Surface key={academy.id} tone="owner">
                  <Text style={styles.cardTitle}>{academy.name}</Text>
                  <Text style={styles.cardMeta}>{academy.sportsOffered} | {academy.city || "All cities"} | {academy.status}</Text>
                </Surface>
              ))
            )}
            {adminCoaches.slice(0, 5).map((coach) => (
              <Surface key={`coach-${coach.id}`} tone="owner">
                <Text style={styles.cardTitle}>{coach.name}</Text>
                <Text style={styles.cardMeta}>{coach.sport} | {coach.experienceYears ?? 0} years | {coach.status}</Text>
                <Text style={styles.helperText}>{coach.certifications || "Certification pending"}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Programs, Batches & Enrollment" subtitle="Training program supply, batch capacity, waitlists, active students, and CRM segmentation.">
            <View style={styles.metricGrid}>
              <Metric label="Published" value={kpis.publishedPrograms ?? 0} tone="owner" />
              <Metric label="Batches" value={kpis.batches ?? 0} tone="owner" />
              <Metric label="Confirmed" value={kpis.confirmedEnrollments ?? 0} tone="owner" />
              <Metric label="Attendance" value={`${kpis.attendancePercent ?? 0}%`} tone="owner" />
            </View>
            {adminTrainingPrograms.slice(0, 4).map((program) => (
              <Surface key={`program-${program.id}`} tone="owner">
                <Text style={styles.cardTitle}>{program.title}</Text>
                <Text style={styles.cardMeta}>{program.sport} | {program.level} | Rs {program.price ?? 0}</Text>
                <Text style={styles.helperText}>{program.status} | Capacity {program.capacity ?? 0}</Text>
              </Surface>
            ))}
            {adminTrainingBatches.slice(0, 4).map((batch) => (
              <Surface key={`batch-${batch.id}`} tone="owner">
                <Text style={styles.cardTitle}>{batch.name}</Text>
                <Text style={styles.cardMeta}>{batch.scheduleLabel} | {batch.enrolledCount ?? 0}/{batch.capacity ?? 0}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Assessment, Talent & Certificates" subtitle="Skill scores, progression, rankings, talent discovery, selector readiness, and QR-verifiable certificates.">
            <View style={styles.metricGrid}>
              <Metric label="Avg Score" value={kpis.averageAssessment ?? 0} tone="owner" />
              <Metric label="Talent" value={kpis.discoverableTalent ?? 0} tone="owner" />
              <Metric label="Certificates" value={kpis.certificates ?? 0} tone="owner" />
              <Metric label="Revenue" value={`Rs ${adminAcademyRevenue?.programRevenue ?? kpis.revenue ?? 0}`} tone="owner" />
            </View>
            {Object.entries(skill).slice(0, 4).map(([key, value]) => (
              <Surface key={key} tone="owner">
                <Text style={styles.cardTitle}>{formatEnumLabel(key)}</Text>
                <Text style={styles.cardMeta}>{String(value)}</Text>
              </Surface>
            ))}
            {adminTalentProfiles.slice(0, 4).map((talent) => (
              <Surface key={`talent-${talent.id}`} tone="owner">
                <Text style={styles.cardTitle}>{talent.playerName}</Text>
                <Text style={styles.cardMeta}>{talent.ageGroup} | {talent.sport} | Score {talent.talentScore ?? 0}</Text>
                <Text style={styles.helperText}>{talent.rankingBand} | {talent.status}</Text>
              </Surface>
            ))}
            {adminAcademyCertificates.slice(0, 4).map((certificate) => (
              <Surface key={`certificate-${certificate.id}`} tone="owner">
                <Text style={styles.cardTitle}>{certificate.certificateCode}</Text>
                <Text style={styles.cardMeta}>{certificate.playerName} | {certificate.certificateType} | {certificate.status}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Video, Governance & Audit" subtitle="Training video feedback, accreditation controls, talent privacy, certification audit, and operational history.">
            {adminAcademyVideos.slice(0, 3).map((video) => (
              <Surface key={`video-${video.id}`} tone="owner">
                <Text style={styles.cardTitle}>{video.category}</Text>
                <Text style={styles.cardMeta}>{video.playerEmail}</Text>
                <Text style={styles.helperText}>{video.coachComments || video.videoUrl}</Text>
              </Surface>
            ))}
            {(adminAcademy?.governanceControls || []).slice(0, 3).map((control: any) => (
              <Surface key={control.key} tone="owner">
                <Text style={styles.cardTitle}>{control.key}</Text>
                <Text style={styles.cardMeta}>{control.status}</Text>
                <Text style={styles.helperText}>{control.description}</Text>
              </Surface>
            ))}
            {adminAcademyAudit.slice(0, 3).map((item) => (
              <Surface key={`academy-audit-${item.id}`} tone="owner">
                <Text style={styles.cardTitle}>{item.action}</Text>
                <Text style={styles.cardMeta}>{item.actor || "system"}</Text>
                <Text style={styles.helperText}>{item.summary}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (adminTab === "corporate") {
      const kpis = adminCorporate?.kpis || {};
      const analytics = adminCorporateAnalytics || adminCorporate?.analytics || {};
      const billing = adminCorporateBilling || adminCorporate?.billing || {};
      return (
        <View style={styles.stack}>
          <Section title="Corporate Sports & Wellness" subtitle="B2B company onboarding, employees, teams, recurring bookings, wellness programs, and annual contracts.">
            <View style={styles.metricGrid}>
              <Metric label="Companies" value={kpis.companies ?? adminCorporateCompanies.length} tone="owner" />
              <Metric label="Employees" value={kpis.employees ?? adminCorporateEmployees.length} tone="owner" />
              <Metric label="Teams" value={kpis.teams ?? adminCorporateTeams.length} tone="owner" />
              <Metric label="Participation" value={`${kpis.participationRate ?? 0}%`} tone="owner" />
            </View>
            {(adminCorporate?.priorityActions || []).slice(0, 3).map((action: any) => (
              <Surface key={action.title} tone="owner">
                <Text style={styles.cardTitle}>{action.title}</Text>
                <Text style={styles.cardMeta}>{action.severity}</Text>
                <Text style={styles.helperText}>{action.detail}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Companies, Locations & Employees" subtitle="Corporate approval pipeline, HR contacts, multi-location structure, department hierarchy, and employee records.">
            {adminCorporateCompanies.length === 0 ? (
              <EmptyState text="No corporate companies registered yet." />
            ) : (
              adminCorporateCompanies.slice(0, 5).map((company) => (
                <Surface key={company.id} tone="owner">
                  <Text style={styles.cardTitle}>{company.name}</Text>
                  <Text style={styles.cardMeta}>{company.city || "All cities"} | {company.industry || "Industry pending"} | {company.status}</Text>
                  <Text style={styles.helperText}>{company.hrEmail || company.adminEmail || "HR contact pending"}</Text>
                </Surface>
              ))
            )}
            <View style={styles.metricGrid}>
              <Metric label="Locations" value={adminCorporateLocations.length} tone="owner" />
              <Metric label="Departments" value={adminCorporateDepartments.length} tone="owner" />
              <Metric label="Active" value={kpis.activeEmployees ?? 0} tone="owner" />
              <Metric label="Sports Hours" value={kpis.sportsHours ?? 0} tone="owner" />
            </View>
          </Section>

          <Section title="Teams, Bookings & Corporate Tournaments" subtitle="Department teams, captaincy, recurring booking plans, corporate tournaments, fixture readiness, and rankings.">
            <View style={styles.metricGrid}>
              <Metric label="Memberships" value={adminCorporateMemberships.length} tone="owner" />
              <Metric label="Bookings" value={adminCorporateBookingPlans.length} tone="owner" />
              <Metric label="Tournaments" value={adminCorporateTournaments.length} tone="owner" />
              <Metric label="Leagues" value={adminCorporateLeagues.length} tone="owner" />
            </View>
            {adminCorporateBookingPlans.slice(0, 4).map((plan) => (
              <Surface key={`corp-plan-${plan.id}`} tone="owner">
                <Text style={styles.cardTitle}>{plan.sport}</Text>
                <Text style={styles.cardMeta}>{plan.recurrenceType} | {plan.generatedBookings ?? 0} generated bookings | Rs {plan.monthlySpend ?? 0}</Text>
                <Text style={styles.helperText}>{plan.companyName || plan.city || "Corporate booking plan"}</Text>
              </Surface>
            ))}
            {adminCorporateTournaments.slice(0, 3).map((tournament) => (
              <Surface key={`corp-tournament-${tournament.id}`} tone="owner">
                <Text style={styles.cardTitle}>{tournament.name}</Text>
                <Text style={styles.cardMeta}>{tournament.scope} | {tournament.sport} | {tournament.status}</Text>
                <Text style={styles.helperText}>{tournament.rankingStatus}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Wellness, Rewards & Participation" subtitle="Wellness challenges, attendance, completion tracking, sports hours, points credit, redemption, and participation analytics.">
            <View style={styles.metricGrid}>
              <Metric label="Programs" value={adminCorporateWellnessPrograms.length} tone="owner" />
              <Metric label="Activities" value={adminCorporateWellnessActivities.length} tone="owner" />
              <Metric label="Rewards" value={adminCorporateRewards.length} tone="owner" />
              <Metric label="Wellness" value={analytics.wellnessScore ?? 0} tone="owner" />
            </View>
            {adminCorporateWellnessPrograms.slice(0, 4).map((program) => (
              <Surface key={`corp-wellness-${program.id}`} tone="owner">
                <Text style={styles.cardTitle}>{program.title}</Text>
                <Text style={styles.cardMeta}>{program.programType} | {program.status} | {program.rewardPoints ?? 0} points</Text>
                <Text style={styles.helperText}>{program.enrolledEmployees ?? 0} enrolled | {program.completedEmployees ?? 0} completed</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Billing, Contracts, CRM & Audit" subtitle="Invoice generation, GST/tax totals, payment state, ARR, renewal probability, account health, and audit history.">
            <View style={styles.metricGrid}>
              <Metric label="ARR" value={`Rs ${billing.activeArr ?? 0}`} tone="owner" />
              <Metric label="Invoices" value={adminCorporateInvoices.length} tone="owner" />
              <Metric label="Outstanding" value={billing.outstandingInvoices ?? 0} tone="owner" />
              <Metric label="Contracts" value={adminCorporateContracts.length} tone="owner" />
            </View>
            {adminCorporateContracts.slice(0, 3).map((contract) => (
              <Surface key={`corp-contract-${contract.id}`} tone="owner">
                <Text style={styles.cardTitle}>{contract.companyName || contract.contractNumber}</Text>
                <Text style={styles.cardMeta}>{contract.planName || "Plan"} | Rs {contract.annualValue ?? 0} | {contract.status}</Text>
                <Text style={styles.helperText}>Renewal probability {contract.renewalProbability ?? 0}%</Text>
              </Surface>
            ))}
            {adminCorporateAudit.slice(0, 4).map((item) => (
              <Surface key={`corp-audit-${item.id}`} tone="owner">
                <Text style={styles.cardTitle}>{item.action}</Text>
                <Text style={styles.cardMeta}>{item.actor || "system"}</Text>
                <Text style={styles.helperText}>{item.summary}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (adminTab === "saas") {
      const kpis = adminSaasDashboard?.kpis || {};
      const tenants = adminSaasDashboard?.tenants || [];
      const domains = adminSaasDashboard?.domains || [];
      const features = adminSaasDashboard?.featureToggles || [];
      const modules = adminSaasDashboard?.marketplaceModules || [];
      const invoices = adminSaasDashboard?.invoices || [];
      const controls = adminSaasDashboard?.globalControls || [];
      const tenantId = Number(adminSaasTenantId || tenants[0]?.id || 0);
      const firstDomainId = Number(domains[0]?.id || 0);
      const firstModuleId = Number(modules[0]?.id || 0);
      const firstInvoiceId = Number(invoices[0]?.id || 0);

      return (
        <View style={styles.stack}>
          <Section title="Global SaaS Control Plane" subtitle="Multi-tenant isolation, white-label branding, custom domains, feature flags, SaaS billing, app builds, marketplace modules, and compliance.">
            <View style={styles.metricGrid}>
              <Metric label="Tenants" value={kpis.tenants ?? tenants.length} tone="owner" />
              <Metric label="Active" value={kpis.activeTenants ?? 0} tone="owner" />
              <Metric label="MRR" value={`Rs ${kpis.mrr ?? 0}`} tone="owner" />
              <Metric label="Domains" value={kpis.customDomains ?? domains.length} tone="owner" />
            </View>
            {controls.slice(0, 4).map((control: any) => (
              <Surface key={control.name} tone="owner">
                <Text style={styles.cardTitle}>{control.name}</Text>
                <Text style={styles.cardMeta}>{control.status}</Text>
                <Text style={styles.helperText}>{control.detail}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Tenant Onboarding" subtitle="Create regional tenants and use the selected tenant id for follow-up actions.">
            <Surface tone="owner">
              <FieldPair
                left={<LabeledInput label="Tenant name" value={adminSaasTenantDraft.name} onChangeText={(value) => setAdminSaasTenantDraft((current) => ({ ...current, name: value }))} placeholder="Truf UAE" />}
                right={<LabeledInput label="Domain" value={adminSaasTenantDraft.domain} onChangeText={(value) => setAdminSaasTenantDraft((current) => ({ ...current, domain: value }))} placeholder="uae.trufsports.com" />}
              />
              <FieldPair
                left={<LabeledInput label="Country" value={adminSaasTenantDraft.countryCode} onChangeText={(value) => setAdminSaasTenantDraft((current) => ({ ...current, countryCode: value.toUpperCase() }))} />}
                right={<LabeledInput label="Currency" value={adminSaasTenantDraft.currency} onChangeText={(value) => setAdminSaasTenantDraft((current) => ({ ...current, currency: value.toUpperCase() }))} />}
              />
              <AppButton label="Create Tenant" tone="owner" onPress={() => runAction(() => createAdminSaasTenant(adminSaasTenantDraft), "Tenant created.")} />
            </Surface>
            <FieldPair
              left={<LabeledInput label="Selected tenant id" value={adminSaasTenantId} onChangeText={setAdminSaasTenantId} keyboardType="numeric" />}
              right={<LabeledInput label="Custom domain" value={adminSaasDomain} onChangeText={setAdminSaasDomain} placeholder="book.brand.com" />}
            />
          </Section>

          <Section title="Branding, Domains & Feature Flags" subtitle="White-label settings keep the current theme tokens while enabling tenant-specific identity and controls.">
            <View style={styles.metricGrid}>
              <Metric label="White Label" value={kpis.whiteLabelTenants ?? 0} tone="owner" />
              <Metric label="Active Domains" value={kpis.activeDomains ?? 0} tone="owner" />
              <Metric label="Features" value={features.length} tone="owner" />
              <Metric label="Modules" value={modules.length} tone="owner" />
            </View>
            <Surface tone="owner">
              <FieldPair
                left={<LabeledInput label="Feature key" value={adminSaasFeatureKey} onChangeText={(value) => setAdminSaasFeatureKey(value.toUpperCase())} />}
                right={<LabeledInput label="App version" value={adminSaasAppVersion} onChangeText={setAdminSaasAppVersion} />}
              />
              <View style={styles.actionRow}>
                <AppButton label="Save Branding" tone="owner" onPress={() => runAction(() => saveAdminSaasBranding({ tenantId, appName: adminSaasTenantDraft.name || "Truf Sports", fontFamily: "Inter", published: true }), "Branding saved.")} disabled={!tenantId} />
                <AppButton label="Enable Feature" tone="owner" onPress={() => runAction(() => setAdminSaasFeature({ tenantId, featureKey: adminSaasFeatureKey, enabled: true, rolloutStrategy: "TENANT_OVERRIDE" }), "Feature enabled.")} disabled={!tenantId} />
              </View>
              <View style={styles.actionRow}>
                <AppButton label="Add Domain" tone="owner" onPress={() => runAction(() => addAdminSaasDomain({ tenantId, domain: adminSaasDomain, routingMode: "CNAME" }), "Domain added.")} disabled={!tenantId || !adminSaasDomain} />
                <AppButton label="Verify First Domain" tone="owner" onPress={() => runAction(() => verifyAdminSaasDomain(firstDomainId), "Domain verified.")} disabled={!firstDomainId} />
              </View>
            </Surface>
            {domains.slice(0, 4).map((domain: any) => (
              <Surface key={`saas-domain-${domain.id}`} tone="owner">
                <Text style={styles.cardTitle}>{domain.domain}</Text>
                <Text style={styles.cardMeta}>{domain.routingMode} | {domain.status}</Text>
                <Text style={styles.helperText}>Tenant {domain.tenantId} | token {domain.verificationToken}</Text>
              </Surface>
            ))}
          </Section>

          <Section title="Subscriptions, Billing & Marketplace" subtitle="Plan entitlements, invoice reconciliation, marketplace installs, app builds, and compliance workflows.">
            <FieldPair
              left={<LabeledInput label="Plan" value={adminSaasPlanCode} onChangeText={(value) => setAdminSaasPlanCode(value.toUpperCase())} />}
              right={<LabeledInput label="Invoice amount" value={adminSaasInvoiceAmount} onChangeText={setAdminSaasInvoiceAmount} keyboardType="numeric" />}
            />
            <FieldPair
              left={<LabeledInput label="Payment amount" value={adminSaasPaymentAmount} onChangeText={setAdminSaasPaymentAmount} keyboardType="numeric" />}
              right={<LabeledInput label="App build version" value={adminSaasAppVersion} onChangeText={setAdminSaasAppVersion} />}
            />
            <View style={styles.actionRow}>
              <AppButton label="Create Subscription" tone="owner" onPress={() => runAction(() => createAdminSaasSubscription({ tenantId, planCode: adminSaasPlanCode, billingCycle: "MONTHLY", price: Number(adminSaasInvoiceAmount || 1) }), "Subscription created.")} disabled={!tenantId} />
              <AppButton label="Generate Invoice" tone="owner" onPress={() => runAction(() => generateAdminSaasInvoice({ tenantId, amount: Number(adminSaasInvoiceAmount || 1), taxAmount: 0 }), "Invoice generated.")} disabled={!tenantId} />
            </View>
            <View style={styles.actionRow}>
              <AppButton label="Pay First Invoice" tone="owner" onPress={() => runAction(() => payAdminSaasInvoice(firstInvoiceId, { amount: Number(adminSaasPaymentAmount || 1), paymentReference: "MOBILE-MANUAL" }), "Invoice payment recorded.")} disabled={!firstInvoiceId} />
              <AppButton label="Install First Module" tone="owner" onPress={() => runAction(() => installAdminSaasModule({ tenantId, moduleId: firstModuleId, licenseType: "FREE" }), "Module installed.")} disabled={!tenantId || !firstModuleId} />
            </View>
            <View style={styles.actionRow}>
              <AppButton label="Request App Build" tone="owner" onPress={() => runAction(() => requestAdminSaasAppBuild({ tenantId, appType: "WHITE_LABEL", platform: "MOBILE", version: adminSaasAppVersion }), "App build requested.")} disabled={!tenantId} />
              <AppButton label="Open Compliance" tone="owner" onPress={() => runAction(() => createAdminSaasComplianceRequest({ tenantId, regulation: "DPDP", requestType: "DATA_EXPORT", notes: "Mobile admin request" }), "Compliance request created.")} disabled={!tenantId} />
            </View>
            {invoices.slice(0, 4).map((invoice: any) => (
              <Surface key={`saas-invoice-${invoice.id}`} tone="owner">
                <Text style={styles.cardTitle}>{invoice.invoiceNumber}</Text>
                <Text style={styles.cardMeta}>{invoice.status} | Rs {invoice.amount ?? 0} | Paid Rs {invoice.paidAmount ?? 0}</Text>
                <Text style={styles.helperText}>Tenant {invoice.tenantId} | due {invoice.dueDate}</Text>
              </Surface>
            ))}
            {tenants.slice(0, 4).map((tenant: any) => (
              <Surface key={`saas-tenant-${tenant.id}`} tone="owner">
                <Text style={styles.cardTitle}>{tenant.name}</Text>
                <Text style={styles.cardMeta}>{tenant.tenantCode} | {tenant.countryCode} | {tenant.currency}</Text>
                <Text style={styles.helperText}>{tenant.status} | {tenant.lifecycleStage} | {tenant.domain}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (adminTab === "campaigns") {
      return (
        <View style={styles.stack}>
          <Section title="CRM Growth Command Center" subtitle="Customer lifetime value, loyalty liability, memberships, referrals, and automation readiness.">
            <View style={styles.metricGrid}>
              <Metric label="CLV" value={`Rs ${adminGrowth?.scorecard?.customerLifetimeValue ?? 0}`} tone="owner" />
              <Metric label="Repeat" value={`${adminGrowth?.retention?.repeatBookingRate ?? 0}%`} tone="owner" />
              <Metric label="VIP" value={adminGrowth?.scorecard?.vipCustomers ?? 0} tone="owner" />
              <Metric label="Wallet Liability" value={`Rs ${adminGrowth?.wallet?.liability ?? 0}`} tone="owner" />
            </View>
            {adminGrowthAutomation.length > 0 ? (
              <View style={{ gap: 10, marginTop: 12 }}>
                {adminGrowthAutomation.slice(0, 3).map((automation) => (
                  <Surface key={automation.key} tone="owner">
                    <Text style={styles.cardTitle}>{automation.trigger}</Text>
                    <Text style={styles.cardMeta}>Audience: {automation.audience} | Status: {automation.status}</Text>
                    <Text style={styles.helperText}>{automation.action}</Text>
                  </Surface>
                ))}
              </View>
            ) : null}
          </Section>
          <Section title="Create Notification Campaign" subtitle="Broadcast push, email, or SMS messages to Cricbook users.">
            <Surface tone="owner">
              <LabeledInput label="Campaign Name" value={campaignForm.name} onChangeText={(val) => setCampaignForm(prev => ({ ...prev, name: val }))} placeholder="e.g. Weekend Discount Offer" />
              <LabeledInput label="Notification Title" value={campaignForm.title} onChangeText={(val) => setCampaignForm(prev => ({ ...prev, title: val }))} placeholder="e.g. Special Offer!" />
              <LabeledInput label="Message Content" value={campaignForm.message} onChangeText={(val) => setCampaignForm(prev => ({ ...prev, message: val }))} placeholder="e.g. Book a turf this weekend and get 20% off!" multiline />
              <FieldPair
                left={
                  <SelectField
                    label="Channel"
                    value={campaignForm.channel}
                    onPress={() => openPicker("Choose Channel", [
                      { label: "Email", value: "EMAIL" },
                      { label: "Push Notification", value: "PUSH" },
                      { label: "SMS", value: "SMS" },
                      { label: "WhatsApp", value: "WHATSAPP" }
                    ], (val) => setCampaignForm(prev => ({ ...prev, channel: val as any })))}
                  />
                }
                right={
                  <SelectField
                    label="Target Role"
                    value={campaignForm.targetRole ? formatEnumLabel(campaignForm.targetRole) : "All Roles"}
                    onPress={() => openPicker("Target Role", [
                      { label: "All Roles", value: "" },
                      { label: "Player", value: "PLAYER" },
                      { label: "Owner", value: "OWNER" },
                      { label: "Admin", value: "ADMIN" }
                    ], (val) => setCampaignForm(prev => ({ ...prev, targetRole: val })))}
                  />
                }
              />
              <AppButton label="Create Campaign" tone="owner" onPress={handleCreateCampaign} />
            </Surface>
          </Section>

          <Section title="Broadcast Queue" subtitle="List of marketing and service push notifications.">
            {adminCampaigns.length === 0 ? (
              <EmptyState text="No campaigns created yet." />
            ) : (
              adminCampaigns.map((campaign) => (
                <Surface key={campaign.id} tone="owner">
                  <Text style={styles.cardTitle}>{campaign.name}</Text>
                  <Text style={styles.cardMeta}>Channel: {campaign.channel} | Status: {campaign.status}</Text>
                  <Text style={styles.cardMeta}>Recipients Broadcast: {campaign.sentCount ?? 0} sent</Text>
                  <Text style={[styles.helperText, { marginTop: 4 }]}>{campaign.title}: {campaign.message}</Text>
                  {campaign.status !== "SENT" && (
                    <View style={{ marginTop: 8 }}>
                      <AppButton label="Send Broadcast Now" tone="owner" onPress={() => handleSendCampaign(campaign.id, campaign.name)} />
                    </View>
                  )}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "bookings") {
      return (
        <View style={styles.stack}>
          <Section title="Bookings & Refunds Override" subtitle="List bookings and override processing manually.">
            {adminBookings.length === 0 ? (
              <EmptyState text="No active bookings registered." />
            ) : (
              adminBookings.map((booking) => (
                <Surface key={booking.id} tone="owner">
                  <Text style={styles.cardTitle}>Booking ID: #B-{booking.id}</Text>
                  <Text style={styles.cardMeta}>Customer: {booking.customerName} ({booking.customerEmail})</Text>
                  <Text style={styles.cardMeta}>Turf: {booking.turfName}</Text>
                  <Text style={styles.cardMeta}>Amount: Rs {booking.amount} | Date: {booking.bookingDate}</Text>
                  <Text style={styles.cardMeta}>Status: {booking.status}</Text>
                  {booking.status !== "CANCELLED" && (
                    <View style={[styles.actionRow, { marginTop: 10 }]}>
                      <AppButton label="Force Cancel" tone="owner" onPress={() => handleOverrideBooking(booking.id, "CANCELLED")} />
                      <AppButton label="Manual Payout Refund" tone="danger" onPress={() => handleRefundBooking(booking.id)} />
                    </View>
                  )}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "support") {
      return (
        <View style={styles.stack}>
          <Section title="Dynamic Help Desk" subtitle="Support coordinates and message polling.">
            {adminSupportTickets.length === 0 ? (
              <EmptyState text="No open support queries logged." />
            ) : (
              adminSupportTickets.map((ticket) => (
                <Surface key={ticket.id} tone="owner">
                  <Text style={styles.cardTitle}>Ticket #{ticket.id} - Status: {ticket.status}</Text>
                  <Text style={[styles.cardMeta, { color: theme.colors.text, marginTop: 4 }]}>Issue: {ticket.issue}</Text>
                  <Text style={[styles.helperText, { marginTop: 6, fontStyle: "italic" }]}>Chat Log:{"\n"}{ticket.chatLog || "Empty conversation."}</Text>

                  {ticket.status !== "RESOLVED" && (
                    <View style={{ marginTop: 12 }}>
                      <LabeledInput
                        label="Assist Response"
                        value={supportReplyText}
                        onChangeText={setSupportReplyText}
                        placeholder="Type reply and resolve ticket..."
                      />
                      <AppButton
                        label="Send reply & Close Ticket"
                        tone="owner"
                        onPress={() => handleSendSupportReply(ticket.id)}
                      />
                    </View>
                  )}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "incidents") {
      return (
        <View style={styles.stack}>
          <Section title="Dispatch Outage Alert" subtitle="Broadcast a platform outage status.">
            <Surface tone="owner">
              <LabeledInput
                label="System Degraded endpoint"
                value={mobileIncidentName}
                onChangeText={setMobileIncidentName}
                placeholder="e.g. Booking SMS gateway out"
              />
              <LabeledInput
                label="Incident message"
                value={mobileIncidentMessage}
                onChangeText={setMobileIncidentMessage}
                placeholder="e.g. Troubleshooting SMS dispatch latency."
                multiline
              />
              <SelectField
                label="Outage Severity"
                value={mobileIncidentSeverity}
                onPress={() => openPicker("Incident Severity", [
                  { label: "Low degradation", value: "LOW" },
                  { label: "Medium warning", value: "MEDIUM" },
                  { label: "High failure", value: "HIGH" },
                  { label: "Critical system outage", value: "CRITICAL" }
                ], setMobileIncidentSeverity)}
              />
              <AppButton label="Dispatch Warning" tone="owner" onPress={handleCreateIncidentOutage} />
            </Surface>
          </Section>

          <Section title="Outages Queue" subtitle="Track live incident outages.">
            {adminIncidents.length === 0 ? (
              <EmptyState text="All systems running optimally." />
            ) : (
              adminIncidents.map((incident) => (
                <Surface key={incident.id} tone="owner">
                  <Text style={styles.cardTitle}>{incident.name}</Text>
                  <Text style={styles.cardMeta}>Severity: {incident.severity} | Status: {incident.status}</Text>
                  <Text style={[styles.helperText, { marginTop: 4 }]}>{incident.message}</Text>
                  {incident.status !== "RESOLVED" && (
                    <View style={{ marginTop: 8 }}>
                      <AppButton label="Resolve Incident" tone="owner" onPress={() => handleResolveIncidentOutage(incident.id)} />
                    </View>
                  )}
                </Surface>
              ))
            )}
          </Section>
        </View>
      );
    }

    if (adminTab === "resale") {
      return (
        <View style={styles.stack}>
          <Section title="Resale Marketplace Monitoring" subtitle="Track live transfer inventory, recovered revenue, and waitlist pressure.">
            <View style={styles.metricGrid}>
              <Metric label="Active" value={resaleAnalytics?.activeListings ?? 0} tone="owner" />
              <Metric label="Sold" value={resaleAnalytics?.soldListings ?? 0} tone="owner" />
              <Metric label="Expired" value={resaleAnalytics?.expiredListings ?? 0} tone="owner" />
              <Metric label="GMV" value={`Rs ${resaleAnalytics?.marketplaceGmv ?? 0}`} tone="owner" />
            </View>
            {(resaleAnalytics?.recentListings || []).map((item) => (
              <Surface key={item.id} tone="owner">
                <Text style={styles.cardTitle}>{item.turfName}</Text>
                <Text style={styles.cardMeta}>{item.city} | {item.bookingDate} | Rs {item.price}</Text>
                <Text style={styles.cardMeta}>{item.status} | seller {item.sellerName} | trust {item.sellerTrustScore}</Text>
              </Surface>
            ))}
            {(resaleAnalytics?.recentListings || []).length === 0 ? <EmptyState text="No resale activity yet." /> : null}
          </Section>
        </View>
      );
    }

    if (adminTab === "teams") {
      return (
        <View style={styles.stack}>
          <Section title="Team Moderation" subtitle="Platform team health, finance movement, polls and attendance signals.">
            <View style={styles.metricGrid}>
              <Metric label="Teams" value={teamAnalytics?.totalTeams ?? 0} tone="owner" />
              <Metric label="Members" value={teamAnalytics?.activeMembers ?? 0} tone="owner" />
              <Metric label="Polls" value={teamAnalytics?.polls ?? 0} tone="owner" />
              <Metric label="Balance" value={`Rs ${teamAnalytics?.teamWalletBalance ?? 0}`} tone="owner" />
            </View>
            {(teamAnalytics?.mostActiveTeams || []).map((team) => (
              <Surface key={team.teamId} tone="owner">
                <Text style={styles.cardTitle}>{team.teamName}</Text>
                <Text style={styles.cardMeta}>{team.city} | {team.memberCount} players | attendance {team.attendanceRate}%</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    if (adminTab === "matchmaking") {
      return (
        <View style={styles.stack}>
          <Section title="Matchmaking Analytics" subtitle="Monitor open-match fill success and network activity.">
            <View style={styles.metricGrid}>
              <Metric label="Open" value={matchmakingAnalytics?.openMatches ?? 0} tone="owner" />
              <Metric label="Full" value={matchmakingAnalytics?.fullMatches ?? 0} tone="owner" />
              <Metric label="Players" value={matchmakingAnalytics?.totalParticipants ?? 0} tone="owner" />
              <Metric label="Fill Rate" value={`${matchmakingAnalytics?.averageFillRate ?? 0}%`} tone="owner" />
            </View>
            {(matchmakingAnalytics?.mostFillableMatches || []).map((match) => (
              <Surface key={match.id} tone="owner">
                <Text style={styles.cardTitle}>{match.title}</Text>
                <Text style={styles.cardMeta}>{match.city} | {match.matchDate} | {match.joinedPlayers}/{match.maxPlayers} joined</Text>
                <Text style={styles.cardMeta}>{match.recommendationReason}</Text>
              </Surface>
            ))}
          </Section>
        </View>
      );
    }

    return null;
  }
}

function createAppStyles(theme: AppTheme) {
  return StyleSheet.create({
  shell: {
    flex: 1,
    backgroundColor: theme.colors.bg,
  },
  shellInset: {
    paddingTop: androidTopInset,
  },
  centered: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    gap: 12,
  },
  bootText: {
    color: theme.colors.muted,
  },
  authWrap: {
    flexGrow: 1,
    padding: 20,
    justifyContent: "center",
  },
  content: {
    flex: 1,
  },
  contentInner: {
    padding: 16,
    gap: 16,
    paddingBottom: 126,
  },
  header: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 12,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
  },
  brandBlock: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    flexShrink: 1,
  },
  brandBadge: {
    width: 42,
    height: 42,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
  },
  brandBadgePlayer: {
    backgroundColor: theme.colors.player,
  },
  brandBadgeOwner: {
    backgroundColor: theme.colors.owner,
  },
  brandBadgeAdmin: {
    backgroundColor: "#9061f9",
  },
  brandBadgeText: {
    color: "#ffffff",
    fontWeight: "900",
  },
  brand: {
    color: theme.colors.text,
    fontSize: 28,
    fontWeight: "900",
  },
  portalLabel: {
    color: theme.colors.muted,
    fontSize: 12,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginTop: 2,
  },
  headerActions: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    flexShrink: 0,
  },
  commandButton: {
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 10,
    minHeight: 44,
    justifyContent: "center",
    backgroundColor: theme.colors.panelSoft,
  },
  commandText: {
    color: theme.colors.text,
    fontWeight: "800",
  },
  logoutButton: {
    borderWidth: 1,
    borderColor: theme.colors.border,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 10,
    minHeight: 44,
    justifyContent: "center",
  },
  logoutText: {
    color: theme.colors.text,
    fontWeight: "700",
  },
  portalSwitch: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    flexDirection: "row",
    flexWrap: "wrap",
    alignItems: "center",
    gap: 8,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
    backgroundColor: theme.colors.bgElevated,
  },
  inlineAction: {
    color: theme.colors.muted,
    fontWeight: "700",
  },
  stack: {
    gap: 16,
  },
  previewRow: {
    flexDirection: "row",
    gap: 8,
  },
  helperText: {
    color: theme.colors.muted,
    lineHeight: 20,
  },
  metricGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
  },
  cardTitle: {
    color: theme.colors.text,
    fontSize: 16,
    fontWeight: "800",
  },
  cardMeta: {
    color: theme.colors.muted,
    lineHeight: 20,
  },
  actionRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10,
  },
  listRow: {
    borderTopWidth: 1,
    borderTopColor: theme.colors.border,
    paddingTop: 10,
    gap: 4,
  },
  listTitle: {
    color: theme.colors.text,
    fontWeight: "700",
  },
  chipWrap: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  slotCard: {
    width: 138,
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.panelSoft,
    borderRadius: 16,
    padding: 12,
    gap: 6,
  },
  slotCardActive: {
    borderColor: theme.colors.player,
    backgroundColor: theme.colors.playerSoft,
  },
  slotCardOwnerActive: {
    borderColor: theme.colors.owner,
    backgroundColor: theme.colors.ownerSoft,
  },
  slotCardBlocked: {
    borderColor: theme.colors.danger,
  },
  slotTitle: {
    color: theme.colors.text,
    fontWeight: "800",
  },
  slotMeta: {
    color: theme.colors.muted,
    fontSize: 12,
  },
  tabScroller: {
    gap: 10,
  },
  loader: {
    position: "absolute",
    right: 18,
    bottom: 18,
  },
  authSubtitle: {
    color: theme.colors.muted,
    lineHeight: 22,
    marginBottom: 4,
  },
  sheetContent: {
    paddingHorizontal: 18,
    paddingVertical: 8,
  },
  sheetLine: {
    color: theme.colors.text,
    lineHeight: 22,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
  },
  globalContextBar: {
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    gap: 12,
  },
  contextButton: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: theme.colors.panelSoft,
    borderWidth: 1,
    borderColor: theme.colors.border,
    borderRadius: 12,
    paddingVertical: 8,
  },
  contextButtonText: {
    color: theme.colors.text,
    fontWeight: "bold",
    fontSize: 13,
  },
  });
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Something went wrong";
}

function MobileWorkspaceHero({
  portal,
  palette,
  t,
}: {
  portal: MobilePortalKind;
  palette: AppTheme;
  t: (key: string, defaultValue: string) => string;
}) {
  const isOwner = portal === "owner";
  const isAdmin = portal === "admin";
  const isStaff = portal === "staff";
  const accent = isAdmin ? "#9061f9" : isStaff ? "#06b6d4" : isOwner ? palette.colors.owner : palette.colors.player;
  const title = isAdmin ? t("operate_cricbook", "Operate Cricbook") : isStaff ? t("execute_ground_ops", "Execute ground operations") : isOwner ? t("run_turf_business", "Run your turf business") : t("play_more_cricket", "Play more cricket");
  const subtitle = isAdmin
    ? t("admin_workspace_subtitle", "Moderation, campaigns, support tickets, and platform incidents.")
    : isStaff
      ? t("staff_workspace_subtitle", "Check-ins, shifts, tasks, maintenance, equipment, and emergency alerts.")
    : isOwner
      ? t("owner_workspace_subtitle", "Slots, bookings, earnings, reviews, and turf settings in one mobile workspace.")
      : t("player_workspace_subtitle", "Teams, matches, bookings, wallet, and personalized turf discovery.");
  const statA = isAdmin ? "Live ops" : isStaff ? "QR check-ins" : isOwner ? "Slot control" : "Smart picks";
  const statB = isAdmin ? "Support" : isStaff ? "Shift ops" : isOwner ? "Revenue" : "Bookings";

  return (
    <View
      style={[
        mobileHeroStyles.card,
        {
          backgroundColor: palette.colors.panel,
          borderColor: palette.colors.border,
          shadowColor: accent,
        },
      ]}
    >
      <View style={[mobileHeroStyles.accent, { backgroundColor: accent }]} />
      <Text style={[mobileHeroStyles.eyebrow, { color: accent }]}>{t("mobile_workspace", "Mobile workspace")}</Text>
      <Text style={[mobileHeroStyles.title, { color: palette.colors.text }]}>{title}</Text>
      <Text style={[mobileHeroStyles.subtitle, { color: palette.colors.muted }]}>{subtitle}</Text>
      <View style={mobileHeroStyles.statRow}>
        {[statA, statB].map((item) => (
          <View key={item} style={[mobileHeroStyles.statPill, { backgroundColor: palette.colors.panelSoft, borderColor: palette.colors.border }]}>
            <Text style={[mobileHeroStyles.statText, { color: palette.colors.text }]}>{item}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

function ThemeModeControl({
  mode,
  onChange,
  palette,
  labels,
  compact = false,
}: {
  mode: ThemeMode;
  onChange: (mode: ThemeMode) => void;
  palette: AppTheme;
  labels?: { light: string; dark: string; auto: string };
  compact?: boolean;
}) {
  const modes: Array<{ value: ThemeMode; label: string }> = [
    { value: "light", label: labels?.light || "Light" },
    { value: "dark", label: labels?.dark || "Dark" },
    { value: "system", label: labels?.auto || "Auto" },
  ];

  return (
    <View style={[
      mobileThemeControlStyles.wrap,
      compact && mobileThemeControlStyles.compactWrap,
      { backgroundColor: palette.colors.panelSoft, borderColor: palette.colors.border },
    ]}>
      {modes.map((item) => {
        const active = item.value === mode;
        return (
          <Pressable
            key={item.value}
            accessibilityRole="tab"
            accessibilityLabel={item.label}
            accessibilityState={{ selected: active }}
            accessibilityHint={`Switch to ${item.label} theme`}
            hitSlop={{ top: 8, right: 8, bottom: 8, left: 8 }}
            onPress={() => onChange(item.value)}
            style={[
              mobileThemeControlStyles.item,
              compact && mobileThemeControlStyles.compactItem,
              active && { backgroundColor: palette.colors.owner },
            ]}
          >
            <Text style={[
              mobileThemeControlStyles.itemText,
              { color: active ? "#ffffff" : palette.colors.muted },
            ]}>
              {item.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

function createMobileStyles(palette: AppTheme) {
  return StyleSheet.create({
    shell: {
      flex: 1,
      backgroundColor: palette.colors.bg,
    },
    header: {
      backgroundColor: palette.colors.bgElevated,
      borderBottomColor: palette.colors.border,
    },
  });
}

const mobileHeroStyles = StyleSheet.create({
  card: {
    marginHorizontal: 16,
    marginBottom: 2,
    borderWidth: 1,
    borderRadius: 24,
    padding: 18,
    gap: 8,
    shadowOpacity: 0.12,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 10 },
    elevation: 2,
    overflow: "hidden",
  },
  accent: {
    position: "absolute",
    right: -26,
    top: -36,
    width: 120,
    height: 120,
    borderRadius: 999,
    opacity: 0.14,
  },
  eyebrow: {
    textTransform: "uppercase",
    letterSpacing: 2,
    fontSize: 11,
    fontWeight: "900",
  },
  title: {
    fontSize: 24,
    fontWeight: "900",
  },
  subtitle: {
    lineHeight: 21,
    maxWidth: 360,
  },
  statRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 4,
  },
  statPill: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  statText: {
    fontSize: 12,
    fontWeight: "800",
  },
});

const mobileThemeControlStyles = StyleSheet.create({
  wrap: {
    flexDirection: "row",
    alignSelf: "stretch",
    borderWidth: 1,
    borderRadius: 16,
    padding: 4,
    gap: 4,
    marginBottom: 14,
  },
  compactWrap: {
    alignSelf: "auto",
    marginBottom: 0,
  },
  item: {
    flex: 1,
    borderRadius: 12,
    paddingVertical: 10,
    paddingHorizontal: 12,
    alignItems: "center",
  },
  compactItem: {
    flex: 0,
    paddingVertical: 8,
    paddingHorizontal: 10,
  },
  itemText: {
    fontSize: 12,
    fontWeight: "800",
  },
});

import { Provider as ReduxProvider, store as reduxStore } from "../store";

export default function CricbookMobileAppWrapped() {
  return (
    <ReduxProvider store={reduxStore}>
      <CricbookMobileApp />
    </ReduxProvider>
  );
}
