
import java.io.*;

public class Picture implements Serializable {

    public String lo_url, hi_url;
    public String lo_name, hi_name, md5_id;
    public boolean downloaded_orig;
    public long serialVersionUID = 2;

    public Picture(String lo_url, String hi_url) {
        this.lo_url = lo_url;
        this.hi_url = hi_url;
        this.lo_name = Helper.extractImgFilenameFromUrl(lo_url);
        this.hi_name = Helper.extractImgFilenameFromUrl(hi_url);
        this.downloaded_orig = false;
    }

    public void downloadHiTemp() {
        Helper.downloadFileFromUrlToFilenameInTemp(hi_url, hi_name);
    }

    public void downloadLoTemp() {
        Helper.downloadFileFromUrlToFilenameInTemp(lo_url, lo_name);
    }

    public void createMD5FromLoTemp() {
        this.md5_id = Helper.createMD5FromFilenameInTemp(lo_url, lo_name);
    }

    public void copyTempImageToStoreAsThumbnail() {
        Helper.copyTempImageToStoreAsThumbnail(lo_name, md5_id);
    }

    public void moveHiTempImageToStore() {
        Helper.moveTempImageToStore(hi_name, md5_id);
        this.downloaded_orig = true;
    }

    public void moveLoTempImageToStore() {
        Helper.moveTempImageToStore(lo_name, md5_id);
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
        int hash = 5;
        hash = 89 * hash + (this.md5_id != null ? this.md5_id.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s", lo_url, hi_url, lo_name, hi_name, md5_id);
    }
}
