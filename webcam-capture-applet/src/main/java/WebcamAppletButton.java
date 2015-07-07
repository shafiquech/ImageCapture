import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.AbstractAction;
import javax.swing.JLabel;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.ImageIcon;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.awt.Image;
import javax.swing.BorderFactory;
import javax.swing.border.EtchedBorder;
import java.awt.Rectangle;
import netscape.javascript.JSObject;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;


public class WebcamAppletButton extends JApplet {

	private class SnapMeAction extends AbstractAction {

		public SnapMeAction() {
			super("Capture");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (webcam.getImage() != null) {
					// save image to PNG file
					//ImageIO.write(webcam.getImage(), "PNG", new File("test.png"));
					uploadImage(webcam.getImage());
				}
				System.out.println("Saved image for " + personName + " "
						+ personId);
				System.out.println("checking displayPanel.getComponents()");
				if (displayPanel.getComponents() != null
						&& displayPanel.getComponents().length > 0) {
					System.out.println("...displayPanel.getComponents() : "
							+ displayPanel.getComponents().length);
					displayPanel.remove(0);
				}
				//HD-12287-April 2014-Spider delegate photos unproportional on certificate print
				Image scaledImage = getScaledImage(webcam.getImage(), 250, 210);
				System.out.println("Displaypanel scaled image width=" + scaledImage.getWidth(null) + " height=" + scaledImage.getHeight(null));
				displayPanel.setIcon(new ImageIcon(scaledImage));
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			
		}
	}

	private static final long serialVersionUID = 3517366452510566924L;

	private Dimension size = WebcamResolution.QVGA.getSize();
	private Dimension captureSize = WebcamResolution.VGA.getSize();
	private Dimension displaySize = WebcamResolution.QQVGA.getSize();
	private Webcam webcam = null;
	private WebcamPanel panel = null;
	private JButton btSnapMe = null;
	private JLabel displayPanel = null;
	private JLabel namePanel = null;
	private String personId;
	private String personName;
	private String userCode;
	private String baseUrl;
	private int image_width = 100;
	private int image_height = 210;
	JSObject win = null;

	public WebcamAppletButton() {
		super();
		System.out.println("Construct");
	}

	@Override
	public void start() {

		System.out.println("Start");

		super.start();		
		webcam = Webcam.getDefault();
		webcam.setViewSize(captureSize);
		System.out.println("captureSize height="+ captureSize.height +" width="+ captureSize.width );

		panel = new WebcamPanel(webcam, displaySize, false);
		panel.setFPSDisplayed(true);
		panel.setFillArea(true);

		btSnapMe = new JButton(new SnapMeAction());
		//btSnapMe.setBounds(0, 280, 30, 10);
		//btSnapMe.setSize(new Dimension(30, 10)); 

		// this.getContentPane().add(comp, index);
		add(panel, BorderLayout.WEST);
		add(btSnapMe, BorderLayout.SOUTH);
		displayPanel = new JLabel();
		displayPanel.add(new JLabel("", JLabel.CENTER));
		displayPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
				"Captured image"));
		displayPanel.setSize(new Dimension(130,100));
		
		 displayPanel.setBounds(new Rectangle(160, 20,
		 130, 100));
		add(displayPanel, BorderLayout.CENTER);
		if (namePanel != null) {

			this.getContentPane().remove(namePanel);
			System.out.println("removing namePanel");
		}
		namePanel = new JLabel();
		System.out.println("Setting name : " + personName);
		namePanel.setText(personName);
		namePanel.setBounds(new Rectangle(0, 0, 300, 10));
		namePanel.setSize(new Dimension(300, 10));
		add(namePanel, BorderLayout.NORTH);
		// layeredPane.add( namePanel, JLayeredPane.DRAG_LAYER);

		displayPanel.setIcon(null);
		displayPanel.repaint();

		if (webcam.isOpen()) {
			webcam.close();
		}

		int i = 0;
		do {
			if (webcam.getLock().isLocked()) {
				System.out.println("Waiting for lock to be released " + i);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					return;
				}
			} else {
				break;
			}
		} while (i++ < 3);

		webcam.open();
		panel.start();
	}

	@Override
	public void destroy() {
		System.out.println("Destroy");
		webcam.close();
		Webcam.shutdown();
		System.out.println("Destroyed");
	}

	@Override
	public void stop() {
		System.out.println("Stop");
		webcam.close();
		System.out.println("Stopped");
	}

	@Override
	public void init() {
		userCode = this.getParameter("userCode");
		baseUrl = this.getParameter("baseUrl");
		personId = this.getParameter("personId");
		personName = this.getParameter("personName");
		System.out.println("usercode=" + userCode + " baseurl=" + baseUrl
				+ " personid=" + personId + " personname=" + personName);
		System.out.println("Init: size set");
	}

	public void uploadImage(Image img) {
		try {			
			URLConnection conn;
			// Url for servlet to save image in db
			String path = baseUrl + "/saveImage?personId=" + personId
					+ "&userCode=" + userCode;
			System.out.println("Saving at servlet path:" + path);
			URL url = new URL(path);
			conn = url.openConnection();
			ImageIcon icon = new ImageIcon(img);

			// Prepare for both input and output
			conn.setDoInput(true);
			conn.setDoOutput(true);

			// Turn off caching
			conn.setUseCaches(false);

			// Set the content type to be java-internal/classname
			conn.setRequestProperty("Content-type",
					"application/x-java-serialized-object");

			// Write the object as post data
			ObjectOutputStream out = new ObjectOutputStream(
					conn.getOutputStream());
			out.writeObject(icon);
			out.flush();
			out.close();
			InputStream ins = conn.getInputStream();
			ObjectInputStream objin = new ObjectInputStream(ins);
			String msg = (String) objin.readObject();
			
			
			// Refresh parent page
						try {
							System.out.println("Setting person image on list");
//							getAppletContext().showDocument(
//									new URL("javascript:refresh(" + personId + ");"));
							win = JSObject.getWindow(this);
							win.call("refresh", new Object[] {personId}); 
						} catch (Exception ex) {
							ex.printStackTrace();
						}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void setPersonId(String personId) {
		this.personId = personId;
	}

	public void setPersonName(String personName) {
		this.personName = personName;
		namePanel.setText(personName);
	}
	
	//HD-12287-April 2014-Spider delegate photos unproportional on certificate print
	 private Image getScaledImage(Image srcImg, int w, int h){
		    BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		    Graphics2D g2 = resizedImg.createGraphics();
		    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		    g2.drawImage(srcImg, 0, 0, w, h, null);
		    g2.dispose();
		    return resizedImg;
		}

}
