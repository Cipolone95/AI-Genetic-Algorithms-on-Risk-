package com.sillysoft.lux.agent;


import Genetic.Alg.GeneticAlg;
import Genetic.Alg.Individual;
import Genetic.Alg.Population;
import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.Random;
import java.util.ArrayList;


/**
 * This agent uses a genetic algorithm to simulate moves on the Lux board.
 *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * PLEASE NOTE THAT SOME CODE WAS COPIED FROM OTHER AI'S IN THE SDK. NOT ALL
 * METHODS WHERE!!!!!!!!!!!!!!! WRITTEN BY THE AUTHORS OF THIS DOCUMENT BUT ARE
 * USED IN THE GAME AND BY THE LUX MAIN ENGINE!!!!!!!!!!! TO PLAY THE GAME. CODE
 * WRITTEN BY THE CREATOR OF THE GAME AND USED BY LUX MAIN ENGINE
 * WILL!!!!!!!!!!!! BE NOTED AS FOLLOWS: USED BY LUX MAIN
 * AGENT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *
 * @version 2
 * @author Adam Tucker, Pete Cipolene and Travis Buff 
 * SDK can be found at
 * https://sillysoft.net/sdk/
 */
public class GeneticAgent extends Pixie implements LuxAgent {

    // USED BY LUX MAIN ENGINE as the players ID
    protected int ID;
    // USED BY LUX MAIN ENGINE as the players ID
    protected int expando;
    // USED BY LUX MAIN ENGINE as the players ID
    protected int expandTo;
    // Used to save the country we want to attack
    protected int countryToAttack;
    // USED BY LUX MAIN ENGINE as the players ID
    // Store some refs the board and to the country array
    protected Board board;
    // USED BY LUX MAIN ENGINE as the players ID
    protected Country[] countries;
    // It might be useful to have a random number generator
    protected Random rand;
    // USED BY LUX MAIN ENGINE as the players ID
    private boolean[] ourConts; // whether we will spend efforts taking/holding
    // each continent
    // Three below fields used in fortify phase
    private int[] pathTo;
    private int from;
    private int to;

    /**
     * Constructor for Gen Agent USED BY LUX MAIN ENGINE
     */
    public GeneticAgent() {
        rand = new Random();

    }

    /**
     * Copy board and countries Constructor for Gen Agent
     */
    public GeneticAgent(Board myBoard, Country[] myCountries) {
        board = myBoard;
        countries = myCountries;
    }

    /**
     * Copy constructor for Gen Agent
     */
    public GeneticAgent(GeneticAgent aGeneticAgent) {
        this(aGeneticAgent.getBoard(), aGeneticAgent.getCountries());
        //no defensive copies are created here, since 
        //there are no mutable object fields (String is immutable)
    }

    /**
     * Alternative style for a copy constructor, using a static newInstance
     * method.
     */
    public static GeneticAgent newInstance(GeneticAgent aGeneticAgent) {
        return new GeneticAgent(aGeneticAgent.getBoard(), aGeneticAgent.getCountries());
    }

    /**
     * Returns the Agents Board
     *
     * @return
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Returns the Agents Country
     *
     * @return
     */
    public Country[] getCountries() {
        return countries;
    }

    // USED BY LUX MAIN ENGINE
    public void setPrefs(int newID, Board theboard) {
        ID = newID; // this is how we distinguish what countries we own
        board = theboard;
        countries = board.getCountries();
        System.out.println("SetPrefs");
        System.out.println(this);
    }

    // USED BY LUX MAIN ENGINE
    public String name() {
        return "GeneticAgent";
    }

    // USED BY LUX MAIN ENGINE
    public float version() {
        return 1.0f;
    }

    // USED BY LUX MAIN ENGINE
    public String description() {
        return "GeneticAgent uses a genetic algorithm for the attack, fortification, and reinforcement phase of risk.";
    }

    /*
	 * Picks country for genetic agent. USED BY LUX MAIN ENGINE
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
	 * USED BY LUX MAIN ENGINE
     */
    public void placeInitialArmies(int numberOfArmies) {
        placeArmies(numberOfArmies);
    }

    /*
    USED BY LUX MAIN ENGINE
     */
    public void cardsPhase(Card[] cards) {
        cashCardsIfPossible(cards);
    }

