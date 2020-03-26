package com.molvix.android.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ConvolutionMatrix {

	public static final int SIZE = 3;
	public double[][] matrix;
	public double factor = 1;
	public double offset = 1;

	ConvolutionMatrix(int size) {
		matrix = new double[size][size];
	}

	public void setAll(double value) {
		for (int x = 0; x < SIZE; ++x) {
			for (int y = 0; y < SIZE; ++y) {
				matrix[x][y] = value;
			}
		}
	}

	void applyConfig(double[][] config) {
		for(int x = 0; x < SIZE; ++x) {
			for(int y = 0; y < SIZE; ++y) {
				matrix[x][y] = config[x][y];
			}
		}
	}

	static Bitmap computeConvolution3x3(Bitmap src, ConvolutionMatrix matrix) {
		int width = src.getWidth();
		int height = src.getHeight();
		Bitmap result = Bitmap.createBitmap(width, height, src.getConfig());

		int A, R, G, B;
		int sumR, sumG, sumB;
		int[][] pixels = new int[SIZE][SIZE];

		for(int y = 0; y < height - 2; ++y) {
			for(int x = 0; x < width - 2; ++x) {
				// get pixel matrix
				for(int i = 0; i < SIZE; ++i) {
					for(int j = 0; j < SIZE; ++j) {
						pixels[i][j] = src.getPixel(x + i, y + j);
					}
				}
				// get alpha of center pixel
				A = Color.alpha(pixels[1][1]);
				// init color sum
				sumR = sumG = sumB = 0;
				// get sum of RGB on matrix
				for(int i = 0; i < SIZE; ++i) {
					for(int j = 0; j < SIZE; ++j) {
						sumR += (Color.red(pixels[i][j]) * matrix.matrix[i][j]);
						sumG += (Color.green(pixels[i][j]) * matrix.matrix[i][j]);
						sumB += (Color.blue(pixels[i][j]) * matrix.matrix[i][j]);
					}
				}
				// get final Red
				R = (int)(sumR / matrix.factor + matrix.offset);
				if(R < 0) { R = 0; }
				else if(R > 255) { R = 255; }

				// get final Green
				G = (int)(sumG / matrix.factor + matrix.offset);
				if(G < 0) { G = 0; }
				else if(G > 255) { G = 255; }

				// get final Blue
				B = (int)(sumB / matrix.factor + matrix.offset);
				if(B < 0) { B = 0; }
				else if(B > 255) { B = 255; }

				// apply new pixel
				result.setPixel(x + 1, y + 1, Color.argb(A, R, G, B));
			}
		}
		src.recycle();
		src = null;
		// final image
		return result;
	}

}