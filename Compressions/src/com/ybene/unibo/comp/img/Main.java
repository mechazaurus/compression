package com.ybene.unibo.comp.img;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {

	public static void main(String[] args) {
		
		// Files
		File file = new File("./ressources/pears.png");
		File fileRed = new File("./ressources/pearsRed.png");
		File fileGreen = new File("./ressources/pearsGreen.png");
		File fileBlue = new File("./ressources/pearsBlue.png");
		
		// Magic happens
		ImageRGB image = new ImageRGB(file);
		
		// Create buffered images
		BufferedImage bufferedRedImage = new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage bufferedGreenImage = new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage bufferedBlueImage = new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_INT_ARGB);
		
		// Fill buffered images
		for (int widthIndex = 0 ; widthIndex < image.getImageWidth() ; widthIndex++) {
			for (int heightIndex = 0 ; heightIndex < image.getImageHeight() ; heightIndex++) {
				bufferedRedImage.setRGB(widthIndex, heightIndex, image.getImageRed()[heightIndex][widthIndex]);
				bufferedGreenImage.setRGB(widthIndex, heightIndex, image.getImageGreen()[heightIndex][widthIndex]);
				bufferedBlueImage.setRGB(widthIndex, heightIndex, image.getImageBlue()[heightIndex][widthIndex]);
			}
		}
		
		// Save images
		try {
			ImageIO.write(bufferedRedImage, "png", fileRed);
			ImageIO.write(bufferedGreenImage, "png", fileGreen);
			ImageIO.write(bufferedBlueImage, "png", fileBlue);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
