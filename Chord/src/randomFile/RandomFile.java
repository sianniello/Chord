package randomFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * This class generate a text file with random characters
 * @author Stefano
 *
 */
public class RandomFile {

	private File file;
	private static int k = 0;
	private final static int c = 1000;	//max number of characters
	private final static int l = 1000;	//max number of lines

	public RandomFile() throws IOException {
		Random r = new Random();
		BufferedWriter writer = null;
		file = new File("file" + k + ".txt");
		String str = "";
		for(int j = 0; j < r.nextInt(l) + 1; j++) {
			for(int i = 0; i <= r.nextInt(c) + 50; i++)
				str = str + ((char)(r.nextInt(26) + 'a'));
			str+="\n";
		}
		writer = new BufferedWriter(new FileWriter(file));
		try {
			writer.write(str);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		k++;
	}

	public File getFile() {
		return file;
	}

}
