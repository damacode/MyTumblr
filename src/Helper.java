
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.nio.channels.*;
import java.security.*;
import java.util.logging.*;
import javax.imageio.*;

public class Helper {

    public static String extractImgFilenameFromUrl(String url) {
        int last = url.lastIndexOf("/");
        return url.substring(last + 1);
    }

    public static void makeSureDirExists(String directory) {
        (new File(directory)).mkdirs();
    }

    public static void downloadFileFromUrlToFilenameInDir(String address, String filename, String dir) {
        FileOutputStream file_output_stream = null;
        try {
            URL url = new URL(address);
            ReadableByteChannel readable_byte_channel = Channels.newChannel(url.openStream());
            Helper.makeSureDirExists(dir);
            file_output_stream = new FileOutputStream(new File(dir + File.separator + filename));
            file_output_stream.getChannel().transferFrom(readable_byte_channel, 0, 1 << 24);
            file_output_stream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                file_output_stream.close();
            } catch (IOException ex) {
                Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void downloadFileFromUrlToFilenameInTemp(String address, String filename) {
        Helper.downloadFileFromUrlToFilenameInDir(address, filename, "temp");
    }

    public static String createMD5FromFile(String filename) {
        InputStream is = null;
        try {
            File updateFile = new File(filename);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(updateFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public static String createMD5FromFilenameInTemp(String address, String filename) {
        Helper.makeSureDirExists("temp");
        return Helper.createMD5FromFile("temp" + File.separator + filename);
    }

    public static void moveTempImageToStore(String filename, String dir) {
        Helper.makeSureDirExists("temp");
        File file1 = new File("temp" + File.separator + filename);
        Helper.makeSureDirExists(Main.blogname + File.separator + dir);
        File file2 = new File(Main.blogname + File.separator + dir + File.separator + filename);
        file1.renameTo(file2);
    }

    public static void copyTempImageToStoreAsThumbnail(String filenname, String dir) {
        Helper.makeSureDirExists("temp");
        Helper.makeSureDirExists(Main.blogname + File.separator + dir);
        Helper.createThumbnail("temp" + File.separator + filenname, 64, 64, 100, Main.blogname + File.separator + dir + File.separator + "thumbnail.jpg");
    }

    @SuppressWarnings("empty-statement")
    public static void copyFile(File sourceFile, File destFile) {
        try {
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            FileChannel source;
            FileChannel destination;
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            long count = 0;
            long size = source.size();
            while ((count += destination.transferFrom(source, count, size - count)) < size);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void createThumbnail(String filename, int thumbWidth, int thumbHeight, int quality, String outFilename) {
        try {
            BufferedImage bi = ImageIO.read(new File(filename));
            int w = bi.getWidth(null);
            int h = bi.getHeight(null);
            if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage bi2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics big = bi2.getGraphics();
                big.drawImage(bi, 0, 0, null);
                bi = bi2;
            }
            bi = Helper.getScaledInstance(bi, thumbWidth, thumbHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
            ImageIO.write(bi, "jpeg", new File(outFilename));
        } catch (IOException ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            w = img.getWidth();
            h = img.getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }
        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }
            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }
            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);
        return ret;
    }

    public static boolean removeDirectory(File directory) {
        if (directory == null) {
            return false;
        }
        if (!directory.exists()) {
            return true;
        }
        if (!directory.isDirectory()) {
            return false;
        }
        String[] list = directory.list();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                File entry = new File(directory, list[i]);
                if (entry.isDirectory()) {
                    if (!removeDirectory(entry)) {
                        return false;
                    }
                } else {
                    if (!entry.delete()) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }
}
