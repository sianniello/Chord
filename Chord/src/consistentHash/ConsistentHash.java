package consistentHash;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class ConsistentHash<T> {

	private final int numberOfReplicas;
	private final SortedMap<Long, T> circle = new TreeMap<Long, T>();
	private HashFunction hashFunction;

	public ConsistentHash(int numberOfReplicas, Collection<T> nodes) {
		this.hashFunction = Hashing.md5();
		this.numberOfReplicas = numberOfReplicas;

		for (T node : nodes) {
			add(node);
		}
	}

	public void add(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.put(hashFunction.hashString(node.toString() + i, Charset.defaultCharset()).asLong(), node);
		}
	}

	public void remove(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.remove(hashFunction.hashString(node.toString() + i, Charset.defaultCharset()).asLong(), node);
		}
	}

	public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hashFunction.hashString(key.toString(), Charset.defaultCharset()).asLong();
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

}