    /**
     * Takes in an individual and calculates its fitness dealing with the number
     * of countries this individual owns verses the number of countries the
     * enemy owns.
     *
     * @param ind Used to see get the information needed to calculate territory
     * fitness
     * @return The territory score
     */
    public int territoryScore(GeneticAgent ind) {
        int territoryScore = 0;
        //holds the number of enemy neighbors next to the country "us"
        int numEnemyNeighbors = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {
            Country us = own.next();
            // Gets the num of enemy neighbors next to the country us
            numEnemyNeighbors = us.getNumberEnemyNeighbors();

            if (1 - numEnemyNeighbors >= 0) {
                territoryScore = (1 - numEnemyNeighbors) + territoryScore;

            }
            //bad to place armies in a country surrounded by friendly countries.
            territoryScore = -10;
        }
        //System.out.println("Final Territory Vantage Score is :" + territoryScore);
        return territoryScore;
    }

    /**
     * Takes in an individual and calculates the fitness dealing with the armies
     * owned by this individual verses the armies owned by the enemy.
     *
     * @param ind Used to see get the information needed to calculate territory
     * fitness
     * @return The territory score
     */
    public int armyVantageScore(GeneticAgent ind) {
        int armyVantageScore = ind.countries.length;

        //holds the number of enemy neighbors next to the country "us"
        int myArmies = 0;
        CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
        while (own.hasNext()) {
            int numEnemyArmies = 0;
            Country us = own.next();
            //get my armies on this country "us'
            myArmies = us.getArmies();
            // Get an array of countries touching "us"
            Country[] nextToMe = us.getAdjoiningList();
            // Loop through the countries next to me. Hopefully starting at index zero
            for (int i = 0; i < nextToMe.length; i++) {
                // Make sure that the nextToMe country isn't mine
                if (nextToMe[i].getOwner() != ind.ID) {
                    // If it isn't mine get the armies that from country[i
                    numEnemyArmies += nextToMe[i].getArmies();
                }
            }
            // if soluttion is negative then they have enemy players have more armies sourrondng the
            // country us
            int solution = (myArmies - numEnemyArmies);
            // If solution is negative armyVantageScore will decrease, other wise it will increase
            armyVantageScore = armyVantageScore + solution;
        }
        return armyVantageScore;
    }

    /**
     * Adds the two fitness methods for total fitness score
     *
     * @param ind Used by fitness functions
     * @return The individuals fitness
     */
    public int getDeployFitness(GeneticAgent ind) {
        return armyVantageScore(ind) + territoryScore(ind);
    }

