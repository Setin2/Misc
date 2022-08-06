import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class Huffman {

    // alphabet size of extended ASCII
    private static final int R = 256;

    // Do not instantiate.
    private Huffman() { }

    // Huffman trie node
    private static class Node implements Comparable<Node> {
        private final char ch;
        private final int freq;
        private final Node left, right;

        Node(char ch, int freq, Node left, Node right) {
            this.ch    = ch;
            this.freq  = freq;
            this.left  = left;
            this.right = right;
        }

        // is the node a leaf node?
        private boolean isLeaf() {
            assert ((left == null) && (right == null)) || ((left != null) && (right != null));
            return (left == null) && (right == null);
        }

        // compare, based on frequency
        public int compareTo(Node that) {
            return this.freq - that.freq;
        }
    }

    /**
     * Reads a sequence of 8-bit bytes from standard input; compresses them
     * using Huffman codes with an 8-bit alphabet; and writes the results
     * to standard output.
     */
    public static void compress() {
        // read the input
		
		try{
			FileInputStream fileInputStream = new FileInputStream("decreasing.txt");
			FileOutputStream fileOutputStream = new FileOutputStream("please.txt");
			BitOutputStream bitOutputStream = new BitOutputStream(fileOutputStream);
			
			String s = null;

		try{
			int i = 0;
			do {
				i = fileInputStream.read();

				if (i >= 0){
					s = s + i;
				}
					
			} while (i != -1);
		} catch (IOException e){
			e.printStackTrace();
		}
	
        char[] input = s.toCharArray();

        // tabulate frequency counts
        int[] freq = new int[R];
        for (int x = 0; x < input.length; x++)
            freq[input[x]]++;

        // build Huffman trie
        Node root = buildTrie(freq);

		for (int x = 0; x < input.length; x++){
			System.out.print(freq[x]);
		}
		
		System.out.println();
	
        // build code table
        String[] st = new String[R];
        buildCode(st, root, "");

        // use Huffman code to encode input
        for (int x = 0; x < input.length; x++) {
            String code = st[input[x]];
            for (int j = 0; j < code.length(); j++) {
                if (code.charAt(j) == '0') {
                    bitOutputStream.writeBit(0);
					System.out.print(0);
                }
                else if (code.charAt(j) == '1') {
                    bitOutputStream.writeBit(1);
					System.out.print(1);
                }
            }
        }
		
		} catch(IOException e){
			e.printStackTrace();
		}
		

    }

    // build the Huffman tree given frequencies
    private static Node buildTrie(int[] freq) {

        // initialze priority queue with singleton trees
        PQHeap pq = new PQHeap();
        for (char c = 0; c < R; c++)
            if (freq[c] > 0)
                pq.insert(new Element(0, new Node(c, freq[c], null, null)));

        // merge two smallest trees
        while (pq.size() > 1) {
            Node left  = (Node) pq.extractMin().getData();
            Node right = (Node) pq.extractMin().getData();
            Node parent = new Node('\0', left.freq + right.freq, left, right);
            pq.insert(new Element (0, parent));
        }
        return (Node) pq.extractMin().getData();
    }

    // make a lookup table from symbols and their encodings
    private static void buildCode(String[] st, Node x, String s) {
        if (!x.isLeaf()) {
            buildCode(st, x.left,  s + '0');
            buildCode(st, x.right, s + '1');
        }
        else {
            st[x.ch] = s;
        }
    }

    /**
     * Sample client that calls {@code compress()} if the command-line
     * argument is "-" an {@code expand()} if it is "+".
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        compress();
    }

}
