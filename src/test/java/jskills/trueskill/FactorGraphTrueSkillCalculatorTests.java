package jskills.trueskill;

import org.testng.annotations.*;

@Test
public class FactorGraphTrueSkillCalculatorTests {

    private FactorGraphTrueSkillCalculator calculator;
    
    @BeforeMethod
    public void setup() {
        calculator = new FactorGraphTrueSkillCalculator();
    }

    public void TestAllTwoTeamScenarios() {
        TrueSkillCalculatorTests.TestAllTwoTeamScenarios(calculator);
    }
    
    public void TestAllTwoPlayerScenarios() {
        TrueSkillCalculatorTests.TestAllTwoPlayerScenarios(calculator);
    }

    public void TestAllMultipleTeamScenarios() {
        TrueSkillCalculatorTests.TestAllMultipleTeamScenarios(calculator);
    }

    public void TestPartialPlayScenarios() {
        TrueSkillCalculatorTests.TestPartialPlayScenarios(calculator);
    }
}