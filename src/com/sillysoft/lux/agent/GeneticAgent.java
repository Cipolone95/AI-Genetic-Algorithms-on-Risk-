package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.*;

import Genetic.Alg.*;

public class GeneticAgent extends Pixie implements LuxAgent {
	// This agent's ownerCode:
	protected int ID;

	// Store some refs the board and to the country array
	protected Board board;
	protected Country[] countries;
	// It might be useful to have a random number generator
	protected Random rand;
	
	//This will contain the genes of our individual genetic agent.
	private Individual geneticAgent = new Individual();
	private boolean[] ourConts;	// whether we will spend efforts taking/holding each continent
	
	public GeneticAgent() {
		rand = new Random();
	}

	// Save references
	public void setPrefs(int newID, Board theboard) {
		ID = newID; // this is how we distinguish what countries we own

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
		// our first choice is the continent with the least # of borders that is totally empty
		if (goalCont == -1 || ! BoardHelper.playerOwnsContinentCountry(-1, goalCont, countries))
			{
			setGoalToLeastBordersCont();
			}

		// so now we have picked a cont...
		return pickCountryInContinent(goalCont);
	}


	/* 
	 * Place initial armies is part of the deploy phase in the individuals chromosome
	 * It is the first byte of the array.
	 * (non-Javadoc)
	 * @see com.sillysoft.lux.agent.SmartAgentBase#placeInitialArmies(int)
	 */
	public void placeInitialArmies(int numberOfArmies) {
		placeArmies(numberOfArmies);
	}

	public void cardsPhase(Card[] cards) {
		cashCardsIfPossible(cards);
	}

	/* Place armies based on deploy phases first gene
	 * Gene uses only up to 0x07 so 0000 0111 over 7 is unused.
	 * (non-Javadoc)
	 * @see com.sillysoft.lux.agent.Pixie#placeArmies(int)
	 */
	public void placeArmies(int numberOfArmies) {
		//placeInitialArmies is based of the first gene in the byte array
		//for the deploy phase.
		byte deployArmies = (geneticAgent.getPhase("deploy"))[0];
		
		//Armies where they can attack the most countries.
		if(deployArmies == 0x01){
			int mostEnemies = -1;
			Country placeOn = null;
			int subTotalEnemies = 0;
			CountryIterator neighbors = null;

			// Use a PlayerIterator to cycle through all the countries that we own.
			CountryIterator own = new PlayerIterator( ID, countries );
			while (own.hasNext()) 
				{
				Country us = own.next();
				subTotalEnemies = us.getNumberEnemyNeighbors();

				// If it's the best so far store it
				if ( subTotalEnemies > mostEnemies )
					{
					mostEnemies = subTotalEnemies;
					placeOn = us;
					}
				}

			// So now placeOn is the country that we own with the most enemies.
			// Tell the board to place all of our armies there
			board.placeArmies( numberOfArmies, placeOn);
		}
		//Armies placed on weakest countries owned.
		else if(deployArmies == 0x02){
			int leftToPlace = numberOfArmies;
			while (leftToPlace > 0)
				{
				int leastArmies = 1000000;
				CountryIterator ours = new PlayerIterator(ID, countries);
				while (ours.hasNext() && leftToPlace > 0)
					{
					Country us = ours.next();

					leastArmies = Math.min(leastArmies, us.getArmies());
					}

				// Now place an army on anything with less or equal to <leastArmies>
				CountryIterator placers = new ArmiesIterator(ID, -(leastArmies), countries);

				while (placers.hasNext())
					{
					Country us = placers.next();
					board.placeArmies(1, us);
					leftToPlace -= 1;
					}
				}
		}
		//places on random countries.
		else if(deployArmies == 0x03){
			int test;
			do {
				test = rand.nextInt(countries.length);
				}
			while (countries[test].getOwner() != ID || countries[test].getWeakestEnemyNeighbor() == null);

			board.placeArmies(numberOfArmies, test);
		}
		//places armies in an even cluster.
		else if(deployArmies == 0x04){
			if (BoardHelper.playerOwnsAnyPositiveContinent( ID, countries, board ))
			{
			// Center the cluster on the biggest continent we own
			int ownCont = getMostValuablePositiveOwnedCont();
			placeArmiesOnClusterBorder( numberOfArmies, countries[BoardHelper.getCountryInContinent(ownCont, countries)] );
			}
		else
			{
			// Center the cluster on the easiest continent to take
			int wantCont = getEasiestContToTake();	// getEasiestContToTake() is a SmartAgentBase method
			placeArmiesToTakeCont( numberOfArmies, wantCont );
			}
		}
		//looking for continents
		else if(deployArmies == 0x05){
			if (placeHogWild(numberOfArmies))
				return;

			if (! setupOurConts(numberOfArmies))
				{
				// then we don't think we can take/hold any continents
				placeArmiesToTakeCont( numberOfArmies, getEasiestContToTake() );
				return;
				}

			// divide our armies amongst the conts we want
			int armiesPlaced = 0;
			boolean oneNeedsHelp = true;
			while (armiesPlaced < numberOfArmies && oneNeedsHelp)
				{
				oneNeedsHelp = false;
				for (int c = 0; c < numContinents; c++)
					{
					if (ourConts[c] && continentNeedsHelp(c))
						{
						placeArmiesToTakeCont( 1, c );
						armiesPlaced++;
						oneNeedsHelp = true;
						}
					}
				}

			// We get here if all our borders are above borderforce.
			placeRemainder(numberOfArmies - armiesPlaced);
		}
		//aggressive towards human player
		else if(deployArmies == 0x06){
			if (placeArmiesToKillDominantPlayer(numberOfArmies))
				return;

			if (BoardHelper.playerOwnsAnyPositiveContinent( ID, countries, board ))
				{
				int ownCont = getMostValuablePositiveOwnedCont();
				placeArmiesOnClusterBorder( numberOfArmies, countries[BoardHelper.getCountryInContinent(ownCont, countries)] );
				}
			else
				{
				int wantCont = getEasiestContToTake();
				placeArmiesToTakeCont( numberOfArmies, wantCont );
				}
		}
		//place first to kill dominant player then place to get continents
		else if(deployArmies == 0x07){
			if (placeArmiesToKillDominantPlayer(numberOfArmies))
			{
			setupOurConts(0);
			return;
			}

		super.placeArmies(numberOfArmies);
		}
	}

	/* 
	 * Attack based on attack phases first gene
	 * Gene uses 
	 * (non-Javadoc)
	 * @see com.sillysoft.lux.agent.Pixie#attackPhase()
	 */
	public void attackPhase() {
		byte attack = (geneticAgent.getPhase("attack"))[0];
	}

	public int moveArmiesIn(int cca, int ccd) {
		return 0;
	}

	public void fortifyPhase() {
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
