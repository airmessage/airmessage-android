package me.tagavari.airmessage.task;

import android.webkit.URLUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;

//Based on https://github.com/PonnamKarthik/RichLinkPreview (no longer maintained)
public class RichPreviewTask {
	@CheckReturnValue
	public static Single<Metadata> fetchMetadata(String url) {
		return Single.create((SingleEmitter<Metadata> emitter) -> {
			//Creating the metadata
			Metadata metaData = new Metadata();
			
			try {
				//Connecting to the site
				Document document = Jsoup.connect(url)
						.timeout(15 * 1000)
						.get();
				
				//Finding meta elements
				Elements elements = document.getElementsByTag("meta");
				
				//Getting the article title
				{
					String title = document.select("meta[property=og:title]").attr("content");
					if(title == null || title.isEmpty()) title = document.title();
					metaData.setTitle(title);
				}
				
				//Getting the article description
				{
					String description = document.select("meta[name=description]").attr("content");
					if(description == null || description.isEmpty()) description = document.select("meta[name=Description]").attr("content");
					if(description == null || description.isEmpty()) description = document.select("meta[property=og:description]").attr("content");
					metaData.setDescription(description);
				}
				
				// getMediaType
				Elements mediaTypes = document.select("meta[name=medium]");
				String type;
				if(mediaTypes.size() > 0) {
					String media = mediaTypes.attr("content");
					type = media.equals("image") ? "photo" : media;
				} else {
					type = document.select("meta[property=og:type]").attr("content");
				}
				metaData.setMediaType(type);
				
				//getImages
				Elements imageElements = document.select("meta[property=og:image]");
				if(imageElements.size() > 0) {
					String image = imageElements.attr("content");
					if(!image.isEmpty()) {
						metaData.setImageURL(resolveURL(url, image));
					}
				}
				if(metaData.getImageURL().isEmpty()) {
					String src = document.select("link[rel=image_src]").attr("href");
					if(!src.isEmpty()) {
						metaData.setImageURL(resolveURL(url, src));
					} else {
						src = document.select("link[rel=apple-touch-icon]").attr("href");
						if(!src.isEmpty()) {
							metaData.setImageURL(resolveURL(url, src));
							metaData.setFavicon(resolveURL(url, src));
						} else {
							src = document.select("link[rel=icon]").attr("href");
							if(!src.isEmpty()) {
								metaData.setImageURL(resolveURL(url, src));
								metaData.setFavicon(resolveURL(url, src));
							}
						}
					}
				}
				
				//Getting the favicon
				{
					String src = document.select("link[rel=apple-touch-icon]").attr("href");
					if(src == null || src.isEmpty()) src = document.select("link[rel=icon]").attr("href");
					if(src == null || src.isEmpty()) metaData.setFavicon(resolveURL(url, src));
				}
				
				//Resolving basic site information
				for(Element element : elements) {
					if(!element.hasAttr("property")) continue;
					
					String property = element.attr("property").trim();
					if(property.equals("og:url")) metaData.setUrl(element.attr("content"));
					else if(property.equals("og:site_name")) metaData.setSiteName(element.attr("content"));
				}
				
				//Checking if an OG url couldn't be found
				if(metaData.getUrl().isEmpty()) {
					try {
						//Attempting to resolve the hostname from the original URL
						URI uri = new URI(url);
						metaData.setUrl(uri.getHost());
					} catch(URISyntaxException exception) {
						//Printing the stack trace
						exception.printStackTrace();
						
						//Falling back to the raw original URL
						metaData.setUrl(url);
					}
				}
				
				//Returning the metadata
				emitter.onSuccess(metaData);
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Emitting the error
				emitter.onError(exception);
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}
	
	private static String resolveURL(String url, String part) {
		if(URLUtil.isValidUrl(part)) {
			return part;
		} else {
			try {
				URI baseURI = new URI(url);
				baseURI = baseURI.resolve(part);
				return baseURI.toString();
			} catch(URISyntaxException | IllegalArgumentException exception) {
				exception.printStackTrace();
				return "";
			}
		}
	}
	
	public static class Metadata {
		private String url = "";
		private String imageURL = "";
		private String title = "";
		private String description = "";
		private String siteName = "";
		private String mediaType = "";
		private String favicon = "";
		
		public String getUrl() {
			return url;
		}
		
		public void setUrl(String url) {
			this.url = url;
		}
		
		public String getImageURL() {
			return imageURL;
		}
		
		public void setImageURL(String imageURL) {
			this.imageURL = imageURL;
		}
		
		public String getTitle() {
			return title;
		}
		
		public void setTitle(String title) {
			this.title = title;
		}
		
		public String getDescription() {
			return description;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
		public String getSiteName() {
			return siteName;
		}
		
		public void setSiteName(String siteName) {
			this.siteName = siteName;
		}
		
		public String getMediaType() {
			return mediaType;
		}
		
		public void setMediaType(String mediaType) {
			this.mediaType = mediaType;
		}
		
		public String getFavicon() {
			return favicon;
		}
		
		public void setFavicon(String favicon) {
			this.favicon = favicon;
		}
	}
}