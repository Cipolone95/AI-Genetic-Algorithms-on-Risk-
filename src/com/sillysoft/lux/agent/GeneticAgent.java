package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.*;

import Genetic.Alg.*;

public class GeneticAgent extends SmartAgentBase implements LuxAgent {
	// This agent's ownerCode:
	protected int ID;

	// Store some refs the board and to the country array
	protected Board board;
	protected Country[] countries;
	// It might be useful to have a random number generator
	protected Random rand;
	
	//This will contain the genes of our individual genetic agent.
	private Individual geneticAgent = new Individual();

	
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
	 * 
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

	public void placeInitialArmies(int numberOfArmies) {
	}

	public void cardsPhase(Card[] cards) {
	}

	public void placeArmies(int numberOfArmies) {
	}

	public void attackPhase() {
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
