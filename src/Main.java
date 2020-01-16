//Imports
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.geom.*;
import java.util.*;

public class Main {

	private static BufferedImage bimage, original;
	private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); //Used to find Monitor Dimensions
	private static int screenHeight = (int) screenSize.getHeight();
	private static int screenWidth = (int) screenSize.getWidth();
	private static JLabel image = new JLabel();
	private static JMenuItem open, save, undo, redo, exit, restore, horizontal, vertical, gray, sepia, invert, blur, bulge, right, left, half;
	private static Deque<BufferedImage> images = new ArrayDeque<BufferedImage>(); //Stack used to monitor previous effects (undo)
	private static Deque<BufferedImage> redoImages = new ArrayDeque<BufferedImage>(); //Stack used to monitor undone effects (redo)
	private static boolean imported;

	//Initializes JMenuItems with all the necessary parameters
	public static void menu(JMenuItem item, KeyStroke key, String action, JMenu menu) {
		item.setAccelerator(key);
		item.addActionListener(new MenuClick());
		item.setActionCommand(action);
		menu.add(item);
	}
	
	//Enables or Disables buttons based on conditions
	public static void enableButtons() {
		save.setEnabled(imported);
		undo.setEnabled(imported && images.size() > 1);
		redo.setEnabled(imported && redoImages.size() > 0);
		restore.setEnabled(imported);
		horizontal.setEnabled(imported);
		vertical.setEnabled(imported);
		gray.setEnabled(imported);
		sepia.setEnabled(imported);
		invert.setEnabled(imported);
		blur.setEnabled(imported);
		bulge.setEnabled(imported);
		right.setEnabled(imported);
		left.setEnabled(imported);
		half.setEnabled(imported);
	}

	//Flips the Image either horizontally or dependently - based on the action command
	public static void flip(String direction) {
		for (int x = 0; direction == "horizontal" ? x < bimage.getWidth() / 2 : x < bimage.getWidth(); x++) { //ternary operators used to determine the counter maximums
			for (int y = 0; direction == "horizontal" ? y < bimage.getHeight() : y < bimage.getHeight() / 2; y++) {
				Color color1 = new Color(bimage.getRGB(x, y));
				int n = direction == "horizontal" ? bimage.getWidth() - x - 1 : x;
				int m = direction == "vertical" ? bimage.getHeight() - y - 1 : y;
				bimage.setRGB(x, y, bimage.getRGB(n, m));
				bimage.setRGB(n, m, color1.getRGB());
			}
		}
	}

	//Gray scale
	public static void gray() {
		for (int x = 0; x < bimage.getWidth(); x++) {
			for (int y = 0; y < bimage.getHeight(); y++) {
				Color imageColor = new Color(bimage.getRGB(x, y));
				int red = (int) imageColor.getRed();
				int green = (int) imageColor.getGreen();
				int blue = (int) imageColor.getBlue();
				red = blue = green = (int) ((red + green + blue) / 3); //Grayscale averages the rgb values
				Color outputColor = new Color(red, green, blue);
				bimage.setRGB(x, y, outputColor.getRGB());
			}
		}
	}

	//Sepia Tone
	public static void sepia() {
		for (int x = 0; x < bimage.getWidth(); x++) {
			for (int y = 0; y < bimage.getHeight(); y++) {
				Color imageColor = new Color(bimage.getRGB(x, y));
				int red = (int) ((imageColor.getRed() * 0.393) + (imageColor.getGreen() * 0.769) + (imageColor.getBlue() * 0.189));
				int green = (int) ((imageColor.getRed() * 0.349) + (imageColor.getGreen() * 0.686) + (imageColor.getBlue() * 0.168));
				int blue = (int) ((imageColor.getRed() * 0.272) + (imageColor.getGreen() * 0.534) + (imageColor.getBlue() * 0.131));
				red = red > 255 ? 255 : red; //RGB scale does not exceed 255
				green = green > 255 ? 255 : green;
				blue = blue > 255 ? 255 : blue;
				Color outputColor = new Color(red, green, blue);
				bimage.setRGB(x, y, outputColor.getRGB());
			}
		}
	}

	//Invert Colors
	public static void invert() {
		for (int x = 0; x < bimage.getWidth(); x++) {
			for (int y = 0; y < bimage.getHeight(); y++) {
				Color imageColor = new Color(bimage.getRGB(x, y));
				Color outputColor = new Color(255 - imageColor.getRed(), 255 - imageColor.getGreen(), 255 - imageColor.getBlue());
				bimage.setRGB(x, y, outputColor.getRGB());
			}
		}
	}

	//Gaussian Blur based on the odd number input r
	public static void blur(int r) { 
		double[][] weightMatrix = new double[r][r]; //Kernel - contains the coefficients for the rgb values
		double totalWeight = 0;
		BufferedImage blurredImage = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < r; i++) {
			for (int j = 0; j < r; j++) { //Calculation using the gaussian distribution
				weightMatrix[i][j] = (1 / (2 * Math.PI * Math.pow(1.5, 2))) * Math.pow(Math.E, -(Math.pow(i - Math.floor(r / 2), 2) + Math.pow(j - Math.floor(r / 2), 2)) / (2 * Math.pow(1.5, 2)));
				totalWeight += weightMatrix[i][j];
			}
		}
		for (int i = 0; i < r; i++) { //Dividing in order to create proportions
			for (int j = 0; j < r; j++) {
				weightMatrix[i][j] /= totalWeight;
			}
		}
		for (int x = 0; x < bimage.getWidth(); x++) { //Loops through each pixel
			for (int y = 0; y < bimage.getHeight(); y++) {
				double red = 0, green = 0, blue = 0;
				try {
					for (int i = 0; i < r; i++) { //Loops through the pixels around each point while applying the weight matrix
						for (int j = 0; j < r; j++) {
							Color pixelColor = new Color(bimage.getRGB(x + i - (int) Math.floor(r / 2), y + j - (int) Math.floor(r / 2)));
							red += pixelColor.getRed() * weightMatrix[i][j];
							green += pixelColor.getGreen() * weightMatrix[i][j];
							blue += pixelColor.getBlue() * weightMatrix[i][j];
						}
					}
					Color blurColor = new Color((int) red, (int) green, (int) blue);
					blurredImage.setRGB(x, y, blurColor.getRGB());
				} catch (ArrayIndexOutOfBoundsException e) {
					continue;
				}
			}
		}
		bimage = blurredImage;
	}

	//Bulges the image from the center based on the selected bulge radius
	public static void bulge(int bulgeRadius) {
		BufferedImage bulgedImage = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB);

		for (int i = 0; i < bimage.getWidth(); i++) {
			for (int j = 0; j < bimage.getHeight(); j++) {
				double radius = Math.sqrt(Math.pow(i - bimage.getWidth() / 2, 2) + Math.pow(j - bimage.getHeight() / 2, 2)); //Calculates the distance from the center of the image to the pixel
				double angle = Math.atan2(j - bimage.getHeight() / 2, i - bimage.getWidth() / 2); //Calculates the standard position angle
				double rn = Math.pow(radius / bulgeRadius, 0.75) * radius; //Performs the transformation to the pixel

				int x = (int) (rn * Math.cos(angle) + bimage.getWidth() / 2); //Calculates the new coordinates
				int y = (int) (rn * Math.sin(angle) + bimage.getHeight() / 2);

				if (x < bimage.getWidth() && y < bimage.getHeight() && x > 0 && y > 0) { //Prevents OutOfBoundsExceptions
					int rgb = bimage.getRGB(x, y);
					bulgedImage.setRGB(i, j, rgb);
				}
			}
		}
		bimage = bulgedImage;
	}

	//Rotates the image 90 degrees to either the right or left
	public static void rotateLR(String action) {
		BufferedImage rotatedImg = new BufferedImage(bimage.getHeight(), bimage.getWidth(), BufferedImage.TYPE_INT_RGB); //Image with inverted dimensions
		for (int x = 0; x < rotatedImg.getWidth(); x++) {
			for (int y = 0; y < rotatedImg.getHeight(); y++) {
				int n = (action.equals("right")) ? y : bimage.getWidth() - y - 1; //Transforms the pixels based on the direction of rotation
				int m = (action.equals("left")) ? x : bimage.getHeight() - x - 1;
				rotatedImg.setRGB(x, y, bimage.getRGB(n, m));
			}
		}
		bimage = rotatedImg;
	}

	//Rotates the image 180 degrees
	public static void rotateH() {
		BufferedImage rotatedImg = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < rotatedImg.getWidth(); x++) {
			for (int y = 0; y < rotatedImg.getHeight(); y++) {
				rotatedImg.setRGB(x, y, bimage.getRGB(bimage.getWidth() - x - 1, bimage.getHeight() - y - 1)); //Pixel transformation
			}
		}
		bimage = rotatedImg;
	}

	//Scales the Image based on the inputed scale factor
	public static void scaleImage(double scaleFactor) {
		BufferedImage dimg = new BufferedImage((int) (scaleFactor * bimage.getWidth()), (int) (scaleFactor * bimage.getHeight()), BufferedImage.TYPE_INT_RGB); //Creates an image with the new dimensions
		Graphics2D g = dimg.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance(scaleFactor, scaleFactor); //Uses the scale factor to create a scale instance
		g.drawRenderedImage(bimage, at); //Draws the image while performing the scale
		g.dispose();
		bimage = dimg;
	}

	public static void main(String[] args) {
		//JComponents
		JFrame frame = new JFrame("Image Editor");
		JPanel panel = new JPanel();
		JMenuBar menubar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenu options = new JMenu("Options");
		JMenu rotate = new JMenu("Rotate");

		//Changes the aesthetics of file chooser
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

		//JMenuItem Initialization
		open = new JMenuItem("Open");
		save = new JMenuItem("Save As");
		undo = new JMenuItem("Undo");
		redo = new JMenuItem("Redo");
		exit = new JMenuItem("Exit");
		restore = new JMenuItem("Restore to Original");
		horizontal = new JMenuItem("Horizontal Flip");
		vertical = new JMenuItem("Vertical Flip");
		gray = new JMenuItem("Gray Scale");
		sepia = new JMenuItem("Sepia Tone");
		invert = new JMenuItem("Invert Colours");
		blur = new JMenuItem("Gaussian Blur");
		bulge = new JMenuItem("Bulge Effect");
		right = new JMenuItem("Right 90");
		left = new JMenuItem("Left 90");
		half = new JMenuItem("180");

		//Establishing the JMenuItem properties
		menu(open, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "open", file);
		menu(save, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "save", file);
		menu(undo, KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo", file);
		menu(redo, KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redo", file);
		file.addSeparator();
		menu(exit, KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK), "exit", file);
		menu(restore, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), "restore", options);
		options.addSeparator();
		menu(horizontal, KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK), "horizontal", options);
		menu(vertical, KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "vertical", options);
		menu(gray, KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "gray", options);
		menu(sepia, KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK), "sepia", options);
		menu(invert, KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "invert", options);
		menu(blur, KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), "blur", options);
		menu(bulge, KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK), "bulge", options);
		menu(right, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), "right", rotate);
		menu(left, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), "left", rotate);
		menu(half, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), "half", rotate);
		options.add(rotate);

		enableButtons();

		menubar.add(file);
		menubar.add(options);

		panel.add(image);

		frame.setLayout(new FlowLayout());
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		frame.setContentPane(panel);
		frame.setJMenuBar(menubar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private static class MenuClick implements ActionListener {

		public void actionPerformed(ActionEvent a) {
			String action = a.getActionCommand();
			if (action.equals("open")) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", ImageIO.getReaderFileSuffixes()); //Limits files to image files
				chooser.setFileFilter(filter);
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					try {
						bimage = ImageIO.read(file); //Reads the image and stores it into the Buffered Image bimage
						if (bimage.getWidth() > screenWidth || bimage.getHeight() > screenHeight) { //Scales the image in case its resolution exceeds the monitor's resolution
							double scaleFactor = ((double) screenWidth / bimage.getWidth() < (double) screenHeight / bimage.getHeight()) ? (double) screenWidth / bimage.getWidth() : (double) screenHeight / bimage.getHeight();
							scaleImage(scaleFactor);
						}
						original = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB); //Copies the scaled image to the buffered image original
						Graphics g = original.createGraphics();
						g.drawImage(bimage, 0, 0, null);
						g.dispose();
						imported = true;
					} catch (IOException e) {
						System.out.println("Error Opening File");
					}
				} else {
					System.out.println("Open command cancelled by user.");
				}
			} else if (action.equals("exit")) { //Exit function
				System.exit(0);
			}
			if (imported) {
				if (action.equals("save")) {
					JFileChooser chooser = new JFileChooser();
					int rval = chooser.showSaveDialog(null);
					if (rval == JFileChooser.APPROVE_OPTION) {
						File saveFile = chooser.getSelectedFile();
						String filePath = saveFile.getAbsolutePath();
						if(!filePath.endsWith(".png")) {
						    saveFile = new File(filePath + ".png");
						}
						try {
							ImageIO.write(bimage, "png", saveFile);
						} catch (IOException ex) {
						}
					}
				} else if (action.equals("undo")) {
					redoImages.addLast(images.getLast()); //The current iteration is added to the redo stack
					images.removeLast(); //The current iteration is removed from the images stack
					BufferedImage img = new BufferedImage(images.getLast().getWidth(), images.getLast().getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics g = img.createGraphics();
					g.drawImage(images.getLast(), 0, 0, null);
					g.dispose();
					bimage = img; //The last iteration is copied to bimage
				} else if (action.equals("redo")) {
					images.addLast(redoImages.getLast()); //The undone iteration is added back to the images stack
					redoImages.removeLast(); //The redone iteration is removed from the redo stack
					BufferedImage img = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics g = img.createGraphics();
					g.drawImage(images.getLast(), 0, 0, null);
					g.dispose();
					bimage = images.getLast(); //The redone iteration is copied to bimage
				} else if (action.equals("restore")) {
					bimage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB); //Copies the image original into bimage
					Graphics g = bimage.createGraphics();
					g.drawImage(original, 0, 0, null);
					g.dispose();
				} else if (action.equals("horizontal") || action.equals("vertical")) {
					flip(action);
				} else if (action.equals("gray")) {
					gray();
				} else if (action.equals("sepia")) {
					sepia();
				} else if (action.equals("invert")) {
					invert();
				} else if (action.equals("blur")) {
					//Creates a separate window to select the blur radius
					JFrame blurFrame = new JFrame("Gaussian Blur");
					JPanel blurPanel = new JPanel();
					JLabel blurLabel = new JLabel("Blur Radius");
					JSlider blurSlider = new JSlider(JSlider.HORIZONTAL, 3, 11, 3);
					JButton blurButton = new JButton("Confirm");
					blurPanel.setLayout(new BoxLayout(blurPanel, BoxLayout.Y_AXIS));
					blurSlider.setMajorTickSpacing(1);
					blurSlider.setPaintTicks(true);
					blurSlider.setPaintLabels(true);
					blurSlider.setSnapToTicks(true);
					blurButton.addActionListener(new ActionListener() { //Dedicated anonymous inner class because there is another button
						public void actionPerformed(ActionEvent e) {
							blur(blurSlider.getValue());
							blurFrame.setVisible(false);
							image.setIcon(new ImageIcon(bimage));
							BufferedImage img = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB);
							Graphics g = img.createGraphics();
							g.drawImage(bimage, 0, 0, null);
							g.dispose();
							images.addLast(img);
							redoImages.clear();
							enableButtons();
						}
					});
					blurPanel.setLayout(new BoxLayout(blurPanel, BoxLayout.Y_AXIS));
					blurPanel.add(blurLabel);
					blurPanel.add(blurSlider);
					blurPanel.add(blurButton);
					blurButton.setAlignmentX(Component.CENTER_ALIGNMENT);
					blurLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
					blurFrame.setVisible(true);
					blurFrame.setContentPane(blurPanel);
					blurFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					blurFrame.pack();
				} else if (action.equals("bulge")) {
					//Creates a separate window to select the bulge radius
					int defaultR = bimage.getHeight() > bimage.getWidth() ? bimage.getHeight() : bimage.getWidth();
					JFrame bulgeFrame = new JFrame("Bulge");
					JPanel bulgePanel = new JPanel();
					JSlider bulgeSlider = new JSlider(JSlider.HORIZONTAL, defaultR / 4, defaultR, defaultR / 2);
					JLabel bulgeLabel = new JLabel("Bulge Radius");
					JButton bulgeButton = new JButton("Confirm");
					bulgePanel.setLayout(new BoxLayout(bulgePanel, BoxLayout.Y_AXIS));
					bulgeSlider.setMajorTickSpacing(defaultR / 4);
					bulgeSlider.setPaintTicks(true);
					bulgeSlider.setPaintLabels(true);
					bulgeSlider.setSnapToTicks(true);
					bulgeButton.addActionListener(new ActionListener() { //Dedicated anonymous inner class because there is another button
						public void actionPerformed(ActionEvent e) {
							bulge(bulgeSlider.getValue());
							bulgeFrame.setVisible(false);
							image.setIcon(new ImageIcon(bimage));
							BufferedImage img = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB);
							Graphics g = img.createGraphics();
							g.drawImage(bimage, 0, 0, null);
							g.dispose();
							images.addLast(img);
							redoImages.clear();
							enableButtons();
						}
					});
					bulgePanel.setLayout(new BoxLayout(bulgePanel, BoxLayout.Y_AXIS));
					bulgePanel.add(bulgeLabel);
					bulgePanel.add(bulgeSlider);
					bulgePanel.add(bulgeButton);
					bulgeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
					bulgeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
					bulgeFrame.setVisible(true);
					bulgeFrame.setContentPane(bulgePanel);
					bulgeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					bulgeFrame.pack();
				} else if (action.equals("left") || action.equals("right")) {
					rotateLR(action);
				} else if (action.equals("half")) {
					rotateH();
				}
				if (!(action.equals("undo") || action.equals("redo") || action.equals("blur") || action.equals("bulge"))) { //For blur and bulge, the anonymous inner classes take care of this
					BufferedImage img = new BufferedImage(bimage.getWidth(), bimage.getHeight(), BufferedImage.TYPE_INT_RGB); //Copied to a local variable so bimage is not influenced
					Graphics g = img.createGraphics();
					g.drawImage(bimage, 0, 0, null);
					g.dispose();
					images.addLast(img);
					redoImages.clear(); //If an effect is applied, there are no effects to redo
				}
			}
			System.out.println(images.size());
			image.setIcon(new ImageIcon(bimage));
			enableButtons();
		}
	}
}
