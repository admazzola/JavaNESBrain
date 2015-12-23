package com.starflask.JavaNESBrain.evolution;

import java.util.ArrayList;


import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.starflask.JavaNESBrain.SuperBrain;
import com.starflask.JavaNESBrain.data.GameDataManager;
import com.starflask.JavaNESBrain.utils.FastMath;



public class GenePool {
	
	int Population = 300;
	
	int StaleSpecies = 15;
	
	List<Species> species = new ArrayList<Species>();
	private int generation = 0;
	int innovation = 10; //becomes equal to numOutputs
	private int currentSpecies = 0;
	private int currentGenome = 0;
	int currentFrame = 0;
	private int maxFitness = 0;
	
	Random rand = new Random();
	
	GameDataManager gameDataManager;
	
	
	public GenePool(GameDataManager gameDataManager)
	{
		this.gameDataManager=gameDataManager;
		
		for (int i=1; i < getPopulation() ; i++ )
		{
		        Genome basic = createBasicGenome();
		        addToSpecies(basic) ;
		}
		
		
	}
	
	public GameDataManager getGameDataManager() {
		return gameDataManager;
	}


	public int newInnovation()
	{		
		return ++innovation;
	}


	public  Genome createBasicGenome()
	{
        Genome genome = new Genome();
		innovation = 1;
 
        genome.maxneuron = getGameDataManager().getNumInputs() ;  //numInputs
        mutate(genome);
       
        return genome;
	}
	
	
	private void mutate(Genome genome)
	{		
    for (String key : genome.mutationRates.keySet()  ){
    	
    	float rate = genome.mutationRates.get(key);
    	
            if ( rand.nextBoolean() ){ //50 50 chance
                   genome.mutationRates.put(key,0.95f*rate ) ;
            }else{
            	   genome.mutationRates.put(key,1.05263f*rate ) ; 
            }
	}

    if (rand.nextFloat() < genome.mutationRates.get("connections")) {
            pointMutate(genome) ;
    }
   
    float p = genome.mutationRates.get("link");
    while (p > 0){
            if (rand.nextFloat() < p) {
                    linkMutate(genome, false);
            }
            p = p - 1;
    }

    p = genome.mutationRates.get("bias");
    while (p > 0) {
            if (rand.nextFloat() < p) {
                    linkMutate(genome, true);
            }
            p = p - 1;
    }
   
    p = genome.mutationRates.get("node");
    while (p > 0) {
            if (rand.nextFloat() < p) {
                    nodeMutate(genome);
            }
            p = p - 1;
    }
   
    p = genome.mutationRates.get("enable");
    while (p > 0) {
            if (rand.nextFloat() < p) {
                    enableDisableMutate(genome, true);
            }
            p = p - 1;
    }

    p = genome.mutationRates.get("disable");
    while (p > 0) {
            if (rand.nextFloat() < p) {
                    enableDisableMutate(genome, false);
            }
            p = p - 1;
    }
	}

	
	float PerturbChance = 0.90f;

private void pointMutate(Genome genome)
{
        float step = genome.mutationRates.get("step") ;
       
        for (int i=0; i <genome.genes.size(); i++ )
        {
                Gene gene = genome.genes.get(i) ;
                if (rand.nextFloat() < PerturbChance) { 
                        gene.setWeight(gene.getWeight() + rand.nextFloat() * step*2 - step) ;
                }else{
                        gene.setWeight(rand.nextFloat()*4-2) ;
		}
	}
}
 

	
	
private void linkMutate(Genome genome, boolean forceBias)
	{
    int neuron1Index = randomNeuronIndex(genome.genes, false) ; 
    int neuron2Index = randomNeuronIndex(genome.genes, true) ;
     
   
    
    Gene newLink = new Gene();
    
    //if the index of the neuron is less than num inputs then it must be an input-neuron.. i should change this architecture
    if (neuron1Index <= getGameDataManager().getNumInputs() && neuron2Index <= getGameDataManager().getNumInputs()) 
    {
            //--Both input nodes
            return;
    }
    
    if (neuron2Index <= getGameDataManager().getNumInputs()) 
    {
           // -- Swap output and input   -- probably should copy !
            int temp = neuron1Index;
            neuron1Index = neuron2Index;
            neuron2Index = temp;
    }

    newLink.setInto(neuron1Index) ; 
    newLink.setOut(neuron2Index) ;
    


    
    if (forceBias)
    	{
            newLink.setInto(getGameDataManager().getNumInputs());
    	}
   
    if (containsLink(genome.genes, newLink) )
    {
            return;
    }
            		
            		
    newLink.innovation = newInnovation();
    newLink.setWeight(rand.nextFloat()*4 -2 ) ;
   
    genome.genes.add(newLink);

}

 
 
private boolean containsLink(List<Gene> genes, Gene link) {

	 for (Gene gene : genes) 
	 {          
         if (gene.getNeuralInIndex() == link.getNeuralInIndex() && gene.getNeuralOutIndex() == link.getNeuralOutIndex()) 
                 return true;
         
	 }
	 return false;
}


//what does this do?  
private int randomNeuronIndex(List<Gene> genes, boolean nonInput)
{
	
HashMap<Integer,Boolean> neuronMatchesInputState = new HashMap<Integer,Boolean>(); 

//every neuron corresponds with a gamepad button 
if (! nonInput ){
        for (int i=0; i < getGameDataManager().getNumInputs() ; i++ ){
        	neuronMatchesInputState.put(i, true);
	}
}

for (int o=0; o < getGameDataManager().getNumOutputs(); o++ ) {
	 
	neuronMatchesInputState.put( SuperBrain.MaxNodes +o, true);
}

for (int i=0; i < genes.size(); i++){
        if ((! nonInput) || genes.get(i).getNeuralInIndex() > getGameDataManager().getNumInputs()) {
        	neuronMatchesInputState.put(genes.get(i).getNeuralInIndex(), true);
        }
        if ((! nonInput) || genes.get(i).getNeuralOutIndex() > getGameDataManager().getNumInputs() ){
        	neuronMatchesInputState.put(genes.get(i).getNeuralOutIndex(), true);
        }
}




int numNeurons = neuronMatchesInputState.size();

int randomIndex  = rand.nextInt(numNeurons+1);

//stop in a random place in the index hashmap and then give out that index
for (Integer key : neuronMatchesInputState.keySet() ) 
{
	randomIndex--;
	
	if (randomIndex == 0 )
	{
		 
            return key;
	}
	
}
 

return 0;
		
}



private void nodeMutate(Genome genome)
{
    if (genome.genes.size() == 0 )
            return;
    

    genome.maxneuron = genome.maxneuron + 1;

    
    int randomIndex = rand.nextInt(genome.genes.size()); 
    Gene gene = genome.genes.get( randomIndex ) ;
    if (!gene.isEnabled()) 
            return;
    
            		
            		
    gene.setEnabled(false);
   
    Gene gene1 = gene.copy();
    gene1.setOut(genome.maxneuron);
    gene1.setWeight(1.0f);
    gene1.innovation = newInnovation();
    gene1.setEnabled(true);
    genome.genes.add(gene1);
   
    Gene gene2 = gene.copy();
    gene2.setInto(genome.maxneuron);
    gene2.innovation = newInnovation();
    gene2.setEnabled(true);
    genome.genes.add(gene2);
}
	

public void enableDisableMutate(Genome genome, boolean enable)
{
	List<Gene> candidates = new ArrayList<Gene>();

	//find the genes that are not this enablestate
	for (Gene gene : genome.genes) {
        if (gene.isEnabled() != enable)
        {
              candidates.add(gene);
        }
	}

	if (candidates.isEmpty())
	{
        return;
      }

		//flip the enablestate of a random candidate
		int randomIndex = rand.nextInt(candidates.size() ) ;
		Gene gene = candidates.get(randomIndex) ;
		gene.setEnabled(! gene.isEnabled()) ;
	
	}


public int getCurrentFrame() {
	 
	return currentFrame;
}


public Species getCurrentSpecies() {
	 
	return species.get(currentSpecies);
}


public Genome getCurrentGenome() {
	 
	 
	return getCurrentSpecies().getGenomes().get(currentGenome  ); 
	
	 
	
}


public int getMaxFitness() {
	return maxFitness;
}


public void setMaxFitness(int maxFitness) {
	this.maxFitness = maxFitness;
}


public void setCurrentSpecies(int currentSpecies) {
	this.currentSpecies = currentSpecies;
}


public void setCurrentGenome(int currentGenome) {
	this.currentGenome = currentGenome;
}


public int getGeneration() {
	return generation;
}


public void setGeneration(int generation) {
	this.generation = generation;
}


public int getCurrentGenomeIndex() {
	 
	return currentGenome;
}


public int getCurrentSpeciesIndex() {
 
	return currentSpecies;
}


public List<Species> getSpecies() {
	 
	return species;
}


public void setCurrentFrame(int i) {
	 currentFrame = i;
	 
}

public void cullSpecies(boolean cutToOne) { 
	
    for (int s = 0; s < getSpecies().size(); s++)
    {
        
        Species specie = getSpecies().get(s);
        
        //this will make better fitness get closer to zero
        Collections.sort( specie.getGenomes(),new Comparator<Genome>(){

     			@Override
     			public int compare(Genome g1, Genome g2) 	
     			{
     				return g1.getFitness() < g2.getFitness() ? 1 :
     	               (g1.getFitness() == g2.getFitness() ? 0 : -1);     				
     			}
             	
     			
     			//If your Comparator's compare(T o1, T o2) return a negative when o1 is greater than o2, you get descending order 
     			
     		       // table.sort(species.genomes, function (a,b)
     		       //         return (a.fitness > b.fitness)
     		       // end)
             });
                          
       
        int remaining = (int) FastMath.ceil(specie.getGenomes().size()/2) ;
        
        if (cutToOne )
        {
            remaining = 1;
        }
        
        while (specie.getGenomes().size() > remaining) 
        {
        	
        	System.out.println("culling " +  specie.getGenomes().get( specie.getGenomes().size()-1 ) + " with " + specie.getGenomes().get( specie.getGenomes().size()-1 ).getFitness() + " fitness "  );
        
        	 specie.getGenomes().remove( specie.getGenomes().size()-1    );  //keep removing the least fit genome  (hopefully this isnt the least fit!)
                
        }
        
        
    }

}



public void newGeneration() {

	 cullSpecies(false); // Cull the bottom half of each species
     rankGlobally();
     removeStaleSpecies();
     rankGlobally();
     
     for (int s = 0 ; s < getSpecies().size(); s++)
     {
             Species specie = getSpecies().get(s);
             calculateAverageFitness(specie) ;
     }
     
     //remove empty species and those with poor average fitness
     removeWeakSpecies();
     int sum = totalAverageFitness();
     
     List<Genome> children = new ArrayList<Genome>();
     
    
     
     for (int s = 0 ; s < getSpecies().size(); s++)
     {	 
    	 Species specie = getSpecies().get(s);
     
    	 if( specie.getGenomes().size() > 0 )
    	 {
             int breed = (int) (FastMath.floor(specie.averageFitness / sum * Population) - 1)  ; //more fitness  = more breeding
             for (int i=0; i < breed; i++) {
            	 children.add( breedChild(specie) );
             }
    	 }
             
     }
     
     cullSpecies(true); //-- Cull all but the top member of each species
        
     
     //make new children from the previous best child
     while (children.size() + getSpecies().size() < Population) 
     {
    	 	//sometimes species size is zero !
    	 
    	 if( getSpecies().size() > 0 )
    	 {
    	 
    	 	int randIndex = rand.nextInt( getSpecies().size() ) ; 
             Species specie = getSpecies().get(randIndex);
             
             if( specie.getGenomes().size() > 0 )
        	 {
            	 children.add(breedChild(specie));
        	 }
             
    	 }
     }
     
     //place those children into species that are most similar to them.. or maybe they are new unique species
     for (int c=0; c < children.size() ; c++)
     {
    	 Genome child = children.get(c);
         addToSpecies(child);
     }
            
     generation++;
     
    
   //  writeFile("backup." .. pool.generation .. "." .. forms.gettext(saveLoadFile))
		
	}

private void rankGlobally() {
	
	List<Genome> genomes = new ArrayList<Genome>();
//	   local global = {}
	   
       for (int s = 0; s < getSpecies().size() ;s++ )
       {
               Species specie = getSpecies().get(s);
               for (int g = 0; g < specie.getGenomes().size();g++)
            	 {
            	   genomes.add( specie.getGenomes().get(g));
            	    
            	 }
                  
       }
       
       
       
       Collections.sort(genomes,new Comparator<Genome>(){

			@Override
			public int compare(Genome g1, Genome g2) 	
			{
				return g1.getFitness() < g2.getFitness() ? 1 :
	               (g1.getFitness() == g2.getFitness() ? 0 : -1);     				
			}
        	
			//ranks in descending order so most fit is at 0
			
			  //table.sort(global, function (a,b)
		      //         return (a.fitness < b.fitness)
		      // end)
        });
       
     
       
       
      
       for (int g=0;g< genomes.size(); g++)
    	   {
    	   genomes.get(g).setGlobalRank(g);
    	   }
	
}

public void  addToSpecies(Genome child)
{
boolean foundSpecies = false;

for (int s=0; s < getSpecies().size() ; s++ ){
	
        Species specie = getSpecies().get(s);
        
        if (!foundSpecies && !specie.getGenomes().isEmpty() && child.sameSpeciesAs(specie.getGenomes().get(0))   ) 
        {
        	specie.getGenomes().add(child);                 
            foundSpecies = true;
        }
        
}

	if (!foundSpecies )
	{
        Species childSpecies = new Species();
       
        childSpecies.getGenomes().add(child);
        getSpecies().add(childSpecies);
         
	}
}





private void calculateAverageFitness(Species specie)
{
        int total = 0;
       
        
        for (int g=0; g < specie.getGenomes().size(); g++)  
        {
                Genome genome = specie.getGenomes().get(g);
                total = total + genome.getGlobalRank();
        }
       
        if(specie.getGenomes().size() > 0 )
        {
        	specie.setAverageFitness( total / specie.getGenomes().size() );
        }
}
 
private int totalAverageFitness()
{
        int total = 0;
        
        for (int s = 0;s < getSpecies().size(); s++)
        {
                Species specie = getSpecies().get(s);
                total = total + specie.getAverageFitness();
        }
 
        return total ;
}




private Genome  breedChild(Species specie){
		Genome child = new Genome();
        
        
        if ( rand.nextFloat() < child.CrossoverChance )  //should it be the childs crossoverchance or a static one?
        {        	
        		int index1 = rand.nextInt(specie.getGenomes().size() ) ; 
                Genome g1 = specie.getGenomes().get(index1);
                int index2 = rand.nextInt(specie.getGenomes().size()  ) ; 
                Genome g2 = specie.getGenomes().get(index2);
                child = crossover(g1, g2);
        }else{
        		int index = rand.nextInt(specie.getGenomes().size()  ) ; 
        		Genome g = specie.getGenomes().get(index);
                child = Genome.copy(g);
        }
       
        mutate(child);
       
        return child;
}






private Genome crossover(Genome g1, Genome g2) //splice two genomes together
{
       // -- Make sure g1 is the higher fitness genome
        if (g2.fitness > g1.fitness )
        {
                Genome tempg = g1;
                g1 = g2;
                g2 = tempg;
        }
 
        Genome  child = new Genome();
       
        HashMap<Integer,Gene> innovations2 = new HashMap<Integer,Gene>();
        
        
        for (int i=0; i < g2.getGenes().size(); i++ )
        {
        		Gene gene = g2.getGenes().get(i);
        		
        		innovations2.put(gene.getInnovation(), gene);
                
        }
       
        for (int i=0; i < g1.getGenes().size(); i++) 
        {
                Gene gene1 = g1.getGenes().get(i);
                Gene gene2 = innovations2.get(gene1.getInnovation());
                
                if (gene2 != null && rand.nextBoolean() && gene2.isEnabled())
                {
                	child.getGenes().add(gene2.copy());
                }else{
                	child.getGenes().add(gene1.copy());
                }
        }
       
        //set to the max
        if( g1.maxneuron > g2.maxneuron)
        {
        	child.maxneuron = g1.maxneuron;
        }else{
        	child.maxneuron = g2.maxneuron;
        }
        
       
        for(String key : g1.mutationRates.keySet()) //give the child the mutations of g1
        {
        	child.mutationRates.put(key, g1.mutationRates.get(key));
        	
        }
        
        return child;
}
 



 
private void removeStaleSpecies()
{
       // local survived = {}
 
        for (int  s = 0; s < getSpecies().size(); s++)
        	{
               Species specie = getSpecies().get(s);
               
               
               
               Collections.sort(specie.getGenomes() ,new Comparator<Genome>(){

       			@Override
       			public int compare(Genome g1, Genome g2) 	
       			{
       				return g1.getFitness() < g2.getFitness() ? 1 :
       	               (g1.getFitness() == g2.getFitness() ? 0 : -1);     				
       			}
               	
               });
              
               
              
               
                if (!specie.getGenomes().isEmpty() && specie.getGenomes().get(0).fitness > specie.topFitness )
                {
                        specie.topFitness = specie.getGenomes().get(0).fitness ;
                        specie.staleness = 0 ;
                }else{
                        specie.staleness = specie.staleness + 1 ;
                }
                
                if (specie.staleness > StaleSpecies && specie.topFitness < maxFitness) 
                {
                	getSpecies().remove(specie);
                       
                }
        	}
 
       
}
 
private void removeWeakSpecies()
{
       //List<Species> survivalists = new ArrayList<Species>();
 
        float sum = totalAverageFitness();
         
        
        if(sum > 0)
        {
        for (int s = 0; s < getSpecies().size();s++)
        {
                Species specie = getSpecies().get(s);
                
               // float breed = FastMath.floor( (specie.averageFitness / sum) * Population );
                
                float breed =  (specie.averageFitness / sum) * Population ;
                
                if (breed < 1f )
                {
                	System.out.println("removing weak breed " + breed + " " + specie);
                	species.remove(specie);
                }else{
                	System.out.println("keeping strong breed " + breed + " " + specie);
                	
                }
        }
 
        }
}
 

public int getPopulation() {
	return Population;
}
/*
	public JSONObject getAsJson() {
	 
		JSONObject obj=new JSONObject();
		obj.put("generation",generation);
		obj.put("maxFitness",maxFitness);
		obj.put("species", species );
	  
	  
		return obj;
	}

	public void loadFromJSON(JSONObject json)
	{
		this.generation = (int) json.get("generation");
		this.maxFitness = (int) json.get("maxFitness");
		this.species = (List<Species>) json.get("species");
	}
*/


}
