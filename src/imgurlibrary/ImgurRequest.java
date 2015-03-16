package imgurlibrary;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * this is the root class for any and all imgur downloading requests.
 * not super because some wrappers exist. anyways, it tells requests
 * where to download and where to trigger event for new images being downloaded.
 * as well, it stores my imgur API credentials so one can access the API.
 * 
 * @author mgosselin
 *
 */

public abstract class ImgurRequest implements Runnable {
	public static String LOCAL_ROOT = System.getenv("USERPROFILE") + "\\Desktop\\imgur";
	protected final String AuthorizationToken = "76535d44f1f94da";
	public final String name;
	protected boolean busy;
	private final ImageListener listener;
	private volatile int downloaded, discovered, failed;
	
	static {
		if (!new File(LOCAL_ROOT).exists()) {
			new File(LOCAL_ROOT).mkdirs();
		}
	}

	protected synchronized final void discovered(int i) {
		discovered+=i;
	}
	
	protected synchronized final void discovered() {
		discovered++;
	}
	
	protected synchronized final void downloaded(int i) {
		downloaded+=i;
	}

	protected synchronized final void downloaded() {
		downloaded++;
	}

	protected synchronized final void failed(int i) {
		failed+=i;
	}

	protected synchronized final void failed() {
		failed++;
	}
	
	protected void sendImage(BufferedImage image) {
		if (listener != null)
			listener.sendImage(image);
	}

	protected ImgurRequest(String name, ImageListener listener) {
		busy = true;
		this.name = name;
		discovered = 0;
		downloaded = 0;
		failed = 0;
		this.listener = listener;
	}

	public final double getProgress() {
		// bc divide by zero error
		return ((double)(downloaded + failed) / (discovered == 0 ? 1 : discovered));
	}

	
	public abstract double getScanProgress();
	
	public final boolean isBusy() {
		return busy;
	}

	public abstract void run();

	protected final String getExt(String MIME) {
		switch (MIME) {
		case "image/jpeg":
			return ".jpeg";
		case "image/png":
			return ".png";
		case "image/gif":
			return ".gif";
		default:
			return ".jpg";
		}
	}

	public int getImagesDiscovered() {
		return discovered;
	}

	public int getImagesDownloaded() {
		return downloaded;
	}

	public String getTitle() {
		return name;
	}
}
