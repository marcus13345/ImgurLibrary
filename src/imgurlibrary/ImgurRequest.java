package imgurlibrary;

import java.awt.image.BufferedImage;
import java.io.File;

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

	public abstract double getScanProgress(); // idunno how YOU want to
												// calculate this but uh

	public final boolean isBusy() { // i aint doing it for you so....
		return busy; // ~All interfaces ever
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
