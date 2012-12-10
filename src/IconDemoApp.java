import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.awt.image.*;
import java.util.List;
import javax.swing.*;

public class IconDemoApp extends JFrame {
    
    private JLabel photographLabel = new JLabel();
    private JToolBar buttonBar = new JToolBar();    
    private String imagedir = "images/";    
    private MissingIcon placeholderIcon = new MissingIcon();
    
    /**
     * List of all the image files to load.
     */
    private String[] imageFileNames = { "sunw01.jpg", "sunw02.jpg",
    "sunw03.jpg", "sunw04.jpg", "sunw05.jpg"};
    
    /**
     * Main entry point to the demo. Loads the Swing elements on the "Event
     * Dispatch Thread".
     *
     * @param args
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                IconDemoApp app = new IconDemoApp();
                app.setVisible(true);
            }
        });
    }
    
    /**
     * Default constructor for the demo.
     */
    public IconDemoApp() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Icon Demo: Please Select an Image");
        
        // A label for displaying the pictures
        photographLabel.setVerticalTextPosition(JLabel.BOTTOM);
        photographLabel.setHorizontalTextPosition(JLabel.CENTER);
        photographLabel.setHorizontalAlignment(JLabel.CENTER);
        photographLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // We add two glue components. Later in process() we will add thumbnail buttons
        // to the toolbar inbetween thease glue compoents. This will center the
        // buttons in the toolbar.
        buttonBar.add(Box.createGlue());
        buttonBar.add(Box.createGlue());
        
        add(buttonBar, BorderLayout.SOUTH);
        add(photographLabel, BorderLayout.CENTER);
        
        setSize(400, 300);
        
        // this centers the frame on the screen
        setLocationRelativeTo(null);
        
        // start the image loading SwingWorker in a background thread
        loadimages.execute();
    }
    

    private SwingWorker<Void, ThumbnailAction> loadimages = new SwingWorker<Void, ThumbnailAction>() {
        @Override
        protected Void doInBackground() throws Exception {
            for (int i = 0; i < imageFileNames.length; i++) {
                ImageIcon icon;
                icon = new ImageIcon(imagedir + imageFileNames[i], imageFileNames[i]);                
                ThumbnailAction thumbAction;
                if(icon != null){                    
                    ImageIcon thumbnailIcon = new ImageIcon(icon.getImage());                    
                    thumbAction = new ThumbnailAction(icon, thumbnailIcon, imageFileNames[i]);                    
                }else{
                    thumbAction = new ThumbnailAction(placeholderIcon, placeholderIcon, imageFileNames[i]);
                }
                publish(thumbAction);
            }
            return null;
        }
        @Override
        protected void process(List<ThumbnailAction> chunks) {
            for (ThumbnailAction thumbAction : chunks) {
                JButton thumbButton = new JButton(thumbAction);
                buttonBar.add(thumbButton, buttonBar.getComponentCount() - 1);
            }
        }
    };

    private class ThumbnailAction extends AbstractAction{
        private Icon displayPhoto;
        public ThumbnailAction(Icon photo, Icon thumb, String desc){
            displayPhoto = photo;
            putValue(SHORT_DESCRIPTION, desc);
            putValue(LARGE_ICON_KEY, thumb);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            photographLabel.setIcon(displayPhoto);
            setTitle("Icon Demo: " + getValue(SHORT_DESCRIPTION).toString());
        }
    }
}