package jskills.trueskill;

import java.util.*;
import java.util.Map.Entry;

import jskills.*;
import jskills.numerics.Range;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.pow;

/**
 * Calculates new ratings for only two teams where each team has 1 or more players.
 * <remarks>
 * When you only have two teams, the math is still simple: no factor graphs are used yet.
 * </remarks>
 */
public class TwoTeamTrueSkillCalculator extends SkillCalculator
{
    public TwoTeamTrueSkillCalculator()
    {
        super(EnumSet.noneOf(SupportedOptions.class), Range.<ITeam>exactly(2), Range.<IPlayer>atLeast(1));
    }

     @Override
    public Map<IPlayer, Rating> calculateNewRatings(@NotNull GameInfo gameInfo,
                                                    Collection<ITeam> teams, 
                                                    int... teamRanks)
    {
        validateTeamCountAndPlayersCountPerTeam(teams);

        List<ITeam> teamsl = RankSorter.sort(teams, teamRanks);

        ITeam team1 = teamsl.get(0);
        ITeam team2 = teamsl.get(1);

        boolean wasDraw = (teamRanks[0] == teamRanks[1]);

        HashMap<IPlayer, Rating> results = new HashMap<>();

        UpdatePlayerRatings(gameInfo,
                            results,
                            team1,
                            team2,
                            wasDraw ? PairwiseComparison.DRAW : PairwiseComparison.WIN);

        UpdatePlayerRatings(gameInfo,
                            results,
                            team2,
                            team1,
                            wasDraw ? PairwiseComparison.DRAW : PairwiseComparison.LOSE);

        return results;
    }

    private static void UpdatePlayerRatings(GameInfo gameInfo,
                                            Map<IPlayer, Rating> newPlayerRatings,
                                            ITeam selfTeam,
                                            ITeam otherTeam,
                                            PairwiseComparison selfToOtherTeamComparison)
    {
        double drawMargin = DrawMargin.getDrawMarginFromDrawProbability(gameInfo.getDrawProbability(), gameInfo.getBeta());
        double betaSquared = pow(gameInfo.getBeta(), 2);
        double tauSquared = pow(gameInfo.getDynamicsFactor(), 2);

        int totalPlayers = selfTeam.size() + otherTeam.size();

        double selfMeanSum = 0;
        for (Rating r : selfTeam.values()) selfMeanSum += r.getMean();
        double otherTeamMeanSum = 0;
        for (Rating r : otherTeam.values()) otherTeamMeanSum += r.getMean();

        double sum = 0;
        for (Rating r : selfTeam.values()) sum += pow(r.getStandardDeviation(), 2);
        for (Rating r : otherTeam.values()) sum += pow(r.getStandardDeviation(), 2);
        
        double c = Math.sqrt(sum + totalPlayers*betaSquared);

        double winningMean = selfMeanSum;
        double losingMean = otherTeamMeanSum;

        switch (selfToOtherTeamComparison)
        {
            case WIN: case DRAW: /* NOP */ break;
            case LOSE:
                winningMean = otherTeamMeanSum;
                losingMean = selfMeanSum;
                break;
        }

        double meanDelta = winningMean - losingMean;

        double v;
        double w;
        double rankMultiplier;

        if (selfToOtherTeamComparison != PairwiseComparison.DRAW)
        {
            // non-draw case
            v = TruncatedGaussianCorrectionFunctions.VExceedsMargin(meanDelta, drawMargin, c);
            w = TruncatedGaussianCorrectionFunctions.WExceedsMargin(meanDelta, drawMargin, c);
            rankMultiplier = selfToOtherTeamComparison.multiplier;
        }
        else
        {
            // assume draw
            v = TruncatedGaussianCorrectionFunctions.VWithinMargin(meanDelta, drawMargin, c);
            w = TruncatedGaussianCorrectionFunctions.WWithinMargin(meanDelta, drawMargin, c);
            rankMultiplier = 1;
        }

        for(Entry<IPlayer, Rating> teamPlayerRatingPair : selfTeam.entrySet())
        {
            Rating previousPlayerRating = teamPlayerRatingPair.getValue();

            double meanMultiplier = (pow(previousPlayerRating.getStandardDeviation(), 2) + tauSquared)/c;
            double stdDevMultiplier = (pow(previousPlayerRating.getStandardDeviation(), 2) + tauSquared)/pow(c, 2);

            double playerMeanDelta = (rankMultiplier*meanMultiplier*v);
            double newMean = previousPlayerRating.getMean() + playerMeanDelta;

            double newStdDev =
                Math.sqrt((pow(previousPlayerRating.getStandardDeviation(), 2) + tauSquared)*(1 - w*stdDevMultiplier));

            newPlayerRatings.put(teamPlayerRatingPair.getKey(), new Rating(newMean, newStdDev));
        }
    }

    @Override
    public double calculateMatchQuality(@NotNull GameInfo gameInfo, Collection<ITeam> teams)
    {
        validateTeamCountAndPlayersCountPerTeam(teams);

        Iterator<ITeam> teamsIt = teams.iterator();
        
        // We've verified that there's just two teams
        Collection<Rating> team1 = teamsIt.next().values();
        int team1Count = team1.size();

        Collection<Rating> team2 = teamsIt.next().values();
        int team2Count = team2.size();

        int totalPlayers = team1Count + team2Count;

        double betaSquared = pow(gameInfo.getBeta(), 2);

        double team1MeanSum = 0;
        for (Rating r : team1) team1MeanSum += r.getMean();
        double team1StdDevSquared = 0;
        for (Rating r : team1) team1StdDevSquared += pow(r.getStandardDeviation(), 2);

        double team2MeanSum = 0;
        for (Rating r : team2) team2MeanSum += r.getMean();
        double team2SigmaSquared = 0;
        for (Rating r : team2) team2SigmaSquared += pow(r.getStandardDeviation(), 2);

        // This comes from equation 4.1 in the TrueSkill paper on page 8            
        // The equation was broken up into the part under the square root sign and 
        // the exponential part to make the code easier to read.

        double sqrtPart
            = Math.sqrt(
                (totalPlayers*betaSquared)
                /
                (totalPlayers*betaSquared + team1StdDevSquared + team2SigmaSquared)
                );

        double expPart
            = Math.exp(
                (-1*pow(team1MeanSum - team2MeanSum, 2))
                /
                (2*(totalPlayers*betaSquared + team1StdDevSquared + team2SigmaSquared))
                );

        return expPart*sqrtPart;
    }
}