package Genetic.Alg;


/**
 * This class is used to make an individual to add to the population
 * for a genetic algorithm
 *
 */
public class Individual {

    //we will need to scale this to fit our project.
	static int ChromosomeLength = 64;
    
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