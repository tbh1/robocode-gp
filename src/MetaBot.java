import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

public class MetaBot {

	// Static Variables //////////////////////////////////////////////////////
	final static 
		String PATH = new String("C:\\robocode\\robots\\sampleex");
		String PACKAGE = new String("sampleex");
		String JARS = new String("C:\\robocode\\libs\\robocode.jar;");
		
	final static int 
		NUM_CHROMOS = 5,
		MIN_DEPTH = RunGP.MIN_DEPTH,
		MAX_DEPTH = RunGP.MAX_DEPTH;
	
	final static double
		PROB_CROSS_ROOT = 0.3,
		PROB_CROSS_TERMINAL = 0.1,
		PROB_JUMP_GENOMES = 0.05,
		PROB_MUTATE_ROOT = 0.01,
		PROB_MUTATE_TERMINAL = 0.15;
	
	static Random random = new Random(System.currentTimeMillis());

	
	//Class Fields //////////////////////////////////////////////////////////
	
	int memberGen = 0, memberID = 0, nodeCount;
	
	String 
		botName = new String(),
		phenome[] = new String[NUM_CHROMOS],
		sourceCode = new String(),
		fileName;

	ExpressionNode genome[] = new ExpressionNode[NUM_CHROMOS];
	
	double fitness;
	

	// Class Methods /////////////////////////////////////////////////////////
	
	public MetaBot(int gen, int botID){
		memberGen = gen;
		memberID = botID;
		botName = "X_GPbot_"+memberGen+"_"+memberID;
		fileName = PACKAGE+"."+botName;
	}
	
	public void init(){
		for(int i = 0; i < NUM_CHROMOS; i++){
			genome[i] = new ExpressionNode(0);
			genome[i].grow(0, 0);
		}
	}
	
	public void construct(){
		for(int i = 0; i < NUM_CHROMOS; i++){
			phenome[i] = genome[i].compose();
			setCode();
		}
	}
	
	public int countNodes(){
		this.nodeCount = 0;
		for(int i = 0; i < genome.length; i++)
			nodeCount+=genome[i].countNodes();
		return nodeCount;
	}
	
	public void setDepths(){
		for(ExpressionNode exp : genome)
			exp.setDepths(0);
	}
	
	
	// Genetic Methods ////////////////////////////////////////////////////////////////////////
	
	public MetaBot crossover(MetaBot p2, int gen, int botID){
		MetaBot child = new MetaBot(gen, botID);
		
		for(int i = 0; i < NUM_CHROMOS; i++){
			child.genome[i] = this.genome[i].clone();
		}
		//*****************************************************************
		int xChromo1 = random.nextInt(NUM_CHROMOS);
		int xChromo2 = random.nextInt(NUM_CHROMOS);
		while(xChromo2 == xChromo1) xChromo2 = random.nextInt(NUM_CHROMOS);
		
		if(random.nextDouble() < PROB_CROSS_ROOT){	// swap entire chromosome
			if(random.nextDouble() < PROB_JUMP_GENOMES){	// do not use the same chromosome
				child.genome[xChromo1].replaceWith(p2.genome[xChromo2]);
			}else	// swap same chromosome
				child.genome[xChromo1].replaceWith(p2.genome[xChromo1]);
		}else{	// use subtrees
			// determine if subtrees will be terminals or functions
			boolean useTerminal1 = (random.nextDouble() < PROB_CROSS_TERMINAL) ? true : false;
			boolean useTerminal2 = (random.nextDouble() < PROB_CROSS_TERMINAL) ? true : false;
			
			// select random subtrees of p2
			// cross-over the subtrees
			child.genome[xChromo1].insert(p2.genome[xChromo1].getSubTree(useTerminal1));
			child.genome[xChromo2].insert(p2.genome[xChromo2].getSubTree(useTerminal2));
			
		}
		
		child.setDepths();
		child.countNodes();
		return child;
	}
	
	public MetaBot mutate(int gen, int botID){
		MetaBot child = new MetaBot(gen, botID);
		
		for(int i = 0; i < NUM_CHROMOS; i++){
			child.genome[i] = this.genome[i].clone();
		}
		
		int m = random.nextInt(NUM_CHROMOS);
		
		if(random.nextDouble() < PROB_MUTATE_ROOT){	// mutate entire chromosome
			child.genome[m] = new ExpressionNode(0);
			child.genome[m].grow(0, 0);
		}
		
		else if(random.nextDouble() < PROB_MUTATE_TERMINAL){
			child.genome[m].mutateTerminal();
		}else{
			child.genome[m].mutateFunction();
		}
		child.setDepths();
		child.countNodes();
		return child;
	}
	
