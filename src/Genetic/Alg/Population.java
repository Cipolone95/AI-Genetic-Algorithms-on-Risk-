package Genetic.Alg;

/**
 *The population class initializes new sized population based off
 *of randomized individuals. This is the starting point of our
 *"species" so to say.
 *After it is initialized it will serve as the population over time as well.
 *
 * *Code is based off of code from the following website
 * http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3
 */
public class Population {

    Individual[] individuals;

    /*
     * Constructor
     * Create random new population if not initialized
     * If initialized allows changing of population size.
     */
    public Population(int populationSize, boolean initialise) {
        individuals = new Individual[populationSize];
        if(initialise){
            // Loop and create individuals
            for (int i = 0; i < size(); i++) {
                Individual newIndividual = new Individual();
                newIndividual.generateIndividual();
                saveIndividual(i, newIndividual);
            }
        }
    }

   
    /**
     * @param index of wanted individual
     * @return individual at this index
     */
    public Individual getIndividual(int index) {
        return individuals[index];
    }

    /**
     * @return individual that is the fittest in this population
     */
    public Individual getFittest() {
        Individual fittest = individuals[0];
        // Loop through individuals to find fittest
        for (int i = 0; i < size(); i++) {
            if (fittest.getFitness() <= getIndividual(i).getFitness()) {
                fittest = getIndividual(i);
            }
        }
        return fittest;
    }

    
    /**
     * @return size of population
     */
    public int size() {
        return individuals.length;
    }

    
    /**
     * Saves specified individual to specified index in individuals.
     */
    public void saveIndividual(int index, Individual indiv) {
        individuals[index] = indiv;
    }
}