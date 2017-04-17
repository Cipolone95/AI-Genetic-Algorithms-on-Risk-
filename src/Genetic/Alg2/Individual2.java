package Genetic.Alg2;
import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;

import java.util.Arrays;
import com.sillysoft.lux.agent.GeneticAgent2;
import java.util.Random;

/**
 * This class is used to make an individual to add to the population
 * for a genetic algorithm
 *
 * *Code is based off of code from the following website
 * http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3
 */
public class Individual2 {
     
    public GeneticAgent2 genAgent;
    public int wantTo;
	//Each phases is 3 bytes in this order; Deploy, attack, fortify.
    static int ChromosomeLength = 6;
    
    private static byte[] genes = new byte[ChromosomeLength];
    //relative fitness of this individual
    //we will have to come up with a scale
    private int fitness = 0;

    // Create a random individual
    public void generateIndividual() {
    	byte gene;
        for (int i = 0; i < size(); i++) {
            byte gene = (byte) Math.round(Math.random());
            while(gene > 8){
            	gene = (byte) Math.round(Math.random());
            }

            genes[i] = gene;
        }
    }

    public static void main(String[] args){
    	Individual2 ind = new Individual2();
    	ind.generateIndividual();
    	String s1 = String.format("%8s", Integer.toBinaryString(genes[0] & 0xFF)).replace(' ', '0');
    	System.out.println(s1);
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
            fitness = Fitness2.getFitness(this);
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