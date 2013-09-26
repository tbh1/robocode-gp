import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * This class represents the main genetic algorithm.
 * It assumes that Robocode is installed at C:\robocode,
 * and writes all files to the "C:\robocode\robots\sampleex" directory.
 * The robocode.jar library must be included in the build path (C:\robocode\libs\robocode.jar by default)
 * 
 * @author Ted Hunter
 *
 */
public class RunGP {
	
	// all rivals to be trained against
	final static String[] rivalsBatch1 = {
		//"sample.SuperCrazy",
		//"sample.SuperTracker"
		//"sample.SuperTrackFire",
		"sample.SuperRamFire",
		//"ary.micro.Weak 1.2"
		//"sheldor.nano.Sabreur_1.1.1"
		//"sample.Sabreur"
		//"mld.LittleBlackBook_1.69e"
		//"mld.Moebius_2.9.3"
	};
	
	final static String[] rivalsBatch2 = {
		"sample.SuperRamfire"
	};
	
	final static int 
		POP_SIZE = 300,
		MAX_GENS = 400,
		MIN_DEPTH = 2,
		MAX_DEPTH = 7,
		ROUNDS = 25,
		TOURNY_SIZE = 6,
		BATTLE_HANDICAP = 20;
	static double 
		PROB_CROSSOVER = 0.85,
		PROB_REPLICATION = 0.05,
		PROB_MUTATION = 0.1,
		PROB_ARCHITECTURE = 0.0,
		
		PROB_INTERNAL_NODE = 0.9,
		PROB_ANY_NODE = 0.1,
		
		fitnesses[] = new double[POP_SIZE],
		slice[] = new double[POP_SIZE],
		totalFitness,
		avgFitness,
		allAvgFitnesses[] = new double[MAX_GENS],
		avgNumNodes[] = new double[MAX_GENS],
		avgNodeCount;
	
	static MetaBot 
		pool[] = new MetaBot[POP_SIZE],
		newPool[] = new MetaBot[POP_SIZE],
		candidates[] = new MetaBot[MAX_GENS],	// should probably store as String[] of file paths
		bestSoFar;
	
	static String botNames[] = new String[POP_SIZE];
	static int genCount = 0, best;
	static Random random;
	
	
	public static void main(String args[]){
		
		random = new Random(System.currentTimeMillis());
		
		bestSoFar = new MetaBot(-1, 0);
		bestSoFar.fitness = 0;
		
		System.out.println("Initializing population");
		initPool();
		compilePool();
		
	
		// -- EC loop 
		while(genCount < MAX_GENS){
			
			for(int i = 0; i < POP_SIZE; i++)
				botNames[i] = pool[i].fileName;
			
			scoreFitnessOnSet(rivalsBatch1);
			
			totalFitness = 0;
			avgFitness = 0;
			best = 0;
			avgNodeCount = 0;
			
			for(int i=0; i<POP_SIZE; i++){
				totalFitness += (pool[i].fitness = fitnesses[i]);
				if(pool[i].fitness > pool[best].fitness) best = i;
				avgNodeCount += pool[i].countNodes();
			}
			
			avgNumNodes[genCount] = (avgNodeCount /= POP_SIZE);
			
			avgFitness = totalFitness/POP_SIZE;
			allAvgFitnesses[genCount] = avgFitness;
			
			// store the best-in-generation
			candidates[genCount] = pool[best];
			if(pool[best].fitness > bestSoFar.fitness) bestSoFar = pool[best];
			
			System.out.println("\nROUND " + genCount
								+ "\nAvg. Fitness:\t" + avgFitness + "\t Avg # of nodes: "+avgNumNodes[genCount]
								+ "\nBest In Round:\t" + candidates[genCount].botName +" - "+ candidates[genCount].fitness 
								+ "\t# nodes " + candidates[genCount].nodeCount
								+ "\nBest So Far:\t" + bestSoFar.botName +" - "+ bestSoFar.fitness +"\n");
			
			storeRunData(genCount, avgFitness, pool[best].fitness, avgNodeCount, pool[best].nodeCount, pool[best].fileName);
			
			//if(++genCount == MAX_GENS) break;
			genCount++;
			// breed next generation
			System.out.println("In breeding stage");
			breedPool();

			
			// set newPool as pool, clear newPool
			pool = newPool;
			newPool = new MetaBot[POP_SIZE];
			
			compilePool();
			
			// delete all old files
			MetaBot.clearBots(genCount-1, POP_SIZE, candidates[genCount-1].memberID);
			
		}
		
		System.out.println("-------Second Round Complete!-------");
		for(int i= 0; i< genCount; i++){
			System.out.println("Round "+i+" average:\t"+allAvgFitnesses[i]);
		}for(int i=0; i< genCount; i++){
			System.out.println("Round "+i+" avg # nodes:\t"+avgNumNodes[i]);
		}
		
	}
	
