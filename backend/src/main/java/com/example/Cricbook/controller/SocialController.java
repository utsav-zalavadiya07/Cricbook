package com.example.Cricbook.controller;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.enums.TeamVisibility;
import com.example.Cricbook.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @PostMapping("/teams")
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateTeamRequest request) {
        return ResponseEntity.ok(socialService.createTeam(userDetails.getUsername(), request));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamResponse>> getMyTeams(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(socialService.getMyTeams(userDetails.getUsername()));
    }

    @GetMapping("/teams/discover")
    public ResponseEntity<List<TeamResponse>> discoverTeams(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) TeamVisibility visibility,
            @RequestParam(required = false, defaultValue = "false") boolean openOnly) {
        return ResponseEntity.ok(socialService.discoverTeams(userDetails.getUsername(), query, city, visibility, openOnly));
    }

    @GetMapping("/teams/invites")
    public ResponseEntity<List<TeamInviteResponse>> getMyInvites(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(socialService.getMyInvites(userDetails.getUsername()));
    }

    @GetMapping("/invites")
    public ResponseEntity<List<TeamInviteResponse>> getMyInvitesCompat(@AuthenticationPrincipal UserDetails userDetails) {
        return getMyInvites(userDetails);
    }

    @GetMapping("/teams/join-requests")
    public ResponseEntity<List<TeamJoinRequestResponse>> getTeamJoinRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(socialService.getJoinRequestsForManagedTeams(userDetails.getUsername()));
    }

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId) {
        return ResponseEntity.ok(socialService.getTeam(userDetails.getUsername(), teamId));
    }

    @PostMapping("/teams/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> inviteTeamMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody InviteTeamMemberRequest request) {
        return ResponseEntity.ok(socialService.inviteTeamMember(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/teams/{teamId}/join-requests")
    public ResponseEntity<TeamJoinRequestResponse> requestToJoinTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody(required = false) RequestJoinTeamRequest request) {
        return ResponseEntity.ok(socialService.requestToJoinTeam(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/teams/join-requests/{requestId}/respond")
    public ResponseEntity<TeamJoinRequestResponse> respondJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody RespondInviteRequest request) {
        return ResponseEntity.ok(socialService.respondToJoinRequest(userDetails.getUsername(), requestId, request));
    }

    @PostMapping("/teams/invites/{inviteId}/respond")
    public ResponseEntity<TeamInviteResponse> respondInvite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inviteId,
            @RequestBody RespondInviteRequest request) {
        return ResponseEntity.ok(socialService.respondToInvite(userDetails.getUsername(), inviteId, request));
    }

    @PostMapping("/teams/{teamId}/leave")
    public ResponseEntity<TeamResponse> leaveTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId) {
        return ResponseEntity.ok(socialService.leaveTeam(userDetails.getUsername(), teamId));
    }

    @PostMapping("/teams/{teamId}/transfer-captain")
    public ResponseEntity<TeamResponse> transferCaptain(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody TeamActionRequest request) {
        return ResponseEntity.ok(socialService.transferCaptain(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/teams/{teamId}/roles")
    public ResponseEntity<TeamResponse> updateMemberRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody TeamActionRequest request) {
        return ResponseEntity.ok(socialService.updateMemberRole(userDetails.getUsername(), teamId, request));
    }

    @DeleteMapping("/teams/{teamId}/members/{userId}")
    public ResponseEntity<TeamResponse> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(socialService.removeMember(userDetails.getUsername(), teamId, userId));
    }

    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId) {
        socialService.deleteTeam(userDetails.getUsername(), teamId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teams/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId) {
        return ResponseEntity.ok(socialService.getTeamMembers(userDetails.getUsername(), teamId));
    }

    @PostMapping("/matches")
    public ResponseEntity<PublicMatchResponse> createPublicMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreatePublicMatchRequest request) {
        return ResponseEntity.ok(socialService.createPublicMatch(userDetails.getUsername(), request));
    }

    @GetMapping("/matches")
    public ResponseEntity<List<PublicMatchResponse>> getOpenMatches() {
        return ResponseEntity.ok(socialService.getOpenMatches());
    }

    @GetMapping("/matches/history")
    public ResponseEntity<List<PublicMatchResponse>> getMyMatchHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(socialService.getMyMatchHistory(userDetails.getUsername()));
    }

    @PostMapping("/matches/{matchId}/join")
    public ResponseEntity<PublicMatchResponse> joinMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.joinMatch(userDetails.getUsername(), matchId));
    }

    @PostMapping("/matches/{matchId}/accept")
    public ResponseEntity<PublicMatchResponse> acceptMatchChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.acceptMatchChallenge(userDetails.getUsername(), matchId));
    }

    @PostMapping("/matches/{matchId}/reject")
    public ResponseEntity<PublicMatchResponse> rejectMatchChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.rejectMatchChallenge(userDetails.getUsername(), matchId));
    }

    @PostMapping("/matches/{matchId}/schedule")
    public ResponseEntity<PublicMatchResponse> scheduleMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId,
            @RequestBody MatchScheduleRequest request) {
        return ResponseEntity.ok(socialService.scheduleMatch(userDetails.getUsername(), matchId, request));
    }

    @PostMapping("/matches/{matchId}/start")
    public ResponseEntity<PublicMatchResponse> startPublicMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.startPublicMatch(userDetails.getUsername(), matchId));
    }

    @PostMapping("/matches/{matchId}/cancel")
    public ResponseEntity<PublicMatchResponse> cancelPublicMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.cancelPublicMatch(userDetails.getUsername(), matchId));
    }

    @PostMapping("/matches/{matchId}/result")
    public ResponseEntity<PublicMatchResponse> submitMatchResult(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId,
            @RequestBody MatchResultRequest request) {
        return ResponseEntity.ok(socialService.submitMatchResult(userDetails.getUsername(), matchId, request));
    }

    @GetMapping("/teams/{teamId}/match-stats")
    public ResponseEntity<TeamMatchStatsResponse> teamMatchStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId) {
        return ResponseEntity.ok(socialService.getTeamMatchStats(userDetails.getUsername(), teamId));
    }

    @GetMapping("/players/discover")
    public ResponseEntity<List<PlayerDiscoverResponse>> discoverPlayers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String availability) {
        return ResponseEntity.ok(socialService.discoverPlayers(userDetails.getUsername(), city, position, availability));
    }

    @GetMapping("/players/nearby")
    public ResponseEntity<List<PlayerDiscoverResponse>> nearbyPlayers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String availability) {
        return ResponseEntity.ok(socialService.nearbyPlayers(userDetails.getUsername(), city, availability));
    }

    @GetMapping("/network")
    public ResponseEntity<List<CommunityDtos.PlayerConnectionResponse>> myNetwork(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(socialService.myNetwork(userDetails.getUsername()));
    }

    @PostMapping("/network/connect")
    public ResponseEntity<CommunityDtos.PlayerConnectionResponse> connectPlayer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommunityDtos.ConnectionRequest request) {
        return ResponseEntity.ok(socialService.connectPlayer(userDetails.getUsername(), request));
    }

    @PostMapping("/network/{connectionId}/respond")
    public ResponseEntity<CommunityDtos.PlayerConnectionResponse> respondConnection(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long connectionId,
            @RequestBody CommunityDtos.ConnectionDecisionRequest request) {
        return ResponseEntity.ok(socialService.respondConnection(userDetails.getUsername(), connectionId, request));
    }

    @PostMapping("/recruitment")
    public ResponseEntity<CommunityDtos.RecruitmentResponse> createRecruitment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommunityDtos.RecruitmentRequest request) {
        return ResponseEntity.ok(socialService.createRecruitment(userDetails.getUsername(), request));
    }

    @GetMapping("/recruitment/open")
    public ResponseEntity<List<CommunityDtos.RecruitmentResponse>> openRecruitments(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(socialService.openRecruitments(city, role));
    }

    @PostMapping("/recruitment/{recruitmentId}/apply")
    public ResponseEntity<CommunityDtos.RecruitmentApplicationResponse> applyRecruitment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long recruitmentId,
            @RequestBody CommunityDtos.RecruitmentApplyRequest request) {
        return ResponseEntity.ok(socialService.applyRecruitment(userDetails.getUsername(), recruitmentId, request));
    }

    @PostMapping("/recruitment/applications/{applicationId}/respond")
    public ResponseEntity<CommunityDtos.RecruitmentApplicationResponse> respondRecruitmentApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long applicationId,
            @RequestBody CommunityDtos.ConnectionDecisionRequest request) {
        return ResponseEntity.ok(socialService.respondRecruitmentApplication(userDetails.getUsername(), applicationId, request));
    }

    @GetMapping("/analytics")
    public ResponseEntity<CommunityDtos.CommunityAnalyticsResponse> communityAnalytics() {
        return ResponseEntity.ok(socialService.communityAnalytics());
    }

    @GetMapping("/admin/moderation")
    public ResponseEntity<CommunityDtos.CommunityAnalyticsResponse> adminCommunityModeration() {
        return ResponseEntity.ok(socialService.communityAnalytics());
    }

    @GetMapping("/feed")
    public ResponseEntity<List<FeedEventResponse>> getFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        return ResponseEntity.ok(socialService.getFeed(userDetails.getUsername(), scope, query, sort, entityType, entityId));
    }

    @PostMapping("/feed")
    public ResponseEntity<FeedEventResponse> createFeedPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommunityDtos.FeedPostRequest request) {
        return ResponseEntity.ok(socialService.createFeedPost(userDetails.getUsername(), request));
    }

    @PatchMapping("/feed/{postId}")
    public ResponseEntity<FeedEventResponse> editFeedPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            @RequestBody CommunityDtos.FeedPostRequest request) {
        return ResponseEntity.ok(socialService.editFeedPost(userDetails.getUsername(), postId, request));
    }

    @DeleteMapping("/feed/{postId}")
    public ResponseEntity<Void> deleteFeedPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {
        socialService.deleteFeedPost(userDetails.getUsername(), postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/feed/{postId}/reactions")
    public ResponseEntity<FeedEventResponse> reactToFeedPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            @RequestBody(required = false) CommunityDtos.FeedReactionRequest request) {
        return ResponseEntity.ok(socialService.reactToFeedPost(userDetails.getUsername(), postId, request));
    }

    @DeleteMapping("/feed/{postId}/reactions")
    public ResponseEntity<FeedEventResponse> removeFeedReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {
        return ResponseEntity.ok(socialService.removeFeedReaction(userDetails.getUsername(), postId));
    }

    @GetMapping("/feed/{postId}/comments")
    public ResponseEntity<List<CommunityDtos.FeedCommentResponse>> getFeedComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {
        return ResponseEntity.ok(socialService.getFeedComments(userDetails.getUsername(), postId));
    }

    @PostMapping("/feed/{postId}/comments")
    public ResponseEntity<CommunityDtos.FeedCommentResponse> addFeedComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            @RequestBody CommunityDtos.FeedCommentRequest request) {
        return ResponseEntity.ok(socialService.addFeedComment(userDetails.getUsername(), postId, request));
    }

    @PatchMapping("/feed/comments/{commentId}")
    public ResponseEntity<CommunityDtos.FeedCommentResponse> editFeedComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @RequestBody CommunityDtos.FeedCommentRequest request) {
        return ResponseEntity.ok(socialService.editFeedComment(userDetails.getUsername(), commentId, request));
    }

    @DeleteMapping("/feed/comments/{commentId}")
    public ResponseEntity<Void> deleteFeedComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {
        socialService.deleteFeedComment(userDetails.getUsername(), commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reports")
    public ResponseEntity<Void> reportContent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommunityDtos.ContentReportRequest request) {
        socialService.reportContent(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/moderation/actions")
    public ResponseEntity<Void> moderateContent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommunityDtos.ContentReportRequest request) {
        socialService.moderateContent(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/matches/{matchId}/participants")
    public ResponseEntity<List<TeamMemberResponse>> getMatchParticipants(@PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.getMatchParticipants(matchId));
    }

    @GetMapping("/chat/team/{teamId}")
    public ResponseEntity<List<ChatMessageResponse>> getTeamMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId) {
        return ResponseEntity.ok(socialService.getTeamMessages(userDetails.getUsername(), teamId));
    }

    @PostMapping("/chat/team/{teamId}")
    public ResponseEntity<ChatMessageResponse> sendTeamMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(socialService.sendTeamMessage(userDetails.getUsername(), teamId, request));
    }

    @GetMapping("/chat/match/{matchId}")
    public ResponseEntity<List<ChatMessageResponse>> getMatchMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.getMatchMessages(userDetails.getUsername(), matchId));
    }

    @PostMapping("/chat/match/{matchId}")
    public ResponseEntity<ChatMessageResponse> sendMatchMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId,
            @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(socialService.sendMatchMessage(userDetails.getUsername(), matchId, request));
    }

    @GetMapping("/chat/direct/{recipientId}")
    public ResponseEntity<List<ChatMessageResponse>> getDirectMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(socialService.getDirectMessages(userDetails.getUsername(), recipientId));
    }

    @PostMapping("/chat/direct/{recipientId}")
    public ResponseEntity<ChatMessageResponse> sendDirectMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long recipientId,
            @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(socialService.sendDirectMessage(userDetails.getUsername(), recipientId, request));
    }
}
