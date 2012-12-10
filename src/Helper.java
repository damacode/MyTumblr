
import java.io.*;
import java.math.*;
import java.net.*;
import java.nio.channels.*;
import java.security.*;
import java.util.*;

public class Helper {

    public static final File temp = new File("." + File.separator + "temp" + File.separator);

    public static boolean makeSureDirectoryExists(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
            return true;
        }
        return false;
    }

    public static boolean removeDirectoryIfItExists(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists()) {
            return true;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        String[] list = dir.list();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                File entry = new File(dir, list[i]);
                if (entry.isDirectory()) {
                    if (!removeDirectoryIfItExists(entry)) {
                        return false;
                    }
                } else {
                    if (!entry.delete()) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    public static List<Object> loadObjectFromFile(File file) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            if (file.exists()) {
                fis = new FileInputStream(file);
                ois = new ObjectInputStream(fis);
                return (List<Object>) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException ex) {
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (ois != null) {
                    ois.close();
                }
            } catch (Exception ex) {
            }
        }
        return null;
    }

    public static boolean saveObjectToFile(File file, List<Object> objects) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            makeSureDirectoryExists(new File(file.getParent()));
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(objects);
            oos.flush();
            return true;
        } catch (Exception ex) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (oos != null) {
                    oos.close();
                }
            } catch (Exception ex) {
            }
        }
        return false;
    }

    public static String downloadHTMLURLtoString(URL url) {
        try {
            StringBuilder text = new StringBuilder();
            Scanner scanner;
            scanner = new Scanner(url.openStream(), "utf-8");
            try {
                while (scanner.hasNextLine()) {
                    text.append(scanner.nextLine()).append("\n");
                }
            } finally {
                scanner.close();
            }
            return text.toString();
        } catch (Exception ex) {
        }
        return null;
    }

    public static File extractMediaFileNameFromURL(URL url) {
        return new File(url.toString().substring(url.toString().lastIndexOf("/") + 1));
    }

    public static File extractMediaThumbNameFromURL(URL url) {
        return new File("thumb" + url.toString().substring(url.toString().lastIndexOf(".")));
    }

    public static boolean downloadURLToFileInDir(URL url, File file, File dir) {
        FileOutputStream fos = null;
        try {
            Helper.makeSureDirectoryExists(dir);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            fos = new FileOutputStream(new File(dir, file.getName()));
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
            fos.close();
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception ex) {
            }
        }
        return true;
    }

    public static boolean downloadFileFromURLToFileInTemp(URL url, File file) {
        Helper.makeSureDirectoryExists(temp);
        return Helper.downloadURLToFileInDir(url, file, Helper.temp);
    }

    public static String createMD5FromFile(File file) {
        InputStream is = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(file);
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
        } catch (NoSuchAlgorithmException | IOException ex) {
            return null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    public static String createMD5FromFileInTemp(File file) {
        return Helper.createMD5FromFile(new File(temp, file.getName()));
    }

    public static void moveTempImageToStore(File file, File dir) {
        Helper.makeSureDirectoryExists(temp);
        File temp_file = new File(temp, file.getName());
        Helper.makeSureDirectoryExists(dir);
        File dir_file = new File(dir, file.getName());
        temp_file.renameTo(dir_file);
    }

    public static void copyStoreImageToLocation(String src, File dir) {
        Helper.makeSureDirectoryExists(dir);
        copyFile(new File(src), new File(dir, new File(src).getName()));
    }

    public static boolean copyFile(File src, File dest) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Exception ex) {
            }
        }
        return true;
    }
}