	private static void initPool(){
		for(int i = 0; i < POP_SIZE; i++){
			pool[i] = new MetaBot(0, i);
			pool[i].init();
		}
	}

	private static void compilePool(){
		System.out.println("Compiling population");
		for(MetaBot bot : pool){
			bot.construct();
			bot.compile();
		}
	}
	
	private static void breedPool(){
		// replicate best in last round
		newPool[0] = candidates[genCount-1].replicate(genCount, 0);
		// replicate best so far
		newPool[1] = bestSoFar.replicate(genCount, 1);
		// breed next generation
		
		double geneticOporator;
		int newPop = 2;
		
		while(newPop < POP_SIZE){
			geneticOporator = random.nextDouble();
			if((geneticOporator -= PROB_CROSSOVER) <= 0){
				int p1 = tournamentSelect();
				int p2 = tournamentSelect();
				
				//System.out.println("Crossing over bots " +p1+ " & " +p2+" -> " +newPop);
				
				newPool[newPop] = pool[p1].crossover(pool[p2], genCount, newPop);
				//newPool[newPop] = pool[tournamentSelect()].crossover(pool[tournamentSelect()], genCount+1, newPop);
			}else if((geneticOporator -= PROB_MUTATION) <= 0){
				//System.out.println("Mutating bot");
				newPool[newPop] = pool[tournamentSelect()].mutate(genCount, newPop);
			}else{ 
				//System.out.println("Replicating Bot");
				newPool[newPop] = pool[tournamentSelect()].replicate(genCount, newPop);
			}
			newPop++;
		}
			        
	}
	
	private static void scoreFitnessOnSet(String[] sampleSet){
		// generate battle between member and opponents from samples package
		BattleRunner arena = new BattleRunner();
		fitnesses = arena.runBatchWithSamples(botNames, sampleSet, ROUNDS);
	}
	
	/*
	private static int rouletteSelect(){
		// divy up roulette selection probabilities 
		for(int i = 0; i < POP_SIZE; i++){
			slice[i] = fitnesses[i]/totalFitness;
		}
		
		double pie = totalFitness * random.nextDouble();
		
		for(int i = 0; i < POP_SIZE; i++)
			if((pie -= pool[i].fitness) <= 0)
				return i;
		// should never reach this
		System.out.println("Warning: roulette selection out of bounds by "+pie);
		return POP_SIZE-1;
	}
	*/
	
	public static int tournamentSelect(){
		int size = TOURNY_SIZE;
		int subPool[] = new int[size];
		for(int i = 0; i < size; i++)
			subPool[i] = random.nextInt(POP_SIZE);
		int best = subPool[0];
		for(int i = 1; i < size; i++)
			if(pool[subPool[i]].fitness > pool[best].fitness) best = subPool[i];
		return best;
	}
	
	public static void storeRunData(int round, double avgFit, double bestFit, double avgNode, int bestNode, String bestBotName){
		FileWriter dataStream;
		try {
			// store all info in single file
			dataStream = new FileWriter(MetaBot.PATH+"\\run_data.txt", true);
			dataStream.write(round+"\t"+avgFit+"\t"+bestFit+"\t"+avgNode+"\t"+bestNode+"\n");
			dataStream.close();
			
			// store each variable in its own file (for graphs)
			dataStream = new FileWriter(MetaBot.PATH+"\\run_data_avgFitness.txt", true);
			dataStream.write(avgFit+"\n");
			dataStream.close();
			
			dataStream = new FileWriter(MetaBot.PATH+"\\run_data_bestFitness.txt", true);
			dataStream.write(bestFit+"\n");
			dataStream.close();
			
			dataStream = new FileWriter(MetaBot.PATH+"\\run_data_avgNodes.txt", true);
			dataStream.write(avgNode+"\n");
			dataStream.close();
			
			dataStream = new FileWriter(MetaBot.PATH+"\\run_data_bestNodes.txt", true);
			dataStream.write(bestNode+"\n");
			dataStream.close();
			
			dataStream = new FileWriter(MetaBot.PATH+"\\run_data_candidates.txt", true);
			dataStream.write(bestBotName+"\n");
			dataStream.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	static String[] sampleBots = {
		"sample.TrackFire",
		"sample.VelociRobot",
		"sample.Walls",
		"sample.RamFire",
		"sample.SpinBot"
	};
	
	static String[] superSampleBots = {
		"sample.SuperTracker",
		"sample.SuperSpinBot",
		"sample.SuperWalls",
		"sample.SuperCrazy",
		"sample.SuperRamFire"
	};


}
