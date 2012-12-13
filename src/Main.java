
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.*;
import org.htmlparser.util.*;

public class Main {

    private static Pattern lores = Pattern.compile("post_photolores=\"https?://([-\\w\\.]+)+(:\\d+)?(/([\\w/_\\.]*(\\?\\S+)?)?)?\"");
    private static List<String> endings = new ArrayList<>();

    static {
        Main.endings.add("_1280");
        Main.endings.add("_700");
        Main.endings.add("_700");
        Main.endings.add("_500");
        Main.endings.add("_400");
        Main.endings.add("_250");
        Main.endings.add("_100");
    }
    private static Gui gui = null;
    private static HashMap<Post, Post> post_post_hash = new HashMap<>();
    protected static HashMap<Picture, Picture> pic_pic_hash = new HashMap<>();
    private static HashMap<Picture, Post> pic_post_hash = new HashMap<>();
    private static HashMap<Post, Post> dup_post_list = new HashMap<>();
    private static String blogname = "";
    private static File blogdir = null;

    public static Set<Picture> getPictures() {
        return Main.pic_pic_hash.keySet();
    }

    public static File getBlogDir() {
        return Main.blogdir;
    }

    public static void setBlogName(String blogname) {
        Main.blogname = blogname;
        Main.blogdir = new File("." + File.separator + Main.blogname + File.separator);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            gui = new Gui();
            gui.setVisible(true);
        } else if (args.length == 3 || args.length == 4) {
            Main.setBlogName(args[0]);
            Main.load();
            if (args.length == 3) {
                int start, limit;
                try {
                    start = Integer.parseInt(args[1]);
                    limit = Integer.parseInt(args[2]);
                } catch (Exception e) {
                    Main.status("usage: Main <blogname> <start_page> <end_page> -hires");
                    Main.status("start_page and end_page must be integers >= 1");
                    return;
                }
                Main.run(start, limit);
                Main.save();
            } else if (args.length == 4 && args[3].equals("-hires")) {
                Main.downloadHiRes();
                Main.save();
            } else {
                Main.status("usage: Main <blogname> <start_page> <end_page> -hires");
            }
        } else {
            Main.status("usage: Main <blogname> <start_page> <end_page> -hires");
        }
    }

    protected static void reset() {
        Helper.removeDirectoryIfItExists(Helper.temp);
        Helper.removeDirectoryIfItExists(blogdir);
        Main.post_post_hash.clear();
        Main.pic_pic_hash.clear();
        Main.pic_post_hash.clear();
        Main.dup_post_list.clear();
    }

    public static void run(int start_page, int end_page) {
        if (start_page < 1 || end_page < 1) {
            Main.status("start_page and end_page must be integers >= 1");
            return;
        }
        int progress = 0;
        if (gui != null) {
            gui.setProgress(progress);
        }
        if (end_page >= start_page) {
            if (gui != null) {
                gui.setMaxProgress(end_page - start_page);
            }
            for (int i = start_page; i <= end_page; i++) {
                boolean exists = Main.handleURL(String.format("http://%s.tumblr.com/page/%s", Main.blogname, i));
                if (!exists) {
                    Main.status(String.format("We ran out of posts to process at page %s.", i));
                    break;
                }
                if (gui != null) {
                    gui.setProgress(progress);
                    progress++;
                }
            }
        } else {
            if (gui != null) {
                gui.setMaxProgress(start_page - end_page);
            }
            for (int i = start_page; i >= end_page; i--) {
                boolean exists = Main.handleURL(String.format("http://%s.tumblr.com/page/%s", Main.blogname, i));
                if (!exists) {
                    Main.status(String.format("We ran out of posts to process at page %s.", i));
                    break;
                }
                if (gui != null) {
                    gui.setProgress(progress);
                    progress++;
                }
            }
        }
        if (gui != null) {
            gui.setProgress(progress);
        }
        Main.writeDuplicates();
    }

    private static void writeDuplicates() {
        Main.status("Writing duplicates.");
        if (!dup_post_list.isEmpty()) {
            Main.status(String.format("%s\t%s", "older_post", "newer_post"));
            for (Post post : dup_post_list.keySet()) {
                Main.status(String.format("%s\t%s", post.post_id, dup_post_list.get(post).post_id));
            }
        } else {
            Main.status("There are no duplicates.");
        }
        Main.status("Writing duplicates done.");
    }

    public static void load() {
        Main.status("Loading databases.");
        File file = new File(blogdir, "picpic.db");
        List<Object> objects = Helper.loadObjectFromFile(file);
        if (objects == null || objects.size() != 1) {
            Main.error("Unable to load database files so creating new database.");
            reset();
        } else {
            Main.post_post_hash = (HashMap<Post, Post>) objects.get(0);
            Main.pic_pic_hash.clear();
            Main.pic_post_hash.clear();
            Main.dup_post_list.clear();
            Main.setupPosts();
        }
        Main.status("Done loading databases.");
    }

    public static void save() {
        Main.status("Saving databases.");
        File file = new File(blogdir, "picpic.db");
        List<Object> objects = new ArrayList<>();
        objects.add(Main.post_post_hash);
        Helper.saveObjectToFile(file, objects);
        Main.status("Done saving databases.");
    }

    private static boolean handleURL(String address) {
        Main.status(String.format("Processing page \"%s\".", address));
        try {
            NodeList posts = getPosts(address);
            if (posts.toNodeArray().length == 0) {
                return false;
            }
            for (Node post_node : posts.toNodeArray()) {
                if (post_node instanceof TagNode) {
                    TagNode post = (TagNode) post_node;
                    Post new_post = new Post(Long.parseLong(post.getAttribute("id").substring(5)));
                    if (!Main.post_post_hash.containsKey(new_post)) {
                        NodeList photo_posts = getPhotoPosts(post.getChildren());
                        NodeList remarks = getRemarks(photo_posts);
                        for (Node node : remarks.toNodeArray()) {
                            Matcher matcher = lores.matcher(node.getText());
                            String media_url = "";
                            if (matcher.find()) {
                                media_url = matcher.group();
                                media_url = media_url.substring(17, media_url.length() - 1);
                            }
                            String thumb = media_url.replace(media_url.substring(media_url.lastIndexOf("_"), media_url.lastIndexOf(".")), "_75sq");
                            URL thumb_url = new URL(thumb);
                            new_post.pictures.add(new Picture(new URL(media_url), thumb_url));
                        }
                        NodeList photoset_posts = getPhotosetPosts(post.getChildren());
                        NodeList iframes = getIFrames(photoset_posts);
                        for (Node node : iframes.toNodeArray()) {
                            if (node instanceof TagNode) {
                                String iframe_url = ((TagNode) node).getAttribute("src");
                                Parser parser2 = new Parser(iframe_url);
                                NodeList a_list = parser2.extractAllNodesThatMatch(new TagNameFilter("a"));
                                Node[] a_array = a_list.toNodeArray();
                                Node[] img_array = a_list.extractAllNodesThatMatch(new TagNameFilter("img"), true).toNodeArray();
                                String media_url;
                                for (int i = 0; i < a_array.length; i++) {
                                    media_url = ((TagNode) img_array[i]).getAttribute("src");
                                    String thumb = media_url.replace(media_url.substring(media_url.lastIndexOf("_"), media_url.lastIndexOf(".")), "_75sq");
                                    URL thumb_url = new URL(thumb);
                                    new_post.pictures.add(new Picture(new URL(media_url), thumb_url));
                                }
                            }
                        }
                        Main.handlePost(new_post);
                    } else {
                        new_post = post_post_hash.get(new_post);
                        handleNonDownloadPost(new_post);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Main.status("Error handling post.");
        }
        return true;
    }

    private static NodeList getPosts(String url) throws ParserException {
        return new Parser(url).extractAllNodesThatMatch(new HasAttributeFilter("class", "my_post load"));
    }

    private static NodeList getPhotoPosts(NodeList node_list) {
        return node_list.extractAllNodesThatMatch(new HasAttributeFilter("class", "my_photo_post"), true);
    }

    private static NodeList getPhotosetPosts(NodeList node_list) {
        return node_list.extractAllNodesThatMatch(new HasAttributeFilter("class", "my_photoset_post"), true);
    }

    private static NodeList getRemarks(NodeList node_list) {
        return node_list.extractAllNodesThatMatch(new NodeClassFilter(RemarkNode.class), true);
    }

    private static NodeList getIFrames(NodeList node_list) {
        return node_list.extractAllNodesThatMatch(new TagNameFilter("iframe"), true);
    }

    public static void downloadHiRes() {
        Main.status("Downloading hi res versions of photos in database.");
        if (gui != null) {
            gui.setProgress(0);
            gui.setMaxProgress(pic_pic_hash.keySet().size());
        }
        int progress = 0;
        for (Picture picture : pic_pic_hash.keySet()) {
            if (!picture.downloaded_hi) {
                tryResUrls(picture);
            }
            if (gui != null) {
                gui.setProgress(progress);
                progress++;
            }
        }
        if (gui != null) {
            gui.setProgress(progress);
            progress++;
        }
        Main.status("Done downloading hi res versions.");
    }

    private static void tryResUrls(Picture picture) {
        String hi_res = "";
        String url = picture.media_url.toString();
        for (String ending : Main.endings) {
            try {
                hi_res = url.replace(url.substring(url.lastIndexOf("_"), url.lastIndexOf(".")), ending);
                URL hi_url = new URL(hi_res);
                File hi_name = Helper.extractMediaFileNameFromURL(hi_url);
                if (hi_name.equals(picture.media_name)) {
                    picture.hi_url = hi_url;
                    picture.hi_name = hi_name;
                    picture.downloaded_hi = true;
                    break;
                } else {
                    boolean success = Helper.downloadFileFromURLToFileInTemp(hi_url, hi_name);
                    if (success) {
                        picture.hi_url = hi_url;
                        picture.hi_name = hi_name;
                        picture.downloaded_hi = true;
                        Helper.moveTempImageToStore(hi_name, new File(Main.blogdir, picture.md5_id));
                        break;
                    }
                }
            } catch (MalformedURLException ex) {
                Main.error(String.format("Attempted hi res url %s is a malformed URL.", hi_res));
            }
        }
    }

    private static void handlePost(Post post) {
        Main.post_post_hash.put(post, post);
        for (Picture picture : post.pictures) {
            Helper.downloadFileFromURLToFileInTemp(picture.thumb_url, picture.thumb_name);
            picture.md5_id = Helper.createMD5FromFileInTemp(picture.thumb_name);
            Helper.moveTempImageToStore(picture.thumb_name, new File(Main.blogdir, picture.md5_id));
            if (!Main.pic_pic_hash.containsKey(picture)) {
                Main.pic_pic_hash.put(picture, picture);
                Main.pic_post_hash.put(picture, post);
                Helper.downloadFileFromURLToFileInTemp(picture.media_url, picture.media_name);
                Helper.moveTempImageToStore(picture.media_name, new File(Main.blogdir, picture.md5_id));
            } else {
                if (!post.equals(Main.pic_post_hash.get(picture))) {
                    dup_post_list.put(post, Main.pic_post_hash.get(picture));
                }
            }
        }
    }

    public static void setupPosts() {
        for (Post post : Main.post_post_hash.keySet()) {
            post = Main.post_post_hash.get(post);
            handleNonDownloadPost(post);
        }
    }

    private static void handleNonDownloadPost(Post post) {
        for (Picture picture : post.pictures) {
            if (!Main.pic_pic_hash.containsKey(picture)) {
                Main.pic_pic_hash.put(picture, picture);
                Main.pic_post_hash.put(picture, post);
            } else {
                if (!post.equals(Main.pic_post_hash.get(picture))) {
                    dup_post_list.put(post, Main.pic_post_hash.get(picture));
                }
            }
        }
    }

    private static void status(String status) {
        if (gui == null) {
            System.out.println(status);
        } else {
            gui.setStatus(status);
        }
    }

    private static void error(String error) {
        if (gui == null) {
            System.err.println(error);
        } else {
            gui.setStatus(error);
        }
    }
}
