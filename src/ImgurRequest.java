import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * now you need to have an instance.
 * because only one request at a time.
 * new request? new instance.
 * later, TODO, make there be a busy
 * variable or something.
 * @author Marcus
 *
 */
public class ImgurRequest {

	// used if no path is pre specified.
	private String baseDir = System.getenv("USERPROFILE") + "\\Desktop\\Imgur\\";

	// my personal API key so i can access imgur api and stuff
	private String CLIENT_ID = "76535d44f1f94da";

	//to track progress
	//prediction because divide by zero errors are infectious
	private volatile int totalImages = 0, imagesComplete = 0, predictedTotal = 1;
	
	//busy? ornahhhh
	private boolean busy = false;
	
	//have we scanned all the pages in the current request?
	private boolean scannedAllPages = true;
	
	//title so this can have a label
	private String title = "";
	
	// sort is usually time but i thought i'd be nice to yall.
	public void saveSubreddit(final String subreddit, final int pages, final String sort) {
		totalImages = imagesComplete = 0;
		scannedAllPages = false;
		busy = true;
		predictedTotal = pages * 60;
		title = subreddit;
		new Thread(new Runnable() {public void run() {

			// https://api.imgur.com/3/gallery/r/{subreddit}/{sort}/{page}
			for (int page = 0; page < pages; page++) {
				try {
					String path = "https://api.imgur.com/3/gallery/r/" + subreddit + "/" + sort + "/" + page + ".json";

					HttpURLConnection connection = (HttpURLConnection) ((new URL(path)).openConnection());

					connection.setRequestMethod("GET");
					connection.addRequestProperty("Authorization", "client-id " + CLIENT_ID);
					connection.connect();
					
					if(connection.getResponseCode() == 200) { 
						
						InputStream response = connection.getInputStream();
						saveImages(response);
						
					}else{
						title = "error code " + connection.getResponseCode();
						busy = false;
					}
					
					scannedAllPages = true;
					
				} catch (Exception e) {
					e.printStackTrace();
					busy = false;
				}
			}
			
		}}).start();
		
		
	}
	
	public void saveImages(final InputStream response) {
		new Thread(new Runnable() {
			public void run() {
				try {
					ObjectMapper om = new ObjectMapper();

					JsonNode root;
					
					root = om.readTree(response);
					
					JsonNode imagesNode = root.get("data");
					Iterator<JsonNode> imagesIterator = imagesNode.iterator();

					// count em up first
					int images = 0;
					while (imagesIterator.hasNext()) {
						images++;
						imagesIterator.next();
					}
					totalImages += images;
					imagesIterator = imagesNode.iterator();

					int imageCounter = 1;
					while (imagesIterator.hasNext()) {

						JsonNode item = imagesIterator.next();

						String id = item.get("id").asText();
						// boolean album = item.get("is_album").asBoolean();
						int fileSize = item.get("size").asInt();

						String extension = item.get("type").asText();
						extension = parseExtension(extension);

						String subreddit = item.get("section").asText();
						
						saveID(id, subreddit + "\\", extension, fileSize);

						imagesComplete ++;
						
						imageCounter++;
					}
					
					if(imagesComplete == totalImages) {
						busy = false;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private String parseExtension(String ext) {
		if (ext.equals("image/jpeg")) {
			return ".jpg";
		}
		if (ext.equals("image/png")) {
			return ".png";
		}
		if (ext.equals("image/gif")) {
			return ".gif";
		}
		// eventually fill this with all possible cases
		// nvm, i think this is all cases.
		return ".jpg";
	}

	public void saveID(String hash, String subfolder, String extension, int filesize) {
		try {
			
			// make sure our directories exist no matter what
			new File(baseDir + subfolder).mkdirs();
			new File(baseDir + "backgrounds\\" + subfolder).mkdirs();

			// if we haven't fully saved this yet...
			if (!(new File(baseDir + subfolder + hash + extension).length() == filesize)) {
				InputStream in = new URL("http://i.imgur.com/" + hash + extension).openConnection().getInputStream();
				OutputStream out = new FileOutputStream(baseDir + subfolder + hash + extension);
				copy(in, out);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(hash);
			System.out.println(extension);
			System.out.println("" + filesize);
		}
	}

	/**
	 * to copy one stream thing to another stream thing. it goes sanic fast.
	 * 
	 * @param input
	 * @param output
	 */
	private void copy(InputStream input, OutputStream output) {
		try {
			IOUtils.copy(input, output);
		} catch (Exception e) {
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
				}
			}
		}
	}

	
	public double getProgress() {
		return scannedAllPages ? (double)imagesComplete/(double)totalImages : (double)imagesComplete/(double)predictedTotal;
	}
	
	public int getImagesDiscovered() {
		return totalImages;
	}

	public int getImagesDownloaded() {
		return imagesComplete;
	}
	
	public String getTitle() {
		return title;
	}

	public boolean isBusy() {
		return busy;
	}

	// you deserve a break
	// Eugenia Suarez
	// Mery del Cerro
	// google them for a good time
}
