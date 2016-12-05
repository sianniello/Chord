package cryptografy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import de.flexiprovider.core.FlexiCoreProvider;
import node.Forwarder;
import node.Node;
import node.Request;

public class Cryptography {

	private PrivateKey pvtKey;
	private PublicKey pubKey;
	
	private PublicKey pubKeyTarget;

	public void keyGeneration() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new FlexiCoreProvider());
			kpg.initialize(1024);
			KeyPair keyPair = kpg.generateKeyPair();
			pvtKey = keyPair.getPrivate();
			pubKey = keyPair.getPublic();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}

	public File encrypt(File file) {
		Security.addProvider(new FlexiCoreProvider());
		File ef = new File(file.getName() + ".crypt");
		try {
			Cipher cipher = Cipher.getInstance("RSA", new FlexiCoreProvider());
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);

			FileInputStream fis = new FileInputStream(file);
			FileOutputStream fos = new FileOutputStream(ef);
			CipherOutputStream cos = new CipherOutputStream(fos, cipher);
			byte[] block = new byte[32];
			int i;
			while ((i = fis.read(block)) != -1) {
				cos.write(block, 0, i);
			}
			fis.close();
			cos.close();
		} catch (InvalidKeyException | IOException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return ef;
	}

	public File decrypt(File file) {
		Security.addProvider(new FlexiCoreProvider());
		File df = new File(file.getName().replaceAll(".crypt", ""));
		try {
			Cipher cipher = Cipher.getInstance("RSA", new FlexiCoreProvider());
			cipher.init(Cipher.DECRYPT_MODE, pvtKey);
			FileInputStream fis = new FileInputStream(file);
			CipherInputStream cis = new CipherInputStream(fis, cipher);
			FileOutputStream fos = new FileOutputStream(df);
			byte[] block = new byte[32];
			int i;
			while ((i = cis.read(block)) != -1) {
				fos.write(block, 0, i);
			}
			fos.close();
			cis.close();
		} catch (InvalidKeyException | IOException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return df; 
	}

	public void pubKeyReq(Node succ, Node n) {
		try {
			new Forwarder().send(new Request(succ.getAddress(), Request.pubKey_REQ, n));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public PublicKey getPubKeyTarget() {
		return pubKeyTarget;
	}

	public void setPubKeyTarget(PublicKey pubKeyTarget) {
		this.pubKeyTarget = pubKeyTarget;
	}
}
