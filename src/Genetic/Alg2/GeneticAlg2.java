package Genetic.Alg2;

import Genetic.Alg2.Individual2;

/**
 * @author Travis Buff, Adam Tucker, Peter Cipolone
 * This class will be the basis for the genetic algorithm that our risk "agent" will be using.
 * *Code is based off of code from the following website
 * http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3
 */
public class GeneticAlg2 {
	
	//the chance that two chromosomes cross over. Crossover will be split down the middle.
	static double crossRate = .7;
	//The chance that a gene inside the chromosome will change.
	static double mutRate = .001;
	//not sure what this is yet.
    private static final int tournamentSize = 5;
    //we will probably keep this.
    private static final boolean elitism = true;

    /* Public methods */
    
    // Evolve a population
    public static Population2 evolvePopulation(Population2 pop) {
        Population2 newPopulation = new Population2(pop.size(), false);

        // Keep our best individual
        if (elitism) {
            newPopulation.saveIndividual(0, pop.getFittest());
        }

        // Crossover population
        int elitismOffset;
        if (elitism) {
            elitismOffset = 1;
        } else {
            elitismOffset = 0;
        }
        // Loop over the population size and create new individuals with
        // crossover
        for (int i = elitismOffset; i < pop.size(); i++) {
            Individual2 indiv1 = tournamentSelection(pop);
            Individual2 indiv2 = tournamentSelection(pop);
            Individual2 newIndiv = crossover(indiv1, indiv2);
            newPopulation.saveIndividual(i, newIndiv);
        }

        // Mutate population
        for (int i = elitismOffset; i < newPopulation.size(); i++) {
            mutate(newPopulation.getIndividual(i));
        }

        return newPopulation;
    }

    // Crossover individuals
    private static Individual2 crossover(Individual2 indiv1, Individual2 indiv2) {
        Individual2 newSol = new Individual2();
        // Loop through genes
        for (int i = 0; i < indiv1.size(); i++) {
            // Crossover
            if (Math.random() <= crossRate) {
                newSol.setGene(i, indiv1.getGene(i));
            } else {
                newSol.setGene(i, indiv2.getGene(i));
            }
        }
        return newSol;
    }

    // Mutate an individual
    private static void mutate(Individual2 indiv) {
        // Loop through genes
        for (int i = 0; i < indiv.size(); i++) {
            if (Math.random() <= mutRate) {
                // Create random gene
                byte gene = (byte) Math.round(Math.random());
                indiv.setGene(i, gene);
            }
        }
    }

    // Select individuals for crossover
    private static Individual2 tournamentSelection(Population2 pop) {
        // Create a tournament population
        Population2 tournament = new Population2(tournamentSize, false);
        // For each place in the tournament get a random individual
        for (int i = 0; i < tournamentSize; i++) {
            int randomId = (int) (Math.random() * pop.size());
            tournament.saveIndividual(i, pop.getIndividual(randomId));
        }
        // Get the fittest
        Individual2 fittest = tournament.getFittest();
        return fittest;
    }
}