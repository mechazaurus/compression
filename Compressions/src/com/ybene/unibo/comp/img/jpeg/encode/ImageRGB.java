package com.ybene.unibo.comp.img.jpeg.encode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageRGB {

	// Attributes
	private int imageHeight, imageWidth;
	private int[][] imageRGB, imageRed, imageGreen, imageBlue;
	
	// Constructors
	public ImageRGB(File file) {
		
		try {
			// Get image's informations
			BufferedImage in = ImageIO.read(file);
			imageHeight = in.getHeight();
			imageWidth = in.getWidth();
			
			// Create the 2D arrays
			imageRGB = new int[imageHeight][imageWidth];
			imageRed = new int[imageHeight][imageWidth];
			imageGreen = new int[imageHeight][imageWidth];
			imageBlue = new int[imageHeight][imageWidth];		
			
			// Parse the image to fill the 2D array
			for (int widthIndex = 0 ; widthIndex < imageWidth ; widthIndex++) {
				for (int heightIndex = 0 ; heightIndex < imageHeight ; heightIndex++) {
					// Original image
					imageRGB[heightIndex][widthIndex] = in.getRGB(widthIndex, heightIndex);
					
					// Getting colors and alpha values
					int alpha, red, green, blue;
					
					// -- Magic happens --
					alpha = (imageRGB[heightIndex][widthIndex] >> 24) & 0xff;
					red = (imageRGB[heightIndex][widthIndex] >> 16) & 0xff;
					green = (imageRGB[heightIndex][widthIndex] >> 8) & 0xff;
					blue = imageRGB[heightIndex][widthIndex] & 0xff;
					
					// Filling arrays for each color
					imageRed[heightIndex][widthIndex] = (alpha<<24) | (red<<16) | (0<<8) | 0;
					imageGreen[heightIndex][widthIndex] = (alpha<<24) | (0<<16) | (green<<8) | 0;
					imageBlue[heightIndex][widthIndex] = (alpha<<24) | (0<<16) | (0<<8) | blue;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Getters
	public int getImageHeight() {
		return imageHeight;
	}
	public int getImageWidth() {
		return imageWidth;
	}
	public int[][] getImageRGB() {
		return imageRGB;
	}
	public int[][] getImageRed() {
		return imageRed;
	}
	public int[][] getImageGreen() {
		return imageGreen;
	}
	public int[][] getImageBlue() {
		return imageBlue;
	}
	
	// Create red, green and blue images
	public void createImagesRGB(String name, String format) {
		
		// Create files
		File fileRed = new File("./ressources/" + name + "Red" + "." + format);
		File fileGreen = new File("./ressources/" + name + "Green" + "." + format);
		File fileBlue = new File("./ressources/" + name + "Blue" + "." + format);
		
		// Create buffered images
		BufferedImage bufferedRedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImage bufferedGreenImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImage bufferedBlueImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
		// Fill buffered images
		for (int widthIndex = 0 ; widthIndex < imageWidth ; widthIndex++) {
			for (int heightIndex = 0 ; heightIndex < imageHeight ; heightIndex++) {
				bufferedRedImage.setRGB(widthIndex, heightIndex, imageRed[heightIndex][widthIndex]);
				bufferedGreenImage.setRGB(widthIndex, heightIndex, imageGreen[heightIndex][widthIndex]);
				bufferedBlueImage.setRGB(widthIndex, heightIndex, imageBlue[heightIndex][widthIndex]);
			}
		}
		
		// Save images
		try {
			ImageIO.write(bufferedRedImage, format, fileRed);
			ImageIO.write(bufferedGreenImage, format, fileGreen);
			ImageIO.write(bufferedBlueImage, format, fileBlue);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
