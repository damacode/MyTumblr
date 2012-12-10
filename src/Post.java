
import java.io.*;
import java.text.*;
import java.util.*;

public class Post implements Comparable<Post>, Serializable {

    public Long post_id;
    public List<Picture> pictures = new ArrayList<>();
    public Date timestamp;
    public long serialVersionUID = 2;
    private static DateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public Post() {
        this.timestamp = Calendar.getInstance().getTime();
    }

    public Post(Long post_id) {
        this();
        this.post_id = post_id;
    }

    public Post(Long post_id, Picture picture) {
        this(post_id);
        this.pictures.add(picture);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Post) {
            if (((Post) obj).post_id.equals(post_id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (int) (this.post_id ^ (this.post_id >>> 32));
        return hash;
    }

    @Override
    public int compareTo(Post post) {
        return this.timestamp.compareTo(post.timestamp);
    }

    @Override
    public String toString() {
        return String.format("(%s,%s)", date_format.format(timestamp), post_id);
    }
}
