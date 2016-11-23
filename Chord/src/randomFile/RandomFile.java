package randomFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomFile {

	private String str;
	private File file;
	private final static int k = 0;

	public RandomFile() throws IOException {
		Random r = new Random();
		BufferedWriter writer = null;
		file = new File("file" + k);
		for(int i = 0; i <= ThreadLocalRandom.current().nextInt(1, 1000 + 1); i++)
			str = str + (char)(r.nextInt(26) + 'a');

		writer = new BufferedWriter(new FileWriter(file));
		try {
			writer.write(str);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public File getFile() {
		return file;
	}

}
