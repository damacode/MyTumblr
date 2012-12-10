
import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.*;
import org.htmlparser.util.*;

public class Main {

    private static Pattern lores = Pattern.compile("post_photolores=\"https?://([-\\w\\.]+)+(:\\d+)?(/([\\w/_\\.]*(\\?\\S+)?)?)?\"");
    private static Pattern highres = Pattern.compile("post_photohighres=\"https?://([-\\w\\.]+)+(:\\d+)?(/([\\w/_\\.]*(\\?\\S+)?)?)?\"");
    private static HashMap<Post, Post> post_post_hash = new HashMap<Post, Post>();
    protected static HashMap<Picture, Picture> pic_pic_hash = new HashMap<Picture, Picture>();
    private static HashMap<Picture, Post> pic_post_hash = new HashMap<Picture, Post>();
    private static HashMap<Post, Post> dup_post_list = new HashMap<Post, Post>();
    protected static String blogname = "data";

    public static void main(String[] args) {
        if (args.length == 0) {
            Gui.main(args);
        } else if (args.length == 3 || args.length == 4) {
            Main.blogname = args[0];
            Main.load();
            if (args.length == 3) {
                int start = Integer.parseInt(args[1]);
                int limit = Integer.parseInt(args[2]);
                Main.run(start, limit);
                Main.save();
            } else if (args.length == 4 && args[3].equals("-hires")) {
                Main.downloadHiRes();
                Main.save();
            } else {
                System.out.println("usage: Main <blogname> <min_page> <max_page> -hires");
            }
        } else {
            System.out.println("usage: Main <blogname> <min_page> <max_page> -hires");
        }
    }

    protected static void reset() {
        Helper.removeDirectory(new File("temp"));
        Helper.removeDirectory(new File(blogname));
        post_post_hash.clear();
        pic_pic_hash.clear();
        pic_post_hash.clear();
        dup_post_list.clear();
        
    }

    protected static void run(int start, int limit) {
        if (start < 1 || limit < 1 || limit < start) {
            System.out.println("usage: Main <blogname> <min_page> <max_page> -hires");
            System.out.println("usage: min_page > 1 && max_page > 1 && min_page <= max_page");
            return;
        }
        for (int i = start; i <= limit; i++) {
            String url = "http://" + blogname + ".tumblr.com/page/" + i;
            boolean exists = Main.handleURL(url);
            if (!exists) {
                System.out.println("We ran out of posts to process at page " + i);
                break;
            }
        }
        Main.writeDuplicates();
    }

    private static void writeDuplicates() {
        System.out.println("Writing duplicates.");
        FileWriter fw = null;
        try {
            fw = new FileWriter(blogname + File.separator + "dup.txt");
            fw.write(String.format("%s\t%s\n", "older_post", "newer_post"));
            for (Post post : dup_post_list.keySet()) {
                fw.write(String.format("%s\t%s\n", post.post_id, dup_post_list.get(post).post_id));
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (Exception ex) {
            }
        }
        System.out.println("Writing duplicates done.");
    }

    protected static void downloadHiRes() {
        System.out.println("Downloading hi res versions of photos in database.");
        for (Picture picture : pic_pic_hash.keySet()) {
            if (!picture.downloaded_orig) {
                picture.downloadHiTemp();
                picture.moveHiTempImageToStore();
            }
        }
        System.out.println("Downloading hi res versions done.");
    }

    protected static void load() {
        System.out.println("Loading databases.");
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        File file;
        try {
            Helper.makeSureDirExists(blogname);
            file = new File(blogname + File.separator + "postpost.db");
            if (file.exists()) {
                fis = new FileInputStream(file);
                ois = new ObjectInputStream(fis);
                post_post_hash = (HashMap<Post, Post>) ois.readObject();
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
                ois.close();
            } catch (Exception ex) {
            }
        }
        System.out.println("Done loading databases.");
    }

    protected static void save() {
        System.out.println("Saving databases.");
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        File file;
        try {
            Helper.makeSureDirExists(blogname);
            file = new File(blogname + File.separator + "postpost.db");
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(Main.post_post_hash);
            oos.flush();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
                oos.close();
            } catch (Exception ex) {
            }
        }
        System.out.println("Done saving databases.");
    }

    private static boolean handleURL(String url) {
        System.out.println("Processing page " + url);
        try {
            NodeList posts = getPosts(url);
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
                            Matcher matcher1 = lores.matcher(node.getText());
                            String lo = "", hi = "";
                            if (matcher1.find()) {
                                lo = matcher1.group();
                                lo = lo.substring(17, lo.length() - 1);
                            }
                            Matcher matcher2 = highres.matcher(node.getText());
                            if (matcher2.find()) {
                                hi = matcher2.group();
                                hi = hi.substring(19, hi.length() - 1);
                            }
                            if (lo.length() > 0 && hi.length() > 0) {
                                Picture pic = new Picture(lo, hi);
                                new_post.picture = pic;
                            }
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
                                String lo = "", hi = "";
                                for (int i = 0; i < a_array.length; i++) {
                                    lo = ((TagNode) img_array[i]).getAttribute("src");
                                    hi = ((TagNode) a_array[i]).getAttribute("href");
                                }
                                if (lo.length() > 0 && hi.length() > 0) {
                                    Picture pic = new Picture(lo, hi);
                                    new_post.picture = pic;
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
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private static void handlePost(Post post) {
        Main.post_post_hash.put(post, post);
        post.picture.downloadLoTemp();
        post.picture.createMD5FromLoTemp();
        if (!Main.pic_pic_hash.containsKey(post.picture)) {
            Main.pic_pic_hash.put(post.picture, post.picture);
            Main.pic_post_hash.put(post.picture, post);
            post.picture.copyTempImageToStoreAsThumbnail();
            post.picture.moveLoTempImageToStore();
        } else {
            post.picture = Main.pic_pic_hash.get(post.picture);
            dup_post_list.put(post, Main.pic_post_hash.get(post.picture));
        }
    }

    private static void handleNonDownloadPost(Post post) {
        if (!Main.pic_pic_hash.containsKey(post.picture)) {
            Main.pic_pic_hash.put(post.picture, post.picture);
            Main.pic_post_hash.put(post.picture, post);
        } else {
            post.picture = Main.pic_pic_hash.get(post.picture);
            dup_post_list.put(post, Main.pic_post_hash.get(post.picture));
        }
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
}
