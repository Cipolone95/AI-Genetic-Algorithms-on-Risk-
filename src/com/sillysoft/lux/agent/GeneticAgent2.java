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
    
    public GeneticAgent2 (Board myBoard, Country[] myCountries) {
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

    public int territoryScore(GeneticAgent2 ind) {
        System.out.println("Begin Territory Vantage Score");
        int territoryScore = ind.countries.length;
        //holds the number of enemy neighbors next to the country "us"
        int numEnemyNeighbors = 0;
        int count = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {
            Country us = own.next();
            numEnemyNeighbors = us.getNumberEnemyNeighbors();
            // territoryScore will be less then one 
            if (numEnemyNeighbors != 0) {
                if (1 - numEnemyNeighbors >= 0) {
                    territoryScore = territoryScore + (1-numEnemyNeighbors);
                    
                }
            } else {
                territoryScore = territoryScore + (1-numEnemyNeighbors);
;
            }
            count++;
        }
        System.out.println("Final Territory Vantage Score is :" + territoryScore);
        return territoryScore;
    }

    public int armyVantageScore(GeneticAgent2 ind) {
        System.out.println("Begin Army Vantage Score");
        int armyVantageScore = ind.countries.length;
        //holds the number of enemy neighbors next to the country "us"
        int myArmies = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {
            int numEnemyArmies = 0;
            Country us = own.next();
            //get my armies on this country "us'
            myArmies = us.getArmies();
            System.out.println("myArmies " + myArmies);
            // Get an array of counries touching "us"
            Country[] nextToMe = us.getAdjoiningList();
            // Loop through the countries next to me. Hoppfully starting at index zero
            for (int i = 0; i < nextToMe.length; i++) {
                // Make sure that the nextToMe country isnt mine
                if (nextToMe[i].getOwner() != ind.ID) {
                    // If it isn't mine get the armies that from country[i
                    System.out.println("Us is  " + us.getName());
                    System.out.println("Next to  " + nextToMe[i].getName());
                    System.out.println("Armies next to  " + nextToMe[i].getArmies());
                    numEnemyArmies += nextToMe[i].getArmies();
                    System.out.println("numEnemyArmies " + numEnemyArmies);

                }
            }
            if (numEnemyArmies != 0) {
                // if myArmies/numEnemyNeighbors is less than 1 that means my enemies have more armies 
                // on adjoining countries and my fitness is bad
                // if myArmies/numEnemyNeighbors is great than 1 that means my enemies have less armies 
                // on adjoining countries and my fitness is good
                int solution = (myArmies - numEnemyArmies);
                System.out.println("Solution" + solution);
                if (solution >= 0) {
                    armyVantageScore = armyVantageScore + solution;
                }
                else {
                    // Solution is negative so it will subtract even though it is adding
                    armyVantageScore = armyVantageScore + solution;
                }
            } 
        }
        System.out.println("Final Army Vantage Score is :" + armyVantageScore);
        return armyVantageScore;
    }

    public int getDeployFitness(GeneticAgent2 ind) {
        return armyVantageScore(ind);
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
                    System.out.println("Getting Fitness Score for ind " + i + " and is " + j + " generation");
                    System.out.println("Country place " + countries[test].getName());
                    int totalVantageScoreForInd = getDeployFitness(ind.genAgent);
                    System.out.println("Fitness Score for ind " + i + " and is " + j + " generation");
                    System.out.println("Total " + totalVantageScoreForInd);
                    System.out.println(totalVantageScoreForInd);
                    String whatever = Integer.toBinaryString(totalVantageScoreForInd);
                    System.out.println("Whatever " + whatever);
                    //ind.setGene(0, totalVantageScoreForInd);
            }
            genPop = GeneticAlg2.evolvePopulation(genPop);
        }
        Individual2 temp = genPop.getFittest();
        //int totalVantageScoreForInd = getDeployFitness(temp);
        System.out.println("Recieved fittest");
        System.out.println(temp.getFitness());
        System.out.println(temp);
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
        byte fortify = (geneticAgent.getPhase("fortify"))[1];

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
