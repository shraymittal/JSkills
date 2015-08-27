package jskills.elo;

import java.util.*;

import jskills.GameInfo;
import jskills.IPlayer;
import jskills.PairwiseComparison;
import jskills.RankSorter;
import jskills.Rating;
import jskills.SkillCalculator;
import jskills.ITeam;
import jskills.numerics.Range;

public abstract class TwoPlayerEloCalculator extends SkillCalculator {

    protected final KFactor kFactor;

    protected TwoPlayerEloCalculator(KFactor kFactor) {
        super(EnumSet.noneOf(SupportedOptions.class), Range.<ITeam> exactly(2),
                Range.<IPlayer> exactly(1));
        this.kFactor = kFactor;
    }

    @Override
    public Map<IPlayer, Rating> calculateNewRatings(GameInfo gameInfo,
            Collection<ITeam> teams, int... teamRanks) {
        validateTeamCountAndPlayersCountPerTeam(teams);
        List<ITeam> teamsl = RankSorter.sort(teams, teamRanks);

        Map<IPlayer, Rating> result = new HashMap<>();
        boolean isDraw = (teamRanks[0] == teamRanks[1]);

        List<IPlayer> players = new ArrayList<>(2);
        for(ITeam team : teamsl)
            players.add(team.keySet().toArray(new IPlayer[1])[0]);

        double player1Rating = teamsl.get(0).get(players.get(0)).getMean();
        double player2Rating = teamsl.get(1).get(players.get(1)).getMean();

        result.put(players.get(0), calculateNewRating(gameInfo, player1Rating, player2Rating, isDraw ? PairwiseComparison.DRAW : PairwiseComparison.WIN));
        result.put(players.get(1), calculateNewRating(gameInfo, player2Rating, player1Rating, isDraw ? PairwiseComparison.DRAW : PairwiseComparison.LOSE));

        return result;
    }

    protected EloRating calculateNewRating(GameInfo gameInfo,
            double selfRating, double opponentRating,
            PairwiseComparison selfToOpponentComparison) {
        double expectedProbability = getPlayerWinProbability(gameInfo, selfRating, opponentRating);
        double actualProbability = getScoreFromComparison(selfToOpponentComparison);
        double k = kFactor.getValueForRating(selfRating);
        double ratingChange = k * (actualProbability - expectedProbability);
        double newRating = selfRating + ratingChange;

        return new EloRating(newRating);
    }

    private static double getScoreFromComparison(PairwiseComparison comparison) {
        switch (comparison) {
        case WIN: return 1;
        case DRAW: return 0.5;
        case LOSE: return 0;
        default: throw new IllegalArgumentException();
        }
    }

    protected abstract double getPlayerWinProbability(GameInfo gameInfo, double playerRating, double opponentRating);

    @Override
    public double calculateMatchQuality(GameInfo gameInfo,
            Collection<ITeam> teams) {
        validateTeamCountAndPlayersCountPerTeam(teams);
        
        // Extract both players from the teams
        List<IPlayer> players = new ArrayList<>(2);
        for(ITeam team : teams) {
            Set<IPlayer> iPlayers = team.keySet();
            players.add(iPlayers.toArray(new IPlayer[iPlayers.size()])[0]);
        }

        // Extract each player's rating from their team
        Iterator<ITeam> teamit = teams.iterator();
        double player1Rating = teamit.next().get(players.get(0)).getMean();
        double player2Rating = teamit.next().get(players.get(1)).getMean();

        // The TrueSkill paper mentions that they used s1 - s2 (rating 
        // difference) to determine match quality. I convert that to a 
        // percentage as a delta from 50% using the cumulative density function 
        // of the specific curve being used
        double deltaFrom50Percent = Math.abs(getPlayerWinProbability(gameInfo, player1Rating, player2Rating) - 0.5);
        return (0.5 - deltaFrom50Percent) / 0.5;
    }
}