    /**
     * Called by lux agent to for first phase of a players turn
     *
     * @param numberOfArmies The number of armies allotted by the game based on
     * continents owned and other factors
     */
    public void placeArmies(int numberOfArmies) {
        // Generate the population
        Population genPop = new Population(25, true);
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < genPop.size(); i++) {
                // Get the first ind from the population
                Individual ind = genPop.getIndividual(i);
                // Gets the a copy of the GeneticAgent to simulate moves
                ind.genAgent = new GeneticAgent(this);
                int test;
                do {
                    // Gets a random from 0 to the number of countries the ind gen agent owns
                    test = rand.nextInt(ind.genAgent.countries.length);
                    // The below condition makes sure we got a random number that matches the country I won
                } while (ind.genAgent.countries[test].getOwner() != ind.genAgent.ID || ind.genAgent.countries[test].getWeakestEnemyNeighbor() == null);
                // Place the armies as a simulation
                ind.genAgent.board.placeArmies(numberOfArmies, test);
                // save the id of the country we want to test
                ind.wantTo = test;
                // Get the fitness
                int totalVantageScoreForInd = getDeployFitness(ind.genAgent);
                // Set the individuals byte
                try {
                    Byte byteScoreForInd = Byte.valueOf(Integer.toString(totalVantageScoreForInd));
                    ind.setGene(0, byteScoreForInd);
                } catch (NumberFormatException E) {
                    ind.setGene(0, (byte) 127);
                }
            }
            // Evolve the population and do it agin
            genPop = GeneticAlg.evolvePopulation(genPop);
        }
        // Get the fittest individual
        Individual temp = genPop.getFittest();
        // Place the armies on the board
        this.board.placeArmies(numberOfArmies, temp.wantTo);

    }

    /**
     * USED BY LUX MAIN ENGINE
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
     * USED BY LUX MAIN ENGINE
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
     * USED BY LUX MAIN ENGINE
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
     * USED BY LUX MAIN ENGINE
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
     * USED BY LUX MAIN ENGINE
     */
    protected void takeOutPlayerCheck(int player) {
        if (BoardHelper.getPlayerArmies(ID, countries) > 5 * BoardHelper.getPlayerArmies(player, countries)) {
            // we outnumber them 10:1. Kill what we can
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

    /**
     * Attack based on attack phases Gene is set based on the simulated state of
     * the board after the individual attacked a country
     *
     * @param ind Takes an individual to get its fitness function
     * @return int The individuals attack fitness
     */
    public int attackFitness(Individual ind) {
        //iterate through enemy countries next to us...find country with lowest armies and attack it
        CountryIterator own = new PlayerIterator(ID, countries);
        int maxArmyDifference = 0;
        while (own.hasNext()) {
            // Gets one of our countries
            Country us = own.next();
            // Get an array of countries touching "us"
            Country[] nextToMe = us.getAdjoiningList();
            for (int i = 0; i < nextToMe.length; i++) {
                // Get one of the nieghbors next to me
                Country neighbor = nextToMe[i];
                if (neighbor.getOwner() != us.getOwner()) {
                    int armyDifference = us.getArmies() - neighbor.getArmies();
                    if (armyDifference > maxArmyDifference) {
                        //max diff is the biggest difference between the amount of armies the individual has
                        // and the armies the enemy has surronding the country us
                        maxArmyDifference = armyDifference;
                        ind.genAgent.countryToAttack = neighbor.getCode();
                    }
                }
            }
        }
        return maxArmyDifference;
    }

    /**
     * Lux main engine calls this method once the attack phase begins
     */
    public void attackPhase() {

        // Used keep attacking while a the fittest ind fitness is above a threshold
        boolean attack = true;
        while (attack) {
            // Generate the population
            Population genPop = new Population(25, true);
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < genPop.size(); i++) {
                    Individual ind = genPop.getIndividual(i);
                    // Gives the individual the current Genetic Agent State
                    ind.genAgent = new GeneticAgent(this);
                    // Gets the individual fitness
                    int bestCountryToAttack = attackFitness(ind);
                    // Set the individuals gene
                    try {
                        Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToAttack));
                        ind.setGene(1, byteScoreForInd);
                    } catch (NumberFormatException E) {
                        ind.setGene(1, (byte) 127);
                    }

                }
                // Evolve the population
                genPop = GeneticAlg.evolvePopulation(genPop);
            }
            Individual temp = genPop.getFittest();
            // This checks if the fitness is below the threshold of its attack fitness 
            // i.e. The Genetic agent does not any countries that I have more armies than
            // its enemies so it shouldn't attack
            if (temp.getGene(1) < 3) {
                attack = false;
            }

            CountryIterator own = new PlayerIterator(temp.genAgent.ID, temp.genAgent.countries);

            boolean looking = true;
            Country cca = null;
            Country ccd = null;
            // This section finds the country we want to attack because we need the attacking country (cca)
            // and the defending country (ccd) where cca is our country and ccd is an enemies country
            while (looking && own.hasNext()) {
                Country us = own.next();
                // You can only attack from a country you own and that country must have 
                // more than 1 army because one army must remain on countries you own
                // So if the country us has 1 army continue to look
                if (us.getArmies() != 1) {
                    // Get the countries next to me
                    Country[] nextToMe = us.getAdjoiningList();
                    for (int i = 0; i < nextToMe.length; i++) {
                        Country neighbor = nextToMe[i];
                        // Ensures that we are not attack ourselves and the
                        if (neighbor.getCode() == temp.genAgent.countryToAttack && neighbor.getOwner() != us.getOwner()) {
                            // We have found the country that the fittest individual has so assign them to a var
                            // so we can call startAttack which starts the attack
                            cca = us;
                            ccd = neighbor;
                            looking = false;
                        }
                    }
                }
            }
            // Just in case cca and ccd wasnt assigned
            if (cca != null || ccd != null) {
                startAttack(cca, ccd);
            }
        }
    }

    /**
     * USED BY LUX MAIN ENGINE
     *
     * @param us The country attacking
     * @param neighbor Country defending
     */
    public void startAttack(Country us, Country neighbor) {
        board.attack(us, neighbor, true);
    }

    /**
     *
     * @param numArmies
     * @param countryId
     * @param ind
     * @return
     */
    public int moveArmiesFitness(int numArmies, int countryId, Individual ind) {
        int moveArmiesScore = 0;
        //holds the number of enemy neighbors next to the country "us"
        int myArmies = 0;
        int numEnemyArmies = 0;
        Country us = ind.genAgent.countries[countryId];
        //get my armies on this country "us'
        myArmies = numArmies;
        // Get an array of countries touching "us"
        Country[] nextToMe = us.getAdjoiningList();
        // Loop through the countries next to me. Hopefully starting at index zero
        for (int i = 0; i < nextToMe.length; i++) {
            // Make sure that the nextToMe country isn't mine
            if (nextToMe[i].getOwner() != ind.genAgent.ID) {
                // If it isn't mine get the armies that from country[i
                numEnemyArmies += nextToMe[i].getArmies();
            }
            // if soluttion is negative then they have enemy players have more armies sourrondng the
            // country us
            int solution = (myArmies - numEnemyArmies);
            // If solution is negative armyVantageScore will decrease, other wise it will increase
            moveArmiesScore = moveArmiesScore + solution;
        }
        System.out.println("Final Army Vantage Score is :" + moveArmiesScore);
        return moveArmiesScore;
    }

    /**
     * Called by lux main engine to move armies if we won a country that we
     * attacked.
     *
     * @param cca Country attacked from
     * @param ccd Country we won
     * @return The number of armies to move
     */
    public int moveArmiesIn(int cca, int ccd) {

        Population genPop = new Population(25, true);
        Individual ind = new Individual();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < genPop.size(); i++) {
                ind = genPop.getIndividual(i);
                ind.genAgent = new GeneticAgent(this);

                //int numOfArmiesToPlace = rand.nextInt(ind.genAgent.countries[cca].getArmies());
                int totalArmiesInCountry = ind.genAgent.countries[cca].getArmies();
                int numOfArmiesToPlace = totalArmiesInCountry - 1;

                System.out.println("Getting Fitness Score for ind " + i + " and is " + j + " generation");
                int bestCountryToMoveArmies = moveArmiesFitness(countries[cca].getArmies(), cca, ind);
                //byte can only hold so much
                //if over max then go for move in!
                try {
                    Byte byteNumOfArmies = Byte.valueOf(Integer.toString(bestCountryToMoveArmies)); //Test work 
                    ind.setGene(2, byteNumOfArmies); //was originally byteScoreForInd
                } catch (NumberFormatException E) {
                    ind.setGene(2, (byte) 127);

                }

            }
            genPop = GeneticAlg.evolvePopulation(genPop);
        }
        Individual temp = genPop.getFittest();
        Byte byteNumArmies = temp.getGene(2);
        return byteNumArmies.intValue();
    }

    /**
     * Fitness is calculated as the country that has the most enemies adjoining
     * it
     *
     * @param countryID The country we checking if it needs to be fortified
     * @return The fitness of the country
     */
    public int fortifyFitness(int countryID, Individual ind) {

        if (countries[countryID].getMoveableArmies() > 0) {
            int numEnemyNeighbors = 0;
            // A list to save the countries that have more than 2 enemy nieghbors
            ArrayList<Integer> countryWithMostNeighborFortifyCanditates = new ArrayList();
            CountryIterator own = new PlayerIterator(ind.genAgent.ID, ind.genAgent.countries);
            while (own.hasNext()) {
                Country us = own.next();
                if (us.getOwner() == ID) {
                    numEnemyNeighbors = us.getNumberEnemyNeighbors();

                    if (numEnemyNeighbors > 2) {
                        countryWithMostNeighborFortifyCanditates.add(us.getCode());
                    }
                }
            }
            int test = 0;
            int dest = 0;
            // Loop through the countries to see if we can find a path to a country that
            // needs armies
            for (int j = 0; j < countryWithMostNeighborFortifyCanditates.size(); j++) {
                // Get a random number from zero to size of list
                test = rand.nextInt(countryWithMostNeighborFortifyCanditates.size());
                // Get the country ID we want to send armies too
                dest =  countryWithMostNeighborFortifyCanditates.get(test);
                // See if there is a possible friendly path to the dest country
                // i.e. there are not any enemy countries blocking our path
                if (BoardHelper.friendlyPathBetweenCountries(countryID, dest, countries) != null) {
                    // Save the dest to for the game to use
                    ind.genAgent.to = dest;
                    // Save the path for the game to know where to send the armies
                    pathTo = BoardHelper.friendlyPathBetweenCountries(countryID, dest, countries);
                    // The ind is the fittest when the number of enemy armies is great than other individuals in the population
                    return countries[countryWithMostNeighborFortifyCanditates.get(test)].getNumberEnemyNeighbors();
                }
            }
        }
        // Didnt find any possible answers for this country
        return 0;
    }

    /**
     * Called by Lux main engine once 3rd phase starts
     */
    public void fortifyPhase() {
        Population genPop = new Population(25, true);
        Individual ind = new Individual();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < genPop.size(); i++) {
                ind = genPop.getIndividual(i);
                ind.genAgent = new GeneticAgent(this);

                int myArmies = 0;
                // A list of country ID's that has at least friendly boardering country
                ArrayList<Integer> countryWithAtLeastOneFriendly = new ArrayList();
                CountryIterator own = new PlayerIterator(ID, countries);
                while (own.hasNext()) {
                    Country us = own.next();
                    if (us.getOwner() == ID) {
                        // Get the number of armies i can move
                        myArmies = us.getMoveableArmies();
                        // If the armies I have have more than 4 armies lets fortify
                        if (myArmies > 4) {
                            Country[] neighbors = us.getAdjoiningList();
                            for (int k = 0; k < neighbors.length; k++) {
                                // If all the countries adjoining the country us are enemies than 
                                // us.getNumberNeighbors() - us.getNumberEnemyNeighbors() will be zero
                                if (us.getNumberNeighbors() - us.getNumberEnemyNeighbors() > 0) {
                                    // If us.getNumberNeighbors() - us.getNumberEnemyNeighbors()is greater than zero
                                    // than at least one country next to me is mine
                                    countryWithAtLeastOneFriendly.add(us.getCode());
                                }
                            }
                        }
                    }
                }

                // Get a random number from 0 to size of the list
                int test = rand.nextInt(countryWithAtLeastOneFriendly.size());
                // Get the code of the country that has enough armies (based on above threshold) to fortify
                // another owned country 
                from = countryWithAtLeastOneFriendly.get(test);
                // Get fitness
                int bestCountryToFortify = fortifyFitness(from,ind);
                try {
                    Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToFortify));
                    ind.setGene(1, byteScoreForInd);
                } catch (NumberFormatException E) {
                    ind.setGene(1, (byte) 127);
                }
            }
            genPop = GeneticAlg.evolvePopulation(genPop);
        }
        Individual fittest = genPop.getFittest();

        if (pathTo != null) {
            int tempCountryID = 0;
            // Loops through the pathTo the dest country we are fortifying
            for (int i = 0; i < pathTo.length - 1; i++) {
                tempCountryID = fittest.genAgent.pathTo[i];
                // Get the country id that we are starting from
                // 1st param gets the number of armies to send to next country
                // 2nd param the country that we start from
                // 3rd param is the country we are sending the armies too
                board.fortifyArmies(fittest.genAgent.countries[tempCountryID].getMoveableArmies(), 
                        fittest.genAgent.pathTo[tempCountryID], 
                        fittest.genAgent.pathTo[(tempCountryID + 1)]);
            }
        }

    }

    
    /**
     * USED BY LUX MAIN ENGINE
     * @return 
     */
    public String youWon() {
        // For variety we store a bunch of answers and pick one at random to
        // return.
        String[] answers = new String[]{"EVOLUTION IS KING", "BLOOD FOR THE BLOOD GOD"};

        return answers[rand.nextInt(answers.length)];
    }
    
    /**
     * USED BY LUX MAIN ENGINE
     * @return 
     */
    public String message(String message, Object data) {
        return null;
    }

}
