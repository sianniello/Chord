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
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import de.flexiprovider.core.FlexiCoreProvider;
import node.Forwarder;
import node.Node;
import node.Request;

public class Cryptography {

	private static PrivateKey pvtKey;
	private static PublicKey pubKey;
	private static String secretKey;

	private PublicKey pubKeyTarget;

	public static void keyGeneration() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new FlexiCoreProvider());
			kpg.initialize(1024);
			KeyPair keyPair = kpg.generateKeyPair();
			pvtKey = keyPair.getPrivate();
			pubKey = keyPair.getPublic();
			secretKey = "";
			for(int i = 0; i <= new Random().nextInt(20) + 10; i++)
				secretKey += (char)(new Random().nextInt(26) + 'a');
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}

	public static File encrypt(File file) {
		SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "AES");
		File ef = new File(file.getName().split("[.]")[0] + ".crypt");
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, key);

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

	public static File decrypt(File file) {
		SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "AES");
		File df = new File(file.getName().replaceAll(".crypt", ""));
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, key);
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

	public void pubKeyReq(Node succ) {
		try {
			new Forwarder().send(new Request(succ.getAddress(), Request.pubKey_REQ));
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

	public static PublicKey getPubKey() {
		return pubKey;
	}

	public static void setPubKey(PublicKey pubKey) {
		Cryptography.pubKey = pubKey;
	}

	public static byte[] encryptSecretKey(String sk, PublicKey pk) {
		byte[] cipherText = null;
		try {
			// get an RSA cipher object and print the provider
			final Cipher cipher = Cipher.getInstance("RSA");
			// encrypt the plain text using the public key
			cipher.init(Cipher.ENCRYPT_MODE, pk);
			cipherText = cipher.doFinal(sk.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cipherText;
	}

	public static String decryptSecretKey(byte[] sk) {
		byte[] dectyptedText = null;
		try {
			// get an RSA cipher object and print the provider
			final Cipher cipher = Cipher.getInstance("RSA");

			// decrypt the text using the private key
			cipher.init(Cipher.DECRYPT_MODE, pvtKey);
			dectyptedText = cipher.doFinal(sk);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new String(dectyptedText);
	}
}
