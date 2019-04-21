package com.ybene.unibo.comp.img;

import java.io.File;

public class Main {

	public static void main(String[] args) {
		
		// Files
		File file = new File("./ressources/pears.png");
		
		// Magic happens
		ImageRGB image = new ImageRGB(file);
		
		image.createImagesRGB("pears", "png");
	}
}
