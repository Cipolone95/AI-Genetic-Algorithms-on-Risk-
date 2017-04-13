package Genetic.Alg;

import Genetic.Alg.Individual;
import com.sillysoft.lux.agent.GeneticAgent;

/**
 * @author Travis Buff, Adam Tucker, Peter Cipolone
 * This class will be the basis for the genetic algorithm that our risk "agent" will be using.
 * *Code is based off of code from the following website
 * http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3
 */
public class GeneticAlg {
	
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
    public static Population evolvePopulation(Population pop) {
        Population newPopulation = new Population(pop.size(), false);

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
            GeneticAgent indiv1 = tournamentSelection(pop);
            GeneticAgent indiv2 = tournamentSelection(pop);
            GeneticAgent newIndiv = crossover(indiv1, indiv2);
            newPopulation.saveIndividual(i, newIndiv);
        }

        // Mutate population
        for (int i = elitismOffset; i < newPopulation.size(); i++) {
            mutate(newPopulation.getIndividual(i));
        }

        return newPopulation;
    }

    // Crossover individuals
    private static GeneticAgent crossover(GeneticAgent indiv1, GeneticAgent indiv2) {
        GeneticAgent newSol = new GeneticAgent();
        // Loop through genes
        for (int i = 0; i < indiv1.geneticAgent.size(); i++) {
            // Crossover
            if (Math.random() <= crossRate) {
                newSol.geneticAgent.setGene(i, indiv1.geneticAgent.getGene(i));
            } else {
                newSol.geneticAgent.setGene(i, indiv2.geneticAgent.getGene(i));
            }
        }
        return newSol;
    }

    // Mutate an individual
    private static void mutate(GeneticAgent indiv) {
        // Loop through genes
        for (int i = 0; i < indiv.geneticAgent.size(); i++) {
            if (Math.random() <= mutRate) {
                // Create random gene
                byte gene = (byte) Math.round(Math.random());
                indiv.geneticAgent.setGene(i, gene);
            }
        }
    }

    // Select individuals for crossover
    private static GeneticAgent tournamentSelection(Population pop) {
        // Create a tournament population
        Population tournament = new Population(tournamentSize, false);
        // For each place in the tournament get a random individual
        for (int i = 0; i < tournamentSize; i++) {
            int randomId = (int) (Math.random() * pop.size());
            tournament.saveIndividual(i, pop.getIndividual(randomId));
        }
        // Get the fittest
        GeneticAgent fittest = tournament.getFittest();
        return fittest;
    }
}