package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveScoringService {

    private final UserRepository userRepository;
    private final TournamentFixtureRepository fixtureRepository;
    private final PublicMatchRepository publicMatchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final LiveMatchScoreRepository liveMatchScoreRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final InningsRepository inningsRepository;
    private final PlayerMatchStatsRepository playerMatchStatsRepository;
    private final MatchAnalyticsRepository matchAnalyticsRepository;
    private final NotificationService notificationService;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TournamentService tournamentService;
    private final AuditLogRepository auditLogRepository;
    private final Map<Long, List<SseEmitter>> scoreStreams = new ConcurrentHashMap<>();

    @Transactional
    public LiveScoreResponse createLiveScore(String username, CreateLiveScoreRequest request) {
        if (request == null) {
            throw new ApiException("Score details are required");
        }
        User scorer = findUser(username);
        TournamentFixture fixture = request.getFixtureId() == null ? null : fixtureRepository.findById(request.getFixtureId())
                .orElseThrow(() -> new ApiException("Fixture not found"));
        PublicMatch publicMatch = request.getPublicMatchId() == null ? null : publicMatchRepository.findById(request.getPublicMatchId())
                .orElseThrow(() -> new ApiException("Match not found"));

        if (fixture != null && !fixture.getTournament().getOrganizer().getId().equals(scorer.getId())) {
            throw new ApiException("Only tournament organizer can score this fixture");
        }
        if (fixture != null && liveMatchScoreRepository.findByFixtureId(fixture.getId()).isPresent()) {
            throw new ApiException("Live score already exists for this fixture");
        }
        if (publicMatch != null) {
            if (!matchParticipantRepository.existsByMatchIdAndUserId(publicMatch.getId(), scorer.getId())) {
                throw new ApiException("Only match participants can score this match");
            }
            if (liveMatchScoreRepository.findByPublicMatchId(publicMatch.getId()).isPresent()) {
                throw new ApiException("Live score already exists for this match");
            }
        }

        String teamAName = fixture == null ? required(request.getTeamAName(), "Team A name is required") : fixture.getTeamA().getName();
        String teamBName = fixture == null ? required(request.getTeamBName(), "Team B name is required") : fixture.getTeamB().getName();
        String battingTeam = defaultValue(request.getBattingTeam(), teamAName);
        if (!battingTeam.equals(teamAName) && !battingTeam.equals(teamBName)) {
            throw new ApiException("Batting team must match Team A or Team B");
        }
        String bowlingTeam = battingTeam.equals(teamAName) ? teamBName : teamAName;

        LiveMatchScore score = liveMatchScoreRepository.save(LiveMatchScore.builder()
                .fixture(fixture)
                .publicMatch(publicMatch)
                .scorer(scorer)
                .title(defaultValue(request.getTitle(), teamAName + " vs " + teamBName))
                .teamAName(teamAName)
                .teamBName(teamBName)
                .battingTeam(battingTeam)
                .bowlingTeam(bowlingTeam)
                .innings(1)
                .runs(0)
                .wickets(0)
                .ballsBowled(0)
                .extras(0)
                .maxOvers(resolveLimit(request.getMaxOvers(), 20, "Max overs must be positive"))
                .status(LiveMatchStatus.NOT_STARTED) // EPIC 15 matches start at SCHEDULED/NOT_STARTED
                .locked(false)
                .isTie(false)
                .isSuperOver(false)
                .build());
        writeAudit(username, "LIVE_SCORE_CREATED", "LiveScore #" + score.getId() + " " + score.getTitle());
        broadcastScoreUpdate(score.getId(), "created");
        return mapScore(score);
    }

    @Transactional
    public LiveScoreResponse startMatch(String username, Long scoreId) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can start this score");
        }
        if (score.getStatus() != LiveMatchStatus.NOT_STARTED) {
            throw new ApiException("Match has already been started or processed");
        }
        score.setStatus(LiveMatchStatus.LIVE);
        LiveMatchScore saved = liveMatchScoreRepository.save(score);

        // Notify participants
        notifyParticipants(saved, "Match Started!", "The match " + saved.getTitle() + " has officially started! Tune in to watch live updates.");
        writeAudit(username, "LIVE_SCORE_STARTED", "LiveScore #" + scoreId);
        broadcastScoreUpdate(scoreId, "started");
        return mapScore(saved);
    }

    @Transactional(readOnly = true)
    public List<LiveScoreResponse> getLiveScores() {
        return liveMatchScoreRepository.findByStatusInOrderByUpdatedAtDesc(List.of(
                        LiveMatchStatus.LIVE,
                        LiveMatchStatus.INNINGS_BREAK,
                        LiveMatchStatus.PAUSED,
                        LiveMatchStatus.NOT_STARTED
                ))
                .stream()
                .map(this::mapScore)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LiveScoreResponse> getScoreArchive() {
        return liveMatchScoreRepository.findByStatusInOrderByUpdatedAtDesc(List.of(
                        LiveMatchStatus.COMPLETED,
                        LiveMatchStatus.CANCELLED
                ))
                .stream()
                .map(this::mapScore)
                .toList();
    }

    @Transactional(readOnly = true)
    public LiveScoreDetailResponse getScore(Long scoreId) {
        LiveMatchScore score = findScore(scoreId);
        List<ScoreEvent> events = scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(scoreId);
        List<InningsSegment> inningsSegments = inningsSegments(score, events);
        InningsSegment activeSegment = inningsSegments.isEmpty() ? new InningsSegment(1, score.getBattingTeam()) : inningsSegments.get(inningsSegments.size() - 1);

        LiveScoreDetailResponse.LiveScoreDetailResponseBuilder builder = LiveScoreDetailResponse.builder()
                .score(mapScore(score))
                .events(events.stream().map(this::mapEvent).toList())
                .innings(inningsSegments.stream().map(this::mapInningsSegment).toList())
                .battingScorecard(battingScorecard(activeSegment.events))
                .bowlingScorecard(bowlingScorecard(activeSegment.events))
                .recentOvers(recentOvers(events))
                .fallOfWickets(fallOfWickets(events))
                .resultSummary(resultSummary(score))
                .winnerTeamName(winnerTeamName(score))
                .verificationStatus(verificationStatus(score))
                .streamUrl("/api/v1/live-scores/" + score.getId() + "/stream")
                .shareUrl(shareUrl(score))
                .auditTrail(auditTrail(score));

        // Dynamic Scoreboard Calculation (Module 3)
        calculateScoreboardComponents(score, events, builder);

        return builder.build();
    }

    @Transactional(readOnly = true)
    public SseEmitter streamScore(Long scoreId) {
        findScore(scoreId);
        SseEmitter emitter = new SseEmitter(0L);
        scoreStreams.computeIfAbsent(scoreId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(scoreId, emitter));
        emitter.onTimeout(() -> removeEmitter(scoreId, emitter));
        emitter.onError(error -> removeEmitter(scoreId, emitter));
        sendScoreEvent(scoreId, emitter, "snapshot");
        return emitter;
    }

    @Transactional
    public LiveScoreDetailResponse recordBall(String username, Long scoreId, ScoreEventRequest request) {
        if (request == null) {
            throw new ApiException("Score event is required");
        }
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can update this score");
        }
        if (score.getStatus() != LiveMatchStatus.LIVE) {
            throw new ApiException("Match is not live");
        }
        if (Boolean.TRUE.equals(score.getLocked())) {
            throw new ApiException("Match is locked by admin");
        }

        Integer runs = request.getRuns() == null ? 0 : request.getRuns();
        Integer extraRuns = request.getExtraRuns() == null ? 0 : request.getExtraRuns();
        if (runs < 0 || extraRuns < 0) {
            throw new ApiException("Runs cannot be negative");
        }
        validateScoreEvent(request, runs, extraRuns);
        boolean wicket = Boolean.TRUE.equals(request.getWicket());
        if (wicket && score.getWickets() >= 10) {
            throw new ApiException("All wickets are already down");
        }
        if (score.getBallsBowled() >= score.getMaxOvers() * 6) {
            throw new ApiException("Maximum overs completed");
        }

        boolean legalDelivery = request.getExtraType() == null
                || (!request.getExtraType().equalsIgnoreCase("WIDE") && !request.getExtraType().equalsIgnoreCase("NO_BALL"));

        // Milestone & Commentary tracking
        int previousBatterRuns = calculateBatterCumulative(scoreId, request.getBatter());
        int currentBatterRuns = previousBatterRuns + runs;

        // Auto-commentary description (Module 8)
        String overLabel = formatOvers(score.getBallsBowled() + (legalDelivery ? 1 : 0));
        String eventDesc = generateCommentaryText(request, legalDelivery);
        String customNote = request.getNote() == null || request.getNote().isBlank() ? "" : " " + request.getNote().trim();
        String finalCommentary = overLabel + " " + eventDesc + customNote;

        ScoreEvent event = scoreEventRepository.save(ScoreEvent.builder()
                .liveScore(score)
                .eventSequence((int) scoreEventRepository.countByLiveScoreId(scoreId) + 1)
                .runs(runs)
                .extraRuns(extraRuns)
                .extraType(request.getExtraType())
                .wicket(wicket)
                .wicketType(request.getWicketType())
                .batter(request.getBatter())
                .bowler(request.getBowler())
                .note(finalCommentary)
                .build());

        // Apply score additions
        score.setRuns(score.getRuns() + runs + extraRuns);
        score.setExtras(score.getExtras() + extraRuns);
        score.setWickets(score.getWickets() + (wicket ? 1 : 0));
        score.setBallsBowled(score.getBallsBowled() + (legalDelivery ? 1 : 0));

        // Milestone Alerts (Module 7)
        if (previousBatterRuns < 50 && currentBatterRuns >= 50) {
            notifyParticipants(score, "Batting Milestone: Fifty!", request.getBatter() + " has reached a brilliant half-century (50 runs) in the match " + score.getTitle() + "!");
        } else if (previousBatterRuns < 100 && currentBatterRuns >= 100) {
            notifyParticipants(score, "Batting Milestone: Century!", request.getBatter() + " has scored a spectacular century (100 runs) in " + score.getTitle() + "!");
        }

        if (wicket) {
            notifyParticipants(score, "Wicket Alert!", "WICKET! " + request.getBatter() + " is out (" + defaultValue(request.getWicketType(), "Bowled") + ") in " + score.getTitle());
        }

        // Innings transitions (Module 5)
        boolean inningsEnded = score.getWickets() >= 10 || score.getBallsBowled() >= score.getMaxOvers() * 6;
        if (score.getInnings() == 1) {
            if (inningsEnded) {
                // Switch Innings
                Innings inn = Innings.builder()
                        .liveScore(score)
                        .inningsNumber(1)
                        .battingTeamName(score.getBattingTeam())
                        .runs(score.getRuns())
                        .wickets(score.getWickets())
                        .ballsBowled(score.getBallsBowled())
                        .extras(score.getExtras())
                        .build();
                inningsRepository.save(inn);

                // Set details for Innings 2
                String previousBatting = score.getBattingTeam();
                score.setInnings(2);
                score.setTargetRuns(score.getRuns() + 1);
                score.setBattingTeam(score.getBowlingTeam());
                score.setBowlingTeam(previousBatting);
                score.setRuns(0);
                score.setWickets(0);
                score.setBallsBowled(0);
                score.setExtras(0);
                score.setStatus(LiveMatchStatus.INNINGS_BREAK);

                notifyParticipants(score, "Innings Break!", "Innings 1 finished. " + previousBatting + " set a target of " + score.getTargetRuns() + " runs.");
            }
        } else {
            // Innings 2 checks
            if (score.getRuns() >= score.getTargetRuns()) {
                score.setStatus(LiveMatchStatus.COMPLETED);
                notifyParticipants(score, "Match Finished!", score.getBattingTeam() + " won the match! Total runs: " + score.getRuns());
                persistCurrentInningsSnapshot(score);
                savePlayerStats(score);
            } else if (inningsEnded) {
                score.setStatus(LiveMatchStatus.COMPLETED);
                if (score.getRuns() == score.getTargetRuns() - 1) {
                    score.setTargetRuns(score.getRuns() + 1);
                    score.setIsTie(true);
                    notifyParticipants(score, "Match Finished!", "It's a TIE between both teams! Scores are level!");
                } else {
                    notifyParticipants(score, "Match Finished!", score.getBowlingTeam() + " won the match!");
                }
                persistCurrentInningsSnapshot(score);
                savePlayerStats(score);
            }
        }

        LiveMatchScore saved = liveMatchScoreRepository.save(score);
        if (saved.getStatus() == LiveMatchStatus.COMPLETED) {
            syncTournamentFixtureIfReady(username, saved);
        }
        writeAudit(username, "LIVE_SCORE_BALL_RECORDED", "LiveScore #" + scoreId + " sequence=" + event.getEventSequence());
        broadcastScoreUpdate(scoreId, "ball-recorded");
        return getScore(scoreId);
    }

    @Transactional
    public LiveScoreDetailResponse undoBall(String username, Long scoreId) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can undo scores");
        }
        if (Boolean.TRUE.equals(score.getLocked())) {
            throw new ApiException("Match is locked by admin");
        }

        List<ScoreEvent> events = scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(scoreId);
        if (events.isEmpty()) {
            throw new ApiException("No events to undo");
        }

        ScoreEvent lastEvent = events.get(events.size() - 1);
        scoreEventRepository.delete(lastEvent);

        recalculateScore(score);
        writeAudit(username, "LIVE_SCORE_BALL_UNDONE", "LiveScore #" + scoreId + " sequence=" + lastEvent.getEventSequence());
        broadcastScoreUpdate(scoreId, "ball-undone");
        return getScore(scoreId);
    }

    @Transactional
    public LiveScoreDetailResponse editBall(String username, Long scoreId, Long eventId, ScoreEventRequest request) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can correct scores");
        }
        if (Boolean.TRUE.equals(score.getLocked())) {
            throw new ApiException("Match is locked by admin");
        }

        ScoreEvent event = scoreEventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException("Score event not found"));
        if (!event.getLiveScore().getId().equals(scoreId)) {
            throw new ApiException("Event does not belong to this match");
        }

        event.setRuns(request.getRuns() == null ? 0 : request.getRuns());
        event.setExtraRuns(request.getExtraRuns() == null ? 0 : request.getExtraRuns());
        validateScoreEvent(request, event.getRuns(), event.getExtraRuns());
        event.setExtraType(request.getExtraType());
        event.setWicket(Boolean.TRUE.equals(request.getWicket()));
        event.setWicketType(request.getWicketType());
        event.setBatter(request.getBatter());
        event.setBowler(request.getBowler());

        boolean legalDelivery = request.getExtraType() == null
                || (!request.getExtraType().equalsIgnoreCase("WIDE") && !request.getExtraType().equalsIgnoreCase("NO_BALL"));
        String overLabel = formatOvers(score.getBallsBowled()); // dynamic during recalculate
        String commentary = generateCommentaryText(request, legalDelivery);
        event.setNote("[Corrected] " + commentary);

        scoreEventRepository.save(event);
        recalculateScore(score);
        writeAudit(username, "LIVE_SCORE_BALL_EDITED", "LiveScore #" + scoreId + " event=" + eventId);
        broadcastScoreUpdate(scoreId, "ball-edited");
        return getScore(scoreId);
    }

    @Transactional
    public LiveScoreDetailResponse deleteBall(String username, Long scoreId, Long eventId) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can correct scores");
        }
        if (Boolean.TRUE.equals(score.getLocked())) {
            throw new ApiException("Match is locked by admin");
        }

        ScoreEvent event = scoreEventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException("Score event not found"));
        if (!event.getLiveScore().getId().equals(scoreId)) {
            throw new ApiException("Event does not belong to this match");
        }

        scoreEventRepository.delete(event);

        // Shift sequences to keep them gap-free
        List<ScoreEvent> remaining = scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(scoreId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setEventSequence(i + 1);
            scoreEventRepository.save(remaining.get(i));
        }

        recalculateScore(score);
        writeAudit(username, "LIVE_SCORE_BALL_DELETED", "LiveScore #" + scoreId + " event=" + eventId);
        broadcastScoreUpdate(scoreId, "ball-deleted");
        return getScore(scoreId);
    }

    @Transactional
    public LiveScoreResponse pauseMatch(String username, Long scoreId) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can pause this match");
        }
        if (score.getStatus() != LiveMatchStatus.LIVE && score.getStatus() != LiveMatchStatus.INNINGS_BREAK) {
            throw new ApiException("Match is not running");
        }
        score.setStatus(LiveMatchStatus.PAUSED);
        LiveMatchScore saved = liveMatchScoreRepository.save(score);
        writeAudit(username, "LIVE_SCORE_PAUSED", "LiveScore #" + scoreId);
        broadcastScoreUpdate(scoreId, "paused");
        return mapScore(saved);
    }

    @Transactional
    public LiveScoreResponse resumeMatch(String username, Long scoreId) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can resume this match");
        }
        if (score.getStatus() != LiveMatchStatus.PAUSED && score.getStatus() != LiveMatchStatus.INNINGS_BREAK) {
            throw new ApiException("Match is not paused");
        }
        score.setStatus(LiveMatchStatus.LIVE);
        LiveMatchScore saved = liveMatchScoreRepository.save(score);
        writeAudit(username, "LIVE_SCORE_RESUMED", "LiveScore #" + scoreId);
        broadcastScoreUpdate(scoreId, "resumed");
        return mapScore(saved);
    }

    @Transactional
    public LiveScoreResponse cancelMatch(String username, Long scoreId) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can cancel this match");
        }
        score.setStatus(LiveMatchStatus.CANCELLED);
        notifyParticipants(score, "Match Cancelled!", "The match " + score.getTitle() + " has been cancelled.");
        LiveMatchScore saved = liveMatchScoreRepository.save(score);
        writeAudit(username, "LIVE_SCORE_CANCELLED", "LiveScore #" + scoreId);
        broadcastScoreUpdate(scoreId, "cancelled");
        return mapScore(saved);
    }

    @Transactional
    public LiveScoreResponse completeScore(String username, Long scoreId) {
        User scorer = findUser(username);
        LiveMatchScore score = findScore(scoreId);
        if (!score.getScorer().getId().equals(scorer.getId())) {
            throw new ApiException("Only assigned scorer can complete this score");
        }
        score.setStatus(LiveMatchStatus.COMPLETED);
        notifyParticipants(score, "Match Completed!", "Scorer completed and locked the match " + score.getTitle() + ".");
        persistCurrentInningsSnapshot(score);
        savePlayerStats(score);
        LiveMatchScore saved = liveMatchScoreRepository.save(score);
        syncTournamentFixtureIfReady(username, saved);
        writeAudit(username, "LIVE_SCORE_COMPLETED", "LiveScore #" + scoreId);
        broadcastScoreUpdate(scoreId, "completed");
        return mapScore(saved);
    }

    @Transactional
    public LiveScoreResponse overrideResult(String username, Long scoreId, String winnerTeamName) {
        User admin = findUser(username);
        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new ApiException("Only administrators can override match results");
        }
        LiveMatchScore score = findScore(scoreId);
        score.setStatus(LiveMatchStatus.COMPLETED);
        score.setLocked(true);
        score.setTitle(score.getTitle() + " - [OVERRIDDEN]");
        liveMatchScoreRepository.save(score);

        notifyParticipants(score, "Admin Override Applied!", "An admin has overridden the match result of " + score.getTitle() + ". Winner assigned: " + winnerTeamName);
        writeAudit(username, "LIVE_SCORE_RESULT_OVERRIDDEN", "LiveScore #" + scoreId + " winner=" + winnerTeamName);
        broadcastScoreUpdate(scoreId, "result-overridden");
        return mapScore(score);
    }

    @Transactional
    public LiveScoreResponse lockMatch(String username, Long scoreId) {
        User admin = findUser(username);
        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new ApiException("Only administrators can lock completed scores");
        }
        LiveMatchScore score = findScore(scoreId);
        score.setLocked(true);
        LiveMatchScore saved = liveMatchScoreRepository.save(score);
        writeAudit(username, "LIVE_SCORE_LOCKED", "LiveScore #" + scoreId);
        broadcastScoreUpdate(scoreId, "locked");
        return mapScore(saved);
    }

    @Transactional(readOnly = true)
    public MatchAnalyticsResponse getAnalytics(Long scoreId) {
        LiveMatchScore score = findScore(scoreId);
        List<ScoreEvent> events = scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(scoreId);

        List<Integer> wormTeamA = new ArrayList<>();
        List<Integer> wormTeamB = new ArrayList<>();
        List<MatchAnalyticsResponse.PartnershipDto> partnerships = new ArrayList<>();

        int runsTeamA = 0;
        int runsTeamB = 0;
        int ballsInCurrentOver = 0;
        int currentInnings = 1;

        Map<String, MatchAnalyticsResponse.PartnershipDto> activePartnership = new HashMap<>();
        String currentStriker = null;
        String currentNonStriker = null;
        int pRuns = 0;
        int pBalls = 0;

        for (ScoreEvent event : events) {
            boolean legal = event.getExtraType() == null ||
                    (!event.getExtraType().equalsIgnoreCase("WIDE") && !event.getExtraType().equalsIgnoreCase("NO_BALL"));

            // Calculate worm graph coordinates (Over-by-over)
            if (event.getEventSequence() == 1) {
                // start point
                wormTeamA.add(0);
                wormTeamB.add(0);
            }

            // check innings transition
            if (event.getEventSequence() > 1 && !event.getBatter().equals(currentStriker) && !event.getBatter().equals(currentNonStriker)) {
                // check if new innings started (if we see different batter list entirely)
                // for simplicity, sequential timeline runs
            }

            // Accumulate runs per over
            if (legal) {
                ballsInCurrentOver++;
            }

            // dynamic innings indicator
            // if we have innings in database, we can check. For now let's assume
            // events can be partitioned. Let's compute runs:
            // Since events don't store innings number explicitly in our entity, we can read them sequentially
            // and check when balls reset or innings switches.
            // Let's assume we count legal balls. First Innings is capped at maxOvers * 6.
            boolean isInnings1 = wormTeamB.isEmpty() || wormTeamA.size() <= score.getMaxOvers();

            if (isInnings1) {
                runsTeamA += event.getRuns() + event.getExtraRuns();
                if (ballsInCurrentOver == 6 || event.getWicket()) {
                    wormTeamA.add(runsTeamA);
                    ballsInCurrentOver = 0;
                }
            } else {
                runsTeamB += event.getRuns() + event.getExtraRuns();
                if (ballsInCurrentOver == 6 || event.getWicket()) {
                    wormTeamB.add(runsTeamB);
                    ballsInCurrentOver = 0;
                }
            }

            // Partnerships details
            if (currentStriker == null) {
                currentStriker = event.getBatter();
            } else if (currentNonStriker == null && !event.getBatter().equals(currentStriker)) {
                currentNonStriker = event.getBatter();
            }

            pRuns += event.getRuns() + event.getExtraRuns();
            if (legal) {
                pBalls++;
            }

            if (event.getWicket()) {
                partnerships.add(MatchAnalyticsResponse.PartnershipDto.builder()
                        .batsman1(currentStriker)
                        .batsman2(currentNonStriker == null ? "Non-striker" : currentNonStriker)
                        .runs(pRuns)
                        .balls(pBalls)
                        .build());
                // reset partnership
                pRuns = 0;
                pBalls = 0;
                currentStriker = null;
                currentNonStriker = null;
            }
        }

        // Add last active partnership if not completed
        if (pRuns > 0 || pBalls > 0) {
            partnerships.add(MatchAnalyticsResponse.PartnershipDto.builder()
                    .batsman1(currentStriker == null ? "Striker" : currentStriker)
                    .batsman2(currentNonStriker == null ? "Non-striker" : currentNonStriker)
                    .runs(pRuns)
                    .balls(pBalls)
                    .build());
        }

        // Player metric indicators (Mocked top strike rates & economies calculated from stats)
        List<MatchAnalyticsResponse.PlayerMetricDto> topBatters = new ArrayList<>();
        List<MatchAnalyticsResponse.PlayerMetricDto> topBowlers = new ArrayList<>();

        return MatchAnalyticsResponse.builder()
                .wormTeamA(wormTeamA)
                .wormTeamB(wormTeamB)
                .partnerships(partnerships)
                .topBatters(topBatters)
                .topBowlers(topBowlers)
                .build();
    }

    private void recalculateScore(LiveMatchScore score) {
        List<ScoreEvent> events = scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(score.getId());

        int totalRuns = 0;
        int totalWickets = 0;
        int totalBalls = 0;
        int totalExtras = 0;
        int currentInnings = 1;
        int firstInningsRuns = 0;

        score.setInnings(1);
        score.setTargetRuns(null);
        score.setStatus(LiveMatchStatus.LIVE);
        score.setIsTie(false);

        for (ScoreEvent event : events) {
            boolean legalDelivery = event.getExtraType() == null
                    || (!event.getExtraType().equalsIgnoreCase("WIDE") && !event.getExtraType().equalsIgnoreCase("NO_BALL"));

            int runs = event.getRuns() + event.getExtraRuns();
            totalRuns += runs;
            totalExtras += event.getExtraRuns();
            totalWickets += event.getWicket() ? 1 : 0;
            totalBalls += legalDelivery ? 1 : 0;

            boolean inningsEnded = totalWickets >= 10 || totalBalls >= score.getMaxOvers() * 6;

            if (score.getInnings() == 1) {
                if (inningsEnded) {
                    firstInningsRuns = totalRuns;
                    score.setInnings(2);
                    score.setTargetRuns(firstInningsRuns + 1);
                    // reset for 2nd innings calculation
                    totalRuns = 0;
                    totalWickets = 0;
                    totalBalls = 0;
                    totalExtras = 0;
                }
            } else {
                if (totalRuns >= score.getTargetRuns()) {
                    score.setStatus(LiveMatchStatus.COMPLETED);
                    break;
                } else if (inningsEnded) {
                    score.setStatus(LiveMatchStatus.COMPLETED);
                    if (totalRuns == score.getTargetRuns() - 1) {
                        score.setIsTie(true);
                    }
                    break;
                }
            }
        }

        // Set final values
        score.setRuns(totalRuns);
        score.setWickets(totalWickets);
        score.setBallsBowled(totalBalls);
        score.setExtras(totalExtras);

        liveMatchScoreRepository.save(score);
    }

    private void calculateScoreboardComponents(LiveMatchScore score, List<ScoreEvent> events, LiveScoreDetailResponse.LiveScoreDetailResponseBuilder builder) {
        if (events.isEmpty()) return;

        Map<String, Integer> batterRuns = new HashMap<>();
        Map<String, Integer> batterBalls = new HashMap<>();
        Map<String, Integer> batterFours = new HashMap<>();
        Map<String, Integer> batterSixes = new HashMap<>();

        Map<String, Integer> bowlerRuns = new HashMap<>();
        Map<String, Integer> bowlerBalls = new HashMap<>();
        Map<String, Integer> bowlerWickets = new HashMap<>();

        String lastStriker = null;
        String lastNonStriker = null;
        String lastBowler = null;

        int activePartnershipRuns = 0;
        int activePartnershipBalls = 0;

        for (ScoreEvent event : events) {
            boolean legal = event.getExtraType() == null ||
                    (!event.getExtraType().equalsIgnoreCase("WIDE") && !event.getExtraType().equalsIgnoreCase("NO_BALL"));

            // Striker details
            String striker = event.getBatter();
            if (striker != null) {
                lastStriker = striker;
                batterRuns.put(striker, batterRuns.getOrDefault(striker, 0) + event.getRuns());
                if (legal) {
                    batterBalls.put(striker, batterBalls.getOrDefault(striker, 0) + 1);
                }
                if (event.getRuns() == 4) {
                    batterFours.put(striker, batterFours.getOrDefault(striker, 0) + 1);
                }
                if (event.getRuns() == 6) {
                    batterSixes.put(striker, batterSixes.getOrDefault(striker, 0) + 1);
                }
            }

            // Bowler details
            String bowler = event.getBowler();
            if (bowler != null) {
                lastBowler = bowler;
                bowlerRuns.put(bowler, bowlerRuns.getOrDefault(bowler, 0) + event.getRuns() + event.getExtraRuns());
                if (legal) {
                    bowlerBalls.put(bowler, bowlerBalls.getOrDefault(bowler, 0) + 1);
                }
                if (event.getWicket()) {
                    bowlerWickets.put(bowler, bowlerWickets.getOrDefault(bowler, 0) + 1);
                }
            }

            // Partnership details
            activePartnershipRuns += event.getRuns() + event.getExtraRuns();
            if (legal) {
                activePartnershipBalls++;
            }

            // If wicket, reset active partnership variables
            if (event.getWicket()) {
                activePartnershipRuns = 0;
                activePartnershipBalls = 0;
            }
        }

        // Deduce active non-striker as someone who batted recently but is not the last striker
        for (ScoreEvent event : events) {
            if (event.getBatter() != null && !event.getBatter().equals(lastStriker)) {
                lastNonStriker = event.getBatter();
            }
        }

        // Set striker stats
        if (lastStriker != null) {
            builder.strikerName(lastStriker)
                    .strikerRuns(batterRuns.getOrDefault(lastStriker, 0))
                    .strikerBalls(batterBalls.getOrDefault(lastStriker, 0))
                    .strikerFours(batterFours.getOrDefault(lastStriker, 0))
                    .strikerSixes(batterSixes.getOrDefault(lastStriker, 0));
        }

        // Set non-striker stats
        if (lastNonStriker != null) {
            builder.nonStrikerName(lastNonStriker)
                    .nonStrikerRuns(batterRuns.getOrDefault(lastNonStriker, 0))
                    .nonStrikerBalls(batterBalls.getOrDefault(lastNonStriker, 0))
                    .nonStrikerFours(batterFours.getOrDefault(lastNonStriker, 0))
                    .nonStrikerSixes(batterSixes.getOrDefault(lastNonStriker, 0));
        }

        // Set bowler stats
        if (lastBowler != null) {
            int balls = bowlerBalls.getOrDefault(lastBowler, 0);
            double overs = (balls / 6) + ((balls % 6) / 10.0);
            builder.bowlerName(lastBowler)
                    .bowlerOvers(overs)
                    .bowlerRuns(bowlerRuns.getOrDefault(lastBowler, 0))
                    .bowlerWickets(bowlerWickets.getOrDefault(lastBowler, 0));
        }

        builder.partnershipRuns(activePartnershipRuns)
                .partnershipBalls(activePartnershipBalls);

        // Required run rate calculation
        if (score.getInnings() == 2 && score.getTargetRuns() != null) {
            int remainingRuns = score.getTargetRuns() - score.getRuns();
            int remainingBalls = (score.getMaxOvers() * 6) - score.getBallsBowled();
            if (remainingBalls > 0) {
                double reqRate = (remainingRuns * 6.0) / remainingBalls;
                builder.requiredRunRate(Math.round(reqRate * 100.0) / 100.0);
            } else {
                builder.requiredRunRate(0.0);
            }
        }
    }

    private List<InningsSegment> inningsSegments(LiveMatchScore score, List<ScoreEvent> events) {
        String firstBattingTeam = score.getInnings() != null && score.getInnings() > 1 ? score.getBowlingTeam() : score.getBattingTeam();
        String secondBattingTeam = score.getInnings() != null && score.getInnings() > 1 ? score.getBattingTeam() : score.getBowlingTeam();
        List<InningsSegment> segments = new ArrayList<>();
        InningsSegment current = new InningsSegment(1, firstBattingTeam);
        segments.add(current);

        for (ScoreEvent event : events) {
            current.events.add(event);
            current.runs += safe(event.getRuns()) + safe(event.getExtraRuns());
            current.extras += safe(event.getExtraRuns());
            current.wickets += Boolean.TRUE.equals(event.getWicket()) ? 1 : 0;
            current.ballsBowled += isLegalDelivery(event.getExtraType()) ? 1 : 0;
            if (current.inningsNumber == 1 && (current.wickets >= 10 || current.ballsBowled >= score.getMaxOvers() * 6)) {
                current = new InningsSegment(2, secondBattingTeam);
                segments.add(current);
            }
        }

        return segments.stream()
                .filter(segment -> !segment.events.isEmpty() || segment.inningsNumber == score.getInnings())
                .toList();
    }

    private LiveScoreInningsResponse mapInningsSegment(InningsSegment segment) {
        return LiveScoreInningsResponse.builder()
                .inningsNumber(segment.inningsNumber)
                .battingTeamName(segment.battingTeamName)
                .runs(segment.runs)
                .wickets(segment.wickets)
                .ballsBowled(segment.ballsBowled)
                .overs(formatOvers(segment.ballsBowled))
                .extras(segment.extras)
                .build();
    }

    private List<LiveScoreBattingRowResponse> battingScorecard(List<ScoreEvent> events) {
        Map<String, BatterAccumulator> rows = new LinkedHashMap<>();
        for (ScoreEvent event : events) {
            String batter = defaultValue(event.getBatter(), "Striker");
            BatterAccumulator row = rows.computeIfAbsent(batter, BatterAccumulator::new);
            row.runs += safe(event.getRuns());
            if (isLegalDelivery(event.getExtraType())) {
                row.balls++;
            }
            if (safe(event.getRuns()) == 4) {
                row.fours++;
            }
            if (safe(event.getRuns()) == 6) {
                row.sixes++;
            }
            if (Boolean.TRUE.equals(event.getWicket())) {
                row.out = true;
                row.dismissal = defaultValue(event.getWicketType(), "WICKET");
            }
        }
        return rows.values().stream()
                .map(row -> LiveScoreBattingRowResponse.builder()
                        .playerName(row.playerName)
                        .runs(row.runs)
                        .balls(row.balls)
                        .fours(row.fours)
                        .sixes(row.sixes)
                        .strikeRate(row.balls == 0 ? 0.0 : round((row.runs * 100.0) / row.balls))
                        .out(row.out)
                        .dismissal(row.dismissal)
                        .build())
                .toList();
    }

    private List<LiveScoreBowlingRowResponse> bowlingScorecard(List<ScoreEvent> events) {
        Map<String, BowlerAccumulator> rows = new LinkedHashMap<>();
        for (ScoreEvent event : events) {
            String bowler = defaultValue(event.getBowler(), "Bowler");
            BowlerAccumulator row = rows.computeIfAbsent(bowler, BowlerAccumulator::new);
            row.runsConceded += bowlerCreditedRuns(event);
            if (isLegalDelivery(event.getExtraType())) {
                row.balls++;
            }
            if (Boolean.TRUE.equals(event.getWicket()) && isBowlerWicket(event.getWicketType())) {
                row.wickets++;
            }
        }
        return rows.values().stream()
                .map(row -> LiveScoreBowlingRowResponse.builder()
                        .playerName(row.playerName)
                        .overs(formatOvers(row.balls))
                        .balls(row.balls)
                        .runsConceded(row.runsConceded)
                        .wickets(row.wickets)
                        .economy(row.balls == 0 ? 0.0 : round((row.runsConceded * 6.0) / row.balls))
                        .build())
                .toList();
    }

    private List<LiveScoreOverSummaryResponse> recentOvers(List<ScoreEvent> events) {
        List<LiveScoreOverSummaryResponse> overs = new ArrayList<>();
        List<ScoreEventResponse> currentEvents = new ArrayList<>();
        int overNumber = 1;
        int legalBalls = 0;
        int overRuns = 0;
        int overWickets = 0;

        for (ScoreEvent event : events) {
            currentEvents.add(mapEvent(event));
            overRuns += safe(event.getRuns()) + safe(event.getExtraRuns());
            overWickets += Boolean.TRUE.equals(event.getWicket()) ? 1 : 0;
            if (isLegalDelivery(event.getExtraType())) {
                legalBalls++;
            }
            if (legalBalls == 6) {
                overs.add(LiveScoreOverSummaryResponse.builder()
                        .overNumber(overNumber)
                        .label("Over " + overNumber)
                        .runs(overRuns)
                        .wickets(overWickets)
                        .events(new ArrayList<>(currentEvents))
                        .build());
                overNumber++;
                legalBalls = 0;
                overRuns = 0;
                overWickets = 0;
                currentEvents.clear();
            }
        }

        if (!currentEvents.isEmpty()) {
            overs.add(LiveScoreOverSummaryResponse.builder()
                    .overNumber(overNumber)
                    .label("Over " + overNumber + " in progress")
                    .runs(overRuns)
                    .wickets(overWickets)
                    .events(currentEvents)
                    .build());
        }
        return overs.stream().skip(Math.max(0, overs.size() - 5)).toList();
    }

    private List<LiveScoreFallOfWicketResponse> fallOfWickets(List<ScoreEvent> events) {
        List<LiveScoreFallOfWicketResponse> wickets = new ArrayList<>();
        int runs = 0;
        int balls = 0;
        int wicketNumber = 0;
        for (ScoreEvent event : events) {
            runs += safe(event.getRuns()) + safe(event.getExtraRuns());
            if (isLegalDelivery(event.getExtraType())) {
                balls++;
            }
            if (Boolean.TRUE.equals(event.getWicket())) {
                wicketNumber++;
                wickets.add(LiveScoreFallOfWicketResponse.builder()
                        .wicketNumber(wicketNumber)
                        .score(runs)
                        .over(formatOvers(balls))
                        .batter(defaultValue(event.getBatter(), "Batter"))
                        .wicketType(defaultValue(event.getWicketType(), "WICKET"))
                        .build());
            }
        }
        return wickets;
    }

    private String winnerTeamName(LiveMatchScore score) {
        if (score.getStatus() != LiveMatchStatus.COMPLETED) {
            return null;
        }
        if (Boolean.TRUE.equals(score.getIsTie())) {
            return "Tie";
        }
        if (score.getFixture() != null && score.getFixture().getWinner() != null) {
            return score.getFixture().getWinner().getName();
        }
        if (score.getInnings() != null && score.getInnings() >= 2 && score.getTargetRuns() != null) {
            return score.getRuns() >= score.getTargetRuns() ? score.getBattingTeam() : score.getBowlingTeam();
        }
        return null;
    }

    private String resultSummary(LiveMatchScore score) {
        if (score.getStatus() == LiveMatchStatus.NOT_STARTED) {
            return "Match ready for scoring";
        }
        if (score.getStatus() == LiveMatchStatus.INNINGS_BREAK) {
            return "Innings break. Target " + score.getTargetRuns();
        }
        if (score.getStatus() == LiveMatchStatus.CANCELLED) {
            return "Match cancelled";
        }
        if (score.getStatus() == LiveMatchStatus.COMPLETED) {
            String winner = winnerTeamName(score);
            if ("Tie".equals(winner)) {
                return "Match tied";
            }
            return winner == null ? "Match completed" : winner + " won";
        }
        return score.getBattingTeam() + " batting at " + score.getRuns() + "/" + score.getWickets();
    }

    private String verificationStatus(LiveMatchScore score) {
        if (Boolean.TRUE.equals(score.getLocked())) {
            return "LOCKED";
        }
        if (score.getStatus() == LiveMatchStatus.COMPLETED) {
            return "PENDING_CAPTAIN_VERIFICATION";
        }
        return "SCORER_CONTROLLED";
    }

    private String shareUrl(LiveMatchScore score) {
        return "/live-scoring?scoreId=" + score.getId();
    }

    private List<String> auditTrail(LiveMatchScore score) {
        List<String> trail = new ArrayList<>();
        trail.add("Created by " + score.getScorer().getName());
        trail.add("Status " + score.getStatus());
        if (score.getUpdatedAt() != null) {
            trail.add("Last updated " + score.getUpdatedAt());
        }
        if (Boolean.TRUE.equals(score.getLocked())) {
            trail.add("Locked by admin workflow");
        }
        return trail;
    }

    private void persistCurrentInningsSnapshot(LiveMatchScore score) {
        boolean exists = inningsRepository.findByLiveScoreIdOrderByInningsNumberAsc(score.getId()).stream()
                .anyMatch(innings -> innings.getInningsNumber().equals(score.getInnings()));
        if (exists) {
            return;
        }
        inningsRepository.save(Innings.builder()
                .liveScore(score)
                .inningsNumber(score.getInnings())
                .battingTeamName(score.getBattingTeam())
                .runs(score.getRuns())
                .wickets(score.getWickets())
                .ballsBowled(score.getBallsBowled())
                .extras(score.getExtras())
                .build());
    }

    private void syncTournamentFixtureIfReady(String username, LiveMatchScore score) {
        if (score.getFixture() == null || score.getFixture().getStatus() == FixtureStatus.COMPLETED) {
            return;
        }
        Map<String, Integer> totals = teamTotals(score);
        Integer teamAScore = totals.get(score.getFixture().getTeamA().getName());
        Integer teamBScore = totals.get(score.getFixture().getTeamB().getName());
        if (teamAScore == null || teamBScore == null) {
            return;
        }
        FixtureResultRequest request = new FixtureResultRequest();
        request.setTeamAScore(teamAScore);
        request.setTeamBScore(teamBScore);
        try {
            tournamentService.recordFixtureResult(username, score.getFixture().getId(), request);
            writeAudit(username, "LIVE_SCORE_TOURNAMENT_SYNCED", "Fixture #" + score.getFixture().getId());
        } catch (ApiException ex) {
            log.warn("Unable to sync live score {} to tournament fixture: {}", score.getId(), ex.getMessage());
        }
    }

    private Map<String, Integer> teamTotals(LiveMatchScore score) {
        Map<String, Integer> totals = new HashMap<>();
        inningsRepository.findByLiveScoreIdOrderByInningsNumberAsc(score.getId())
                .forEach(innings -> totals.put(innings.getBattingTeamName(), innings.getRuns()));
        return totals;
    }

    private boolean isLegalDelivery(String extraType) {
        return extraType == null || (!extraType.equalsIgnoreCase("WIDE") && !extraType.equalsIgnoreCase("NO_BALL"));
    }

    private int bowlerCreditedRuns(ScoreEvent event) {
        String extraType = event.getExtraType();
        int extraRuns = safe(event.getExtraRuns());
        boolean bowlerExtra = extraType != null && (extraType.equalsIgnoreCase("WIDE") || extraType.equalsIgnoreCase("NO_BALL"));
        return safe(event.getRuns()) + (bowlerExtra ? extraRuns : 0);
    }

    private boolean isBowlerWicket(String wicketType) {
        return wicketType == null || !wicketType.equalsIgnoreCase("RUN_OUT");
    }

    private void validateScoreEvent(ScoreEventRequest request, int runs, int extraRuns) {
        if (runs > 6) {
            throw new ApiException("Runs cannot be greater than six for a single delivery");
        }
        if (extraRuns > 7) {
            throw new ApiException("Extra runs cannot be greater than seven for a single delivery");
        }
        if (request.getExtraType() != null && !request.getExtraType().isBlank()) {
            String extraType = request.getExtraType().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("WIDE", "NO_BALL", "BYE", "LEG_BYE").contains(extraType)) {
                throw new ApiException("Invalid extra type");
            }
        }
        if (Boolean.TRUE.equals(request.getWicket()) && request.getWicketType() != null && !request.getWicketType().isBlank()) {
            String wicketType = request.getWicketType().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("BOWLED", "CAUGHT", "RUN_OUT", "LBW", "STUMPED", "HIT_WICKET").contains(wicketType)) {
                throw new ApiException("Invalid wicket type");
            }
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void savePlayerStats(LiveMatchScore score) {
        List<ScoreEvent> events = scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(score.getId());
        if (events.isEmpty()) return;

        Map<String, Integer> bRuns = new HashMap<>();
        Map<String, Integer> bBalls = new HashMap<>();
        Map<String, Integer> b4s = new HashMap<>();
        Map<String, Integer> b6s = new HashMap<>();

        Map<String, Integer> wRuns = new HashMap<>();
        Map<String, Integer> wBalls = new HashMap<>();
        Map<String, Integer> wWickets = new HashMap<>();

        Set<String> uniquePlayers = new HashSet<>();

        for (ScoreEvent event : events) {
            boolean legal = event.getExtraType() == null ||
                    (!event.getExtraType().equalsIgnoreCase("WIDE") && !event.getExtraType().equalsIgnoreCase("NO_BALL"));

            if (event.getBatter() != null) {
                String batter = event.getBatter().trim();
                uniquePlayers.add(batter);
                bRuns.put(batter, bRuns.getOrDefault(batter, 0) + event.getRuns());
                if (legal) {
                    bBalls.put(batter, bBalls.getOrDefault(batter, 0) + 1);
                }
                if (event.getRuns() == 4) {
                    b4s.put(batter, b4s.getOrDefault(batter, 0) + 1);
                }
                if (event.getRuns() == 6) {
                    b6s.put(batter, b6s.getOrDefault(batter, 0) + 1);
                }
            }

            if (event.getBowler() != null) {
                String bowler = event.getBowler().trim();
                uniquePlayers.add(bowler);
                wRuns.put(bowler, wRuns.getOrDefault(bowler, 0) + event.getRuns() + event.getExtraRuns());
                if (legal) {
                    wBalls.put(bowler, wBalls.getOrDefault(bowler, 0) + 1);
                }
                if (event.getWicket()) {
                    wWickets.put(bowler, wWickets.getOrDefault(bowler, 0) + 1);
                }
            }
        }

        // Save stat records for resolved users
        for (String playerName : uniquePlayers) {
            User resolved = resolveUser(score, playerName);
            if (resolved == null) continue; // Skip unresolved text players

            PlayerMatchStats stats = playerMatchStatsRepository.findByLiveScoreIdAndUserId(score.getId(), resolved.getId())
                    .orElseGet(() -> PlayerMatchStats.builder()
                            .user(resolved)
                            .liveScore(score)
                            .build());
            boolean firstTrackedMatch = stats.getId() == null;
            stats.setRunsScored(bRuns.getOrDefault(playerName, 0));
            stats.setBallsFaced(bBalls.getOrDefault(playerName, 0));
            stats.setFours(b4s.getOrDefault(playerName, 0));
            stats.setSixes(b6s.getOrDefault(playerName, 0));
            stats.setWicketsTaken(wWickets.getOrDefault(playerName, 0));
            stats.setOversBowled(wBalls.getOrDefault(playerName, 0) / 6.0);
            stats.setRunsConceded(wRuns.getOrDefault(playerName, 0));
            stats.setCatches(stats.getCatches() == null ? 0 : stats.getCatches());
            stats.setRunouts(stats.getRunouts() == null ? 0 : stats.getRunouts());

            playerMatchStatsRepository.save(stats);

            // Update Player Profile aggregates
            PlayerProfile profile = playerProfileRepository.findByUserId(resolved.getId())
                    .orElse(PlayerProfile.builder()
                            .user(resolved)
                            .matchesPlayed(0)
                            .wins(0)
                            .mvpCount(0)
                            .rating(5.0)
                            .build());

            if (firstTrackedMatch) {
                profile.setMatchesPlayed((profile.getMatchesPlayed() == null ? 0 : profile.getMatchesPlayed()) + 1);
                profile.setRating(Math.min(5.0, (profile.getRating() == null ? 0.0 : profile.getRating()) + 0.1));
            }
            playerProfileRepository.save(profile);
        }
    }

    private User resolveUser(LiveMatchScore score, String name) {
        // Resolve user via matches rosters or exact database match
        Optional<User> uOpt = userRepository.findByEmailOrPhone(name);
        if (uOpt.isPresent()) return uOpt.get();

        // Check if teams roster contains name
        Team homeTeam = score.getPublicMatch() != null ? score.getPublicMatch().getHomeTeam() :
                (score.getFixture() != null ? score.getFixture().getTeamA() : null);
        Team awayTeam = score.getPublicMatch() != null ? score.getPublicMatch().getAwayTeam() :
                (score.getFixture() != null ? score.getFixture().getTeamB() : null);

        if (homeTeam != null) {
            for (TeamMember m : teamMemberRepository.findByTeamIdOrderByCreatedAtAsc(homeTeam.getId())) {
                if (m.getUser().getName().equalsIgnoreCase(name)) return m.getUser();
            }
        }
        if (awayTeam != null) {
            for (TeamMember m : teamMemberRepository.findByTeamIdOrderByCreatedAtAsc(awayTeam.getId())) {
                if (m.getUser().getName().equalsIgnoreCase(name)) return m.getUser();
            }
        }

        // Search user table loosely by name
        return userRepository.findAll().stream()
                .filter(user -> user.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private void notifyParticipants(LiveMatchScore score, String title, String msg) {
        try {
            // Reuses EPIC 6 notification queue dispatcher
            log.info("Milestone Alert [{}]: {}", title, msg);
            notificationService.pushNotification(score.getScorer().getId(), score.getScorer().getEmail(), title, msg);
        } catch (Exception e) {
            log.error("Unable to push live score notification alert", e);
        }
    }

    private int calculateBatterCumulative(Long scoreId, String batterName) {
        if (batterName == null) return 0;
        return scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(scoreId)
                .stream()
                .filter(e -> batterName.equalsIgnoreCase(e.getBatter()))
                .mapToInt(ScoreEvent::getRuns)
                .sum();
    }

    private String generateCommentaryText(ScoreEventRequest req, boolean legal) {
        String batter = req.getBatter() == null ? "Striker" : req.getBatter();
        String bowler = req.getBowler() == null ? "Bowler" : req.getBowler();

        if (Boolean.TRUE.equals(req.getWicket())) {
            return "OUT! " + batter + " is back to the pavilion! Dismissed (" + defaultValue(req.getWicketType(), "Bowled") + ") by " + bowler + ".";
        }
        if (req.getRuns() == 6) {
            return "SIX! Spectacular shot from " + batter + " off " + bowler + "! Absolutely crushed it over the fence.";
        }
        if (req.getRuns() == 4) {
            return "FOUR! Gorgeous cover drive from " + batter + " off " + bowler + ". Elegance at its finest!";
        }
        if (req.getRuns() == 0 && req.getExtraRuns() == 0) {
            return "No run. " + bowler + " targets the stumps and " + batter + " defends it safely.";
        }

        String extrasDetail = "";
        if (req.getExtraType() != null) {
            extrasDetail = " [Extras: " + req.getExtraRuns() + " " + req.getExtraType() + "]";
        }

        return batter + " scores " + req.getRuns() + " run(s) off " + bowler + "." + extrasDetail;
    }

    private LiveScoreResponse mapScore(LiveMatchScore score) {
        double runRate = score.getBallsBowled() == 0 ? 0.0 : (score.getRuns() * 6.0) / score.getBallsBowled();
        int projected = score.getBallsBowled() == 0 ? 0 : (int) Math.round(runRate * score.getMaxOvers());
        return LiveScoreResponse.builder()
                .id(score.getId())
                .fixtureId(score.getFixture() == null ? null : score.getFixture().getId())
                .publicMatchId(score.getPublicMatch() == null ? null : score.getPublicMatch().getId())
                .scorerId(score.getScorer().getId())
                .scorerName(score.getScorer().getName())
                .title(score.getTitle())
                .teamAName(score.getTeamAName())
                .teamBName(score.getTeamBName())
                .battingTeam(score.getBattingTeam())
                .bowlingTeam(score.getBowlingTeam())
                .innings(score.getInnings())
                .runs(score.getRuns())
                .wickets(score.getWickets())
                .ballsBowled(score.getBallsBowled())
                .overs(formatOvers(score.getBallsBowled()))
                .extras(score.getExtras())
                .targetRuns(score.getTargetRuns())
                .maxOvers(score.getMaxOvers())
                .runRate(Math.round(runRate * 100.0) / 100.0)
                .projectedScore(projected)
                .status(score.getStatus())
                .locked(Boolean.TRUE.equals(score.getLocked()))
                .tie(Boolean.TRUE.equals(score.getIsTie()))
                .superOver(Boolean.TRUE.equals(score.getIsSuperOver()))
                .winnerTeamName(winnerTeamName(score))
                .resultSummary(resultSummary(score))
                .shareUrl(shareUrl(score))
                .updatedAt(score.getUpdatedAt())
                .build();
    }

    private ScoreEventResponse mapEvent(ScoreEvent event) {
        return ScoreEventResponse.builder()
                .id(event.getId())
                .eventSequence(event.getEventSequence())
                .runs(event.getRuns())
                .extraRuns(event.getExtraRuns())
                .extraType(event.getExtraType())
                .wicket(event.getWicket())
                .wicketType(event.getWicketType())
                .batter(event.getBatter())
                .bowler(event.getBowler())
                .note(event.getNote())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private void broadcastScoreUpdate(Long scoreId, String eventName) {
        List<SseEmitter> emitters = scoreStreams.get(scoreId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sendScoreEvent(scoreId, emitter, eventName);
        }
    }

    private void sendScoreEvent(Long scoreId, SseEmitter emitter, String eventName) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(getScore(scoreId)));
        } catch (IOException | IllegalStateException ex) {
            removeEmitter(scoreId, emitter);
        }
    }

    private void removeEmitter(Long scoreId, SseEmitter emitter) {
        List<SseEmitter> emitters = scoreStreams.get(scoreId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            scoreStreams.remove(scoreId);
        }
    }

    private void writeAudit(String actor, String action, String entity) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .adminId(actor == null ? "system" : actor)
                    .action(action)
                    .entity(entity)
                    .build());
        } catch (Exception ex) {
            log.warn("Unable to write live scoring audit {} for {}", action, entity);
        }
    }

    private LiveMatchScore findScore(Long scoreId) {
        return liveMatchScoreRepository.findById(scoreId)
                .orElseThrow(() -> new ApiException("Live score not found"));
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Integer resolveLimit(Integer value, Integer fallback, String message) {
        Integer resolved = value == null ? fallback : value;
        if (resolved <= 0) {
            throw new ApiException(message);
        }
        return resolved;
    }

    private String formatOvers(Integer balls) {
        return (balls / 6) + "." + (balls % 6);
    }

    private static class InningsSegment {
        private final Integer inningsNumber;
        private final String battingTeamName;
        private final List<ScoreEvent> events = new ArrayList<>();
        private int runs;
        private int wickets;
        private int ballsBowled;
        private int extras;

        private InningsSegment(Integer inningsNumber, String battingTeamName) {
            this.inningsNumber = inningsNumber;
            this.battingTeamName = battingTeamName;
        }
    }

    private static class BatterAccumulator {
        private final String playerName;
        private int runs;
        private int balls;
        private int fours;
        private int sixes;
        private boolean out;
        private String dismissal = "not out";

        private BatterAccumulator(String playerName) {
            this.playerName = playerName;
        }
    }

    private static class BowlerAccumulator {
        private final String playerName;
        private int balls;
        private int runsConceded;
        private int wickets;

        private BowlerAccumulator(String playerName) {
            this.playerName = playerName;
        }
    }
}
