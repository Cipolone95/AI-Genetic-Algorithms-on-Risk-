package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

import Genetic.Alg.GeneticAlg;
import Genetic.Alg.Individual;
import Genetic.Alg.Population;

import java.util.Random;

public class GeneticAgent extends Pixie implements LuxAgent {
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
	public Individual geneticAgent;
	private boolean[] ourConts; // whether we will spend efforts taking/holding
	// each continent

	public GeneticAgent() {
		rand = new Random();
		geneticAgent = new Individual();

	}

	// Save references
        @Override
	public void setPrefs(int newID, Board theboard) {
		ID = newID; // this is how we distinguish what countries we own
		board = theboard;
		countries = board.getCountries();
	}

        @Override
	public String name() {
		return "GeneticAgent";
	}

        @Override
	public float version() {
		return 1.0f;
	}

        @Override
	public String description() {
		return "GeneticAgent uses a genetic algorithm for the attack, fortification, and reinforcement phase of risk.";
	}

	/*
	 * Picks country for genetic agent.
	 */
        @Override
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
        @Override
	public void placeInitialArmies(int numberOfArmies) {
		placeArmies(numberOfArmies);
	}

        @Override
	public void cardsPhase(Card[] cards) {
		cashCardsIfPossible(cards);
	}

	/**
	 * Takes in an individual and calculates its fitness dealing with the number
	 * of countries this individual owns verses the number of countries the
	 * enemy owns.
     * @param ind
     * @return territoryScore
	 */
	public int territoryScore(GeneticAgent ind) {
		int territoryScore = ind.countries.length;
		// holds the number of enemy neighbors next to the country "us"
		int numEnemyNeighbors = 0;
		CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
		// sifts through our countries
		while (own.hasNext()) {
			Country us = own.next();
			numEnemyNeighbors = us.getNumberEnemyNeighbors();
			// territoryScore will be less then one
			if (numEnemyNeighbors != 0) {
				if (1 - numEnemyNeighbors >= 0) {
					territoryScore = territoryScore + (1 - numEnemyNeighbors);

				}
			} else {
				// bad to place armies in a country surrounded by friendly
				// countries.
				territoryScore = -10;
			}

		}

		return territoryScore;
	}

	/**
	 * Takes in an individual and calculates the fitness dealing with the armies
	 * owned by this individual verses the armies owned by the enemy.
     * @param ind
     * @return Army Vantage Score
	 */
	public int armyVantageScore(GeneticAgent ind) {
		int armyVantageScore = ind.countries.length;

		// holds the number of enemy neighbors next to the country "us"
		int myArmies = 0;
		CountryIterator own = new PlayerIterator(ind.ID, ind.countries);
		while (own.hasNext()) {
			int numEnemyArmies = 0;
			Country us = own.next();
			// get my armies on this country "us'
			myArmies = us.getArmies();

			// Get an array of countries touching "us"
			Country[] nextToMe = us.getAdjoiningList();
			// Loop through the countries next to me. Hopefully starting at
			// index zero
			for (int i = 0; i < nextToMe.length; i++) {
				// Make sure that the nextToMe country isn't mine
				if (nextToMe[i].getOwner() != ind.ID) {
					// If it isn't mine get the armies that from country[i]

					numEnemyArmies += nextToMe[i].getArmies();

				}
			}
			if (numEnemyArmies != 0) {
				// if myArmies/numEnemyNeighbors is less than 1 that means my
				// enemies have more armies
				// on adjoining countries and my fitness is bad
				// if myArmies/numEnemyNeighbors is great than 1 that means my
				// enemies have less armies
				// on adjoining countries and my fitness is good
				int solution = (myArmies - numEnemyArmies);

				if (solution >= 0) {
					armyVantageScore = solution;
				} else {
					// Solution is negative so it will subtract even though it
					// is adding
					armyVantageScore = 0;
				}
			}
		}

		return armyVantageScore;
	}

	/**
	 * Deploy "placeArmies" uses the fitness score from ArmyVantage and
	 * Territory score and sends it back for complete deploy fitness score.
     * @param ind
     * @return 
	 */
	public int getDeployFitness(GeneticAgent ind) {
		return armyVantageScore(ind) + territoryScore(ind);
	}

