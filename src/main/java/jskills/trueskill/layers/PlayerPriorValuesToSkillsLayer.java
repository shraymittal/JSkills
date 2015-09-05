package jskills.trueskill.layers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jskills.IPlayer;
import jskills.ITeam;
import jskills.Rating;
import jskills.factorgraphs.DefaultVariable;
import jskills.factorgraphs.KeyedVariable;
import jskills.factorgraphs.Schedule;
import jskills.factorgraphs.ScheduleStep;
import jskills.factorgraphs.Variable;
import jskills.numerics.GaussianDistribution;
import jskills.trueskill.TrueSkillFactorGraph;
import jskills.trueskill.factors.GaussianPriorFactor;

import static java.lang.Math.pow;

// We intentionally have no Posterior schedule since the only purpose here is to 
public class PlayerPriorValuesToSkillsLayer extends
    TrueSkillFactorGraphLayer<DefaultVariable<GaussianDistribution>, 
                              GaussianPriorFactor,
                              KeyedVariable<IPlayer, GaussianDistribution>>
{
    private final Collection<ITeam> _Teams;

    public PlayerPriorValuesToSkillsLayer(TrueSkillFactorGraph parentGraph,
                                          Collection<ITeam> teams)
    {
        super(parentGraph);
        _Teams = teams;
    }

    @Override
    public void buildLayer()
    {
        for(ITeam currentTeam : _Teams)
        {
            @SuppressWarnings("Convert2Diamond") List<KeyedVariable<IPlayer, GaussianDistribution>> currentTeamSkills = new ArrayList<>();

            for(Entry<IPlayer, Rating> currentTeamPlayer : currentTeam.entrySet())
            {
                KeyedVariable<IPlayer, GaussianDistribution> playerSkill =
                    CreateSkillOutputVariable(currentTeamPlayer.getKey());
                addLayerFactor(CreatePriorFactor(currentTeamPlayer.getValue(), playerSkill));
                currentTeamSkills.add(playerSkill);
            }

            addOutputVariableGroup(currentTeamSkills);
        }
    }

    @Override
    public Schedule<GaussianDistribution> createPriorSchedule()
    {
        Collection<Schedule<GaussianDistribution>> schedules = getLocalFactors().stream().map(prior -> new ScheduleStep<>("Prior to Skill Step", prior, 0)).collect(Collectors.toList());
        return scheduleSequence(schedules, "All priors");
    }

    private GaussianPriorFactor CreatePriorFactor(Rating priorRating,
                                                  Variable<GaussianDistribution> skillsVariable)
    {
        return new GaussianPriorFactor(priorRating.getMean(),
                                       pow(priorRating.getStandardDeviation(), 2) +
                                       pow(getParentFactorGraph().getGameInfo().getDynamicsFactor(), 2), skillsVariable);
    }

    private KeyedVariable<IPlayer, GaussianDistribution> CreateSkillOutputVariable(IPlayer key)
    {
        //noinspection Convert2Diamond
        return new KeyedVariable<>(key, GaussianDistribution.UNIFORM, "%s's skill", key.toString());
    }
}