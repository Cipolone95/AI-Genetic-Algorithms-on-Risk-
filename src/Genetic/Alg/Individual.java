package Genetic.Alg;

import com.sillysoft.lux.agent.GeneticAgent;
import java.util.Random;

/**
 * This class is used to make an individual to add to the population
 * for a genetic algorithm
 *
 * *Code is based off of code from the following website
 * http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3
 * 
 * @version 2
 * @author Adam Tucker, Pete Cipolene and Travis Buff 
 */
public class Individual {
     
    public GeneticAgent genAgent;
    public int wantTo;
	//Each phases is 3 bytes in this order; Deploy, attack, fortify.
    static int ChromosomeLength = 6;
    
    private static byte[] genes = new byte[ChromosomeLength];
    //relative fitness of this individual
    //we will have to come up with a scale
    private int fitness = 0;

    // Create a random individual
    public void generateIndividual() {
            new Random().nextBytes(genes);
    }
    
    /**
     * returns byte from specified index.
     */
    public byte getGene(int index) {
        return genes[index];
    }
    
    /**
     * Returns the whole chromosome of this individual.
     * @return the whole chromosome
     */
    public byte[] getChromosome(){
    	return genes;
    }


    /**
     * sets byte as specified index.
     */
    public void setGene(int index, byte value) {
        genes[index] = value;
        fitness = 0;
    }

    
    /**
     * returns size of genes.
     */
    public int size() {
        return genes.length;
    }

    /**
     * returns fitness value of this individual.
     */
    public int getFitness() {
        if (fitness == 0) {
            fitness = Fitness.getFitness(this);
        }
        return fitness;
    }

    @Override
    public String toString() {
        String geneString = "";
        for (int i = 0; i < size(); i++) {
            geneString += getGene(i);
        }
        return geneString;
    }
}