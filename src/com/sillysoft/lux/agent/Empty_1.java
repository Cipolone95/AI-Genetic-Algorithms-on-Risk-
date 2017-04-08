package com.sillysoft.lux.agent;

import Genetic.Alg.Individual;
import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.Date;

import java.util.Random;
import java.util.List;

public class Empty_1 extends Pixie implements LuxAgent 
{
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
        protected AgentLogger logger;

	// This will contain the genes of our individual genetic agent.
	private Individual geneticAgent;
	private boolean[] ourConts; // whether we will spend efforts taking/holding
								// each continent

	public Empty_1() {
		rand = new Random();
                
	}

	// Save references
	public void setPrefs(int newID, Board theboard) {
		ID = newID; // this is how we distinguish what countries we own
                String addToFile = Integer.toString(ID) + new Date().getTime();
                this.logger = new AgentLogger(addToFile);
                this.geneticAgent = new Individual();
                logger.log("Genetic Agent create at" + new Date().getTime());
		board = theboard;
		countries = board.getCountries();
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

	/*
	 * Place armies based on deploy phases first gene Gene uses only up to 0x07
	 * so 0000 0111 over 7 is unused. (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#placeArmies(int)
	 */
	public void placeArmies(int numberOfArmies) {
                logger.log("Place Armies");
                byte[] deployGene = new byte[1];
                //holds the terrioty advantage score
                int territoryScore = 0;
                int armyVantageScore = 0;
                //holds the number of enemy neighbors next to the country "us"
                int numEnemyNeighbors = 0;
                int numEnemyArmies = 0;
                int myArmies = 0;
                CountryIterator own = new PlayerIterator(ID, countries);
			while (own.hasNext()) {
				Country us = own.next();
                                //get my armies on this country "us'
                                myArmies = us.getArmies();
                                // Get an array of counries touching "us"
                                Country [] nextToMe = us.getAdjoiningList();
                                // Loop through the countries next to me. Hoppfully starting at index zero
                                for (int i = 0; i <= nextToMe.length; i++) {
                                    // Make sure that the nextToMe country isnt mine
                                    if (nextToMe[i].getOwner() != this.ID) {
                                        // If it isn't mine get the armies that from country[i]
                                        numEnemyArmies += BoardHelper.getPlayerArmies(nextToMe[i].getOwner(), nextToMe);
                                    }
                                }
                                if (numEnemyArmies != 0) {
                                    // if myArmies/numEnemyNeighbors is less than 1 that means my enemies have more armies 
                                    // on adjoining countries and my fitness is bad
                                    // if myArmies/numEnemyNeighbors is great than 1 that means my enemies have less armies 
                                    // on adjoining countries and my fitness is bad
                                armyVantageScore = myArmies/numEnemyNeighbors;
                                }
                                else {
                                    armyVantageScore = 1;
                                }
                                
				numEnemyNeighbors = us.getNumberEnemyNeighbors();
                                // territoryScore will be less then one 
                                if (numEnemyNeighbors != 0) {
                                territoryScore = 1/numEnemyNeighbors;
                                }
                                else {
                                    territoryScore = 0;
                                }
				// If it's the best so far store it
			}
                
		// placeInitialArmies is based of the first gene in the byte array
		// for the deploy phase.
		byte deployArmies = (geneticAgent.getPhase("deploy"))[0];
                //byte deployArmies = 0X01;
		// Armies where they can attack the most countries.
		if (deployArmies == 0x01) {
			int mostEnemies = -1;
			Country placeOn = null;
			int subTotalEnemies = 0;
			CountryIterator neighbors = null;

			// Use a PlayerIterator to cycle through all the countries that we
			// own.
			CountryIterator own2 = new PlayerIterator(ID, countries);
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
			board.placeArmies(numberOfArmies, placeOn);
		}
		// Armies placed on weakest countries owned.
		else if (deployArmies == 0x02) {
			int leftToPlace = numberOfArmies;
			while (leftToPlace > 0) {
				int leastArmies = 1000000;
				CountryIterator ours = new PlayerIterator(ID, countries);
				while (ours.hasNext() && leftToPlace > 0) {
					Country us = ours.next();

					leastArmies = Math.min(leastArmies, us.getArmies());
				}

				// Now place an army on anything with less or equal to
				// <leastArmies>
				CountryIterator placers = new ArmiesIterator(ID, -(leastArmies), countries);

				while (placers.hasNext()) {
					Country us = placers.next();
					board.placeArmies(1, us);
					leftToPlace -= 1;
				}
			}
		}
		// places on random countries.
		else if (deployArmies == 0x03) {
			int test;
			do {
				test = rand.nextInt(countries.length);
			} while (countries[test].getOwner() != ID || countries[test].getWeakestEnemyNeighbor() == null);

			board.placeArmies(numberOfArmies, test);
		}
		// places armies in an even cluster.
		else if (deployArmies == 0x04) {
			if (BoardHelper.playerOwnsAnyPositiveContinent(ID, countries, board)) {
				// Center the cluster on the biggest continent we own
				int ownCont = getMostValuablePositiveOwnedCont();
				placeArmiesOnClusterBorder(numberOfArmies,
						countries[BoardHelper.getCountryInContinent(ownCont, countries)]);
			} else {
				// Center the cluster on the easiest continent to take
				int wantCont = getEasiestContToTake(); // getEasiestContToTake()
														// is a SmartAgentBase
														// method
				placeArmiesToTakeCont(numberOfArmies, wantCont);
			}
		}
		// looking for continents
		else if (deployArmies == 0x05) {
			if (placeHogWild(numberOfArmies))
				return;

			if (!setupOurConts(numberOfArmies)) {
				// then we don't think we can take/hold any continents
				placeArmiesToTakeCont(numberOfArmies, getEasiestContToTake());
				return;
			}

			// divide our armies amongst the conts we want
			int armiesPlaced = 0;
			boolean oneNeedsHelp = true;
			while (armiesPlaced < numberOfArmies && oneNeedsHelp) {
				oneNeedsHelp = false;
				for (int c = 0; c < numContinents; c++) {
					if (ourConts[c] && continentNeedsHelp(c)) {
						placeArmiesToTakeCont(1, c);
						armiesPlaced++;
						oneNeedsHelp = true;
					}
				}
			}

			// We get here if all our borders are above borderforce.
			placeRemainder(numberOfArmies - armiesPlaced);
		}
		// aggressive towards human player
		else if (deployArmies == 0x06) {
			if (placeArmiesToKillDominantPlayer(numberOfArmies))
				return;

			if (BoardHelper.playerOwnsAnyPositiveContinent(ID, countries, board)) {
				int ownCont = getMostValuablePositiveOwnedCont();
				placeArmiesOnClusterBorder(numberOfArmies,
						countries[BoardHelper.getCountryInContinent(ownCont, countries)]);
			} else {
				int wantCont = getEasiestContToTake();
				placeArmiesToTakeCont(numberOfArmies, wantCont);
			}
		}
		// place first to kill dominant player then place to get continents
		else if (deployArmies == 0x07) {
			if (placeArmiesToKillDominantPlayer(numberOfArmies)) {
				setupOurConts(0);
				return;
			}

			super.placeArmies(numberOfArmies);
		}
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
						if (!board.getAgentName(neighbors[j].getOwner()).equals(name()))
						// don't attack other commies, until all the running
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
							if (board.attack(neigbors[n], countries[borders[b]], true) > 0)
								return;
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
	 * Attack based on attack phases first gene Gene uses (non-Javadoc)
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
		}
		// look for weak countries to overtake.
		else if (attack == 0x02) {

			if (expando == -1)
				return; // nowhere to go

			// Now we see if we have a good chance of taking the weakest link
			// over:
			if (expandTo != -1 && countries[expando].getArmies() > countries[expandTo].getArmies()) {
				// We attack till dead, with max dice:
				board.attack(expando, expandTo, true);
			}

			attackHogWild();
			attackStalemate();
		}
		// attack amount depending on armies size.
		else if (attack == 0x03) {
			attackPhase(true);

			// If we have tons of armies then attack more
			if (BoardHelper.getPlayerArmies(ID, countries) > 300) {
				while (attackPhase(false)) {
				}
			}
		}
		// expand in cluster looking for easy expansions
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
		}
		// will attack and try to get a card
		else if (attack == 0x05) {
			for (int i = 0; i < numContinents; i++) {
				if (ourConts[i]) {
					attackInContinent(i);
				}
			}

			attackForCard();
			attackHogWild();
			attackStalemate();
		}
		// attack to kill a player.
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
		}
		// looks for continent take over attacks
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
	 * DEPLOY OR ATTACK PHASE PART 2!!!! (non-Javadoc)
	 * 
	 * @see com.sillysoft.lux.agent.Pixie#moveArmiesIn(int, int)
	 */
	public int moveArmiesIn(int cca, int ccd) {
		return 0;
	}

	public void fortifyPhase() {
            for (int i = 0; i < numContinents; i++)
		{
		if (BoardHelper.playerOwnsContinent(ID, i, countries))
			{
			fortifyContinent( i );
			}
		else
			{
			fortifyContinentScraps(i);
			}
		}
	}

	public String youWon() {
		// For variety we store a bunch of answers and pick one at random to
		// return.
		String[] answers = new String[] { "I won", "beee!" };

		return answers[rand.nextInt(answers.length)];
	}

	public String message(String message, Object data) {
		return null;
	}

}
