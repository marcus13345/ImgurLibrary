package imgurlibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * this class is a class that will simply take a subreddit, and a number of pages
 * and download them to the path set out in the parents class, imgur request.
 * 
 * @author mgosselin
 *
 */

public class SubredditRequest extends ImgurRequest {
	private final int pages;
	private int pagesScanned;
	private static final String BASE_URL = "https://api.imgur.com/3/gallery/r/";
	private final String URL;
	private final String subreddit;
	private final String LOCAL_ROOT;

	public SubredditRequest(String subreddit, int pages, boolean async) {
		this(subreddit, pages, async, null);
	}

	public SubredditRequest(String subreddit, int pages, boolean async, ImageListener listener) {
		super(subreddit, listener);
		this.pages = pages;
		pagesScanned = 0;
		busy = true;
		URL = BASE_URL + subreddit + "/time/";
		this.subreddit = subreddit;
		LOCAL_ROOT = ImgurRequest.LOCAL_ROOT + File.separatorChar + subreddit;
		if (!new File(LOCAL_ROOT).exists()) {
			new File(LOCAL_ROOT).mkdirs();
		}
		if (async)
			new Thread(this).start();
		else
			run();
	}

	@Override
	public double getScanProgress() {
		return pagesScanned / (double) (pages);
	}

	@Override
	public void run() {
		// && busy so we can easily exit if a fatal error happens
		for (int page = 0; page < pages && busy; page++) {

			try {

				String path = "https://api.imgur.com/3/gallery/r/" + subreddit + "/time/" + page + ".json";

				HttpURLConnection connection = (HttpURLConnection) ((new URL(path)).openConnection());

				connection.setRequestMethod("GET");
				connection.addRequestProperty("Authorization", "client-id 76535d44f1f94da");
				connection.connect();

				if (connection.getResponseCode() == 200) {

					InputStream response = connection.getInputStream();

					savePage(response);

				} else {
					busy = false;
				}
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}
	}

	private void savePage(final InputStream response) {
		new Thread(new Runnable() {
			public void run() {
				try {
					ObjectMapper om = new ObjectMapper();
					JsonNode root = om.readTree(response);
					JsonNode data = root.get("data");
					Iterator<JsonNode> iterator = data.iterator();

					while (iterator.hasNext()) {
						iterator.next();
						discovered();
					}
					pagesScanned++;

					// reset the iterator ya doof
					iterator = data.iterator();

					while (iterator.hasNext()) {
						JsonNode image = iterator.next();
						// no albums here so no worries
						String id = image.get("id").asText();
						String ext = getExt(image.get("type").asText());
						InputStream in = new URL("http://i.imgur.com/" + id + ext).openConnection().getInputStream();
						OutputStream out = new FileOutputStream(new File(LOCAL_ROOT + File.separatorChar + id + ext));
						IOUtils.copy(in, out);
						downloaded();
						sendImage(ImageIO.read(new File(LOCAL_ROOT + File.separatorChar + id + ext)));
					}
				} catch (Exception e) {
					e.printStackTrace();
					failed();
				}
			}
		}).start();
	}
	
	public static void main(String[] args) {
		new SubredditRequest("derp", 1, true);
	}
}