package version1;
/*Class : Remove pages with less freq_of_occurance.*/
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

public class PruneBottomPages {

	public static void main(String[] args) {
		// dummy
	}

    /*Inputs map of Pageid to freq_of_PageId.
     * Returns map ranked in the decreasing order of freq_of_PageId.
     */
	<K,V extends Comparable<? super V>>
	SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
		SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
				new Comparator<Map.Entry<K,V>>() {
					@Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
						if(e2.getValue().compareTo(e1.getValue())==0 || e2.getValue().compareTo(e1.getValue())>0)
							return 1;
						else
							return -1;
					}
				}
				);
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}

    /*Inputs map of Pageid to freq_of_PageId.
     * Ranks the map by the decreasing order of freq_of_PageId.
     * Retuns top 20% of the ranked Map.
     * Returning 20% by the 80-20 power law*/
	public HashMap<String, Integer> prune(String a,Map<String, Integer> pga){
		HashMap<String, Integer> pruned = new HashMap<>();
		int count = 0; 
		//System.out.println(" initial "+ pga.size());
		int limit = (int) Math.ceil(pga.size() / 5.0);
		for(Entry<String, Integer> i:entriesSortedByValues(pga)){
			pruned.put(i.getKey(), i.getValue());
			if(count++ > limit)
				break;
		}
		//System.out.println(" now "+pruned.size());
		return pruned;
	}//End prune()

	/*Return only the top lim percentage of the links*/
	public HashMap<Long, Integer> prune(Map<Long, Integer> pga, int lim){
		HashMap<Long, Integer> pruned = new HashMap<>();
		int count = 0;
		int limit = (int) Math.ceil(pga.size() * lim / 100.0);
		for(Entry<Long, Integer> i:entriesSortedByValues(pga)){
			pruned.put(i.getKey(), i.getValue());
			if(count++ > limit)
				break;
		}
		return pruned;
	}//End prune()

	
    /*Inputs map of Pageid to freq_of_PageId.
     * Ranks the map by the decreasing order of freq_of_PageId.
     * Returns PageId of the top entry in the ranked Map.*/
	public String getTop(String a,Map<String, Integer> pga){
		for(Entry<String, Integer> i:entriesSortedByValues(pga)){
			return i.getKey();
		}
		return null;
	}//End getTop()

}
