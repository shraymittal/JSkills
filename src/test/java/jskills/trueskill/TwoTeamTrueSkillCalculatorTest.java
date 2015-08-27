package jskills.trueskill;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class TwoTeamTrueSkillCalculatorTest {

    private TwoTeamTrueSkillCalculator calculator;

    @BeforeMethod
    public void setup() {
        calculator = new TwoTeamTrueSkillCalculator();
    }

    public void TestAllTwoPlayerScenarios() {
        // This calculator supports up to two teams with many players each
        TrueSkillCalculatorTests.TestAllTwoPlayerScenarios(calculator);
    }

    public void TestAllTwoTeamScenarios() {
        // This calculator supports up to two teams with many players each
        TrueSkillCalculatorTests.TestAllTwoTeamScenarios(calculator);
    }
}