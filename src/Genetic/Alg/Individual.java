package Genetic.Alg;
import java.util.Arrays;

/**
 * This class is used to make an individual to add to the population
 * for a genetic algorithm
 *
 * *Code is based off of code from the following website
 * http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3
 */
public class Individual {

	//Each phases is 3 bytes in this order; Deploy, attack, fortify.
	static int ChromosomeLength = 6;
    
    private byte[] genes = new byte[ChromosomeLength];
    //relative fitness of this individual
    //we will have to come up with a scale
    private int fitness = 0;

    // Create a random individual
    public void generateIndividual() {
        for (int i = 0; i < size(); i++) {
            byte gene = (byte) Math.round(Math.random());
            genes[i] = gene;
        }
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
     * Returns gene phase based of parameter.
     * @return Returns specific phase requested.
     */
    public byte[] getPhase(String phase){
    	switch(phase){
    	case "deploy":
    		return Arrays.copyOfRange(genes,0,1);
    	case "attack":
    		return Arrays.copyOfRange(genes, 2, 3);
    	case "fortify":
    		return Arrays.copyOfRange(genes, 4, 5);
    	}
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