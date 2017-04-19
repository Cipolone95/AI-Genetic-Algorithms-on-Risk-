package com.sillysoft.lux.agent;

import Genetic.Alg2.Fitness2;
import Genetic.Alg2.GeneticAlg2;
import Genetic.Alg2.Individual2;
import Genetic.Alg2.Population2;
import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.Date;

import java.util.Random;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GeneticAgent2 extends Pixie implements LuxAgent {
// This agent's ownerCode:

    protected int ID;
    // used by some genes as indication to expand.
    protected int expando;
    protected int expandTo;
    protected int countryToAttack;
    // Store some refs the board and to the country array
    protected Board board;
    protected Country[] countries;
    // It might be useful to have a random number generator
    protected Random rand;

    // This will contain the genes of our individual genetic agent.
    public Individual2 geneticAgent;
    private boolean[] ourConts; // whether we will spend efforts taking/holding
    // each continent

    public GeneticAgent2() {
        rand = new Random();
        geneticAgent = new Individual2();

    }

    public GeneticAgent2(Board myBoard, Country[] myCountries) {
        board = myBoard;
        countries = myCountries;
    }

    /**
     * Copy constructor.
     */
    public GeneticAgent2(GeneticAgent2 aGeneticAgent2) {
        this(aGeneticAgent2.getBoard(), aGeneticAgent2.getCountries());
        //no defensive copies are created here, since 
        //there are no mutable object fields (String is immutable)
    }

    /**
     * Alternative style for a copy constructor, using a static newInstance
     * method.
     */
    public static GeneticAgent2 newInstance(GeneticAgent2 aGeneticAgent2) {
        return new GeneticAgent2(aGeneticAgent2.getBoard(), aGeneticAgent2.getCountries());
    }

    public Board getBoard() {
        return board;
    }

    public Country[] getCountries() {
        return countries;
    }

    // Save references
    public void setPrefs(int newID, Board theboard) {
        ID = newID; // this is how we distinguish what countries we own
        board = theboard;
        countries = board.getCountries();
        System.out.println("SetPrefs");
        System.out.println(this);
    }

    public String name() {
        return "GeneticAgent";
    }

    public float version() {
        return 1.0f;
    }

    public String description() {
        return "GeneticAgent uses a genetic algorithm for the attack, fortification, and reinforcement phase of risk.";
    }

    /*
	 * Picks country for genetic agent.
     */
    public int pickCountry() {
        // our first choice is the continent with the least # of borders that is
        // totally empty

        if (goalCont == -1 || !BoardHelper.playerOwnsContinentCountry(-1, goalCont, countries)) {
            setGoalToLeastBordersCont();
        }

        // so now we have picked a cont...
        return pickCountryInContinent(goalCont);
    }

    /*
	 * Place initial armies is part of the deploy phase in the individuals
	 * chromosome It is the first byte of the array. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.SmartAgentBase#placeInitialArmies(int)
     */
    public void placeInitialArmies(int numberOfArmies) {
        placeArmies(numberOfArmies);
    }

    public void cardsPhase(Card[] cards) {
        cashCardsIfPossible(cards);
    }

    /**
     * Takes in an individual and calculates its fitness dealing
     * with the number of countries this individual owns verses the number of countries
     * the enemy owns.
     */
    public int territoryScore(GeneticAgent2 ind) {
        //System.out.println("Begin Territory Vantage Score");
        int territoryScore = ind.countries.length;
        //holds the number of enemy neighbors next to the country "us"
        int numEnemyNeighbors = 0;
        int count = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {
            Country us = own.next();
            numEnemyNeighbors = us.getNumberEnemyNeighbors();
            // territoryScore will be less then one 
            if (numEnemyNeighbors != 0 ) {
                if (1 - numEnemyNeighbors >= 0) {
                    territoryScore = territoryScore + (1 - numEnemyNeighbors);

                }
            } else {
                //territoryScore = territoryScore + (1 - numEnemyNeighbors);
            	//bad to place armies in a country surrounded by friendly countries.
            	territoryScore = -10;
            }
            count++;
        }
        //System.out.println("Final Territory Vantage Score is :" + territoryScore);
        return territoryScore;
    }

    /**
     * Takes in an individual and calculates the fitness dealing with
     * the armies owned by this individual verses the armies owned by the enemy.
     */
    public int armyVantageScore(GeneticAgent2 ind) {
        //System.out.println("Begin Army Vantage Score");
        int armyVantageScore = ind.countries.length;

        //holds the number of enemy neighbors next to the country "us"
        int myArmies = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {
            int numEnemyArmies = 0;
            Country us = own.next();
            //get my armies on this country "us'
            myArmies = us.getArmies();
            //System.out.println("myArmies " + myArmies);
            // Get an array of countries touching "us"
            Country[] nextToMe = us.getAdjoiningList();
            // Loop through the countries next to me. Hopefully starting at index zero
            for (int i = 0; i < nextToMe.length; i++) {
                // Make sure that the nextToMe country isn't mine
                if (nextToMe[i].getOwner() != ind.ID) {
                    // If it isn't mine get the armies that from country[i
                    //System.out.println("Us is  " + us.getName());
                    //System.out.println("Next to  " + nextToMe[i].getName());
                    //System.out.println("Armies next to  " + nextToMe[i].getArmies());
                    numEnemyArmies += nextToMe[i].getArmies();
                    //System.out.println("numEnemyArmies " + numEnemyArmies);

                }
            }
            if (numEnemyArmies != 0) {
                // if myArmies/numEnemyNeighbors is less than 1 that means my enemies have more armies 
                // on adjoining countries and my fitness is bad
                // if myArmies/numEnemyNeighbors is great than 1 that means my enemies have less armies 
                // on adjoining countries and my fitness is good
                int solution = (myArmies - numEnemyArmies);
                //System.out.println("Solution" + solution);
                if (solution >= 0) {
                    armyVantageScore = solution;
                } else {
                    // Solution is negative so it will subtract even though it is adding
                    armyVantageScore = 0;
                }
            }
        }
        //System.out.println("Final Army Vantage Score is :" + armyVantageScore);
        return armyVantageScore;
    }

    /**
     * 
     */
    public int getDeployFitness(GeneticAgent2 ind) {
        return armyVantageScore(ind) + territoryScore(ind);
    }

    /*
	 * Place armies based on deploy phases first gene Gene uses only up to 0x07
	 * so 0000 0111 over 7 is unused. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#placeArmies(int)
     */
    public void placeArmies(int numberOfArmies) {
        /*byte[] deployGene = new byte[1];
        //holds the terrioty advantage score
        int totalVantageScore = getDeployFitness(this.geneticAgent);
        Byte byteScore = Byte.valueOf(Integer.toString(totalVantageScore));
        geneticAgent.setGene(0, byteScore);
         */
        Population2 genPop = new Population2(25, true);
        for (int j = 0; j < 1; j++) {
            for (int i = 0; i < genPop.size(); i++) {
                //GeneticAgent2 tempAgent = new GeneticAgent2(board,countries);
                // placeInitialArmies is based of the first gene in the byte array
                // for the deploy phase.
                Individual2 ind = genPop.getIndividual(i);
                ind.genAgent = this;
                //   places on random countries.
                int test;
                do {
                    test = rand.nextInt(ind.genAgent.countries.length);
                } while (ind.genAgent.countries[test].getOwner() != ind.genAgent.ID || ind.genAgent.countries[test].getWeakestEnemyNeighbor() == null);

                ind.genAgent.board.placeArmies(numberOfArmies, test);
                ind.wantTo = test;
                //System.out.println("Getting Fitness Score for ind " + i + " and is " + j + " generation");
                //System.out.println("Country place " + countries[test].getName());
                int totalVantageScoreForInd = getDeployFitness(ind.genAgent);
                //System.out.println("Fitness Score for ind " + i + " and is " + j + " generation");
                //System.out.println("Total " + totalVantageScoreForInd);
                //System.out.println(totalVantageScoreForInd);
                String whatever = Integer.toBinaryString(totalVantageScoreForInd);
                //System.out.println("Whatever " + whatever);
                //ind.setGene(0, totalVantageScoreForInd);
            }
            genPop = GeneticAlg2.evolvePopulation(genPop);
        }
        Individual2 temp = genPop.getFittest();
        //int totalVantageScoreForInd = getDeployFitness(temp);
        //System.out.println("Recieved fittest");
        //System.out.println(temp.getFitness());
        //System.out.println(temp);
        this.board.placeArmies(numberOfArmies, temp.wantTo);

    }

    /**
     * used by gene in attack phase. We pick expando as the country we own that
     * has the weakest enemy country beside it.
     */
    protected void setExpandos() {
        int leastNeighborArmies = 1000000;
        expando = -1;
        expandTo = -1;

        for (int i = 0; i < board.getNumberOfCountries(); i++) {
            if (countries[i].getOwner() == ID) {
                // This means this COULD be expando.

                // Get country[i]'s neighbors:
                Country[] neighbors = countries[i].getAdjoiningList();

                // Now loop through the neighbors and find the weakest:
                for (int j = 0; j < neighbors.length; j++) {
                    if (neighbors[j].getOwner() != ID && neighbors[j].getArmies() < leastNeighborArmies) {
                        if (!board.getAgentName(neighbors[j].getOwner()).equals(name())) // don't attack other commies, until all the running
                        // dogs are dead
                        {
                            leastNeighborArmies = neighbors[j].getArmies();
                            expando = i;
                            expandTo = neighbors[j].getCode();
                        }
                    }
                }
            }
        }
    }

    /**
     * used by gene in attackphase.
     */
    public boolean attackPhase(boolean careAboutOdds) {
        boolean attacked = false;
        CountryIterator ours = new PlayerIterator(ID, countries);
        while (ours.hasNext()) {
            Country us = ours.next();
            Country weak = us.getWeakestEnemyNeighbor();

            if (weak != null && us.getArmies() > 1 && (us.getArmies() > weak.getArmies() * 1.5 || !careAboutOdds)) {
                board.attack(us, weak, true);
                attacked = true;
            }
        }
        return attacked;
    }

    /**
     * used by gene during attackphase
     */
    protected void attackFromCluster(Country root) {
        // now run some attack methods for the cluster centered around root:
        if (root != null) {
            // expand as long as possible the easiest ways
            while (attackEasyExpand(root)) {
            }

            attackFillOut(root);

            while (attackEasyExpand(root)) {
            }

            while (attackConsolidate(root)) {
            }

            while (attackSplitUp(root, 1.2f)) {
            }
        }
    }

    /**
     * used by an attack phase gene. A check to see if someone else owns this
     * continent. If they do then we try to kill it
     */
    protected void takeOutContinentCheck(int cont) {
        if (BoardHelper.anyPlayerOwnsContinent(cont, countries)) {
            if (countries[BoardHelper.getCountryInContinent(cont, countries)].getOwner() != ID) {
                debug("enemy owns continent " + cont);
                // then an enemy owns this continent.
                // Check all of it's borders for a weak spot
                int[] borders = BoardHelper.getContinentBorders(cont, countries);
                for (int b = 0; b < borders.length; b++) {
                    Country[] neigbors = countries[borders[b]].getAdjoiningList();
                    for (int n = 0; n < neigbors.length; n++) {
                        if (neigbors[n].getOwner() == ID && neigbors[n].getArmies() > countries[borders[b]].getArmies()
                                && neigbors[n].canGoto(countries[borders[b]])) {
                            // kill him
                            debug("attacking to take out continent " + cont);
                            if (board.attack(neigbors[n], countries[borders[b]], true) > 0) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * used by attack phase gene Checks to see if any player can be easily taken
     * out of the game.
     */
    protected void takeOutPlayerCheck(int player) {
        if (BoardHelper.getPlayerArmies(ID, countries) > 5 * BoardHelper.getPlayerArmies(player, countries)) {
            // we outnumber them 10:1. Kill what we can
            debug("try to eliminate player " + player);
            for (int i = 0; i < countries.length; i++) {
                if (countries[i].getOwner() == player) {
                    Country[] list = countries[i].getAdjoiningList();
                    for (int l = 0; l < list.length && (countries[i].getOwner() == player); l++) {
                        if (list[l].getOwner() == ID && list[l].getArmies() > 1) {
                            if (list[l].canGoto(i)) { // this 'if' statement
                                // should only return
                                // false when single
                                // direction borders are
                                // on the map
                                board.attack(list[l], countries[i], true);
                            }
                        }
                    }
                }
            }
        }
    }

    //attackFitnessMethodGoesHere
    public int attackFitness(Individual2 ind) {
        //iterate through enemy countries next to us...find country with lowest armies and attack it
        CountryIterator own = new PlayerIterator(ID, countries);

        int maxArmyDifference = 0;
        int count = 0;
        while (own.hasNext()) {
            System.out.println("Next");
            Country us = own.next();
            // Get an array of countries touching "us"
            Country[] nextToMe = us.getAdjoiningList();
            for (int i = 0; i < nextToMe.length; i++) {
                Country neighbor = nextToMe[i];
                if (neighbor.getOwner() != us.getOwner()) {
                    int armyDifference = us.getArmies() - neighbor.getArmies();
                    if (armyDifference > maxArmyDifference) {
                        maxArmyDifference = armyDifference;
                        System.out.println("US" + us.getName() + us.getArmies() + " and them" + neighbor.getName() + neighbor.getArmies());
                        System.out.println("max army diff " + maxArmyDifference);
                        ind.genAgent.countryToAttack = neighbor.getCode();
                    }
                }
            }
            count++;
        }
        return maxArmyDifference;
    }

    /*
	 * Attack based on attack phases first gene Gene uses only up to 0x07 so
	 * 0000 0111 over 7 is unused. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#attackPhase()
     */
    public void attackPhase() {

        /*byte[] deployGene = new byte[1];
        //holds the terrioty advantage score
        int totalVantageScore = getDeployFitness(this.geneticAgent);
        Byte byteScore = Byte.valueOf(Integer.toString(totalVantageScore));
        geneticAgent.setGene(0, byteScore);
         */
        boolean attack = true;
        int count = 0;
        while (count < 10) {
            System.out.println("Attacking " + count + " times!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                    + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                    + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                    + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            Population2 genPop = new Population2(5, true);
            Individual2 ind = new Individual2();
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < genPop.size(); i++) {
                    ind = genPop.getIndividual(i);
                    ind.genAgent = this;
                    //System.out.println("Getting Fitness Score for ind " + i + " and is " + j + " generation");
                    int bestCountryToAttack = attackFitness(ind);
                    
                    //byte can only hold so much
                    //if over max then go for attack!
                    try{
                    	Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToAttack));
                    	ind.setGene(1, byteScoreForInd);
                    }catch(NumberFormatException E){
                    	ind.setGene(1, (byte)127);
                    }
                    
                }
                genPop = GeneticAlg2.evolvePopulation(genPop);
            }
            Individual2 temp = genPop.getFittest();
            temp.genAgent = this;
            //System.out.println("Cond for attack" + (temp.getGene(1) > 1));

            CountryIterator own = new PlayerIterator(ID, countries);

            boolean looking = true;
            Country cca = null;
            Country ccd = null;
            while (looking && own.hasNext()) {
                //System.out.println("Iterating");
                Country us = own.next();
                // Get an array of countries touching "us"
                if (us.getArmies() != 1) {
                    Country[] nextToMe = us.getAdjoiningList();
                    for (int i = 0; i < nextToMe.length; i++) {
                        Country neighbor = nextToMe[i];
                        System.out.println("Neig code " + neighbor.getCode() + " Country name is " + neighbor.getName());
                        System.out.println("Temp agent attack id " + temp.genAgent.countryToAttack);
                        System.out.println("Neighbor id" + neighbor.getOwner());
                        System.out.println("us ownder + " + us.getOwner() + " with country name " + us.getName());
                        if (neighbor.getCode() == temp.genAgent.countryToAttack && neighbor.getOwner() != us.getOwner()) {
                            cca = us;
                            ccd = neighbor;
                            System.out.println("us " + cca);
                            System.out.println("them " + ccd);
                            looking = false;
                        }
                    }
                }
            }
            if (cca != null || ccd != null) {
                startAttack(cca, ccd);
            }
            count++;
        }
    }

    public void startAttack(Country us, Country neighbor) {
        board.attack(us, neighbor, true);
    }

    public int moveArmiesFitness(int numArmies, int countryId, Individual2 ind) {
        System.out.println("Begin Move armies Score");

        int moveArmiesScore = 0;
        //holds the number of enemy neighbors next to the country "us"
        int myArmies = 0;
        int numEnemyArmies = 0;
        Country us = ind.genAgent.countries[countryId];
        //get my armies on this country "us'
        myArmies = numArmies;
        //System.out.println("myArmies " + myArmies);
        // Get an array of countries touching "us"
        Country[] nextToMe = us.getAdjoiningList();
        // Loop through the countries next to me. Hopefully starting at index zero
        for (int i = 0; i < nextToMe.length; i++) {
            // Make sure that the nextToMe country isn't mine
            if (nextToMe[i].getOwner() != ind.genAgent.ID) {
                // If it isn't mine get the armies that from country[i
                //System.out.println("Us is  " + us.getName());
                //System.out.println("Next to  " + nextToMe[i].getName());
                //System.out.println("Armies next to  " + nextToMe[i].getArmies());
                numEnemyArmies += nextToMe[i].getArmies();
                //System.out.println("numEnemyArmies " + numEnemyArmies);

            }
            if (numEnemyArmies != 0) {
                // if myArmies/numEnemyNeighbors is less than 1 that means my enemies have more armies 
                // on adjoining countries and my fitness is bad
                // if myArmies/numEnemyNeighbors is great than 1 that means my enemies have less armies 
                // on adjoining countries and my fitness is good
                int solution = (myArmies - numEnemyArmies);
                System.out.println("Solution on line 540 in GA2 " + solution);
                if (solution >= 0) {
                    moveArmiesScore = moveArmiesScore + solution;
                } else {
                    // Solution is negative so it will subtract even though it is adding
                    moveArmiesScore = moveArmiesScore + solution;
                }
                
            }
        }
        System.out.println("Final Army Vantage Score is :" + moveArmiesScore);
        return moveArmiesScore;
    }

    /*
	 * 2nd part of the attack phase. Uses second gene from the attack phase.
	 * only uses up to 0x04 bits from the gene. This method deals with moving in
	 * armies after taking over a territory. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#moveArmiesIn(int, int)
     */
    public int moveArmiesIn(int cca, int ccd) {

        Population2 genPop = new Population2(100, true);
        Individual2 ind = new Individual2();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < genPop.size(); i++) {
                ind = genPop.getIndividual(i);
                ind.genAgent = this;
                System.out.println("Individual :" + i + " " + ind);
                //   placed on countries we already own
                int test; //Do we need this variable? It doesn't do anything 

                
                //TESTING THINGS HERE
                //int numOfArmiesToPlace = rand.nextInt(ind.genAgent.countries[cca].getArmies());
                int totalArmiesInCountry = ind.genAgent.countries[cca].getArmies();
                int numOfArmiesToPlace = totalArmiesInCountry - 1; 

                System.out.println("Getting Fitness Score for ind " + i + " and is " + j + " generation");
                int bestCountryToMoveArmies = moveArmiesFitness(countries[cca].getArmies(), cca, ind);
                //byte can only hold so much
                //if over max then go for move in!
                try{
                	Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToMoveArmies));
                	Byte byteNumOfArmies = Byte.valueOf(Integer.toString(numOfArmiesToPlace)); //Test work 
                	ind.setGene(1, byteScoreForInd);
                	ind.setGene(2, byteNumOfArmies); //was originally byteScoreForInd
                }catch(NumberFormatException E){
                	ind.setGene(1, (byte)127);
                	ind.setGene(2, (byte)127);
                	
                }
                
            }
            genPop = GeneticAlg2.evolvePopulation(genPop);
        }
        Individual2 temp = genPop.getFittest();
        Byte byteNumArmies = temp.getGene(2);
        System.out.println("placing this many:"+byteNumArmies);
        // this should never be reached
        System.out.println("Byte is " + byteNumArmies.intValue());
        return byteNumArmies.intValue();
    }

    public int fortifyFitness(int countryID) {

    	int countryCodeBestProspect = -1;
            if (countries[countryID].getOwner() == ID && countries[countryID].getMoveableArmies() > 0) {
                // This means we've found a country of ours that we can move
                // from if we want to.

                // We determine the best country by counting the enemy
                // neighbors it has.
                // The most enemy neighbors is where we move. Also, if there
                // are 0 enemy
                // neighbors where the armies are on now, we move to a
                // random neighbor (in
                // the hopes we'll find an enemy eventually).
                // To cycle through the neighbors we could use a
                // NeighborIterator,
                // but we can also directly use the country's AdjoingingList
                // array.
                // Let's use the array...
                Country[] neighbors = countries[countryID].getAdjoiningList();
                int bestEnemyNeighbors = 0;
                int enemyNeighbors = 0;

                for (int j = 0; j < neighbors.length; j++) {
                    if (neighbors[j].getOwner() == ID) {
                        enemyNeighbors = neighbors[j].getNumberEnemyNeighbors();

                        if (enemyNeighbors > bestEnemyNeighbors) {
                            // Then so far this is the best country to move
                            // to:
                            countryCodeBestProspect = neighbors[j].getCode();
                            bestEnemyNeighbors = enemyNeighbors;
                        }
                    }
                }

            }
            System.out.println(countryCodeBestProspect);
       return countryCodeBestProspect;
    }
            /*
	 * The fortifyPhase will be based off of the fortify 1st gene in the
	 * individuals chromosome. The fortifyphase is used for the player to
	 * fortify themselves and prepare for the next turn.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#fortifyPhase()
             */
    public void fortifyPhase() {

        Population2 genPop = new Population2(100, true);
        Individual2 ind = new Individual2();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < genPop.size(); i++) {
                ind = genPop.getIndividual(i);
                ind.genAgent = this;
                System.out.println("Individual :" + i + " " + ind);
                //   placed on countries we already own

                int randCountry = rand.nextInt(ind.genAgent.countries.length);

                System.out.println("Getting Fitness Score for ind " + i + " and is " + j + " generation");
                int bestCountryToFortify = fortifyFitness(randCountry);
                Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToFortify));
                ind.setGene(3, byteScoreForInd);
            }
            genPop = GeneticAlg2.evolvePopulation(genPop);
        }
        Individual2 temp = genPop.getFittest();
		Byte bestEnemyNeighbors = temp.getGene(3);

		// Now let's calculate the number of enemies of the country
		// where the armies
		// already are, to see if they should stay here:
			Country[] neighbors = countries[bestEnemyNeighbors].getAdjoiningList();
			for (int i = 0; i < neighbors.length; i++) {
				int enemyNeighbors = countries[i].getNumberEnemyNeighbors();

				// If there's a better country to move to, move:
				if (bestEnemyNeighbors > enemyNeighbors) {
					// Then the armies should move:
					// So now the country that had the best ratio should be
					// moved to:
					board.fortifyArmies(countries[i].getMoveableArmies(), (int) bestEnemyNeighbors,
							neighbors[i].getCode());
				} // If there are no good places to move to, move to a random
					// place:
				else if (enemyNeighbors == 0) {
					// We choose an int from [0, neighbors.length]:
					int randCC = rand.nextInt(neighbors.length);
					board.fortifyArmies(countries[i].getMoveableArmies(), i, neighbors[randCC].getCode());

				}
			}
	}

    public String youWon() {
        // For variety we store a bunch of answers and pick one at random to
        // return.
        String[] answers = new String[]{"EVOLUTION IS KING", "BLOOD FOR THE BLOOD GOD"};

        return answers[rand.nextInt(answers.length)];
    }

    public String message(String message, Object data) {
        return null;
    }

}