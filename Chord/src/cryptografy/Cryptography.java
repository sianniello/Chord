package cryptografy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import de.flexiprovider.core.FlexiCoreProvider;
import node.Forwarder;
import node.Node;
import node.Request;

public class Cryptography {

	protected static PrivateKey pvtKey;
	private static PublicKey pubKey;
	private static String secretKey;
	private PublicKey pubKeyTarget;	//public key of receiver

	private static SecretKey secKey;
	private static byte[] encryptSecretKey;

	public static void keyGeneration() {
		Security.addProvider(new FlexiCoreProvider());
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "FlexiCore");
			kpg.initialize(1024);
			KeyPair keyPair = kpg.generateKeyPair();
			pvtKey = keyPair.getPrivate();
			pubKey = keyPair.getPublic();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			e.printStackTrace();
		}

	}

	public static File encrypt(File file) {
		File ef = new File(file.getName().split("[.]")[0] + ".crypt");
		try {
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			generator.init(128); // The AES key size in number of bits
			secKey = generator.generateKey();

			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secKey);

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

	/**
	 * Decrypt file
	 * @param file
	 * @return
	 */
	public static File decrypt(File file, String k) {
		SecretKeySpec key = new SecretKeySpec(k.getBytes(), "AES");
		File df = new File(file.getName().replaceAll("crypt", "txt"));
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
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

	/**
	 * Encrypt secret key with RSA public key
	 * @param plain secret key
	 * @param public key of target
	 * @return 
	 */
	public static byte[] encryptSecretKey(PublicKey pk) {
		byte[] cipherText = null;
		try {
			Cipher cipher = Cipher.getInstance("RSA", "FlexiCore");
			// encrypt the plain text using the public key
			cipher.init(Cipher.ENCRYPT_MODE, pk);
			cipherText = cipher.doFinal(secKey.getEncoded());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encryptSecretKey = cipherText;
	}

	public static String decryptSecretKey(byte[] sk) {
		byte[] dectyptedText = null;
		try {
			// get an RSA cipher object and print the provider
			final Cipher cipher = Cipher.getInstance("RSA", "FlexiCore");

			// decrypt the text using the private key
			cipher.init(Cipher.DECRYPT_MODE, pvtKey);
			dectyptedText = cipher.doFinal(sk);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new String(dectyptedText);
	}

	public static String getSecretKey() {
		return secretKey;
	}
}
