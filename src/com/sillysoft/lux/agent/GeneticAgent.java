package com.sillysoft.lux.agent;

import com.sillysoft.lux.agent.SmartAgentBase;
import Genetic.Alg.Fitness;
import Genetic.Alg.GeneticAlg;
import Genetic.Alg.Individual;
import Genetic.Alg.Population;
import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

import java.util.ArrayList;
import java.util.Date;

import java.util.Random;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GeneticAgent extends SmartAgentBase implements LuxAgent {
// This agent's ownerCode:

    protected int ID;
    // used by some genes as indication to expand.
    protected int expando;
    protected int expandTo;
    // Store some refs the board and to the country array
    protected Board board;
    protected Country[] countries;
    // It might be useful to have a random number generator
    protected Random rand;

    // This will contain the genes of our individual genetic agent.
    public Individual geneticAgent;
    private boolean[] ourConts; // whether we will spend efforts taking/holding
    // each continent
    boolean PlaceToAttack = false;
    protected int borderForce = 20;
    
    public GeneticAgent() {
        rand = new Random();
        geneticAgent = new Individual();
        geneticAgent.generateIndividual();

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

    public int territoryScore(GeneticAgent ind) {
        int territoryScore = 0;
        //holds the number of enemy neighbors next to the country "us"
        int numEnemyNeighbors = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {

            Country us = own.next();
            numEnemyNeighbors = us.getNumberEnemyNeighbors();
            // territoryScore will be less then one 
            if (numEnemyNeighbors != 0) {
                if (Math.round(1 / numEnemyNeighbors) == 1) {
                    territoryScore++;
                }
            } else {
                territoryScore++;
            }
        }
        return territoryScore;
    }

    public int armyVantageScore(GeneticAgent ind) {
        int armyVantageScore = 0;
        //holds the number of enemy neighbors next to the country "us"
        int numEnemyArmies = 0;
        int myArmies = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {

            Country us = own.next();
            //get my armies on this country "us'
            myArmies = us.getArmies();
            // Get an array of counries touching "us"
            Country[] nextToMe = us.getAdjoiningList();
            // Loop through the countries next to me. Hoppfully starting at index zero
            for (int i = 0; i < nextToMe.length; i++) {
                // Make sure that the nextToMe country isnt mine
                if (nextToMe[i].getOwner() != ind.ID) {
                    // If it isn't mine get the armies that from country[i]
                    numEnemyArmies += BoardHelper.getPlayerArmies(nextToMe[i].getOwner(), nextToMe);
                }
            }
            if (numEnemyArmies != 0) {
                // if myArmies/numEnemyNeighbors is less than 1 that means my enemies have more armies 
                // on adjoining countries and my fitness is bad
                // if myArmies/numEnemyNeighbors is great than 1 that means my enemies have less armies 
                // on adjoining countries and my fitness is good
                if (myArmies / numEnemyArmies > 1) {
                    armyVantageScore++;
                }
            } else {
                armyVantageScore++;
            }
        }
        return armyVantageScore;
    }

    public int getDeployFitness(GeneticAgent ind) {
        return armyVantageScore(ind) + territoryScore(ind);
    }

    
 // This method is a hook that EvilPixie uses to place better during hogwild
    boolean placeHogWild(int numberOfArmies)
    	{
    	return false;
    	}

 // returns true if we want at least one continent
    boolean setupOurConts(int numberOfArmies)
    	{
    	if (ourConts == null)
    		ourConts = new boolean[numContinents];

    	// calculate the armies needed to conquer each continent
    	int[] neededForCont = new int[numContinents];
    	for (int i = 0; i < numContinents; i++)
    		{
    		neededForCont[i] = BoardHelper.getEnemyArmiesInContinent(ID, i, countries);	// enemies in the cont
    		neededForCont[i] -= BoardHelper.getPlayerArmiesInContinent(ID, i, countries);	// minus our armies in the cont
    		// also minus our armies in countries neighboring the cont
    		neededForCont[i] -= BoardHelper.getPlayerArmiesAdjoiningContinent(ID, i, countries);
    		}

    	// say we can give at most (1/numContinents)*numberOfArmies armies to each continent.
    	boolean wantACont = false; 	// if we think we can take/hold any continents
    	for (int i = 0; i < numContinents; i++)
    		{
    		if (neededForCont[i] < (1.0/numContinents)*numberOfArmies && board.getContinentBonus(i) > 0)
    			{
    			ourConts[i] = true;
    			wantACont = true;
    			}
    		else
    			ourConts[i] = false;
    		}
    	return wantACont;
    	}
 
    boolean borderCountryNeedsHelp(Country border)
	{
	return border.getArmies() <= borderForce;
	}  
    
 // a test of whether or not we should send some armies this cont's way
    protected boolean continentNeedsHelp(int cont)
    	{
    	// if we don't own it then it deffinately needs some help
    	if (! BoardHelper.playerOwnsContinent(ID, cont, countries) )
    		return true;

    	// otherwise we own it.
    	// check each border
    	int[] borders = BoardHelper.getContinentBorders(cont, countries);
    	for (int i = 0; i < borders.length; i++)
    		{
    		if (borderCountryNeedsHelp(countries[ borders[i] ]))
    			return true;
    		}

    	return false;
    	}
    
    protected void placeRemainder(int numberOfArmies)
	{
    	placeNearEnemies(numberOfArmies);
	}

 // place evenly amongst our countries that have enemies
    protected void placeNearEnemies(int numberOfArmies)
    	{
    	int i = 0;
    	while (numberOfArmies > 0)
    		{
    		if (countries[i].getOwner() == ID && countries[i].getNumberEnemyNeighbors() > 0)
    			{
    			board.placeArmies(1, i);
    			numberOfArmies--;
    			}
    		i = (i+1)%numCountries;
    		}
    	}

    /*
	 * Place armies based on deploy phases first gene Gene uses only up to 0x07
	 * so 0000 0111 over 7 is unused. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#placeArmies(int)
     */
    public void placeArmies(int numberOfArmies) {
        System.out.println("SetPrefs");
        /*byte[] deployGene = new byte[1];
        //holds the terrioty advantage score
        int totalVantageScore = getDeployFitness(this);
        Byte byteScore = Byte.valueOf(Integer.toString(totalVantageScore));
        geneticAgent.setGene(0, byteScore);
        */
       // Population genPop = new Population(10, true);
        //for (int j = 0; j < 3; j++) {
          //  for (int i = 0; i < genPop.size(); i++) {
                // placeInitialArmies is based of the first gene in the byte array
                // for the deploy phase.
               // GeneticAgent ind = genPop.getIndividual(i);
               // ind.countries = this.countries;
              //  ind.board = this.board;
                

                byte deployArmies = (this.geneticAgent.getPhase("deploy"))[0];
                //byte deployArmies = 0X01;
                // Armies where they can attack the most countries.
                if (deployArmies == 0x01) {
                    int mostEnemies = -1;
                    Country placeOn = null;
                    int subTotalEnemies = 0;
                    CountryIterator neighbors = null;

                    // Use a PlayerIterator to cycle through all the countries that we
                    // own.
                    CountryIterator own2 = new PlayerIterator(this.ID, this.countries);
                    while (own2.hasNext()) {
                        Country us = own2.next();
                        subTotalEnemies = us.getNumberEnemyNeighbors();

                        // If it's the best so far store it
                        if (subTotalEnemies > mostEnemies) {
                            mostEnemies = subTotalEnemies;
                            placeOn = us;
                        }
                    }

                    // So now placeOn is the country that we own with the most enemies.
                    // Tell the board to place all of our armies there
                    this.board.placeArmies(numberOfArmies, placeOn);
                } // Armies placed on weakest countries owned.
                else if (deployArmies == 0x02) {
                    int leftToPlace = numberOfArmies;
                    while (leftToPlace > 0) {
                        int leastArmies = 1000000;
                        CountryIterator ours = new PlayerIterator(this.ID, this.countries);
                        while (ours.hasNext() && leftToPlace > 0) {
                            Country us = ours.next();

                            leastArmies = Math.min(leastArmies, us.getArmies());
                        }

                        // Now place an army on anything with less or equal to
                        // <leastArmies>
                        CountryIterator placers = new ArmiesIterator(this.ID, -(leastArmies), this.countries);

                        while (placers.hasNext()) {
                            Country us = placers.next();
                            this.board.placeArmies(1, us);
                            leftToPlace -= 1;
                        }
                    }
                } // places on random countries.
                else if (deployArmies == 0x03) {
                    int test;
                    do {
                        test = rand.nextInt(this.countries.length);
                    } while (this.countries[test].getOwner() != this.ID || this.countries[test].getWeakestEnemyNeighbor() == null);

                    this.board.placeArmies(numberOfArmies, test);
                } // places armies in an even cluster.
                else if (deployArmies == 0x04) {
                    this.PlaceToAttack = true;
                    if (BoardHelper.playerOwnsAnyPositiveContinent(this.ID, this.countries, this.board)) {
                        // Center the cluster on the biggest continent we own
                        int ownCont = this.getMostValuablePositiveOwnedCont();
                        placeArmiesOnClusterBorder(numberOfArmies,
                                this.countries[BoardHelper.getCountryInContinent(ownCont, this.countries)]);
                    } else {
                        // Center the cluster on the easiest continent to take
                        int wantCont = this.getEasiestContToTake(); // getEasiestContToTake()
                        // is a SmartAgentBase
                        // method
                        this.placeArmiesToTakeCont(numberOfArmies, wantCont);
                    }
                } // looking for continents
                else if (deployArmies == 0x05) {
                    this.PlaceToAttack = true;
                    if (this.placeHogWild(numberOfArmies)) {
                        return;
                    }

                    if (!this.setupOurConts(numberOfArmies)) {
                        // then we don't think we can take/hold any continents
                        this.placeArmiesToTakeCont(numberOfArmies, getEasiestContToTake());
                        return;
                    }

                    // divide our armies amongst the conts we want
                    int armiesPlaced = 0;
                    boolean oneNeedsHelp = true;
                    while (armiesPlaced < numberOfArmies && oneNeedsHelp) {
                        oneNeedsHelp = false;
                        for (int c = 0; c < numContinents; c++) {
                            if (ourConts[c] && this.continentNeedsHelp(c)) {
                                this.placeArmiesToTakeCont(1, c);
                                armiesPlaced++;
                                oneNeedsHelp = true;
                            }
                        }
                    }

                    // We get here if all our borders are above borderforce.
                    this.placeRemainder(numberOfArmies - armiesPlaced);
                } // aggressive towards human player
                else if (deployArmies == 0x06) {
                    this.PlaceToAttack = true;
                    if (this.placeArmiesToKillDominantPlayer(numberOfArmies)) {
                        return;
                    }

                    if (BoardHelper.playerOwnsAnyPositiveContinent(this.ID, this.countries, this.board)) {
                        int ownCont = this.getMostValuablePositiveOwnedCont();
                        this.placeArmiesOnClusterBorder(numberOfArmies,
                                this.countries[BoardHelper.getCountryInContinent(ownCont, this.countries)]);
                    } else {
                        int wantCont = this.getEasiestContToTake();
                        this.placeArmiesToTakeCont(numberOfArmies, wantCont);
                    }
                } // place first to kill dominant player then place to get continents
                else if (deployArmies == 0x07) {
                    if (this.placeArmiesToKillDominantPlayer(numberOfArmies)) {
                        this.setupOurConts(0);
                        return;
                    }

                    placeArmies(numberOfArmies);
                }
              //  int totalVantageScoreForind = getDeployFitness(this);
               // Byte byteScoreForInd = Byte.valueOf(Integer.toString(totalVantageScoreForInd));
               // this.geneticAgent.setGene(0, byteScoreForInd);
            
           // genPop = GeneticAlg.evolvePopulation(genPop);
        
        
       // board = genPop.getFittest().board;
       // countries = genPop.getFittest().countries;
                
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

    
 // Execute all the attacks possible in this continent where we outnumber the enemy
    protected void attackInContinent( int cont )
    	{
    	float outnumberBy = 1;
    	CountryIterator continent = new ContinentIterator(cont, countries);
    	while (continent.hasNext())
    		{
    		Country c = continent.next();
    		if (c.getOwner() != ID)
    			{
    			// try and find a neighbor that we own, and attack this country
    			CountryIterator neighbors = new NeighborIterator(c);
    			while (neighbors.hasNext())
    				{
    				Country possAttack = neighbors.next();
    				if (possAttack.getOwner() == ID && possAttack.getArmies() > c.getArmies()*outnumberBy && c.getOwner() != ID && possAttack.canGoto(c))
    					{
    					board.attack(possAttack, c, true);
    					}
    				}
    			}
    		}
    	}

    
    /*
	 * Attack based on attack phases first gene Gene uses only up to 0x07 so
	 * 0000 0111 over 7 is unused. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#attackPhase()
     */
    public void attackPhase() {

        byte attack = (geneticAgent.getPhase("attack"))[0];
        //byte attack = 0x01;

        // attack as much as possible
        if (attack == 0x01) {
            // Keep cycling until we make no attacks
            boolean madeAttack = true;
            while (madeAttack) {
                madeAttack = false; // reset it. if we win an attack somewhere
                // we set it to true.

                // cycle through all of the countries that we have 2 or more
                // armies on
                CountryIterator armies = new ArmiesIterator(ID, 2, countries);
                while (armies.hasNext()) {
                    Country us = armies.next();

                    // Find its weakest neighbor
                    Country weakestNeighbor = us.getWeakestEnemyNeighbor();

                    // So we have found the best matchup for Country <us>. (if
                    // there are any enemies)
                    // will only attack if it is a good-chance of winning.
                    if (weakestNeighbor != null && us.getArmies() > weakestNeighbor.getArmies()) {
                        // will always attack till death.
                        board.attack(us, weakestNeighbor, true);

                        // Set madeAttack to true, so that we loop through all
                        // our armies again
                        madeAttack = true;
                    }
                }
            }
        } // look for weak countries to overtake.
        else if (attack == 0x02) {

            if (expando == -1) {
                return; // nowhere to go
            }
            // Now we see if we have a good chance of taking the weakest link
            // over:
            if (expandTo != -1 && countries[expando].getArmies() > countries[expandTo].getArmies()) {
                // We attack till dead, with max dice:
                board.attack(expando, expandTo, true);
            }

            attackHogWild();
            attackStalemate();
        } // attack amount depending on armies size.
        else if (attack == 0x03) {
            attackPhase(true);

            // If we have tons of armies then attack more
            if (BoardHelper.getPlayerArmies(ID, countries) > 300) {
                while (attackPhase(false)) {
                }
            }
        } // expand in cluster looking for easy expansions
        else if (attack == 0x04) {
            if (BoardHelper.playerOwnsAnyPositiveContinent(ID, countries, board)) {
                int ownCont = getMostValuablePositiveOwnedCont();
                Country root = countries[BoardHelper.getCountryInContinent(ownCont, countries)];
                attackFromCluster(root);
            } else {
                // get our biggest army group:
                Country root = BoardHelper.getPlayersBiggestArmy(ID, countries);
                attackFromCluster(root);
            }

            attackHogWild(); // this only does anything if we outnumber everyone
            attackStalemate();
        } // will attack and try to get a card
        else if (attack == 0x05) {
            for (int i = 0; i < numContinents; i++) {
                if (ourConts[i]) {
                    attackInContinent(i);
                }
            }

            attackForCard();
            attackHogWild();
            attackStalemate();
        } // attack to kill a player.
        else if (attack == 0x06) {
            if (mustKillPlayer != -1) {
                // do our best to take out this guy
                attackToKillPlayer(mustKillPlayer);
            }

            // but do other attacks also...
            if (BoardHelper.playerOwnsAnyPositiveContinent(ID, countries, board)) {
                int ownCont = getMostValuablePositiveOwnedCont();
                Country root = countries[BoardHelper.getCountryInContinent(ownCont, countries)];
                attackFromCluster(root);
            } else {
                // get our biggest army group:
                Country root = BoardHelper.getPlayersBiggestArmy(ID, countries);
                attackFromCluster(root);
            }

            attackForCard();
            attackHogWild();
            attackStalemate();
        } // looks for continent take over attacks
        else if (attack == 0x07) {
            if (mustKillPlayer != -1) {
                // do our best to take out this guy
                attackToKillPlayer(mustKillPlayer);
            }

            for (int i = 0; i < numContinents; i++) {
                if (ourConts[i]) {
                    attackInContinent(i);
                }
                takeOutContinentCheck(i);
            }

            int numPlayers = board.getNumberOfPlayers();
            for (int p = 0; p < numPlayers; p++) {
                if (p != ID) {
                    takeOutPlayerCheck(p);
                }
            }

            // Only attack for a card if we outnumber someone 5:1
            attackForCard(5);
            attackHogWild();
            attackStalemate();
        }
    }

    /*
	 * 2nd part of the attack phase. Uses second gene from the attack phase.
	 * only uses up to 0x04 bits from the gene. This method deals with moving in
	 * armies after taking over a territory. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#moveArmiesIn(int, int)
     */
    public int moveArmiesIn(int cca, int ccd) {
        byte reOccupy = (geneticAgent.getPhase("attack"))[1];

        if (reOccupy == 0x00) {
            // moves armies into countries with more enemies.
            int Aenemies = countries[cca].getNumberEnemyNeighbors();
            int Denemies = countries[ccd].getNumberEnemyNeighbors();

            // If the attacking country had more enemies, then we leave all
            // possible
            // armies in the country they attacked from (thus we move in 0):
            if (Aenemies > Denemies) {
                return 0;
            }

            // Otherwise the defending country has more neighboring enemies,
            // move in everyone:
            return countries[cca].getArmies() - 1;
        } // divides armies evenly between the two countries
        else if (reOccupy == 0x01) {
            int totalArmies = countries[cca].getArmies();

            return ((totalArmies + 1) / 2);
        } // looks to place armies next to weak countries
        else if (reOccupy == 0x02) {
            Country attackWeak = countries[cca].getWeakestEnemyNeighbor();
            Country defendWeak = countries[ccd].getWeakestEnemyNeighbor();

            if (attackWeak == null) {
                return 1000000;
            }
            if (defendWeak == null) {
                return 0;
            }

            if (attackWeak.getArmies() < defendWeak.getArmies()) {
                return 0;
            }

            return 1000000;
        } // cluster armies
        else if (reOccupy == 0x03) {
            int test = obviousMoveArmiesInTest(cca, ccd);
            if (test != -1) {
                return test;
            }

            test = memoryMoveArmiesInTest(cca, ccd);
            if (test != -1) {
                return test;
            }

            Country aweakest = countries[cca].getWeakestEnemyNeighborInContinent(goalCont);
            Country dweakest = countries[ccd].getWeakestEnemyNeighborInContinent(goalCont);

            if (dweakest == null || (aweakest != null && aweakest.getArmies() < dweakest.getArmies())) // attacking country has a weaker neighbor. leave armies there
            {
                return 0;
            }

            return 1000000;
        } // tries to move armies into a more advantages position
        else if (reOccupy == 0x04) {
            // test if they border any enemies at all:
            int attackerEnemies = countries[cca].getNumberEnemyNeighbors();
            int defenderEnemies = countries[ccd].getNumberEnemyNeighbors();

            if (attackerEnemies > defenderEnemies) {
                return 0;
            } else if (defenderEnemies > attackerEnemies) {
                return 1000000;
            } else if (attackerEnemies > 0) // then they are tied at above 0
            {
                return countries[cca].getArmies() / 2;
            }

            // move our armies into continents that we want.
            if (ourConts[countries[cca].getContinent()] && ourConts[countries[ccd].getContinent()]) { // we
                // want
                // both
                return countries[cca].getArmies() / 2;
            } else if (ourConts[countries[cca].getContinent()]) {
                return 0; // leave them in attacker
            } else if (ourConts[countries[ccd].getContinent()]) {
                return 1000000; // send to defender
            }

            // so now we want none of them
            // see if either of them border something we want
            int attackerEnemiesWanted = 0, defenderEnemiesWanted = 0;
            CountryIterator e = new NeighborIterator(countries[cca]);
            while (e.hasNext()) {
                Country test = e.next();
                if (test.getOwner() != ID && ourConts[test.getContinent()]) {
                    attackerEnemiesWanted++;
                }
            }
            e = new NeighborIterator(countries[ccd]);
            while (e.hasNext()) {
                Country test = e.next();
                if (test.getOwner() != ID && ourConts[test.getContinent()]) {
                    defenderEnemiesWanted++;
                }
            }

            if (attackerEnemiesWanted > defenderEnemiesWanted) {
                return 0;
            } else if (defenderEnemiesWanted > attackerEnemiesWanted) {
                return 1000000;
            } else if (attackerEnemiesWanted > 0) // then they are tied at above 0
            {
                return countries[cca].getArmies() / 2;
            }

            // So if we get here then they both border zero countries that we
            // want.
            // Now if we are here then neither have any enemies.
            // we won't be able to use them to attack this turn
            // now just move in xxagentxx
            debug("Pixie moveArmiesIn not fully imped");
            return countries[cca].getArmies() / 2;
        }

        // this should never be reached
        return 0;
    }

    protected void fortifyContinent( int cont )
	{
	// We work from the borders back, fortifying closer.
	// Start out by getting a List of the cont's borders:
	int[] borders = BoardHelper.getContinentBorders(cont, countries);
	List cluster = new ArrayList();
	for (int i = 0; i < borders.length; i++) {
		cluster.add(countries[borders[i]]);
		}

	// So now the cluster borders are in <cluster>. fill it up while fortifying towards the borders.
	for (int i = 0; i < cluster.size(); i++) {
		CountryIterator neighbors = new NeighborIterator( (Country)cluster.get(i) );
		while ( neighbors.hasNext()) {
			Country neighbor = neighbors.next();
			if ( neighbor.getOwner() == ID && ! cluster.contains(neighbor) && neighbor.getContinent() == cont) {
				// Then <neighbor> is part of the cluster. fortify any armies back and add to the List
				board.fortifyArmies( neighbor.getMoveableArmies(), neighbor, (Country)cluster.get(i) );
				cluster.add(neighbor);
				}
			}
		}
	}

    
 // called on continents that we don't own.
 // fortify our guys towards weak enemy countries.
 protected void fortifyContinentScraps( int cont)
 	{
 	CountryIterator e = new ContinentIterator(cont, countries);
 	while (e.hasNext())
 		{
 		Country c = e.next();
 		if (c.getOwner() == ID && c.getMoveableArmies() > 0)
 			{
 			// we COULD move armies from 'c'
 			int weakestArmies = 1000000;
 			Country weakestLink = null;
 			// if it has a neighbor with a weaker enemy then move there
 			CountryIterator n = new NeighborIterator(c);
 			while (n.hasNext())
 				{
 				Country possMoveTo = n.next();
 				if (possMoveTo.getOwner() == ID)
 					{
 					Country themWeak = possMoveTo.getWeakestEnemyNeighbor();
 					if (themWeak != null && themWeak.getArmies() < weakestArmies)
 						{
 						weakestArmies = possMoveTo.getWeakestEnemyNeighbor().getArmies();
 						weakestLink = possMoveTo;
 						}
 					}
 				}
 			Country hereWeakest = c.getWeakestEnemyNeighbor();
 			// if a neighbor has a weaker country then we do here move our armies
 			if (hereWeakest == null || weakestArmies < hereWeakest.getArmies())
 				{
 				if (weakestLink != null)
 					board.fortifyArmies( c.getMoveableArmies(), c, weakestLink );
 				}
 			}
 		}
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
        byte fortify = (geneticAgent.getPhase("fortify"))[0];

        if (fortify == 0x00) {
            // Cycle through all the countries and find countries that we could
            // move from:
            for (int i = 0; i < board.getNumberOfCountries(); i++) {
                if (countries[i].getOwner() == ID && countries[i].getMoveableArmies() > 0) {
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
                    Country[] neighbors = countries[i].getAdjoiningList();
                    int countryCodeBestProspect = -1;
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
                    // Now let's calculate the number of enemies of the country
                    // where the armies
                    // already are, to see if they should stay here:
                    enemyNeighbors = countries[i].getNumberEnemyNeighbors();

                    // If there's a better country to move to, move:
                    if (bestEnemyNeighbors > enemyNeighbors) {
                        // Then the armies should move:
                        // So now the country that had the best ratio should be
                        // moved to:
                        board.fortifyArmies(countries[i].getMoveableArmies(), i, countryCodeBestProspect);
                    } // If there are no good places to move to, move to a random
                    // place:
                    else if (enemyNeighbors == 0) {
                        // We choose an int from [0, neighbors.length]:
                        int randCC = rand.nextInt(neighbors.length);
                        board.fortifyArmies(countries[i].getMoveableArmies(), i, neighbors[randCC].getCode());
                    }
                }
            }
        } // If we own two touching continents we equalize the armies between
        // them.
        else if (fortify == 0x01) {
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i = 0; i < board.getNumberOfCountries(); i++) {
                    if (countries[i].getOwner() == ID && countries[i].getMoveableArmies() > 0) {
                        // This means we COULD fortify out of this country if we
                        // wanted to.

                        // Get country[i]'s neighbors:
                        Country[] neighbors = countries[i].getAdjoiningList();

                        // Now loop through the neighbors and see if we own any
                        // of them.
                        for (int j = 0; j < neighbors.length && countries[i].getMoveableArmies() > 0; j++) {
                            if (neighbors[j].getOwner() == ID) {
                                int difference = countries[i].getArmies() - neighbors[j].getArmies();
                                // So we own a neighbor. Let's see if they have
                                // more than one army difference:
                                if (difference > 1) {
                                    // So we move half the difference:
                                    board.fortifyArmies(difference / 2, i, neighbors[j].getCode());
                                    changed = true;
                                    debug("fort");
                                }
                            }
                        }
                    }
                }
            }
        } else if (fortify == 0x02) {
            // don't fortify.
        } // fortifies cluster armies.
        else if (fortify == 0x03) {
            if (BoardHelper.playerOwnsAnyPositiveContinent(ID, countries, board)) {
                int ownCont = getMostValuablePositiveOwnedCont();
                fortifyCluster(countries[BoardHelper.getCountryInContinent(ownCont, countries)]);
            } else {
                Country root = BoardHelper.getPlayersBiggestArmy(ID, countries);
                fortifyCluster(root);
            }
        } // if a continent is owned fortify it, else fortify what you can.
        else if (fortify == 0x04) {
            for (int i = 0; i < numContinents; i++) {
                if (BoardHelper.playerOwnsContinent(ID, i, countries)) {
                    fortifyContinent(i);
                } else {
                    fortifyContinentScraps(i);
                }
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