	public MetaBot replicate(int gen, int botID){
		MetaBot child = new MetaBot(gen, botID);

		for(int i = 0; i < NUM_CHROMOS; i++){
			child.genome[i] = new ExpressionNode(0);
			child.genome[i].replaceWith(this.genome[i]);
		}

		child.setDepths();
		child.nodeCount = this.nodeCount;
		return child;
	}	

	
	// FileIO Methods ///////////////////////////////////////////////////////////////////////////
	
	private void setCode(){
		sourceCode =
				"package "+PACKAGE+";" +
				"\nimport robocode.*;" +
				"\nimport robocode.util.Utils;" +
				"\nimport java.awt.Color;\n" +
				"\n" +		
				"\npublic class " + botName + " extends AdvancedRobot {" +
				"\n" +
				//"\n static double runVar1 = 0;" +
				//"\n static double runVar2 = 0;" +
				//"\n" +
				"\n	public void run() {" +
				"\n" +
				"\nsetAdjustGunForRobotTurn(true);" +
				//"\nsetAdjustRadarForGunTurn(true);" +
				"\n" +
				"\n		setColors(Color.red,Color.blue,Color.green);" +	
				"\n		while(true) {" +
				"\n			turnGunRight(Double.POSITIVE_INFINITY);" +
				//"\n			turnRight(runVar1);" +
				//"\n			setAhead(runVar2);" +
				"\n		}" +
				"\n" +	
				"\n	}" +
				"\n	public void onScannedRobot(ScannedRobotEvent e) {" +
				"\n" +
				"\n // --- PHENOME 1 ---" +
				"\n		setAhead(" + phenome[0] + ");" +
				"\n" +
				"\n // --- PHENOME 2 ---" +
				"\n		setTurnRight("+ phenome[1] +");"  +
				"\n" +
				"\n // --- PHENOME 3 ---" +
				"\n		setTurnGunRight("+ phenome[2] +");"  +
				"\n" +
				"\n // --- PHENOME 4 ---" +
				"\n		setTurnRadarRight("+ phenome[3] +");"  +
				"\n" +
				"\n // --- PHENOME 5 ---" +
				"\n		setFire("+ phenome[4] +");"  +
				//"\n}" +
				"\n" +
				//"\n // --- PHENOME 6,7 ---" +
				//"\n		runVar1 = " + phenome[5] + ";" +
				//"\n" +
				//"\n		runVar2 = " + phenome[6] + ";" +
				//"\n" +
				"\n	}" +
				"\n" +	
				/*
				"\npublic void onHitByBullet(HitByBulletEvent e) {" +
				"\n" +
				"\n // --- PHENOME 6 ---" +
				"\n		setTurnRadarRight("+ phenome[7] +");"  +
				"\n"
				"\n	}" +
				"\n" +
				"\npublic void onHitWall(HitWallEvent e) {" +
				"\n		back(20);" +
				"\n		setAhead("+ phenome[8] +");"  +
				"\n	}" +
				*/
				"\n}"
			;
	}
	
	
	/**
	 * Writes the Robot class source code to a .java file and compiles it
	 * @return the absolute path to the generated .class file
	 */
	String compile(){
		try{
			FileWriter fstream = new FileWriter(PATH+"\\"+botName+".java");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(sourceCode);
			out.close();
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
		// Compile code
		try {
			execute("javac -cp " + JARS + " " + PATH + "\\" + botName + ".java");
		}catch(Exception e){
			e.printStackTrace();
		}
		return (PATH+"\\"+botName+".class");
	}
	
	public static void execute(String command) throws Exception{
		Process process = Runtime.getRuntime().exec(command);
		printMsg(command + " stdout:", process.getInputStream());
		printMsg(command + " stderr:", process.getErrorStream());
		process.waitFor();
		if(process.exitValue() != 0)
			System.out.println(command + "exited with value " + process.exitValue());
	}
	
	private static void printMsg(String name, InputStream ins) throws Exception {
		String line = null;
		BufferedReader in = new BufferedReader(new InputStreamReader(ins));
		while((line = in.readLine()) != null){
			System.out.println(name + " " + line);
		}
	}
	
	public static void clearBots(int gen, int pop, int bestID){
		
		System.out.println("Deleting unused bot files");
		
		File oldJava, oldClass;
		
		for(int i = 0; i < pop; i++){
			if(i == bestID || gen == 0 && i < 10) continue;
			oldJava = new File(PATH+"\\"+"X_GPbot_"+gen+"_"+i+".java");
			oldClass = new File(PATH+"\\"+"X_GPbot_"+gen+"_"+i+".class");
			oldJava.delete();
			oldClass.delete();
		}
	}
	
}





