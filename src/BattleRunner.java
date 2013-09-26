import robocode.BattleResults;
import robocode.control.*;
import robocode.control.events.*;

public class BattleRunner {

	RobocodeEngine engine;
	BattlefieldSpecification battlefield;
	BattleObserver battleObserver;
	
	final static int BATTLE_HANDICAP = RunGP.BATTLE_HANDICAP;
	
	public BattleRunner(){
		engine = new RobocodeEngine(new java.io.File("C:/Robocode"));
		battleObserver = new BattleObserver();
		engine.addBattleListener(battleObserver);
		engine.setVisible(false);
		battlefield = new BattlefieldSpecification(800, 600);
	}
	
	public double[] runBatchWithSamples(String bots[], String samples[], int rounds){
		engine = new RobocodeEngine(new java.io.File("C:/Robocode"));
		double fitnesses[] = new double[bots.length];
		String bot, opponent;
		BattleResults[] results;
		
		System.out.println("Running battles against sample batch");
		for(int i = 0; i < bots.length; i++){
			double fitnessScore = 0;
			for(int j = 0; j < samples.length; j++){
				bot = bots[i];
				opponent = samples[j];
				
				RobotSpecification[] selectedBots = engine.getLocalRepository(bot+", "+opponent);
				BattleSpecification battleSpec = new BattleSpecification(rounds, battlefield, selectedBots);
				engine.runBattle(battleSpec, true);
				
				results = battleObserver.getResults();
				int myBot = (results[0].getTeamLeaderName().equals(bots[i]) ? 0 : 1);
				int opBot = (myBot ==1 ? 0 : 1);
				int botScore = results[myBot].getScore();
				
				double totalScore = botScore + results[opBot].getScore();
				double roundFitness = (botScore + BATTLE_HANDICAP)/(totalScore+BATTLE_HANDICAP);
				
				fitnessScore += roundFitness;
			}
			fitnesses[i] = fitnessScore / samples.length;	// take average of each round score

		}
		
		return fitnesses;
	}
	
	public double[] runBatchWithCoevolution(String bots[], int rounds){
		double fitnesses[] = new double[bots.length];
		return fitnesses;
	}
	
	
}

// based on example from Robocode Control API JavaDocs
class BattleObserver extends BattleAdaptor {
	
	robocode.BattleResults[] results;
	
	public void onBattleCompleted(BattleCompletedEvent e){
		results = e.getIndexedResults();
	}
	
	public void onBattleError(BattleErrorEvent e){
		System.out.println("Error running battle: " + e.getError());
	}
	
	public BattleResults[] getResults(){		
		return results;
	}
	
}