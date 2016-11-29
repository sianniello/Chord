package randomFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class RandomFile {

	private File file;
	private static int k = 0;
	private final static int l = 10000;

	public RandomFile() throws IOException {
		Random r = new Random();
		BufferedWriter writer = null;
		file = new File("file" + k + ".txt");
		String str = "";
		for(int i = 0; i <= r.nextInt(l); i++)
			str = str + ((char)(r.nextInt(26) + 'a'));

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
