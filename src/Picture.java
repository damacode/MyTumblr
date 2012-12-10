
import java.io.*;
import java.net.*;
import java.util.Objects;

public class Picture implements Serializable {

    private static final long serialVersionUID = 4;
    protected URL media_url, thumb_url, hi_url;
    protected File media_name, thumb_name, hi_name;
    protected String md5_id;
    protected boolean downloaded_hi;

    public Picture(URL media_url, URL thumb_url) {
        this.media_url = media_url;
        this.thumb_url = thumb_url;
        this.media_name = Helper.extractMediaFileNameFromURL(media_url);
        this.thumb_name = Helper.extractMediaThumbNameFromURL(thumb_url);
        this.downloaded_hi = false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Picture) {
            if (((Picture) obj).md5_id.equals(this.md5_id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.md5_id);
        return hash;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s", media_url, thumb_url, hi_url, media_name, thumb_name, hi_name, md5_id);
    }
}
