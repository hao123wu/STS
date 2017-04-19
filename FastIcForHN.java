package methods.basic.ours.IC.fastIC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import methods.Method;
import methods.basic.ours.IC.WnNominalIC;
import methods.basic.ours.IC.sentenceISmapping.AlignmentAssistedNominalizationMapping;
import methods.basic.ours.IC.sentenceISmapping.IcISmapping;
import openAndLoad.Input;
import openAndLoad.LoadRamWN;
import tools.Linear;
import edu.mit.jwi.item.ISynsetID;

//It is suit for hierarchical network such as WordNet.
public class FastIcForHN implements Method{

	public WnNominalIC wnNominalIC;
	
	public List<List<ISynsetID>> sentencesList;
	public IcISmapping wsd;
	public AlignmentAssistedNominalizationMapping aanISmapping;
//	public IcWsdOriginal owsd=new IcWsdOriginal();

	public FastIcForHN(){
		this.wnNominalIC=new WnNominalIC(new LoadRamWN().dict);
		this.wsd=new IcISmapping(wnNominalIC);
		this.aanISmapping=new AlignmentAssistedNominalizationMapping(wnNominalIC);
	}
	
	public FastIcForHN(WnNominalIC wnIC){
		this.wnNominalIC=wnIC;
		this.wsd=new IcISmapping(wnIC);
		this.aanISmapping=new AlignmentAssistedNominalizationMapping(wnIC);
	}
	
	public double score(Input input){
		sentencesList=aanISmapping.getSentencePairSynsetIDlist(input);

		double sentence1IC=getNominalSynsetIDlistTotalIC(sentencesList.get(0));		
		double sentence2IC=getNominalSynsetIDlistTotalIC(sentencesList.get(1));
		sentencesList.get(0).addAll(sentencesList.get(1));
		double twoSentencesTotalIC=getNominalSynsetIDlistTotalIC(sentencesList.get(0));		
		double twoSentencesCommonIC=sentence1IC+sentence2IC-twoSentencesTotalIC;
		if(twoSentencesTotalIC==0){
			return 0.0;
		}
		double similarity=twoSentencesCommonIC/twoSentencesTotalIC;
		
		return similarity;
	}

	//kernel algorithm part 1: fast IC union of multiple synsetIDs FOR hierarchical network knowledge base!!!
	public double getNominalSynsetIDlistTotalIC(List<ISynsetID> synsetIDlist){

		if(synsetIDlist==null||synsetIDlist.size()==0){
			return 0d;
		}
		
		synsetIDlist=Linear.getUniqueElementsList(synsetIDlist);
		Set<List<ISynsetID>> originalInformationSpace=new HashSet<List<ISynsetID>>();				
		double totalIC=0d;
		for(int i=0;i<synsetIDlist.size();i++){
			if(synsetIDlist.get(i)==null){
				continue;
			}
			List<ISynsetID> intersectionPoints=getIntersectionPoints(originalInformationSpace, synsetIDlist.get(i));
			double informationGain=wnNominalIC.getNominalSynsetIDic(synsetIDlist.get(i))-getNominalSynsetIDlistTotalIC(intersectionPoints);
			totalIC+=informationGain;
		}

		return totalIC;
	}
	
	//kernel algorithm part 2.
	public List<ISynsetID> getIntersectionPoints(Set<List<ISynsetID>> originalInformationSpace, ISynsetID increasedPoint){
		
		List<ISynsetID> intersectionPointList =new ArrayList<ISynsetID>();
		Set<List<ISynsetID>> newRootPaths=wnNominalIC.subsumer.getRootPaths(increasedPoint);

		if(originalInformationSpace.size()==0){
			originalInformationSpace.addAll(newRootPaths);
			return intersectionPointList;
		}
		
		Set<List<ISynsetID>> toBeAddedRootPaths=new HashSet<List<ISynsetID>>();
		Set<List<ISynsetID>> toBeRemovedRootPaths=new HashSet<List<ISynsetID>>();
		outerLoop:  for(List<ISynsetID> newRootPath:newRootPaths){
			int intersectionPos=newRootPath.size()-1;
			for(List<ISynsetID> originalList:originalInformationSpace){			
				int[] tempPos=Linear.getTwoListsIntersectionPosition(originalList, newRootPath);
				if(tempPos[1]==0){
					intersectionPointList.add(increasedPoint);
					break outerLoop;
				}
				if(tempPos[0]==0){
					toBeRemovedRootPaths.add(originalList);
				}
				if(tempPos[1]<intersectionPos){
					intersectionPos=tempPos[1];
				}
			}
			
			toBeAddedRootPaths.add(newRootPath);
			intersectionPointList.add(newRootPath.get(intersectionPos));
		}
		
		if(toBeRemovedRootPaths.size()>0){
			originalInformationSpace.removeAll(toBeRemovedRootPaths);
		}
		
		if(toBeAddedRootPaths.size()>0){
			originalInformationSpace.addAll(toBeAddedRootPaths);
		}
		
		return intersectionPointList;
	}

	public static void main(String[] args){
		
		FastIcForHN fic=new FastIcForHN();
//		fic.sentencesList=fic.owsd.makeSureSynsetIDListOfWordsMethod3("a cushion is a fabric case filled with soft material , which you put on a seat to make it more comfortable .	a pillow is a rectangular cushion which you rest your head on when you are in bed .", fic.wnIC);
//		fic.sentencesList.get(0).addAll(fic.sentencesList.get(1));
		System.out.println(fic.getNominalSynsetIDlistTotalIC(fic.sentencesList.get(1)));

	}
}