	/*
	 * Place armies based on deploy phases Makes a population of 100 and goes
	 * through 3 generations and picks the most fit individual and moves
	 * accordingly.
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#placeArmies(int)
	 */
        @Override
	public void placeArmies(int numberOfArmies) {

		Population genPop = new Population(25, true);
		for (int j = 0; j < 1; j++) {
			for (int i = 0; i < genPop.size(); i++) {

				// placeInitialArmies is based of the first gene in the byte
				// array
				// for the deploy phase.
				Individual ind = genPop.getIndividual(i);
				ind.genAgent = this;
				// places on random countries.
				int test;
				do {
					test = rand.nextInt(ind.genAgent.countries.length);
				} while (ind.genAgent.countries[test].getOwner() != ind.genAgent.ID
						|| ind.genAgent.countries[test].getWeakestEnemyNeighbor() == null);

				ind.genAgent.board.placeArmies(numberOfArmies, test);
				ind.wantTo = test;

			}
			genPop = GeneticAlg.evolvePopulation(genPop);
		}
		Individual temp = genPop.getFittest();
		// places armies from most fit individual.
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
						if (!board.getAgentName(neighbors[j].getOwner()).equals(name())) // don't
																							// attack
																							// other
																							// commies,
																							// until
																							// all
																							// the
																							// running
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
	 * used by Lux main engine.
     * @param careAboutOdds
     * @return 
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
	 * Return fitness for attackphase based on the individual it is sent.
     * @param ind
     * @return 
	 */
	public int attackFitness(Individual ind) {
		// iterate through enemy countries next to us...find country with lowest
		// armies and attack it
		CountryIterator own = new PlayerIterator(ID, countries);

		int maxArmyDifference = 0;
		while (own.hasNext()) {
			Country us = own.next();
			// Get an array of countries touching "us"
			Country[] nextToMe = us.getAdjoiningList();
			for (int i = 0; i < nextToMe.length; i++) {
				Country neighbor = nextToMe[i];
				if (neighbor.getOwner() != us.getOwner()) {
					int armyDifference = us.getArmies() - neighbor.getArmies();
					if (armyDifference > maxArmyDifference) {
						maxArmyDifference = armyDifference;
						ind.genAgent.countryToAttack = neighbor.getCode();
					}
				}
			}
		}
		return maxArmyDifference;
	}

	/*
	 * Attack for attack phase Makes a population of 100 and goes through 3
	 * generations and picks the most fit individual and attacks accordingly.
	 * 
	 */
        @Override
	public void attackPhase() {

		int count = 0;
		while (count < 10) {
			Population genPop = new Population(5, true);
			Individual ind = new Individual();
			for (int j = 0; j < 3; j++) {
				for (int i = 0; i < genPop.size(); i++) {
					ind = genPop.getIndividual(i);
					ind.genAgent = this;

					int bestCountryToAttack = attackFitness(ind);

					// byte can only hold so much
					// if over max then go for attack!
					try {
						Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToAttack));
						ind.setGene(1, byteScoreForInd);
					} catch (NumberFormatException E) {
						ind.setGene(1, (byte) 127);
					}

				}
				genPop = GeneticAlg.evolvePopulation(genPop);
			}
			Individual temp = genPop.getFittest();
			temp.genAgent = this;

			CountryIterator own = new PlayerIterator(ID, countries);

			boolean looking = true;
			Country cca = null;
			Country ccd = null;
			while (looking && own.hasNext()) {
				Country us = own.next();
				// Get an array of countries touching "us"
				if (us.getArmies() != 1) {
					Country[] nextToMe = us.getAdjoiningList();
					for (int i = 0; i < nextToMe.length; i++) {
						Country neighbor = nextToMe[i];
						if (neighbor.getCode() == temp.genAgent.countryToAttack
								&& neighbor.getOwner() != us.getOwner()) {
							cca = us;
							ccd = neighbor;
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

	/**
	 * Used by Lux main engine to go through with the attack.
     * @param us
     * @param neighbor
	 */
	public void startAttack(Country us, Country neighbor) {
		board.attack(us, neighbor, true);
	}

	/**
	 * Calculates the fitness for moving armies based on the individual it
	 * receives.
     * @param numArmies
     * @param countryId
     * @param ind
     * @return 
	 */
	public int moveArmiesFitness(int numArmies, int countryId, Individual ind) {

		int moveArmiesScore = 0;
		// holds the number of enemy neighbors next to the country "us"
		int myArmies = 0;
		int numEnemyArmies = 0;
		Country us = ind.genAgent.countries[countryId];
		// get my armies on this country "us'
		myArmies = numArmies;
		// System.out.println("myArmies " + myArmies);
		// Get an array of countries touching "us"
		Country[] nextToMe = us.getAdjoiningList();
		// Loop through the countries next to me. Hopefully starting at index
		// zero
		for (int i = 0; i < nextToMe.length; i++) {
			// Make sure that the nextToMe country isn't mine
			if (nextToMe[i].getOwner() != ind.genAgent.ID) {
				// If it isn't mine get the armies that from country[i
				// System.out.println("Us is " + us.getName());
				// System.out.println("Next to " + nextToMe[i].getName());
				// System.out.println("Armies next to " +
				// nextToMe[i].getArmies());
				numEnemyArmies += nextToMe[i].getArmies();
				// System.out.println("numEnemyArmies " + numEnemyArmies);

			}
			if (numEnemyArmies != 0) {
				// if myArmies/numEnemyNeighbors is less than 1 that means my
				// enemies have more armies
				// on adjoining countries and my fitness is bad
				// if myArmies/numEnemyNeighbors is great than 1 that means my
				// enemies have less armies
				// on adjoining countries and my fitness is good
				int solution = (myArmies - numEnemyArmies);
				if (solution >= 0) {
					moveArmiesScore = moveArmiesScore + solution;
				} else {
					// Solution is negative so it will subtract even though it
					// is adding
					moveArmiesScore = moveArmiesScore + solution;
				}

			}
		}
		return moveArmiesScore;
	}

	/*
	 * 2nd part of the attack phase. Moves armies in after attack happens.
	 */
        @Override
	public int moveArmiesIn(int cca, int ccd) {

		Population genPop = new Population(100, true);
		Individual ind = new Individual();
		for (int j = 0; j < 3; j++) {
			for (int i = 0; i < genPop.size(); i++) {
				ind = genPop.getIndividual(i);
				ind.genAgent = this;
				// placed on countries we already own

				int totalArmiesInCountry = ind.genAgent.countries[cca].getArmies();
				int numOfArmiesToPlace = totalArmiesInCountry - 1;

				int bestCountryToMoveArmies = moveArmiesFitness(countries[cca].getArmies(), cca, ind);
				// byte can only hold so much
				// if over max then go for move in!
				try {
					Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToMoveArmies));
					Byte byteNumOfArmies = Byte.valueOf(Integer.toString(numOfArmiesToPlace)); // Test
																								// work
					ind.setGene(1, byteScoreForInd);
					ind.setGene(2, byteNumOfArmies); // was originally
														// byteScoreForInd
				} catch (NumberFormatException E) {
					ind.setGene(1, (byte) 127);
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
	 * Calculates fortify fitness for specified Country owned by individual.
     * @param countryID
     * @return 
	 */
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
	 */
        @Override
	public void fortifyPhase() {

		Population genPop = new Population(100, true);
		Individual ind = new Individual();
		for (int j = 0; j < 3; j++) {
			for (int i = 0; i < genPop.size(); i++) {
				ind = genPop.getIndividual(i);
				ind.genAgent = this;
				// placed on countries we already own
				int randCountry = rand.nextInt(ind.genAgent.countries.length);
				int bestCountryToFortify = fortifyFitness(randCountry);
				Byte byteScoreForInd = Byte.valueOf(Integer.toString(bestCountryToFortify));
				ind.setGene(3, byteScoreForInd);
			}
			genPop = GeneticAlg.evolvePopulation(genPop);
		}
		Individual temp = genPop.getFittest();
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
				board.fortifyArmies(countries[i].getMoveableArmies(), (int) bestEnemyNeighbors, neighbors[i].getCode());
			} // If there are no good places to move to, move to a random
				// place:
			else if (enemyNeighbors == 0) {
				// We choose an int from [0, neighbors.length]:
				int randCC = rand.nextInt(neighbors.length);
				board.fortifyArmies(countries[i].getMoveableArmies(), i, neighbors[randCC].getCode());

			}
		}
	}

        @Override
	public String youWon() {
		// For variety we store a bunch of answers and pick one at random to
		// return.
		String[] answers = new String[] { "EVOLUTION IS KING", "BLOOD FOR THE BLOOD GOD" };

		return answers[rand.nextInt(answers.length)];
	}

        @Override
	public String message(String message, Object data) {
		return null;
	}

}